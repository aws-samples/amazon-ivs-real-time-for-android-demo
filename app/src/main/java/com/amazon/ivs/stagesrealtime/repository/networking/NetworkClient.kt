package com.amazon.ivs.stagesrealtime.repository.networking

import com.amazon.ivs.stagesrealtime.BuildConfig
import com.amazon.ivs.stagesrealtime.common.REQUEST_TIMEOUT
import com.amazon.ivs.stagesrealtime.common.extensions.json
import com.amazon.ivs.stagesrealtime.repository.PreferenceProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit

class NetworkClient(private val preferenceProvider: PreferenceProvider) {
    private val okHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(ErrorInterceptor())
            .addInterceptor { chain ->
                val updatedRequest = chain
                    .request()
                    .newBuilder()
                    .addHeader("x-api-key", preferenceProvider.apiKey ?: "")
                    .build()
                chain.proceed(updatedRequest)
            }
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)
        }
        builder.build()
    }
    private var _retrofitClient: Retrofit? = null
    private var _api: Api? = null
    private var _currentBaseUrl = ""

    fun getOrCreateApi(): Api {
        val api = if (_currentBaseUrl == preferenceProvider.customerCode && _api != null) {
            _api!!
        } else {
            _currentBaseUrl = preferenceProvider.customerCode ?: ""
            val url = "https://$_currentBaseUrl.cloudfront.net/"
            Timber.d("Create API: $url")
            _retrofitClient = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(url)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            _retrofitClient!!.create(Api::class.java)
        }
        _api = api
        return api
    }

    fun destroyApi() {
        _api = null
        _retrofitClient = null
        _currentBaseUrl = ""
    }
}
