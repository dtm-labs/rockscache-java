package io.github.dtm.cache.java

import io.github.dtm.cache.spi.JedisProvider
import io.github.dtm.cache.spi.RedisProvider
import redis.clients.jedis.JedisPool

class JedisCacheTest : AbstractCacheTest() {

    override fun createRedisProvider(): RedisProvider =
        JedisProvider(JedisPool("localhost", 6379))
}