package io.github.dtm.cache.java;

import io.github.dtm.cache.*;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Java can not use kotlin data class,
 * it must use builder pattern
 *
 * @author 陈涛
 */
public class OptionsBuilder {

    private Duration delay = Constants.DEFAULT_DELAY;
    
    private Duration emptyExpire = Constants.DEFAULT_EMPTY_EXPIRE;
    
    private Duration lockExpire = Constants.DEFAULT_LOCK_EXPIRE;
    
    private Duration lockSleep = Constants.DEFAULT_LOCK_SLEEP;
    
    private int waitReplicas = Constants.DEFAULT_WAIT_REPLICAS;
    
    private Duration waitReplicasTimeout = Constants.DEFAULT_WAIT_REPLICAS_TIMEOUT;
    
    private float randomExpireAdjustment = Constants.DEFAULT_RANDOM_EXPIRE_ADJUSTMENT;
    
    private boolean disableCacheRead = Constants.DEFAULT_DISABLE_CACHE_READ;
    
    private boolean disableCacheDelete = Constants.DEFAULT_DISABLE_CACHE_DELETE;
    
    private Consistency consistency = Constants.DEFAULT_CONSISTENCY;

    private int batchSize = Constants.DEFAULT_BATCH_SIZE;

    /**
     * Set delay delete time for keys that are tag deleted. default is 10s
     */
    @NotNull
    public OptionsBuilder setDelay(@NotNull Duration delay) {
        this.delay = delay;
        return this;
    }

    /**
     * Set Expire time for empty result. default is 60s
     */
    @NotNull
    public OptionsBuilder setEmptyExpire(@NotNull Duration emptyExpire) {
        this.emptyExpire = emptyExpire;
        return this;
    }

    /**
     * Get expire time for the lock which is allocated when updating cache. default is 3s
     */
    @NotNull
    public OptionsBuilder setLockExpire(@NotNull Duration lockExpire) {
        this.lockExpire = lockExpire;
        return this;
    }

    /**
     * Set the sleep interval time if try lock failed. default is 100ms
     */
    @NotNull
    public OptionsBuilder setLockSleep(@NotNull Duration lockSleep) {
        this.lockSleep = lockSleep;
        return this;
    }

    /**
     * Get the number of replicas to wait for. default is 0
     */
    @NotNull
    public OptionsBuilder setWaitReplicas(int waitReplicas) {
        this.waitReplicas = waitReplicas;
        return this;
    }

    /**
     * Set the number of replicas to wait for. default is 3000ms
     * if WaitReplicas is > 0, WaitReplicasTimeout is the timeout for WAIT command.
     */
    @NotNull
    public OptionsBuilder setWaitReplicasTimeout(@NotNull Duration waitReplicasTimeout) {
        this.waitReplicasTimeout = waitReplicasTimeout;
        return this;
    }

    /**
     * RandomExpireAdjustment is the random adjustment for the expire time. default 0.1
     * if the expire time is set to 600s, and this value is set to 0.1, then the actual expire time will be 540s - 600s
     * solve the problem of cache avalanche.
     */
    @NotNull
    public OptionsBuilder setRandomExpireAdjustment(float randomExpireAdjustment) {
        this.randomExpireAdjustment = randomExpireAdjustment;
        return this;
    }

    /**
     * Set the flag to disable read cache. default is false
     * when redis is down, set this flat to downgrade.
     */
    @NotNull
    public OptionsBuilder setDisableCacheRead(boolean disableCacheRead) {
        this.disableCacheRead = disableCacheRead;
        return this;
    }

    /**
     * Set the flag to disable delete cache. default is false
     * when redis is down, set this flat to downgrade.
     */
    @NotNull
    public OptionsBuilder setDisableCacheDelete(boolean disableCacheDelete) {
        this.disableCacheDelete = disableCacheDelete;
        return this;
    }

    /**
     * Set the flag consistency. default is {@link Consistency#EVENTUAL}
     * <ul>
     *     <li>
     *          {@link Consistency#EVENTUAL}: Returned the original value
     *              if value calculation is not done
     *     </li>
     *     <li>
     *         {@link Consistency#STRONG}: Returned value consistent with
     *              the db result, but performance is bad.
     *     </li>
     *     <li>
     *         {@link Consistency#TRY_STRONG}: Throws
     *         {@link DirtyCacheException} if value calculation is not done.
     *         The UI should show loading animation for this exception.
     *     </li>
     * </ul>
     */
    @NotNull
    public OptionsBuilder setConsistency(@NotNull Consistency consistency) {
        this.consistency = consistency;
        return this;
    }

    /**
     * Set the BatchSize, default is 128.
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
    public OptionsBuilder setBatchSize(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must not be less than 1");
        }
        this.batchSize = batchSize;
        return this;
    }

    public Options build() {
        return new Options(
                delay,
                emptyExpire,
                lockExpire,
                lockSleep,
                waitReplicas,
                waitReplicasTimeout,
                randomExpireAdjustment,
                disableCacheRead,
                disableCacheDelete,
                consistency,
                batchSize
        );
    }
}
