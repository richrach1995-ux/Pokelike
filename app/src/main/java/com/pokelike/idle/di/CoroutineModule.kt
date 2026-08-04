package com.pokelike.idle.di

import com.pokelike.idle.util.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Stellt den prozessweiten Coroutine-Scope bereit.
 *
 * Warum ein eigener Scope und nicht `GlobalScope`:
 * `GlobalScope` ist nicht abbrechbar und nicht testbar. Der hier erzeugte Scope
 * gehoert dem Singleton-Component und lebt damit exakt so lange wie der
 * Prozess. Arbeit, die einen Bildschirmwechsel ueberdauern muss - Autosave,
 * Spiel-Uhr, Billing-Callbacks -, gehoert hierhin.
 *
 * [SupervisorJob] ist bewusst gewaehlt: Faellt ein Kind mit einer Exception
 * aus, sollen die uebrigen weiterlaufen. Ohne Supervisor wuerde ein
 * fehlgeschlagener Analytics-Aufruf die Spiel-Uhr mit abreissen.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        dispatchers: DispatcherProvider,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)
}
