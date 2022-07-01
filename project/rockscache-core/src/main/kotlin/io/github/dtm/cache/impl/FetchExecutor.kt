package io.github.dtm.cache.impl

import io.github.dtm.cache.Consistency
import io.github.dtm.cache.DirtyCacheException
import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.ValueSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

/**
 * @author 陈涛
 */
internal class FetchExecutor<K, V>(
    private val client: CacheClientImpl,
    private val keyPrefix: String,
    private val options: Options,
    private val keySerializer: KeySerializer<K>,
    private val valueSerializer: ValueSerializer<V>,
    private val expire: Duration,
    private val loader: (Collection<K>) -> Map<K, V>,
    keys: Collection<K>
) {
    private val owner = UUID.randomUUID().toString()

    private val keyMap: Map<String, K> =
        keys.associateBy({"$keyPrefix${keySerializer.serialize(it)}"}) { it }

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
            owner
        )

        val list = client.provider.eval(
            LUA_GET,
            redisKeyList,
            args
        ) as List<Any?>
        val map = mutableMapOf<String, Pair<Any?, Any?>>()
        for (i in redisKeyList.indices) {
            val key = redisKeyList[i]
            val value = list[2 * i]
            val status = list[2 * i + 1] as String?
            map[key] = value to status
        }
        return map
    }

    private fun luaSet(map: Map<String, V?>) {
        val args = mutableListOf(owner)
        for (value in map.values) {
            args += if (value === null) {
                options.emptyExpire
            } else {
                expire
            }.toSeconds().toString()
            args += if (value === null) {
                ""
            } else {
                valueSerializer.serialize(value)
            }
        }
        client.provider.eval(LUA_SET, map.keys.toList(), args)
    }

    @Suppress("UNCHECKED_CAST")
    private fun weakFetch(): Map<K, V> {

        val map = luaGet(keyMap.keys)

        while (true) {
            val lockedByOtherKeys = map.filterValues { it.first === null && it.second != null && it.second != LOCKED }.keys
            if (lockedByOtherKeys.isEmpty()) {
                break
            }
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug(
                    "empty result for $lockedByOtherKeys locked by other, so sleep ${options.lockSleep.toMillis()}ms"
                )
            }
            Thread.sleep(options.lockSleep.toMillis())
            map += luaGet(lockedByOtherKeys)
        }

        val syncKeys = map.filterValues { it.first === null && it.second == LOCKED }.keys
        val asyncKeys = map.filterValues { it.first !== null && it.second == LOCKED }.keys
        if (asyncKeys.isNotEmpty()) {
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug("Async fetch keys: $asyncKeys")
            }
            client.asyncFetchService.add {
                fetchNew(asyncKeys)
            }
        }
        if (options.consistency == Consistency.TRY_STRONG) {
            val dirtyKeys = map.filterValues { it.first !== null && it.second !== null }.keys
            if (dirtyKeys.isNotEmpty()) {
                client.asyncFetchService.add {
                    fetchNew(syncKeys)
                }
                throw DirtyCacheException(
                    "There are some dirty data in cache: $dirtyKeys",
                    dirtyKeys
                )
            }
        }
        return fetchMore(map, syncKeys)
    }

    private fun strongFetch(): Map<K, V> {

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("strongFetch: keys: ${keyMap.keys}")
        }

        val map = luaGet(keyMap.keys)

        while (true) {
            val lockedByOtherKeys = map.filterValues { it.first !== null && it.second != null && it.second != LOCKED }.keys
            if (lockedByOtherKeys.isEmpty()) {
                break
            }
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug(
                    "$lockedByOtherKeys are locked by other, so sleep ${options.lockSleep.toMillis()}ms"
                )
            }
            Thread.sleep(options.lockSleep.toMillis())
            map += luaGet(lockedByOtherKeys)
        }

        val lockedKeys = map.filterValues { it.second == LOCKED }.keys
        return fetchMore(map, lockedKeys)
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchMore(
        alreadyMap: Map<String, Pair<Any?, Any?>>,
        missedKeys: Collection<String>
    ): Map<K, V> {
        val resultMap = mutableMapOf<K, V>()
        for ((redisKey, tuple) in alreadyMap) {
            val key = keyMap[redisKey]!!
            val value = tuple.first
            if (value != null && value != "") {
                resultMap[key] = valueSerializer.deserialize(value as String)
            }
        }
        if (missedKeys.isEmpty()) {
            return resultMap
        }
        val loadedMap = fetchNew(missedKeys)
        for ((key, value) in loadedMap) {
            if (value != null) {
                resultMap[key] = value
            }
        }
        return resultMap
    }

    private fun fetchNew(missedRedisKeys: Collection<String>): Map<K, V?> {
        if (missedRedisKeys.isEmpty()) {
            return emptyMap()
        }
        val missedKeys = missedRedisKeys.map { keyMap[it]!! }
        if (LOGGER.isInfoEnabled) {
            LOGGER.info("Loading from database: $missedRedisKeys")
        }
        val loadedMap = try {
            loader(missedKeys)
        } catch (ex: Throwable) {
            LOGGER.error("Failed to load $missedKeys from database")
            throw ex
        }
        if (options.emptyExpire == Duration.ZERO) {
            val nullKeys = missedRedisKeys - loadedMap.keys.map { redisKeyMap[it]!! }.toSet()
            if (nullKeys.isNotEmpty()) {
                client.provider.delete(nullKeys)
            }
        }
        val writeMap = mutableMapOf<String, V?>()
        for (missedRedisKey in missedRedisKeys) {
            val key = keyMap[missedRedisKey]
            val value = loadedMap[key]
            if (value != null || options.emptyExpire != Duration.ZERO) {
                writeMap[missedRedisKey] = value
            }
        }
        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Loaded ${writeMap.keys} from database and save them into redis")
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
            |local list = {}
            |for index, key in ipairs(KEYS) do
            |    local v = redis.call('HGET', key, 'value')
            |    local lu = redis.call('HGET', key, 'lockUntil')
            |    list[index * 2 - 1] = v
            |    if lu ~= false and tonumber(lu) < tonumber(ARGV[1]) or lu == false and v == false then
            |        redis.call('HSET', key, 'lockUntil', ARGV[2])
            |        redis.call('HSET', key, 'lockOwner', ARGV[3])
            |        list[2 * index] = 'LOCKED'
            |    else
            |        list[2 * index] = lu
            |    end
            |end
            |return list
        """.trimMargin()

        // Args: LockOwner, (Expire, Value)*
        @JvmStatic
        private val LUA_SET = """-- luaSet
            |for index, key in ipairs(KEYS) do
            |    local o = redis.call('HGET', key, 'lockOwner')
            |    if o == ARGV[1] then
            |        local expire = ARGV[index * 2]
            |        local value = ARGV[index * 2 + 1]
            |        redis.call('EXPIRE', key, expire)
            |        redis.call('HDEL', key, 'lockOwner')
            |        redis.call('HDEL', key, 'lockUntil')
            |        redis.call('HSET', key, 'value', value)
            |    end
            |end
        """.trimMargin()
    }
}