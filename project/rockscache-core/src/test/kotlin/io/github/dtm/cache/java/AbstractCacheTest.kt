package io.github.dtm.cache.java

import io.github.dtm.cache.Cache
import io.github.dtm.cache.CacheClient
import io.github.dtm.cache.Consistency
import io.github.dtm.cache.DirtyCacheException
import io.github.dtm.cache.spi.RedisProvider
import java.time.Duration
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.*

abstract class AbstractCacheTest {

    private lateinit var provider: RedisProvider

    private lateinit var cacheClient: CacheClient

    private lateinit var cache: Cache<Int, String>

    private lateinit var dbMap: MutableMap<Int, String>

    private var dbReadCount = 0

    @BeforeTest
    @Suppress("UNCHECKED_CAST")
    fun init() {
        provider = createRedisProvider()
        provider.delete(
            listOf(
                "test-scope-int-to-str-1",
                "test-scope-int-to-str-2",
                "test-scope-int-to-str-3",
                "test-scope-int-to-str-4"
            )
        )
        cacheClient = CacheClient
            .newBuilder()
            .setKeyPrefix("test-scope-")
            .setProvider(provider)
            .build()
        cache = cacheClient
            .newCache("int-to-str-", Int::class, String::class) {
                loader = { keys ->
                    dbReadCount++
                    Thread.sleep(1000)
                    keys.associateBy({ it }) {
                        dbMap[it]
                    }.filterValues { it !== null } as Map<Int, String>
                }
            }
        dbMap = mutableMapOf(
            1 to "One",
            2 to "Two",
            3 to "Three"
        )
        dbReadCount = 0
    }

    @AfterTest
    fun uninit() {
        cacheClient.close()
    }

    protected abstract fun createRedisProvider(): RedisProvider

    @Test
    fun testFetchWithMixMode() {
        expect(0) { dbReadCount }
        expect("One") {cache.fetch(1) }
        expect(1) { dbReadCount }
        expect("One") {cache.fetch(1, Consistency.STRONG) }
        expect(1) { dbReadCount }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)

        parallel(5) {
            expect("One") { cache.fetch(1) }
        }
        Thread.sleep(100)

