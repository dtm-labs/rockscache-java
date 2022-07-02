package io.github.dtm.cache

class LockException(
    val redisKeys: Collection<String>,
    cause: Throwable? = null
) : RuntimeException(
    "Cannot lock the keys $redisKeys",
    cause
)