package io.github.dtm.cache.spi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

interface KeySerializer<T> {
    
    fun serialize(key: T): String
    
    companion object {

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>): KeySerializer<T> =
            jackson(type, ObjectMapper())

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>, mapper: ObjectMapper): KeySerializer<T> {
            return object : KeySerializer<T> {
                override fun serialize(value: T): String =
                    mapper.writeValueAsString(value)
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
            jackson(type.java, mapper ?: jacksonObjectMapper())
    }
}