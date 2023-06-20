package com.amazon.ivs.stagesrealtime.repository.networking

import okhttp3.Interceptor
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class ErrorInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        try {
            val response = chain.proceed(request)
            val bodyString = response.body?.string()
            // Check response for errors or anything else you want to catch out before passing to repo.
            // You should throw exception here by yourself so the 'catch' block executes.
            // val interceptedResponse = bodyString.asObject<InterceptedResponse>()

            // Rebuild the body
            val body = bodyString?.toResponseBody(response.body?.contentType())
            return response.newBuilder().body(body).build()
        } catch (exception: Exception) {
            // Do additional exception modification and throw it
            throw exception
        }
    }
}
