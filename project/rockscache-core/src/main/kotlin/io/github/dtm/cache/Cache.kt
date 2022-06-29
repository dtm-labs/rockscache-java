package io.github.dtm.cache

interface Cache<K, V> {

    fun toCache(consistency: Consistency): Cache<K, V>

    fun fetch(key: K): V? =
        fetchAll(setOf(key))[key]

    fun fetch(key: K, consistency: Consistency): V? =
        fetchAll(setOf(key), consistency)[key]

    fun fetchAll(keys: Collection<K>): Map<K, V?>

    fun fetchAll(keys: Collection<K>, consistency: Consistency): Map<K, V?>

    fun tagAsDeleted(key: K) {
        tagAllAsDeleted(setOf(key))
    }

    fun tagAllAsDeleted(keys: Collection<K>)
}