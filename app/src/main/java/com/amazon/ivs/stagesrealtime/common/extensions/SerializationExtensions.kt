package com.amazon.ivs.stagesrealtime.common.extensions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

inline fun <reified T> T.toJson() = json.encodeToString(this)

inline fun <reified T> String.asObject(): T = json.decodeFromString(this)

inline fun <reified T> Map<String, String>.asObject(): T = JSONObject(this).toString().asObject()
