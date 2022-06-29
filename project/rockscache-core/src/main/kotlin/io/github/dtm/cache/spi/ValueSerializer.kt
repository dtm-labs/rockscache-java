package io.github.dtm.cache.spi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

interface ValueSerializer<T> {

    fun serialize(value: T): ByteArray

    fun deserialize(bytes: ByteArray): T

    companion object {

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>): ValueSerializer<T> =
            jackson(type, ObjectMapper())

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>, mapper: ObjectMapper): ValueSerializer<T> {
            return object : ValueSerializer<T> {

                override fun serialize(value: T): ByteArray =
                    mapper.writeValueAsBytes(value)

                override fun deserialize(bytes: ByteArray): T =
                    mapper.readValue(bytes, type)
            }
        }

        /**
         * For kotlin, not java
         */
        @JvmStatic
        fun <T: Any> jackson(
            type: KClass<T>,
            mapper: ObjectMapper? = null
        ): ValueSerializer<T> =
            jackson(type.java, mapper ?: jacksonObjectMapper())
    }
}