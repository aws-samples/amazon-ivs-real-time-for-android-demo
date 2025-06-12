package com.amazon.ivs.stagesrealtimecompose

import android.app.Application
import android.content.Context
import com.amazon.ivs.stagesrealtimecompose.core.common.LineNumberDebugTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

lateinit var appContext: Context

@HiltAndroidApp
class App: Application() {
    override fun onCreate() {
        appContext = this
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        }
    }
}
