package com.amazon.ivs.stagesrealtimecompose.core.handlers

import android.content.Context
import androidx.core.content.edit
import com.amazon.ivs.stagesrealtimecompose.appContext
import com.amazon.ivs.stagesrealtimecompose.core.common.BITRATE_DEFAULT
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val PREFERENCES_NAME = "StagesRTPreferences"

object PreferencesHandler {
    private val sharedPreferences by lazy { appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    var user by stringPreference()
    var session by stringPreference()
    var bitrate by intPreference(defaultValue = BITRATE_DEFAULT.roundToInt())
    var simulcastEnabled by boolPreference()
    var videoStatsEnabled by boolPreference()

    private fun stringPreference() = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getString(property.name, null)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            sharedPreferences.edit { putString(property.name, value) }
        }
    }

    private fun intPreference(defaultValue: Int = 0) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getInt(property.name, defaultValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            sharedPreferences.edit { putInt(property.name, value) }
        }
    }

    private fun boolPreference() = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getBoolean(property.name, false)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            sharedPreferences.edit { putBoolean(property.name, value) }
        }
    }
}
