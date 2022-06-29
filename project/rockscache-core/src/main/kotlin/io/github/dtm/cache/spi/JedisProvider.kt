package io.github.dtm.cache.spi

import redis.clients.jedis.JedisPool

class JedisProvider(
    private val pool: JedisPool
) : AbstractRedisProvider() {

    override fun executeLuaCommand(command: LuaCommand): Any =
        pool.resource.use { jedis ->
            jedis.eval(command.lua, command.keys, command.args)
        }

    override fun executeLuaCommands(
        commands: Collection<LuaCommand>
    ): List<Any> =
        pool.resource.use { jedis ->
            jedis.pipelined().use { pipeline ->
                for (command in commands) {
                    pipeline.eval(command.lua, command.keys, command.args)
                }
                pipeline.syncAndReturnAll()
            }
        }

    override fun delete(keys: Collection<String>) {
        pool.resource.use { jedis ->
            jedis.del(*keys.toTypedArray())
        }
    }
}