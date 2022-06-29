package io.github.dtm.cache

/**
 * @author 陈涛
 */
class DirtyCacheException(
    message: String,
    val dirtyRedisKeys: Set<String>
) : RuntimeException(message)