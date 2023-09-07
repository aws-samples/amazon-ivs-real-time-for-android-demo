package com.amazon.ivs.stagesrealtime.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * This file is used for creating an [AppSettings] [DataStore] by using [kotlinx.serialization]
 * to allow easy use of declaring our settings object and updating it with minimal boilerplate.
 *
 * The Stages realtime sample app uses only [AppSettings] for everything we need to store here,
 * however you can adapt this to your app either by using one object, or turning the [AppSettingsSerializer]
 * into a generic class: to do so, follow these steps:
 *
 * 1. Replace all mentions of AppSettings type into the generic T type
 * 2. Turn the object AppSettingsSerializer into a class, and rename to be more precise,
 * like JsonDataStoreSerializer
 * 3. Take the serializer: KSerializer<T> and createDefaultValue: () -> T as new arguments in the constructor
 * 4. Replace the defaultValue to call the function, and the serializer with the new member variable
 * 5. Use the [Context.appSettingsStore] function by passing in your custom type and creating a new
 * instance of your JsonDataStoreSerializer, passing in the serializer as with [AppSettings.serializer]
 * and default value with the call to your constructor.
 */

private const val APP_SETTINGS_DATA_STORE_FILE_NAME = "appSettings.json"

object AppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings
        get() = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            val settings = Json.decodeFromString(
                deserializer = AppSettings.serializer(),
                string = input.readBytes().decodeToString()
            )
            Timber.v("Retrieving app settings: $settings")
            settings
        } catch (e: SerializationException) {
            Timber.e(e, "Failed to serialize app settings")
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        Timber.v("Saving new app settings: $t")
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(
                    serializer = AppSettings.serializer(),
                    value = t
                ).encodeToByteArray()
            )
        }
    }
}

val Context.appSettingsStore: DataStore<AppSettings> by dataStore(
    fileName = APP_SETTINGS_DATA_STORE_FILE_NAME,
    serializer = AppSettingsSerializer
)
