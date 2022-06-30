package io.github.dtm.cache.impl

import io.github.dtm.cache.LockScope
import io.github.dtm.cache.Options
import java.time.Duration

/**
 * @author 陈涛
 */
internal class LockScopeImpl(
    private val client: CacheClientImpl,
    private val options: Options,
    redisKeys: Set<String>,
    private val owner: String,
) : LockScope {

    private val redisKeys: List<String> = redisKeys.toList()

    fun tryLock(waitTimeout: Duration, leaseTimeout: Duration): Boolean {
        if (leaseTimeout < Duration.ofSeconds(1)) {
            throw IllegalArgumentException("leaseTime cannot be shorter than 1 second")
        }
        val maxWaitMillis = System.currentTimeMillis() + waitTimeout.toMillis()
        while (true) {
            val nowMillis = System.currentTimeMillis()
            val untilMillis = nowMillis + leaseTimeout.toMillis()
            val result = client.provider.eval(
                LUA_LOCK,
                redisKeys,
                listOf(
                    owner,
                    nowMillis.toString(),
                    untilMillis.toString(),
                    leaseTimeout.seconds.toString()
                )
            )
            if (result == "LOCKED") {
                return true
            }
            if (System.currentTimeMillis() >= maxWaitMillis) {
                return false
            }
            Thread.sleep(options.lockSleep.toMillis())
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
        """.trimMargin()

        @JvmStatic
        private val LUA_UNLOCK = """-- luaLock
            |for index, key in ipairs(KEYS) do
            |    local lo = redis.call('HGET', key, 'lockOwner')
	        |    if lo == ARGV[1] then
		    |        redis.call('DEL', key)
	        |    end
            |end
        """.trimMargin()
    }
}