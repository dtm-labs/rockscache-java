package io.github.dtm.cache.spi

/**
 * @author 陈涛
 */
interface RedisProvider {

    fun eval(lua: ByteArray, keys: List<String>, args: List<Any?>): Any?

    fun delete(keys: Collection<String>)

    fun waitReplicas(replicas: Int, timeout: Long): Long
}