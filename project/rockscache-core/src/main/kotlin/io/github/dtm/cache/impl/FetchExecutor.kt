package io.github.dtm.cache.impl

import io.github.dtm.cache.Consistency
import io.github.dtm.cache.DirtyCacheException
import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.RedisProvider
import io.github.dtm.cache.spi.ValueSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

internal class FetchExecutor<K, V>(
    private val keyPrefix: String,
    private val options: Options,
    private val provider: RedisProvider,
    private val keySerializer: KeySerializer<K>,
    private val valueSerializer: ValueSerializer<V>,
    private val expire: Duration,
    private val loader: (Collection<K>) -> Map<K, V>,
    keys: Collection<K>
) {
    private val uuid = UUID.randomUUID()

    private val keyMap: Map<String, K> =
        keys.associateBy({"$keyPrefix$it"}) { it }

    private val redisKeyMap: Map<K, String> =
        keyMap.entries.associateBy({it.value}) {
            it.key
        }

    fun execute(): Map<K, V> =
        if (options.consistency == Consistency.STRONG) {
            strongFetch()
        } else {
            weakFetch()
        }

    @Suppress("UNCHECKED_CAST")
    private fun luaGet(redisKeys: Collection<String>): MutableMap<String, Pair<Any?, Any?>> {

        val redisKeyList = redisKeys as? List<String> ?: redisKeys.toList()
        val now = System.currentTimeMillis()
        val args = listOf(
            now.toString(),
            (now + options.lockExpire.toMillis()).toString(),
            uuid.toString()
        )

        val rows = provider.eval {
            for (redisKey in redisKeyList) {
                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug("luaGet: redisKey: $redisKey, args: $args")
                }
                append(LUA_GET, listOf(redisKey), args)
            }
        }

        return redisKeyList.indices.associateByTo(
            mutableMapOf(),
            {redisKeyList[it]},
            { index ->
                val row = rows[index]
                if (row is Throwable) {
                    LOGGER.error(
                        "Failed to read redis: redisKey:${redisKeyList[index]}"
                    )
                    throw row
                }
                (row as List<Any?>).let {
                    it[0] to it[1]
                }
            }
        )
    }

    private fun luaSet(map: Map<String, V>) {
        provider.eval {
            for ((key, value) in map) {
                append(
                    LUA_SET,
                    listOf(key),
                    listOf(
                        value.toString(),
                        uuid.toString(),
                        if (value == null) {
                            options.emptyExpire
                        } else {
                            expire
                        }.toSeconds().toString()
                    )
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun weakFetch(): Map<K, V> {

        val map = luaGet(keyMap.keys)

        while (true) {
            val missedKeys = map.filterValues { it.first === null && it.second != null && it.second != LOCKED }.keys
            if (missedKeys.isEmpty()) {
                break
            }
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug(
                    "empty result for $missedKeys locked by other, so sleep ${options.lockSleep.toMillis()}ms"
                )
            }
            Thread.sleep(options.lockSleep.toMillis())
            map += luaGet(missedKeys)
        }

        val asyncLoadingKeys = map.filterValues { it.first !== null && it.second == LOCKED }.keys
        if (asyncLoadingKeys.isNotEmpty()) {
            AsyncFetchService.add {
                fetchNew(asyncLoadingKeys)
            }
            if (options.consistency == Consistency.ALLOW_DIRTY_CACHE_EXCEPTION) {
                throw DirtyCacheException(
                    "There are some dirty data in cache: $asyncLoadingKeys",
                    asyncLoadingKeys
                )
            }
        }
        val syncLoadingKeys = map.filterValues { it.first === null && it.second == LOCKED }.keys
        return fetchMore(map, syncLoadingKeys)
    }

    private fun strongFetch(): Map<K, V> {

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("strongFetch: keys: ${keyMap.keys}")
        }

        val map = luaGet(keyMap.keys)
        println("initialized map: $map")

        while (true) {
            val missedKeys = map.filterValues { it.first !== null && it.second != null && it.second != LOCKED }.keys
            if (missedKeys.isEmpty()) {
                break
            }
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug(
                    "$missedKeys are locked by other, so sleep ${options.lockSleep.toMillis()}ms"
                )
            }
            Thread.sleep(options.lockSleep.toMillis())
            map += luaGet(missedKeys)
            println("map after sleep: $map")
        }

        val lockedKeys = map.filterValues { it.second == LOCKED }.keys
        println("lockedKeys: $lockedKeys")
        return fetchMore(map, lockedKeys)
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchMore(
        alreadyMap: Map<String, Pair<Any?, Any?>>,
        missedKeys: Collection<String>
    ): Map<K, V> {
        val resultMap = alreadyMap.entries.associateBy({
            keyMap[it.key] as K
        }) {
            it.value.first as V
        }
        if (missedKeys.isEmpty()) {
            return resultMap
        }
        return resultMap + fetchNew(missedKeys)
    }

    private fun fetchNew(missedRedisKeys: Collection<String>): Map<K, V> {
        val missedKeys = missedRedisKeys.map { keyMap[it]!! }
        if (LOGGER.isInfoEnabled) {
            LOGGER.info("Loading from database: $missedRedisKeys")
        }
        val loadedMap = loader(missedKeys)
        if (LOGGER.isInfoEnabled) {
            LOGGER.info("Loaded from database: $missedRedisKeys")
        }
        if (options.emptyExpire == Duration.ZERO) {
            val nullKeys = missedRedisKeys - loadedMap.keys.map { redisKeyMap[it]!! }.toSet()
            if (nullKeys.isNotEmpty()) {
                provider.delete(nullKeys)
            }
        }
        val writeMap = missedRedisKeys.associateBy({it}) {
            val key = keyMap[it]!!
            loadedMap[key]!!
        }
        luaSet(writeMap)
        return loadedMap
    }

    companion object {

        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(FetchExecutor::class.java)

        private const val LOCKED = "LOCKED"

        @JvmStatic
        private val LUA_GET = """-- luaGet
            |local v = redis.call('HGET', KEYS[1], 'value')
	        |local lu = redis.call('HGET', KEYS[1], 'lockUntil')
	        |if lu ~= false and tonumber(lu) < tonumber(ARGV[1]) or lu == false and v == false then
		    |   redis.call('HSET', KEYS[1], 'lockUntil', ARGV[2])
		    |   redis.call('HSET', KEYS[1], 'lockOwner', ARGV[3])
		    |   return { v, '$LOCKED' }
	        |end
	        |return {v, lu}
        """.trimMargin()

        @JvmStatic
        private val LUA_SET = """-- luaSet
            |local o = redis.call('HGET', KEYS[1], 'lockOwner')
            |if o ~= ARGV[2] then
            |    return
            |end
            |redis.call('HSET', KEYS[1], 'value', ARGV[1])
            |redis.call('HDEL', KEYS[1], 'lockUntil')
            |redis.call('HDEL', KEYS[1], 'lockOwner')
            |redis.call('EXPIRE', KEYS[1], ARGV[3])
        """.trimMargin()
    }
}