package io.github.dtm.cache

import io.github.dtm.cache.impl.CacheBuilderImpl
import io.github.dtm.cache.impl.CacheClientImpl
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.RedisProvider
import io.github.dtm.cache.spi.ValueSerializer
import kotlin.reflect.KClass

interface CacheClient {

    /**
     * For kotlin, not java
     */
    fun <K: Any, V: Any> newCacheBuilder(
        keyPrefix: String,
        keyType: KClass<K>,
        valueType: KClass<V>
    ): CacheBuilder<K, V> =
        newCacheBuilder(
            keyPrefix,
            KeySerializer.jackson(keyType),
            ValueSerializer.jackson(valueType)
        )

    /**
     * For java, not kotlin
     */
    fun <K, V> newCacheBuilder(
        keyPrefix: String,
        keyType: Class<K>,
        valueType: Class<V>
    ): CacheBuilder<K, V> =
        newCacheBuilder(
            keyPrefix,
            KeySerializer.jackson(keyType),
            ValueSerializer.jackson(valueType)
        )

    fun <K, V> newCacheBuilder(
        keyPrefix: String,
        keySerializer: KeySerializer<K>,
        valueSerializer: ValueSerializer<V>
    ): CacheBuilder<K, V>

    /**
     * For kotlin, not java
     */
    fun <K: Any, V: Any> newCache(
        keyPrefix: String,
        keyType: KClass<K>,
        valueType: KClass<V>,
        block: CacheBuilderDsl<K, V>.() -> Unit
    ): Cache<K, V> =
        newCache(
            keyPrefix,
            KeySerializer.jackson(keyType),
            ValueSerializer.jackson(valueType),
            block
        )

    /**
     * For kotlin, not java
     */
    fun <K, V> newCache(
        keyPrefix: String,
        keySerializer: KeySerializer<K>,
        valueSerializer: ValueSerializer<V>,
        block: CacheBuilderDsl<K, V>.() -> Unit
    ): Cache<K, V> =
        newCacheBuilder(keyPrefix, keySerializer, valueSerializer).let {
            (it as CacheBuilderImpl<K, V>).toDsl().block()
            it.build()
        }

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