@file:JvmName("Constants")
package io.github.dtm.cache

import java.time.Duration

/**
 * Constants shared by java and kotlin.
 *
 * @author 陈涛
 */
@JvmField
val DEFAULT_DELAY: Duration = Duration.ofSeconds(10)

@JvmField
val DEFAULT_EMPTY_EXPIRE: Duration = Duration.ofSeconds(60)

@JvmField
val DEFAULT_LOCK_EXPIRE: Duration = Duration.ofSeconds(3)

@JvmField
val DEFAULT_LOCK_SLEEP: Duration = Duration.ofMillis(100)

const val DEFAULT_WAIT_REPLICAS = 0

@JvmField
val DEFAULT_WAIT_REPLICAS_TIMEOUT: Duration = Duration.ofMillis(3000)

const val DEFAULT_RANDOM_EXPIRE_ADJUSTMENT = 0.1f

const val DEFAULT_DISABLE_CACHE_READ = false

const val DEFAULT_DISABLE_CACHE_DELETE = false

@JvmField
val DEFAULT_CONSISTENCY = Consistency.EVENTUAL