package io.github.dtm.cache.spi

abstract class AbstractRedisProvider : RedisProvider {

    final override fun eval(block: LuaAppender.() -> Unit): List<Any> {
        val commands = mutableListOf<LuaCommand>()
        val appender = object : LuaAppender {
            override fun append(
                lua: String,
                keys: List<String>,
                args: List<String>
            ) {
                commands += LuaCommand(lua, keys, args)
            }
        }
        appender.block()
        if (commands.isEmpty()) {
            return emptyList()
        }
        if (commands.size == 1) {
            return listOf(executeLuaCommand(commands[0]))
        }
        return executeLuaCommands(commands)
    }

    protected abstract fun executeLuaCommand(
        command: LuaCommand
    ): Any

    protected abstract fun executeLuaCommands(
        commands: Collection<LuaCommand>
    ): List<Any>

    protected data class LuaCommand(
        val lua: String,
        val keys: List<String>,
        val args: List<String>
    )
}