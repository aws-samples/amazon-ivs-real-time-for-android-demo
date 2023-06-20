package com.amazon.ivs.stagesrealtime.di

import android.content.Context
import com.amazon.ivs.stagesrealtime.repository.*
import com.amazon.ivs.stagesrealtime.repository.chat.ChatManager
import com.amazon.ivs.stagesrealtime.repository.networking.NetworkClient
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
    fun provideStageRepository(
        @ApplicationContext context: Context,
        preferenceProvider: PreferenceProvider,
        networkClient: NetworkClient,
        chatManager: ChatManager
    ) = StageRepository(context, preferenceProvider, networkClient, chatManager)

    @Provides
    @Singleton
    fun providePreferenceProvider(
        @ApplicationContext context: Context
    ) = PreferenceProvider(context)

    @Provides
    @Singleton
    fun provideNetworkClient(
        preferenceProvider: PreferenceProvider
    ) = NetworkClient(preferenceProvider)

    @Singleton
    @Provides
    fun provideChatManager() = ChatManager()
}
