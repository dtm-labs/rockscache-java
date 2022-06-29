package io.github.dtm.cache.java

import io.github.dtm.cache.Cache
import io.github.dtm.cache.CacheClient
import io.github.dtm.cache.Consistency
import io.github.dtm.cache.DirtyCacheException
import io.github.dtm.cache.spi.JedisProvider
import redis.clients.jedis.JedisPool
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.expect

class FetchTest {

    private val provider = JedisProvider(JedisPool("localhost", 6379))

    private lateinit var cache: Cache<Int, String>

    private lateinit var dbMap: MutableMap<Int, String>

    @BeforeTest
    @Suppress("UNCHECKED_CAST")
    fun init() {
        provider.delete(
            listOf(
                "test-scope-int-to-str-1",
                "test-scope-int-to-str-2",
                "test-scope-int-to-str-3"
            )
        )
        cache = CacheClient
            .newBuilder()
            .setKeyPrefix("test-scope-")
            .setProvider(provider)
            .build()
            .newCache("int-to-str-", Int::class, String::class) {
                loader = { keys ->
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
    }

    @Test
    fun testFetch() {
        expect("One") {cache.fetch(1) }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)
        Thread.sleep(100)
        for (i in 0..5) {
            expect("One") { cache.fetch(1) }
        }
        for (i in 0..5) {
            expect("ONE") { cache.fetch(1, Consistency.STRONG) }
        }
    }

    @Test
    fun testFetchAll() {
        expect(
            mapOf(
                1 to "One",
                2 to "Two",
                3 to "Three"
            )
        ) { cache.fetchAll(listOf(1, 2, 3)) }
        dbMap[1] = "ONE"
        dbMap[2] = "TWO"
        dbMap[3] = "THREE"
        cache.tagAllAsDeleted(listOf(1, 2, 3))

        Thread.sleep(100)

        for (i in 0..5) {
            expect(
                mapOf(
                    1 to "One",
                    2 to "Two",
                    3 to "Three"
                )
            ) { cache.fetchAll(listOf(1, 2, 3)) }
        }

        for (i in 0..5) {
            expect(
                mapOf(
                    1 to "ONE",
                    2 to "TWO",
                    3 to "THREE"
                )
            ) { cache.fetchAll(listOf(1, 2, 3), Consistency.STRONG) }
        }
    }

    @Test(expected = DirtyCacheException::class)
    fun testAllowDirtyCacheException() {
        expect("One") {cache.fetch(1) }
        dbMap[1] = "ONE"
        cache.tagAsDeleted(1)
        Thread.sleep(100)
        cache.fetch(1, Consistency.ALLOW_DIRTY_CACHE_EXCEPTION)
    }
}