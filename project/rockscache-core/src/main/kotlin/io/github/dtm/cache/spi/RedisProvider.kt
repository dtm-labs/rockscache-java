package io.github.dtm.cache.spi

/**
 * @author 陈涛
 */
interface RedisProvider {

    fun eval(lua: String, keys: List<String>, args: List<String>): Any?

    fun delete(keys: Collection<String>)

    fun waitReplicas(replicas: Int, timeout: Long): Long
}