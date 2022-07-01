package io.github.dtm.cache.spi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

/**
 * @author 陈涛
 */
interface KeySerializer<K> {

    fun serialize(key: K): String

    companion object {

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <K> jackson(type: Class<K>): KeySerializer<K> =
            jackson(type, ObjectMapper().registerModule(JavaTimeModule()))

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <K> jackson(type: Class<K>, mapper: ObjectMapper): KeySerializer<K> {
            return object : KeySerializer<K> {
                override fun serialize(key: K): String =
                    mapper.writeValueAsString(key)
            }
        }

        /**
         * For kotlin, not java
         */
        @JvmStatic
        fun <K: Any> jackson(
            type: KClass<K>,
            mapper: ObjectMapper? = null
        ): KeySerializer<K> =
            jackson(
                type.java,
                mapper
                    ?: jacksonObjectMapper()
                        .registerModule(JavaTimeModule())
            )
    }
}