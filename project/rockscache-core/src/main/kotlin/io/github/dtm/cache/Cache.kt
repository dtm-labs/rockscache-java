package io.github.dtm.cache

import java.time.Duration

/**
 * @author 陈涛
 */
interface Cache<K, V> {

    fun toCache(consistency: Consistency): Cache<K, V>

    fun fetch(key: K): V? =
        fetchAll(setOf(key))[key]

    fun fetch(key: K, consistency: Consistency): V? =
        fetchAll(setOf(key), consistency)[key]

    fun fetchAll(keys: Collection<K>): Map<K, V>

    fun fetchAll(keys: Collection<K>, consistency: Consistency): Map<K, V>

    fun tagAsDeleted(key: K) {
        tagAllAsDeleted(setOf(key))
    }

    fun tagAllAsDeleted(keys: Collection<K>)

    fun lockOperator(key: K, lockId: String): LockOperator<K> =
        lockAllOperator(setOf(key), lockId)

    fun lockAllOperator(keys: Collection<K>, lockId: String): LockOperator<K>
}