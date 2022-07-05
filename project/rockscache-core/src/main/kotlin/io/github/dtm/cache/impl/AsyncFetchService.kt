package io.github.dtm.cache.impl

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author 陈涛
 */
internal class AsyncFetchService {

    private val closed = AtomicBoolean()

    // Use virtual thread of loom in the future
    private val executorService = Executors.newCachedThreadPool()

    fun add(block: () -> Unit) {
        if (closed.get()) {
            LOGGER.warn("AsyncFetchService is closed")
        } else {
            executorService.execute(block)
        }
    }

    val isClosed: Boolean
        get() = closed.get()

    fun close() {
        if (closed.compareAndSet(false, true)) {
            executorService.shutdown();
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AsyncFetchService::class.java)
    }
}