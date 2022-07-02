package io.github.dtm.cache

import java.time.Duration

/**
 * @author 陈涛
 */
interface LockOperator<K> {

    /**
     * Note:
     * Even if lock or lockAll fails, unlock needs to be called with a reliable message
     */
    fun lock(waitDuration: Duration)

    fun unlock()
}