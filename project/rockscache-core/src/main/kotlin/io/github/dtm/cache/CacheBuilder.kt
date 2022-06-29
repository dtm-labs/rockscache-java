package io.github.dtm.cache

import io.github.dtm.cache.java.Loader
import java.time.Duration

interface CacheBuilder<K, V> {

    /**
     * For kotlin, not java
     */
    fun setLoader(loader: (Collection<K>) -> Map<K, V?>): CacheBuilder<K, V>

    /**
     * For java, not kotlin
     */
    fun setLoader(loader: Loader<K, V>): CacheBuilder<K, V>

    fun setExpire(expire: Duration): CacheBuilder<K, V>

    fun setConsistency(consistency: Consistency): CacheBuilder<K, V>

    fun build(): Cache<K, V>
}