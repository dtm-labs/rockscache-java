package io.github.dtm.cache

import java.time.Duration

/**
 * @author 陈涛
 */
interface CacheBuilderDsl<K, V> {

    var loader: ((Collection<K>) -> Map<K, V>)?

    var expire: Duration?

    var consistency: Consistency?
}