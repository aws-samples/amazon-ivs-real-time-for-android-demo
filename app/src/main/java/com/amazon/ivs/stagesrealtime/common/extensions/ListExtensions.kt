package com.amazon.ivs.stagesrealtime.common.extensions

fun <T> List<T>.getByIndexOrFirst(index: Int) = getOrElse(index) { firstOrNull() }
fun <T> List<T>.getByIndexOrLast(index: Int) = getOrElse(index) { lastOrNull() }
