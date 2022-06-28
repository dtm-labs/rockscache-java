package io.github.dtm.cache

import io.github.dtm.cache.java.BatchLoader
import io.github.dtm.cache.java.Loader
import io.github.dtm.cache.provider.Provider
import java.time.Duration

interface CacheClient<T> {

    fun <X> subClient(keyPrefix: String): CacheClient<X>

    /**
     * This method is designed for java, not for kotlin
     */
    fun fetch(
        key: String,
        expire: Duration,
        loader: Loader<T>
    ): T? =
        fetch(key, expire) {
            loader.load(it)
        }

    /**
     * This method is designed for kotlin, not for java
     */
    fun fetch(
        key: String,
        expire: Duration,
        loader: (String) -> T
    ): T? =
        fetchAll(
            setOf(key),
            expire
        ) {
            val k = it.first()
            mapOf(k to loader(k))
        }[key]

    /**
     * This method is designed for java, not for kotlin
     */
    fun fetchAll(
        keys: Collection<String>,
        expire: Duration,
        loader: BatchLoader<T>
    ): Map<String, T>

    /**
     * This method is designed for kotlin, not for java
     */
    fun fetchAll(
        keys: Collection<String>,
        expire: Duration,
        loader: (Collection<String>) -> Map<String, T>
    ): Map<String, T>

    interface Builder {

        fun setOptions(options: Options): Builder

        fun setProvider(provider: Provider): Builder

        fun build(): CacheClient<Any?>
    }

    companion object {

        @JvmStatic
        fun newBuilder(): Builder {
            TODO()
        }
    }
}