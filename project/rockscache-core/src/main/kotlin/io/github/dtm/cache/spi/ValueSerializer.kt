package io.github.dtm.cache.spi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

interface ValueSerializer<T> {

    fun serialize(value: T): String

    fun deserialize(bytes: String): T

    companion object {

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>): ValueSerializer<T> =
            jackson(type, ObjectMapper().registerModule(JavaTimeModule()))

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <T> jackson(type: Class<T>, mapper: ObjectMapper): ValueSerializer<T> {
            return object : ValueSerializer<T> {

                override fun serialize(value: T): String =
                    mapper.writeValueAsString(value)

                override fun deserialize(content: String): T =
                    mapper.readValue(content, type)
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
            jackson(
                type.java,
                mapper
                    ?: jacksonObjectMapper()
                        .registerModule(JavaTimeModule())
            )
    }
}