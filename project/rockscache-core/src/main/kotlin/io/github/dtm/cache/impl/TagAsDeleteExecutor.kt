package io.github.dtm.cache.impl

import io.github.dtm.cache.spi.Provider

internal class TagAsDeleteExecutor(
    private val provider: Provider,
    private val redisKeys: Collection<String>
) {
    fun execute() {

    }

    companion object {

        @JvmStatic
        private val LUA_DELETE =
            """--  delete
		    |redis.call('HSET', KEYS[1], 'lockUtil', 0) 
		    |redis.call('HDEL', KEYS[1], 'lockOwner') 
		    |redis.call('EXPIRE', KEYS[1], ARGV[1]) 
            """.trimMargin()
    }
}