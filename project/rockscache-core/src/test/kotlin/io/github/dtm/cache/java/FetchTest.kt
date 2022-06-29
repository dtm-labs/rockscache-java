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
            .setKeyPrefix("test-scope2-")
            .setProvider(JedisProvider(JedisPool("localhost", 6379)))
            .build()
            .newCache("int2str-", Int::class, String::class) {
                loader = { keys ->
                    println("LOAD------------------------------------------------")
                    keys.associateBy({it}) {
                        when (it) {
                            1 -> "One"
                            else -> TODO()
                        }
                    }
                }
            }
        cache.fetch(1)
        cache.tagAsDeleted(1)
        cache.fetch(1)
    }
}