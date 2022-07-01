package io.github.dtm.cache.java

import io.github.dtm.cache.spi.LettuceProvider
import io.github.dtm.cache.spi.RedisProvider
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI

class LettuceCacheTest : AbstractCacheTest() {

    override fun createRedisProvider(): RedisProvider =
        LettuceProvider(
            RedisClient.create(
                RedisURI
                    .builder()
                    .withHost("localhost")
                    .withPort(6379)
                    .build()
            ),
            true
        )
}