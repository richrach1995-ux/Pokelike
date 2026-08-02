package com.runeveil.saga.di

import android.content.Context
import com.runeveil.saga.audio.AudioEngine
import com.runeveil.saga.data.di.ApplicationScope
import com.runeveil.saga.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * App-level bindings. Everything data-related is provided by
 * `com.runeveil.saga.data.di` — this module only owns things that need an
 * Android UI/media context.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioEngine(
        @ApplicationContext context: Context,
        settings: SettingsRepository,
        @ApplicationScope scope: CoroutineScope,
    ): AudioEngine = AudioEngine(context, settings, scope)
}
