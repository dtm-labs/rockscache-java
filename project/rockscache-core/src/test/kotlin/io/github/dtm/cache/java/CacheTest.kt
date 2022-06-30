package io.github.dtm.cache.java

import io.github.dtm.cache.Cache
import io.github.dtm.cache.CacheClient
import io.github.dtm.cache.Consistency
import io.github.dtm.cache.DirtyCacheException
import io.github.dtm.cache.spi.JedisProvider
import redis.clients.jedis.JedisPool
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.*

class CacheTest {

    private val provider = JedisProvider(JedisPool("localhost", 6379))

    private lateinit var cacheClient: CacheClient

    private lateinit var cache: Cache<Int, String>

    private lateinit var dbMap: MutableMap<Int, String>

    private var dbReadCount = 0

    @BeforeTest
    @Suppress("UNCHECKED_CAST")
    fun init() {
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
                    keys.associateBy({it}) {
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

    @Test
    fun testFetch() {
        expect(0) { dbReadCount }
        expect("One") {cache.fetch(1) }
        expect(1) { dbReadCount }
        expect("One") {cache.fetch(1, Consistency.STRONG) }
        expect(1) { dbReadCount }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)
        Thread.sleep(100)
        for (i in 0..5) {
            expect("One") { cache.fetch(1) }
        }
        expect(2) { dbReadCount }
        for (i in 0..5) {
            expect("ONE") { cache.fetch(1, Consistency.STRONG) }
        }
        expect(2) { dbReadCount }
    }

    @Test
    fun testFetchAll() {
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

        for (i in 0..5) {
            expect(
                mapOf(
                    1 to "One",
                    2 to "Two",
                    3 to "Three"
                )
            ) { cache.fetchAll(listOf(1, 2, 3, 4)) }
        }
        expect(2) { dbReadCount }

        for (i in 0..5) {
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
    fun testAllowDirtyCacheException() {
        for (i in 0..5) {
            expect("One") { cache.fetch(1, Consistency.TRY_STRONG) }
        }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)
        Thread.sleep(100)
        for (i in 0..5) {
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
        val threadCount = 3
        val logs = mutableListOf<String>()
        val logLock = ReentrantLock()
        val executorService = Executors.newFixedThreadPool(threadCount)
        for (i in 1..threadCount) {
            Thread.sleep(10)
            executorService.execute {
                cache.tryLockAll(listOf(1, 2, 4), Duration.ofSeconds(4), Duration.ofSeconds(6))?.execute {
                    logLock.withLock { logs += "enter-$i" }
                    Thread.sleep(2000)
                    logLock.withLock { logs += "leave-$i" }
                }
            }
        }
        Thread.sleep(6500)
        expect("[enter-1, leave-1, enter-2, leave-2, enter-3, leave-3]") {
            logs.toString()
        }
    }
}