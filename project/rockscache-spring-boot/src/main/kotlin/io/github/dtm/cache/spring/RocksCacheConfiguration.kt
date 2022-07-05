package io.github.dtm.cache.spring

import io.github.dtm.cache.*
import io.github.dtm.cache.spi.RedisProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration

@Configuration
open class RocksCacheConfiguration {

    @ConditionalOnMissingBean(Options::class)
    @Bean
    open fun options(
        // Why use @Value one by one here?, why don't use @ConfigurationProperties?
        //
        // Beacuse I met problem, please view
        // https://stackoverflow.com/questions/72816139/how-to-do-implement-immutable-spring-properties-without-constructorbinding
        @Value("\${rockscache.delay:#{null}}") delay: Duration?,
        @Value("\${rockscache.emptyExpire:#{null}}") emptyExpire: Duration?,
        @Value("\${rockscache.lockExpire:#{null}}") lockExpire: Duration?,
        @Value("\${rockscache.lockSleep:#{null}}") lockSleep: Duration?,
        @Value("\${rockscache.waitReplicas:#{null}}") waitReplicas: Int?,
        @Value("\${rockscache.waitReplicasTimeout:#{null}}") waitReplicasTimeout: Duration?,
        @Value("\${rockscache.isDisableCacheRead:#{null}}") isDisableCacheRead: Boolean?,
        @Value("\${rockscache.isDisableCacheDelete:#{null}}") isDisableCacheDelete: Boolean?,
        @Value("\${rockscache.consistency:#{null}}") consistency: Consistency?,
        @Value("\${rockscache.batchSize:#{null}}") batchSize: Int?
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
    open fun redisProvider(connectionFactory: RedisConnectionFactory): RedisProvider =
        SpringRedisProvider(connectionFactory)

    @ConditionalOnMissingBean(CacheClient::class)
    @Bean(destroyMethod = "close")
    open fun cacheClient(
        @Value("\${rockscache.globalKeyPrefix:#{null}}") globalKeyPrefix: String?,
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