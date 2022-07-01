package io.github.dtm.cache.spring

import io.github.dtm.cache.spi.AbstractRedisProvider
import io.github.dtm.cache.spi.RedisProvider
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisConnectionUtils

class SpringRedisProvider(
    private val connectionFactory: RedisConnectionFactory
) : AbstractRedisProvider(true) {

    override fun eval(lua: ByteArray, keyCount: Int, keyAndArgs: Array<ByteArray>): Any? =
        execute {
            eval(lua, ReturnType.MULTI, keyCount, *keyAndArgs)
        }

    override fun delete(keys: Collection<String>) {
        execute {
            del(*keys.map { it.toByteArray() }.toTypedArray())
        }
    }

    override fun waitReplicas(replicas: Int, timeout: Long): Long =
        execute {
            execute(
                "WAIT",
                replicas.toString().toByteArray(),
                timeout.toString().toByteArray()
            )
        } as Long

    private inline fun <R> execute(block: RedisConnection.() -> R): R {
        val con = RedisConnectionUtils.getConnection(connectionFactory)
        return try {
            con.block()
        } finally {
            RedisConnectionUtils.releaseConnection(con, connectionFactory)
        }
    }
}