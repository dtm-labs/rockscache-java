package io.github.dtm.cache

import java.time.Duration
import java.util.function.Function

/**
 * @author 陈涛
 */
interface CacheBuilder<K, V> {

    /**
     * For kotlin, not java
     */
    fun setKtLoader(
        loader: (Collection<K>) -> Map<K,V>
    ): CacheBuilder<K, V>

    /**
     * For kotlin, not java
     */
    fun setKtLoader(
        keyExtractor: (V) -> K,
        loader: (Iterable<K>) -> Iterable<V>
    ): CacheBuilder<K, V> =
        setKtLoader { keys ->
            loader(keys)
                .filterNotNull()
                .associateBy(keyExtractor)
        }

    /**
     * For java, not kotlin
     */
    fun setJavaLoader(
        loader: Function<Collection<K>, Map<K, V>>
    ): CacheBuilder<K, V> =
        setKtLoader(loader::apply)

    /**
     * For java, not kotlin
     */
    fun setJavaLoader(
        keyExtractor: Function<V, K>,
        loader: Function<Iterable<K>, Iterable<V>>
    ): CacheBuilder<K, V> =
        setKtLoader(keyExtractor::apply, loader::apply)

    fun setExpire(expire: Duration): CacheBuilder<K, V>

    fun setConsistency(consistency: Consistency): CacheBuilder<K, V>

    fun build(): Cache<K, V>
}