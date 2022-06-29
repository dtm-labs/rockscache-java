package io.github.dtm.cache.impl

import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.Provider
import io.github.dtm.cache.spi.Serializer
import java.util.*

internal class FetchExecutor<K, V>(
    private val keyPrefix: String,
    private val options: Options,
    private val provider: Provider,
    private val keySerializer: Serializer<K>,
    private val valueSerializer: Serializer<V>,
    private val keys: Collection<K>,
    private val loader: (Collection<K>) -> Map<K, V>
) {
    private val uuid = UUID.randomUUID()

    private val keyMap: Map<K, String> =
        keys.associateBy({it}) { "$keyPrefix$it" }

    fun execute(): Map<K, V> {
        val now = System.currentTimeMillis()
        val args = listOf(
            now.toString(),
            (now + options.lockExpire.toMillis()).toString(),
            uuid.toString()
        )
        provider.executeBatchScript {
            for (redisKey in keyMap.values) {
                append(LUA_GET, listOf(redisKey), args)
            }
        }
        TODO()
    }

    companion object {

        @JvmStatic
        private val LUA_GET = """-- luaGet
            |local v = redis.call('HGET', KEYS[1], 'value')
            |local lu = redis.call('HGET', KEYS[1], 'lockUtil')
            |if lu ~= false and tonumber(lu) < tonumber(ARGV[1]) or lu == false and v == false then
            |    redis.call('HSET', KEYS[1], 'lockUtil', ARGV[2])
            |    redis.call('HSET', KEYS[1], 'lockOwner', ARGV[3])
            |    return { v, 'LOCKED' }
            |end
            |return {v, lu}
            """.trimIndent()
    }
}