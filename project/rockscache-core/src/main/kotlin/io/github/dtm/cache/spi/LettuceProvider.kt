package io.github.dtm.cache.spi

import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec

class LettuceProvider(
    redisClient: RedisClient,
    autoCloseClient: Boolean = false
) : AbstractRedisProvider(false), CloseableRedisProvider {

    private val con = redisClient.connect(
        RedisCodec.of(
            StringCodec.UTF8,
            ByteArrayCodec.INSTANCE
        )
    )

    private val redisClient = redisClient.takeIf { autoCloseClient }

    override fun eval(lua: ByteArray, keys: Array<String>, args: Array<ByteArray>): Any? =
        con.sync().eval(lua, ScriptOutputType.MULTI, keys, *args)

    override fun delete(keys: Collection<String>) {
        con.sync().del(*keys.toTypedArray())
    }

    override fun waitReplicas(replicas: Int, timeout: Long): Long =
        con.sync().waitForReplication(replicas, timeout)

    override fun close() {
        try {
            con.close()
        } finally {
            redisClient?.shutdownAsync()
        }
    }
}