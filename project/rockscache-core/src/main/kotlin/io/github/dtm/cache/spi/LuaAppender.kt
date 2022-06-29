package io.github.dtm.cache.spi

interface LuaAppender {

    fun append(
        lua: String,
        keys: List<String>,
        args: List<String>
    )
}