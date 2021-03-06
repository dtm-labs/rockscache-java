package io.github.dtm.cache.impl

import io.github.dtm.cache.*
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.ValueSerializer
import java.time.Duration

/**
 * @author 陈涛
 */
internal class CacheBuilderImpl<K, V>(
    private val client: CacheClientImpl,
    private val keyPrefix: String,
    private val options: Options,
    private val keySerializer: KeySerializer<K>,
    private val valueSerializer: ValueSerializer<V>
): CacheBuilder<K, V> {

    private var expire: Duration? = null

    private var loader: ((Collection<K>) -> Map<K, V>)? = null

    private var consistency: Consistency? = null

    override fun setKtLoader(
        loader: (Collection<K>) -> Map<K, V>
    ): CacheBuilder<K, V> {
        this.loader = loader
        return this
    }

    override fun setExpire(expire: Duration): CacheBuilder<K, V> {
        if (expire < Duration.ofSeconds(1)) {
            throw IllegalArgumentException("expire must not less than 1 second.")
        }
        this.expire = expire
        return this
    }

    override fun setConsistency(consistency: Consistency): CacheBuilder<K, V> {
        this.consistency = consistency
        return this
    }

    override fun build(): Cache<K, V> =
        CacheImpl(
            client = client,
            keyPrefix = keyPrefix,
            options = consistency
                ?.let { options.copy(consistency = it) }
                ?: options,
            keySerializer = keySerializer,
            valueSerializer = valueSerializer,
            expire = expire ?: Duration.ofMinutes(5),
            loader = loader ?: error("loader of cache is missing")
        )

    internal fun toDsl(): CacheBuilderDsl<K, V> =
        object : CacheBuilderDsl<K, V> {
            override var loader: ((Collection<K>) -> Map<K, V>)?
                by this@CacheBuilderImpl::loader

            override var expire: Duration?
                by this@CacheBuilderImpl::expire

            override var consistency: Consistency?
                by this@CacheBuilderImpl::consistency
        };
}