package com.amazon.ivs.stagesrealtime.repository

import android.content.Context
import com.amazon.ivs.stagesrealtime.common.DEFAULT_VIDEO_BITRATE
import com.amazon.ivs.stagesrealtime.common.extensions.asObject
import com.amazon.ivs.stagesrealtime.common.extensions.toJson
import com.amazon.ivs.stagesrealtime.common.getNewUserAvatar
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val PREFERENCES_NAME = "StagesRTPreferences"

class PreferenceProvider(context: Context) {
    var stageId by stringPreference()
    var customerCode by stringPreference()
    var userAvatar by userAvatarPreference()
    var bitrate by bitratePreference()
    var apiKey by stringPreference()

    private val sharedPreferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    private fun stringPreference() = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(property.name, null)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            sharedPreferences.edit().putString(property.name, value).apply()
        }
    }

    private fun userAvatarPreference() = object : ReadWriteProperty<Any?, UserAvatar> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getString(property.name, null)?.asObject() ?: getNewUserAvatar()

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: UserAvatar) {
            sharedPreferences.edit().putString(property.name, value.toJson()).apply()
        }
    }

    private fun bitratePreference() = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getInt(property.name, DEFAULT_VIDEO_BITRATE)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            sharedPreferences.edit().putInt(property.name, value).apply()
        }
    }
}
