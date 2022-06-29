package io.github.dtm.cache.spi

import redis.clients.jedis.JedisPool

class JedisProvider(
    private val pool: JedisPool
) : Provider {

    override fun executeBatchScript(
        block: LuaAppender.() -> Unit
    ): List<Any> =
        pool.resource.use { jedis ->
            jedis.pipelined().use { pipeline ->
                val appender = object : LuaAppender {
                    override fun append(lua: String, keys: List<String>, args: List<String>) {
                        pipeline.eval(lua, keys, args)
                    }
                }
                block(appender)
                pipeline.syncAndReturnAll()
            }
        }
}