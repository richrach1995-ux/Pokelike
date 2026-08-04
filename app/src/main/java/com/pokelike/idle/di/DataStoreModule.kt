package com.pokelike.idle.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * Stellt den DataStore fuer die Einstellungen bereit.
 *
 * Erzeugt ueber [PreferenceDataStoreFactory] statt ueber die
 * `preferencesDataStore`-Delegateigenschaft: Nur so laesst sich der
 * Coroutine-Scope vorgeben. Die Delegatvariante bindet sich an einen internen
 * Scope, den Tests nicht kontrollieren koennen.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Dateiname des Einstellungsspeichers.
     *
     * Muss mit dem Eintrag in `backup_rules.xml` uebereinstimmen - dort ist
     * diese Datei als einzige ausdruecklich fuer das Geraete-Backup
     * freigegeben.
     */
    private const val SETTINGS_FILE_NAME = "pokelike_settings"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE_NAME) },
    )
}
