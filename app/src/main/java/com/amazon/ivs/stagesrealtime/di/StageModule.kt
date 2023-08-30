package com.amazon.ivs.stagesrealtime.di

import android.content.Context
import com.amazon.ivs.stagesrealtime.repository.*
import com.amazon.ivs.stagesrealtime.repository.stage.usecases.CreateStageUseCase
import com.amazon.ivs.stagesrealtime.repository.stage.usecases.CreateStageUseCaseImpl
import com.amazon.ivs.stagesrealtime.repository.stage.usecases.JoinStageUseCase
import com.amazon.ivs.stagesrealtime.repository.stage.usecases.JoinStageUseCaseImpl
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

    @Provides
    @Singleton
    fun provideStageRepository(repository: StageRepositoryImpl): StageRepository = repository

    @Provides
    @Singleton
    fun provideCreateStageUseCase(useCase: CreateStageUseCaseImpl): CreateStageUseCase = useCase

    @Provides
    @Singleton
    fun provideJoinStageUseCase(useCase: JoinStageUseCaseImpl): JoinStageUseCase = useCase
}
