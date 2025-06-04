package com.amazon.ivs.stagesrealtime.di

import android.content.Context
import com.amazon.ivs.stagesrealtime.repository.appSettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StageModule {
    @Provides
    @Singleton
    fun provideAppSettingsStore(@ApplicationContext context: Context) = context.appSettingsStore
}
