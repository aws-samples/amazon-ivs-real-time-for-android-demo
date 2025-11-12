package com.amazon.ivs.stagesrealtimecompose

import android.app.Application
import android.content.Context
import com.amazon.ivs.stagesrealtimecompose.core.common.LineNumberDebugTree
import timber.log.Timber

/**
 * Given the nature of the project - there is no need for dagger / hilt usage.
 * Having singleton objects instead of repositories / view models in this particular scenario has more benefits
 * than flaws.
 */
lateinit var appContext: Context

class App: Application() {
    override fun onCreate() {
        appContext = this
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        }
    }
}
