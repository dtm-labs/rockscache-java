package io.github.dtm.cache.impl

import java.util.concurrent.Executors

internal class AsyncFetchService {

    @Volatile
    private var closed = false

    // Use virtual thread of loom in the future
    private val executorService = Executors.newCachedThreadPool()

    fun add(block: () -> Unit) {
        executorService.execute(block)
    }

    val isClosed: Boolean
        get() = closed

    fun close() {
        closed = true
    }
}