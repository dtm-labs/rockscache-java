package io.github.dtm.cache.spi

import java.io.Closeable

interface CloseableRedisProvider : RedisProvider, Closeable {

    override fun close()
}