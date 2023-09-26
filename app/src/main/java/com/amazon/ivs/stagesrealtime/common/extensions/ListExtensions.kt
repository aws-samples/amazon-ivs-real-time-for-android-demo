package com.amazon.ivs.stagesrealtime.common.extensions

fun <T> List<T>.getByIndexOrFirst(index: Int) = try {
    getOrElse(index) { firstOrNull() }
} catch (e: Exception) {
    null
}

fun <T> List<T>.getByIndexOrLast(index: Int) = try {
    getOrElse(index) { lastOrNull() }
} catch (e: Exception) {
    null
}
