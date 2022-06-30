package io.github.dtm.cache

/**
 * @author 陈涛
 */
interface LockScope {

    fun unlock()

    /**
     * For kotlin, not java
     */
    fun <R> execute(block: () -> R): R {
        return try {
            block()
        } finally {
            unlock()
        }
    }
}