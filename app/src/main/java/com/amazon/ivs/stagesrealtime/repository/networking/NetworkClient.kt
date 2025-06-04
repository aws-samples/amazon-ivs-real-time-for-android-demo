package com.amazon.ivs.stagesrealtime.repository.networking

import androidx.datastore.core.DataStore
import com.amazon.ivs.stagesrealtime.BuildConfig
import com.amazon.ivs.stagesrealtime.common.REQUEST_TIMEOUT
import com.amazon.ivs.stagesrealtime.common.extensions.json
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An [Api] network client factory for retrieving an object to communicate with the backend,
 * which is automatically recreated if the [AppSettings.customerCode] is changed.
 *
 * Any class requiring access to the backend should take the [NetworkClient] as a constructor argument
 * and use the [Api] as follows:
 * ```kt
 * private val api get() = networkClient.getOrCreateApi()
 *
 * suspend fun verifyConnectionCode() {
 *     // it will automatically reuse the created API instance or create a new one if needed
 *     api.verifyConnectionCode()
 * }
 * ```
 */
@Singleton
class NetworkClient @Inject constructor(private val appSettingsStore: DataStore<AppSettings>) {
    private var _api: Api? = null
    private var _currentBaseUrl = ""

    suspend fun getOrCreateApi(): Api {
        val customerCode = appSettingsStore.data.first().customerCode
        val api = if (_currentBaseUrl == customerCode && _api != null) {
            _api!!
        } else {
            _currentBaseUrl = customerCode ?: ""
            val url = "https://$_currentBaseUrl.cloudfront.net/"
            Timber.d("Creating API: $url")
            val retrofitClient = Retrofit.Builder()
                .client(getOkHttpClient())
                .baseUrl(url)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            retrofitClient.create(Api::class.java)
        }
        _api = api
        return api
    }

    fun destroyApi() {
        _api = null
        _currentBaseUrl = ""
    }

    private suspend fun getOkHttpClient(): OkHttpClient {
        // runBlocking is used to have the same synchronous behaviour as with SharedPreferences
        val apiKey = appSettingsStore.data.first().apiKey
        Timber.d("Adding header: $apiKey")
        val builder = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val updatedRequest = chain
                    .request()
                    .newBuilder()
                    .addHeader("x-api-key", apiKey ?: "")
                    .build()
                chain.proceed(updatedRequest)
            }
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)
        }
        return builder.build()
    }
}
