package io.github.dtm.cache.spi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

/**
 * @author 陈涛
 */
interface KeySerializer<T> {
    
    fun serialize(key: T): String
    
    companion object {

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>): KeySerializer<T> =
            jackson(type, ObjectMapper().registerModule(JavaTimeModule()))

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>, mapper: ObjectMapper): KeySerializer<T> {
            return object : KeySerializer<T> {
                override fun serialize(key: T): String =
                    mapper.writeValueAsString(key)
            }
        }

        /**
         * For kotlin, not java
         */
        @JvmStatic
        fun <T: Any> jackson(
            type: KClass<T>,
            mapper: ObjectMapper? = null
        ): KeySerializer<T> =
            jackson(
                type.java,
                mapper
                    ?: jacksonObjectMapper()
                        .registerModule(JavaTimeModule())
            )
    }
}