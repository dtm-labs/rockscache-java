package io.github.dtm.cache.spi

interface Provider {

    fun executeBatchScript(
        block: LuaAppender.() -> Unit
    ): List<Any>
}