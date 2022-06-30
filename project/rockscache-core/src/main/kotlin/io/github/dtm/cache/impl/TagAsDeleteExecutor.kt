package io.github.dtm.cache.impl

import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.RedisProvider
import java.lang.IllegalStateException

/**
 * @author 陈涛
 */
internal class TagAsDeleteExecutor(
    private val options: Options,
    private val provider: RedisProvider,
    private val redisKeys: List<String>
) {
    fun execute() {
        val args = listOf(options.delay.seconds.toString())
        provider.eval(LUA_TAG_AS_DELETE, redisKeys, args)
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
            """
                | --lua tagAsDelete
		        | for index, key in ipairs(KEYS) do
                |     redis.call('HSET', key, 'lockUntil', 0) 
                |     redis.call('HDEL', key, 'lockOwner') 
                |     redis.call('EXPIRE', key, ARGV[1])
                | end
            """.trimMargin()
    }
}