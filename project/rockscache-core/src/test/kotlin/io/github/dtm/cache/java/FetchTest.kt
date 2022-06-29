package io.github.dtm.cache.java

import io.github.dtm.cache.Cache
import io.github.dtm.cache.CacheClient
import io.github.dtm.cache.spi.JedisProvider
import redis.clients.jedis.JedisPool
import java.time.Duration
import kotlin.test.Test

class FetchTest {

    @Test
    fun test() {
        val cache = CacheClient
            .newBuilder()
            .setKeyPrefix("rockscache-java")
            .setProvider(JedisProvider(JedisPool("localhost", 6379)))
            .build()
            .createCache("int-to-string", Int::class, String::class)
        cache.fetch(1, Duration.ZERO) {
            TODO()
        }
    }
}