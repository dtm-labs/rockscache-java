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
            .setKeyPrefix("test-scope-")
            .setProvider(JedisProvider(JedisPool("localhost", 6379)))
            .build()
            .createCache("int2str-", Int::class, String::class)
        cache.fetch(1, Duration.ofMinutes(1)) {
            println("LOAD------------------------------------------------")
            when (it) {
                1 -> "One"
                else -> TODO()
            }
        }.let {
            println(it)
        }
    }
}