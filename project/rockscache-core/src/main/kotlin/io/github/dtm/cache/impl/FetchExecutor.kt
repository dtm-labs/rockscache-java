package io.github.dtm.cache.impl

import io.github.dtm.cache.Consistency
import io.github.dtm.cache.LoadingException
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
    private val loader: (Collection<K>) -> Map<K, V?>,
    keys: Collection<K>
) {
    private val uuid = UUID.randomUUID()

    private val keyMap: Map<String, K> =
        keys.associateBy({"$keyPrefix$it"}) { it }

    private val redisKeyMap: Map<K, String> =
        keyMap.entries.associateBy({it.value}) {
            it.key
        }

    fun execute(): Map<K, V?> =
        if (options.consistency == Consistency.STRONG) {
            TODO()
        } else {
            weakFetch()
        }

    @Suppress("UNCHECKED_CAST")
    private fun luaGet(redisKeys: Collection<String>): MutableMap<String, Pair<Any?, Any>> {

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
                (row as List<Any>).let {
                    it[0] to it[1]
                }
            }
        )
    }

    private fun luaSet(map: Map<String, V?>) {
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
    private fun weakFetch(): Map<K, V?> {
        val map = luaGet(keyMap.keys.toList())
        while (true) {
            val missedKeys = map.filterValues { it.first === null && it.second != "LOCKED" }.keys
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

        if (options.consistency == Consistency.ALLOW_LOADING_EXCEPTION) {
            val loadingKeys = map.filterValues { it.first != null && it.second == "LOCKED" }
            if (loadingKeys.isNotEmpty()) {
                LOGGER.warn("Keys: $loadingKeys is loading, throws LoadingException")
                throw LoadingException("Some data is loading")
            }
        }
        val resultMap = map.entries.associateBy({
            keyMap[it.key] as K
        }) {
            it.value.first as V?
        }
        val missedKeys = map.filterValues { it.first == null && it.second == "LOCKED" }.keys
        if (missedKeys.isEmpty()) {
            return resultMap
        }
        return resultMap + fetchNew(missedKeys)
    }

    private fun fetchNew(missedRedisKeys: Collection<String>): Map<K, V?> {
        val missedKeys = missedRedisKeys.map { keyMap[it]!! }
        if (LOGGER.isInfoEnabled) {
            LOGGER.info("Load from database: $missedRedisKeys")
        }
        val loadedMap = loader(missedKeys)
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

        @JvmStatic
        private val LUA_GET = """-- luaGet
            |local v = redis.call('HGET', KEYS[1], 'value')
            |local lu = redis.call('HGET', KEYS[1], 'lockUntil')
            |if lu ~= false and tonumber(lu) < tonumber(ARGV[1]) or lu == false and v == false then
            |    redis.call('HSET', KEYS[1], 'lockUntil', tonumber(ARGV[2]))
            |    redis.call('HSET', KEYS[1], 'lockOwner', ARGV[3])
            |    return { v, 'LOCKED' }
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