package io.github.dtm.cache

import io.github.dtm.cache.java.OptionsBuilder
import java.time.Duration

/**
 * @author 陈涛
 */
data class Options(

    /**
     * Delay delete time for keys that are tag deleted. default is 10s
     */
    val delay: Duration = DEFAULT_DELAY,

    /**
     * Get Expire time for empty result. default is 60s
     */
    val emptyExpire: Duration = DEFAULT_EMPTY_EXPIRE,

    /**
     * Get expire time for the lock which is allocated when updating cache. default is 3s
     */
    val lockExpire: Duration = DEFAULT_LOCK_EXPIRE,

    /**
     * Get the sleep interval time if try lock failed. default is 100ms
     */
    val lockSleep: Duration = DEFAULT_LOCK_SLEEP,

    /**
     * Get the number of replicas to wait for. default is 0
     */
    val waitReplicas: Int = DEFAULT_WAIT_REPLICAS,

    /**
     * Get the number of replicas to wait for. default is 3000ms
     * if WaitReplicas is > 0, WaitReplicasTimeout is the timeout for WAIT command.
     */
    val waitReplicasTimeout: Duration = DEFAULT_WAIT_REPLICAS_TIMEOUT,

    /**
     * RandomExpireAdjustment is the random adjustment for the expire time. default 0.1
     * if the expire time is set to 600s, and this value is set to 0.1, then the actual expire time will be 540s - 600s
     * solve the problem of cache avalanche.
     */
    val getRandomExpireAdjustment: Float = DEFAULT_RANDOM_EXPIRE_ADJUSTMENT,

    /**
     * Get the flag to disable read cache. default is false
     * when redis is down, set this flat to downgrade.
     */
    val isDisableCacheRead: Boolean = DEFAULT_DISABLE_CACHE_READ,

    /**
     * Get the flag to disable delete cache. default is false
     * when redis is down, set this flat to downgrade.
     */
    val isDisableCacheDelete: Boolean = DEFAULT_DISABLE_CACHE_DELETE,

    /**
     * Get the flag consistency. default is [Consistency.EVENTUAL]
     *
     *  *
     * [Consistency.EVENTUAL]: Returned the original value
     * if value calculation is not done
     *
     *  *
     * [Consistency.STRONG]: Returned value consistent with
     * the db result, but performance is bad.
     *
     *  *
     * [Consistency.ALLOW_LOADING_EXCEPTION]: Throws
     * [LoadingException] if value calculation is not done.
     * The UI should show loading animation for this exception.
     *
     *
     */
    val consistency: Consistency = DEFAULT_CONSISTENCY,

    /**
     * BatchSize, default is 128.
     *
     * <p>
     *      If user want to fetch/tagAsDeleted 150 keys: key1, key2, ..., key250
     *      , they will be split into to three batches
     * </p>
     * <ul>
     *     <li>key1, key2, ..., key100</li>
     *     <li>key101, key102, ..., key200</li>
     *     <li>key201, key2, ..., key250</li>
     * </ul>
     */
    val batchSize: Int = DEFAULT_BATCH_SIZE
) {

    companion object {

        /**
         * Only for java.
         */
        @JvmStatic
        fun newBuilder(): OptionsBuilder =
            OptionsBuilder()
    }
}