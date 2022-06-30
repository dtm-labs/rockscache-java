package io.github.dtm.cache.spi

import redis.clients.jedis.JedisPool

class JedisProvider(
    private val pool: JedisPool
) : RedisProvider {

    override fun eval(lua: String, keys: List<String>, args: List<String>): Any? =
        pool.resource.use { jedis ->
            jedis.eval(lua, keys, args)
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