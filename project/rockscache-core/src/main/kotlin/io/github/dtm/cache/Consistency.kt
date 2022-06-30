package io.github.dtm.cache

/**
 * @author 陈涛
 */
enum class Consistency {
    EVENTUAL,
    STRONG,
    ALLOW_DIRTY_CACHE_EXCEPTION
}