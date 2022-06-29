package io.github.dtm.cache

import io.github.dtm.cache.impl.CacheClientImpl
import io.github.dtm.cache.spi.Provider
import io.github.dtm.cache.spi.Serializer
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
        createCache(keyPrefix, keyType.java, valueType.java)

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
            Serializer.jackson(keyType),
            Serializer.jackson(valueType)
        )

    fun <K, V> createCache(
        keyPrefix: String,
        keySerializer: Serializer<K>,
        valueSerializer: Serializer<V>
    ): Cache<K, V>

    interface Builder {

        fun setOptions(options: Options): Builder

        fun setProvider(provider: Provider): Builder

        fun setKeyPrefix(keyPrefix: String): Builder

        fun build(): CacheClient
    }

    companion object {

        @JvmStatic
        fun newBuilder(): Builder =
            CacheClientImpl.BuilderImpl()
    }
}