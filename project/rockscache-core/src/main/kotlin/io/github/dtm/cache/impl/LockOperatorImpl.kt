package io.github.dtm.cache.impl

import io.github.dtm.cache.LockException
import io.github.dtm.cache.LockOperator
import io.github.dtm.cache.Options
import java.time.Duration

/**
 * @author 陈涛
 */
internal class LockOperatorImpl<K>(
    private val client: CacheClientImpl,
    private val options: Options,
    private val owner: String,
    redisKeys: Set<String>
): LockOperator<K> {

    private val redisKeys = redisKeys.toList()

    override fun lock(waitTimeout: Duration) {
        val latestStartMillis = System.currentTimeMillis() + waitTimeout.toMillis()
        try {
            while (true) {
                val nowMillis = System.currentTimeMillis()
                val untilMillis = nowMillis + LEASE_TIMEOUT.toMillis()
                val result = client.provider.eval(
                    LUA_LOCK,
                    redisKeys,
                    listOf(
                        owner,
                        nowMillis.toString(),
                        untilMillis.toString(),
                        LEASE_TIMEOUT.seconds.toString()
                    )
                )
                val status = if (result is List<*>) {
                    result[0]
                } else {
                    result
                }
                if (LOCKED_BYTES.contentEquals(status as ByteArray)) {
                    return
                }
                if (System.currentTimeMillis() >= latestStartMillis) {
                    throw LockException(redisKeys)
                }
                Thread.sleep(options.lockSleep.toMillis())
            }
        } catch (ex: Throwable) {
            if (ex is LockException) {
                throw ex
            }
            throw LockException(redisKeys, ex)
        }
    }

    override fun unlock() {
        client.provider.eval(LUA_UNLOCK, redisKeys, listOf(owner))
    }

    companion object {

        @JvmStatic
        private val LUA_LOCK = """
            |local exists_arr = {}
            |local lu_arr = {}
            |local lo_arr = {}
            |for index, key in ipairs(KEYS) do
            |    local exists = redis.call('EXISTS', key)
            |    local lu = redis.call('HGET', key, 'lockUntil')
            |    local lo = redis.call('HGET', key, 'lockOwner')
            |    if lu == false or tonumber(lu) < tonumber(ARGV[2]) or lo == ARGV[1] then
            |        exists_arr[index] = exists
            |        lu_arr[index] = lu
            |        lo_arr[index] = lo
            |        redis.call('HSET', key, 'lockUntil', ARGV[3])
            |        redis.call('HSET', key, 'lockOwner', ARGV[1])
            |        if exists == 0 then
            |            redis.call('EXPIRE', key, ARGV[4])
            |        end
            |    else
            |        for undo_index = 1, index - 1 do
            |            if exists_arr[undo_index] == 0 then
            |                redis.call('DEL', KEYS[undo_index])
            |            else
            |                redis.call('HSET', KEYS[undo_index], 'lockUntil', lu_arr[undo_index])
            |                redis.call('HSET', KEYS[undo_index], 'lockOwner', lo_arr[undo_index])    
            |            end
            |        end
            |        return 'FAILED'
            |    end
            |end
            |return 'LOCKED'
        """.trimMargin().toByteArray()

        @JvmStatic
        private val LUA_UNLOCK = """-- luaLock
            |for index, key in ipairs(KEYS) do
            |    local lo = redis.call('HGET', key, 'lockOwner')
            |    if lo == ARGV[1] then
            |        redis.call('DEL', key)
            |    end
            |end
        """.trimMargin().toByteArray()

        @JvmStatic
        private val LOCKED_BYTES = "LOCKED".toByteArray()

        @JvmStatic
        private val LEASE_TIMEOUT = Duration.ofHours(24)
    }
}