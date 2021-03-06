package io.github.dtm.cache.impl

import io.github.dtm.cache.*
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.ValueSerializer
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.time.Duration

/**
 * @author 陈涛
 */
internal class CacheImpl<K, V>(
    private val client: CacheClientImpl,
    private val keyPrefix: String,
    private val options: Options,
    private val keySerializer: KeySerializer<K>,
    private val valueSerializer: ValueSerializer<V>,
    private val expire: Duration,
    private val loader: (Collection<K>) -> Map<K, V>
) : Cache<K, V> {

    override fun toCache(consistency: Consistency): Cache<K, V> =
        if (options.consistency == consistency) {
            this
        } else {
            CacheImpl(
                client,
                keyPrefix,
                options.copy(consistency = consistency),
                keySerializer,
                valueSerializer,
                expire,
                loader
            )
        }

    override fun fetchAll(keys: Collection<K>): Map<K, V> =
        fetchAll(keys, options.consistency)

    override fun fetchAll(keys: Collection<K>, consistency: Consistency): Map<K, V> {
        if (client.asyncFetchService.isClosed) {
            throw IllegalStateException("The cache client has been closed")
        }
        return if (options.isDisableCacheRead) {
            loader(keys)
        } else {
            val keySet = keys as? Set<K> ?: keys.toSet()
            var resultMap: Map<K, V> = emptyMap()
            split(keySet, options.batchSize) {
                val map = FetchExecutor(
                    client,
                    keyPrefix,
                    if (consistency == options.consistency) {
                        options
                    } else {
                        options.copy(consistency = consistency)
                    },
                    keySerializer,
                    valueSerializer,
                    expire,
                    loader,
                    it
                ).execute()
                resultMap = if (resultMap.isEmpty()) {
                    map
                } else {
                    resultMap + map
                }
            }
            resultMap
        }
    }

    override fun tagAllAsDeleted(keys: Collection<K>) {
        if (options.isDisableCacheDelete || keys.isEmpty()) {
            return
        }
        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Delete keys, keyPrefix: $keyPrefix, keys: $keys")
        }
        val redisKeys = keys.map { "$keyPrefix${keySerializer.serialize(it)}" }.toSet()
        split(redisKeys, options.batchSize) {
            TagAsDeleteExecutor(options, client.provider, it).execute()
        }
    }

    override fun lockAllOperator(keys: Collection<K>, lockId: String): LockOperator<K> {
        if (keys.size > options.batchSize) {
            throw IllegalArgumentException(
                "keys.size() is ${keys.size}, it is greater than batchSize ${options.batchSize}"
            )
        }
        if (lockId.isEmpty()) {
            throw IllegalArgumentException(
                "lockId cannot be empty"
            )
        }
        return LockOperatorImpl(
            client,
            options,
            lockId,
            keys.map { "${keyPrefix}${keySerializer.serialize(it)}" }.toSet()
        )
    }

    companion object {

        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(CacheClientImpl::class.java)
    }
}