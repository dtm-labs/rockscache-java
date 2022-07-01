package io.github.dtm.cache.spi

import redis.clients.jedis.JedisPool

/**
 * @author 陈涛
 */
class JedisProvider(
    private val pool: JedisPool
) : AbstractRedisProvider(true) {

    override fun eval(
        lua: ByteArray,
        keyCount: Int,
        keyAndArgs: Array<ByteArray>
    ): Any? =
        pool.resource.use { jedis ->
            jedis.eval(lua, keyCount, *keyAndArgs)
        }

    override fun delete(keys: Collection<String>) {
        pool.resource.use { jedis ->
            jedis.del(*keys.toTypedArray())
        }
    }

    override fun waitReplicas(replicas: Int, timeout: Long): Long =
        pool.resource.use { jedis ->
            jedis.waitReplicas(replicas, timeout)
        }
}