package io.github.dtm.cache.impl

import io.github.dtm.cache.Cache
import io.github.dtm.cache.Options
import io.github.dtm.cache.spi.KeySerializer
import io.github.dtm.cache.spi.RedisProvider
import io.github.dtm.cache.spi.ValueSerializer
import org.slf4j.LoggerFactory
import java.time.Duration

internal class CacheImpl<K, V>(
    private val keyPrefix: String,
    private val options: Options,
    private val provider: RedisProvider,
    private val keySerializer: KeySerializer<K>,
    private val valueSerializer: ValueSerializer<V>
) : Cache<K, V> {

    override fun fetchAll(
        keys: Collection<K>,
        expire: Duration,
        loader: (Collection<K>) -> Map<K, V?>
    ): Map<K, V?> =
        if (options.isDisableCacheRead) {
            loader(keys)
        } else {
            val keySet = keys as? Set<K> ?: keys.toSet()
            var resultMap: Map<K, V?> = emptyMap()
            split(keySet, options.batchSize) {
                val map = FetchExecutor(
                    keyPrefix,
                    options,
                    provider,
                    keySerializer,
                    valueSerializer,
                    it,
                    expire,
                    loader
                ).execute()
                resultMap = if (resultMap.isEmpty()) {
                    map
                } else {
                    resultMap + map
                }
            }
            resultMap
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
            TagAsDeleteExecutor(provider, it).execute()
        }
    }

    companion object {

        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(CacheClientImpl::class.java)

        @JvmStatic
        private fun <E> split(set: Set<E>, batchSize: Int, handler: (Collection<E>) -> Unit) {
            if (set.size < batchSize) {
                handler(set)
            } else {
                var list = mutableListOf<E>()
                for (e in set) {
                    list.add(e)
                    if (list.size >= batchSize) {
                        handler(list)
                        list = mutableListOf<E>()
                    }
                }
                if (list.isNotEmpty()) {
                    handler(list)
                }
            }
        }
    }
}