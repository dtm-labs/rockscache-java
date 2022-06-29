package io.github.dtm.cache.impl

import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.RedisProvider
import java.lang.IllegalStateException

internal class TagAsDeleteExecutor(
    private val options: Options,
    private val provider: RedisProvider,
    private val redisKeys: Collection<String>
) {
    fun execute() {
        val args = listOf(options.delay.seconds.toString())
        provider.eval {
            for (redisKey in redisKeys) {
                append(LUA_TAG_AS_DELETE, listOf(redisKey), args)
            }
        }
        if (options.waitReplicas > 0) {
            val replicas = provider.waitReplicas(
                options.waitReplicas,
                options.waitReplicasTimeout.toMillis()
            )
            if (replicas < options.waitReplicas) {
                throw IllegalStateException(
                    "wait replicas ${options.waitReplicas} failed. result replicas: $replicas"
                )
            }
        }
    }

    companion object {

        @JvmStatic
        private val LUA_TAG_AS_DELETE =
            """--lua tagAsDelete
		    |redis.call('HSET', KEYS[1], 'lockUntil', 0) 
		    |redis.call('HDEL', KEYS[1], 'lockOwner') 
		    |redis.call('EXPIRE', KEYS[1], ARGV[1]) 
            """.trimMargin()
    }
}