        expect(2) { dbReadCount }
        parallel(5) {
            expect("ONE") { cache.fetch(1, Consistency.STRONG) }
        }
        expect(2) { dbReadCount }
    }

    @Test
    fun testFetchWeakOnly() {
        expect(0) { dbReadCount }
        expect("One") {cache.fetch(1) }
        expect(1) { dbReadCount }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)

        parallel(5) {
            expect("One") { cache.fetch(1) }
        }
        Thread.sleep(100)
        expect(2) { dbReadCount }

        Thread.sleep(1000)
        parallel(5) {
            expect("ONE") { cache.fetch(1) }
        }
        expect(2) { dbReadCount }
    }

    @Test
    fun testFetchTryStrongOnly() {
        expect(0) { dbReadCount }
        expect("One") {cache.fetch(1) }
        expect(1) { dbReadCount }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)

        parallel(5) {
            try {
                expect("One") { cache.fetch(1, Consistency.TRY_STRONG) }
                fail("Expect ${DirtyCacheException::class.qualifiedName}")
            } catch (ignored: DirtyCacheException) {
            }
        }
        Thread.sleep(100)
        expect(2) { dbReadCount }

        Thread.sleep(1000)
        parallel(5) {
            expect("ONE") { cache.fetch(1, Consistency.TRY_STRONG) }
        }
        expect(2) { dbReadCount }
    }

    @Test
    fun testFetchAllWithMixMode() {
        expect(0) { dbReadCount }
        expect(
            mapOf(
                1 to "One",
                2 to "Two",
                3 to "Three"
            )
        ) { cache.fetchAll(listOf(1, 2, 3, 4)) }
        expect(1) { dbReadCount }
        expect(
            mapOf(
                1 to "One",
                2 to "Two",
                3 to "Three"
            )
        ) { cache.fetchAll(listOf(1, 2, 3, 4), Consistency.STRONG) }
        expect(1) { dbReadCount }
        dbMap[1] = "ONE"
        dbMap[2] = "TWO"
        dbMap[3] = "THREE"
        cache.tagAllAsDeleted(listOf(1, 2, 3, 4))

        Thread.sleep(100)

        parallel(5) {
            expect(
                mapOf(
                    1 to "One",
                    2 to "Two",
                    3 to "Three"
                )
            ) { cache.fetchAll(listOf(1, 2, 3, 4)) }
        }
        Thread.sleep(100)
        expect(2) { dbReadCount }

        parallel(5) {
            expect(
                mapOf(
                    1 to "ONE",
                    2 to "TWO",
                    3 to "THREE"
                )
            ) { cache.fetchAll(listOf(1, 2, 3, 4), Consistency.STRONG) }
        }
        expect(2) { dbReadCount }
    }

    @Test
    fun testFetchAllWeakOnly() {
        expect(0) { dbReadCount }
        expect(
            mapOf(
                1 to "One",
                2 to "Two",
                3 to "Three"
            )
        ) { cache.fetchAll(listOf(1, 2, 3, 4)) }
        expect(1) { dbReadCount }
        dbMap[1] = "ONE"
        dbMap[2] = "TWO"
        dbMap[3] = "THREE"
        cache.tagAllAsDeleted(listOf(1, 2, 3, 4))

        Thread.sleep(100)

        parallel(5) {
            expect(
                mapOf(
                    1 to "One",
                    2 to "Two",
                    3 to "Three"
                )
            ) { cache.fetchAll(listOf(1, 2, 3, 4)) }
        }
        Thread.sleep(100)
        expect(2) { dbReadCount }

        Thread.sleep(1000)
        parallel(5) {
            expect(
                mapOf(
                    1 to "ONE",
                    2 to "TWO",
                    3 to "THREE"
                )
            ) { cache.fetchAll(listOf(1, 2, 3, 4)) }
        }
        expect(2) { dbReadCount }
    }

    @Test
    fun testFetchAllTryStrongOnly() {
        expect(0) { dbReadCount }
        expect(
            mapOf(
                1 to "One",
                2 to "Two",
                3 to "Three"
            )
        ) { cache.fetchAll(listOf(1, 2, 3, 4)) }
        expect(1) { dbReadCount }
        dbMap[1] = "ONE"
        dbMap[2] = "TWO"
        dbMap[3] = "THREE"
        cache.tagAllAsDeleted(listOf(1, 2, 3, 4))

        Thread.sleep(100)

        parallel(5) {
            try {
                expect(
                    mapOf(
                        1 to "One",
                        2 to "Two",
                        3 to "Three"
                    )
                ) { cache.fetchAll(listOf(1, 2, 3, 4), Consistency.TRY_STRONG) }
                fail("Expect ${DirtyCacheException::class.qualifiedName}")
            } catch (ignored: DirtyCacheException) {

            }
        }
        Thread.sleep(100)
        expect(2) { dbReadCount }

        Thread.sleep(1000)
        parallel(5) {
            expect(
                mapOf(
                    1 to "ONE",
                    2 to "TWO",
                    3 to "THREE"
                )
            ) { cache.fetchAll(listOf(1, 2, 3, 4), Consistency.TRY_STRONG) }
        }
        expect(2) { dbReadCount }
    }

    @Test
    fun testTryStrongFetch() {
        parallel(5) {
            expect("One") { cache.fetch(1, Consistency.TRY_STRONG) }
        }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)
        Thread.sleep(100)
        parallel(5) {
            try {
                cache.fetch(1, Consistency.TRY_STRONG)
                fail("Expects ${DirtyCacheException::class.qualifiedName}")
            } catch (ignored: DirtyCacheException) {
            }
        }

        Thread.sleep(1100)
        expect("ONE") {cache.fetch(1, Consistency.TRY_STRONG) }
        expect(2) { dbReadCount }
    }

    @Test
    fun testLockForUpdate() {
        val threadCount = 2
        val logLock = ReentrantLock()
        val executorService = Executors.newFixedThreadPool(threadCount)
        val reentrantCount = AtomicInteger(0)
        for (i in 1..threadCount) {
            executorService.execute {
                cache.lockAllOperator(listOf(1, 2, 4), UUID.randomUUID().toString()).apply {
                    lock(Duration.ofSeconds(4))
                    try {
                        expect(0) { reentrantCount.getAndIncrement() }
                        Thread.sleep(2000)
                        expect(0) { reentrantCount.decrementAndGet() }
                    } finally {
                        unlock()
                    }
                }
            }
        }
        Thread.sleep(2000L * (threadCount + 1))
    }

    private fun parallel(threadCount: Int, action: () -> Unit) {
        var throwableRef = AtomicReference<Throwable>()
        Executors.newFixedThreadPool(threadCount).invokeAll(
            (1..threadCount).map {
                val runnable: Callable<Void> = Callable {
                    try {
                        action()
                    } catch (ex: Throwable) {
                        throwableRef.compareAndSet(null, ex)
                    }
                    null
                }
                runnable
            }
        )
        val throwable = throwableRef.get()
        if (throwable != null) {
            throw throwable
        }
    }
}