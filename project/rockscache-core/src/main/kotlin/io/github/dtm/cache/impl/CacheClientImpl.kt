package io.github.dtm.cache.impl

import io.github.dtm.cache.CacheBuilder
import io.github.dtm.cache.CacheClient
import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.RedisProvider
import io.github.dtm.cache.spi.ValueSerializer
import java.time.Duration

/**
 * @author 陈涛
 */
internal class CacheClientImpl(
    val provider: RedisProvider,
    private val options: Options,
    private val keyPrefix: String
) : CacheClient {

    val asyncFetchService = AsyncFetchService()

    init {
        if (options.delay == Duration.ZERO || options.lockExpire == Duration.ZERO) {
            throw IllegalArgumentException(
                "cache options error: delay and lockExpire should not be 0, " +
                    "you should call NewDefaultOptions() to get default options"
            )
        }
    }

    override fun <K, V> newCacheBuilder(
        keyPrefix: String,
        keySerializer: KeySerializer<K>,
        valueSerializer: ValueSerializer<V>
    ): CacheBuilder<K, V> =
        CacheBuilderImpl(
            this,
            "${this.keyPrefix}$keyPrefix",
            options,
            keySerializer,
            valueSerializer
        )

    override fun close() {
        try {
            asyncFetchService.close()
        } finally {
            (provider as? AutoCloseable)?.close()
        }
    }

    internal class BuilderImpl : CacheClient.Builder {

        private var options: Options? = null

        private var provider: RedisProvider? = null

        private var keyPrefix: String? = null

        override fun setOptions(options: Options): CacheClient.Builder {
            this.options = options
            return this
        }

        override fun setProvider(provider: RedisProvider): CacheClient.Builder {
            this.provider = provider
            return this
        }

        override fun setKeyPrefix(keyPrefix: String): CacheClient.Builder {
            this.keyPrefix = keyPrefix
            return this
        }

        override fun build(): CacheClient =
            CacheClientImpl(
                provider ?: error("Provider is not specified"),
                options ?: Options(),
                keyPrefix ?: ""
            )
    }
}