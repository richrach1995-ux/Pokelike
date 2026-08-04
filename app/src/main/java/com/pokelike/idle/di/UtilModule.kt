package com.pokelike.idle.di

import com.pokelike.idle.util.AndroidGameLogger
import com.pokelike.idle.util.DefaultDispatcherProvider
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.GameLogger
import com.pokelike.idle.util.SystemTimeSource
import com.pokelike.idle.util.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindet die Infrastruktur-Interfaces an ihre Produktionsimplementierungen.
 *
 * [Binds] statt [dagger.Provides], weil hier lediglich ein Interface auf eine
 * bereits injizierbare Klasse abgebildet wird. Dagger erzeugt dafuer keinen
 * zusaetzlichen Factory-Code.
 *
 * Tests ersetzen dieses Modul ueber `@TestInstallIn` und schieben damit eine
 * kontrollierbare Zeitquelle bzw. Test-Dispatcher unter, ohne dass Produktions-
 * code davon etwas mitbekommt.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        implementation: DefaultDispatcherProvider,
    ): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindTimeSource(
        implementation: SystemTimeSource,
    ): TimeSource

    @Binds
    @Singleton
    abstract fun bindGameLogger(
        implementation: AndroidGameLogger,
    ): GameLogger
}
