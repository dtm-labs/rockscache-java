package io.github.dtm.cache.spring

import io.github.dtm.cache.*
import io.github.dtm.cache.spi.JedisProvider
import io.github.dtm.cache.spi.RedisProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPool
import java.time.Duration

@Configuration
open class RocksCacheConfiguration {

    @ConditionalOnMissingBean(Options::class)
    @Bean
    open fun options(
        @Value("rockscache.delay") delay: Duration?,
        @Value("rockscache.emptyExpire") emptyExpire: Duration?,
        @Value("rockscache.lockExpire") lockExpire: Duration?,
        @Value("rockscache.lockSleep") lockSleep: Duration?,
        @Value("rockscache.waitReplicas") waitReplicas: Int?,
        @Value("rockscache.waitReplicasTimeout") waitReplicasTimeout: Duration?,
        @Value("rockscache.isDisableCacheRead") isDisableCacheRead: Boolean?,
        @Value("rockscache.isDisableCacheDelete") isDisableCacheDelete: Boolean?,
        @Value("rockscache.consistency") consistency: Consistency?,
        @Value("rockscache.batchSize") batchSize: Int?
    ): Options =
        Options(
            delay = delay ?: DEFAULT_DELAY,
            emptyExpire = emptyExpire ?: DEFAULT_EMPTY_EXPIRE,
            lockExpire = lockExpire ?: DEFAULT_LOCK_EXPIRE,
            lockSleep = lockSleep ?: DEFAULT_LOCK_SLEEP,
            waitReplicas = waitReplicas ?: DEFAULT_WAIT_REPLICAS,
            waitReplicasTimeout = waitReplicasTimeout ?: DEFAULT_WAIT_REPLICAS_TIMEOUT,
            isDisableCacheRead = isDisableCacheRead ?: DEFAULT_DISABLE_CACHE_READ,
            isDisableCacheDelete = isDisableCacheDelete ?: DEFAULT_DISABLE_CACHE_DELETE,
            consistency = consistency ?: DEFAULT_CONSISTENCY,
            batchSize = batchSize ?: DEFAULT_BATCH_SIZE
        )

    @ConditionalOnMissingBean(RedisProvider::class)
    @Bean
    open fun redisProvider(pool: JedisPool): RedisProvider =
        JedisProvider(pool)

    @ConditionalOnMissingBean(CacheClient::class)
    @Bean
    open fun cacheClient(
        @Value("rockscache.globalKeyPrefix") globalKeyPrefix: String?,
        redisProvider: RedisProvider,
        options: Options
    ): CacheClient =
        CacheClient
            .newBuilder()
            .setKeyPrefix(globalKeyPrefix ?: "")
            .setProvider(redisProvider)
            .setOptions(options)
            .build()
}