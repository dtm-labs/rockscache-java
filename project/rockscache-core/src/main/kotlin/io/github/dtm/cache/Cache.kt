package io.github.dtm.cache

import io.github.dtm.cache.java.BatchLoader
import io.github.dtm.cache.java.Loader
import java.time.Duration

interface Cache<K, V> {

    /**
     * This method is designed for java, not for kotlin
     */
    fun fetch(
        key: K,
        expire: Duration,
        loader: Loader<K, V>
    ): V? =
        fetch(key, expire) {
            loader.load(it)
        }

    /**
     * This method is designed for kotlin, not for java
     */
    fun fetch(
        key: K,
        expire: Duration,
        loader: (K) -> V?
    ): V? =
        fetchAll(
            setOf(key),
            expire
        ) {
            val k = it.first()
            mapOf(k to loader(k))
        }[key]

    /**
     * This method is designed for java, not for kotlin
     */
    fun fetchAll(
        keys: Collection<K>,
        expire: Duration,
        loader: BatchLoader<K, V>
    ): Map<K, V?> =
        fetchAll(keys, expire) {
            loader.loadAll(keys)
        }

    /**
     * This method is designed for kotlin, not for java
     */
    fun fetchAll(
        keys: Collection<K>,
        expire: Duration,
        loader: (Collection<K>) -> Map<K, V?>
    ): Map<K, V?>

    fun tagAsDeleted(key: K) {
        tagAllAsDeleted(setOf(key))
    }

    fun tagAllAsDeleted(keys: Collection<K>)
}