@file:JvmName("Utils")
package io.github.dtm.cache.impl

fun <E> split(set: Set<E>, batchSize: Int, handler: (List<E>) -> Unit) {
    if (set.size < batchSize) {
        handler(set.toList())
    } else {
        var list = mutableListOf<E>()
        for (e in set) {
            list.add(e)
            if (list.size >= batchSize) {
                handler(list)
                list = mutableListOf<E>()
            }
        }
        if (list.isNotEmpty()) {
            handler(list)
        }
    }
}