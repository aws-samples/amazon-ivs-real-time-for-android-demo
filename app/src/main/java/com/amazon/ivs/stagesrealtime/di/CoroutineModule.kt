package com.amazon.ivs.stagesrealtime.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Injects all our needed coroutines and scopes across the application, allowing us to easily
 * replace them in testing if need be.
 *
 * (While this sample does not cover testing, this is still a highly
 * recommended practice that should be done, otherwise testing will be problematic and highly susceptible to
 * race conditions)
 *
 * For example, any class wired up the Hilt component graph (done by using @Inject constructor()),
 * these can be obtained like so:
 *
 * ```kt
 * class MyClass @Inject constructor(
 *     @IOScope ioScope: CoroutineScope,
 *     @DefaultScope defaultScope: CoroutineScope,
 *     @MainScope mainScope: MainScope
 * )
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    @Provides
    @Singleton
    @IOScope
    fun provideIOScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides
    @Singleton
    @DefaultScope
    fun provideDefaultScope(): CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Provides
    @Singleton
    @MainScope
    fun provideMainScope(): CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IOScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainScope
