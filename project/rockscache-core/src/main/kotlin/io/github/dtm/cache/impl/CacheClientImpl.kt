package io.github.dtm.cache.impl

import io.github.dtm.cache.Cache
import io.github.dtm.cache.CacheClient
import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.Provider
import io.github.dtm.cache.spi.Serializer
import java.time.Duration

internal class CacheClientImpl(
    private val options: Options,
    private val provider: Provider,
    private val keyPrefix: String
) : CacheClient {

    init {
        if (options.delay == Duration.ZERO || options.lockExpire == Duration.ZERO) {
            throw IllegalArgumentException(
                "cache options error: delay and lockExpire should not be 0, " +
                    "you should call NewDefaultOptions() to get default options"
            )
        }
    }

    override fun <K, V> createCache(
        keyPrefix: String,
        keySerializer: Serializer<K>,
        valueSerializer: Serializer<V>
    ): Cache<K, V> =
        CacheImpl(
            keyPrefix,
            options,
            provider,
            keySerializer,
            valueSerializer
        )

    internal class BuilderImpl : CacheClient.Builder {

        private var options: Options? = null

        private var provider: Provider? = null

        private var keyPrefix: String? = null

        override fun setOptions(options: Options): CacheClient.Builder {
            this.options = options
            return this
        }

        override fun setProvider(provider: Provider): CacheClient.Builder {
            this.provider = provider
            return this
        }

        override fun setKeyPrefix(keyPrefix: String): CacheClient.Builder {
            this.keyPrefix = keyPrefix
            return this
        }

        override fun build(): CacheClient =
            CacheClientImpl(
                options ?: Options(),
                provider ?: error("Provider is not specified"),
                keyPrefix ?: ""
            )
    }
}