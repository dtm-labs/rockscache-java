package io.github.dtm.cache

import io.github.dtm.cache.impl.CacheClientImpl
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.RedisProvider
import io.github.dtm.cache.spi.ValueSerializer
import kotlin.reflect.KClass

interface CacheClient{

    /**
     * For kotlin, not java
     */
    fun <K: Any, V: Any> createCache(
        keyPrefix: String,
        keyType: KClass<K>,
        valueType: KClass<V>
    ): Cache<K, V> =
        createCache(
            keyPrefix,
            KeySerializer.jackson(keyType),
            ValueSerializer.jackson(valueType)
        )

    /**
     * For java, not kotlin
     */
    fun <K, V> createCache(
        keyPrefix: String,
        keyType: Class<K>,
        valueType: Class<V>
    ): Cache<K, V> =
        createCache(
            keyPrefix,
            KeySerializer.jackson(keyType),
            ValueSerializer.jackson(valueType)
        )

    fun <K, V> createCache(
        keyPrefix: String,
        keySerializer: KeySerializer<K>,
        valueSerializer: ValueSerializer<V>
    ): Cache<K, V>

    interface Builder {

        fun setOptions(options: Options): Builder

        fun setProvider(provider: RedisProvider): Builder

        fun setKeyPrefix(keyPrefix: String): Builder

        fun build(): CacheClient
    }

    companion object {

        @JvmStatic
        fun newBuilder(): Builder =
            CacheClientImpl.BuilderImpl()
    }
}