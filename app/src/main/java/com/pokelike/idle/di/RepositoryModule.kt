package com.pokelike.idle.di

import com.pokelike.idle.data.preferences.SettingsDataStoreRepository
import com.pokelike.idle.data.repository.GameRepositoryImpl
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindet die Repository-Schnittstellen der Domaenenschicht an ihre
 * Umsetzungen in der Datenschicht.
 *
 * Diese Bindung ist die Stelle, an der Clean Architecture praktisch wird:
 * Use Cases und ViewModels kennen ausschliesslich die Schnittstellen. Ein
 * spaeterer Wechsel - etwa auf ein Repository mit Cloud-Sicherung, das den
 * Server als verbindliche Quelle nutzt - beschraenkt sich auf dieses Modul.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGameRepository(implementation: GameRepositoryImpl): GameRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: SettingsDataStoreRepository,
    ): SettingsRepository
}
