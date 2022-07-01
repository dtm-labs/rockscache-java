package io.github.dtm.cache.spi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

/**
 * @author 陈涛
 */
interface ValueSerializer<V> {

    fun serialize(value: V): ByteArray

    fun deserialize(bytes: ByteArray): V

    companion object {

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <V> jackson(type: Class<V>): ValueSerializer<V> =
            jackson(type, ObjectMapper().registerModule(JavaTimeModule()))

        /**
         * For java, not kotlin
         */
        @JvmStatic
        fun <V> jackson(type: Class<V>, mapper: ObjectMapper): ValueSerializer<V> {
            return object : ValueSerializer<V> {

                override fun serialize(value: V): ByteArray =
                    mapper.writeValueAsBytes(value)

                override fun deserialize(content: ByteArray): V =
                    mapper.readValue(content, type)
            }
        }

        /**
         * For kotlin, not java
         */
        @JvmStatic
        fun <V: Any> jackson(
            type: KClass<V>,
            mapper: ObjectMapper? = null
        ): ValueSerializer<V> =
            jackson(
                type.java,
                mapper
                    ?: jacksonObjectMapper()
                        .registerModule(JavaTimeModule())
            )
    }
}