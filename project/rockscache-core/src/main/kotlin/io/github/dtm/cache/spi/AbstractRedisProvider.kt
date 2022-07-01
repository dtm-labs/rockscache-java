package io.github.dtm.cache.spi

import java.lang.IllegalArgumentException

/**
 * @author 陈涛
 */
abstract class AbstractRedisProvider(
    private val mergeKeyAndArgs: Boolean
) : RedisProvider {

    @Suppress("UNCHECKED_CAST")
    final override fun eval(
        lua: ByteArray,
        keys: List<String>,
        args: List<Any?>
    ): Any? =
        if (mergeKeyAndArgs) {
            val keyAndArgs = Array<ByteArray?>(keys.size + args.size) { null }
            for (i in keys.indices) {
                keyAndArgs[i] = keys[i].toByteArray()
            }
            for (i in args.indices) {
                val arg = args[i]
                keyAndArgs[keys.size + i] = when (arg) {
                    is String -> arg.toByteArray()
                    is ByteArray -> arg
                    else -> throw IllegalArgumentException(
                        "args[$i] is neither string nor byte array"
                    )
                }
            }
            eval(
                lua,
                keys.size,
                keyAndArgs as Array<ByteArray>
            )
        } else {
            eval(
                lua,
                keys.toTypedArray(),
                args.mapIndexed { index, arg ->
                    when (arg) {
                        is String -> arg.toByteArray()
                        is ByteArray -> arg
                        else -> throw IllegalArgumentException(
                            "args[$index] is neither string nor byte array"
                        )
                    }
                }.toTypedArray()
            )
        }

    protected open fun eval(
        lua: ByteArray,
        keys: Array<String>,
        args: Array<ByteArray>
    ): Any? =
        throw NotImplementedError(
            "eval(ByteArray, Array<String>, Array<ByteArray>) is not implemented"
        )

    protected open fun eval(
        lua: ByteArray,
        keyCount: Int,
        keyAndArgs: Array<ByteArray>
    ): Any? =
        throw NotImplementedError(
            "eval(ByteArray, Int, Array<ByteArray>) is not implemented"
        )
}