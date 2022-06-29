package io.github.dtm.cache.spi

interface RedisProvider {

    fun eval(
        block: LuaAppender.() -> Unit
    ): List<Any>

    fun delete(keys: Collection<String>)
}