package com.pokelike.idle.di

import android.content.Context
import androidx.room.Room
import com.pokelike.idle.data.database.PokelikeDatabase
import com.pokelike.idle.data.database.dao.GameStateDao
import com.pokelike.idle.data.database.migration.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Stellt Datenbank und DAO bereit.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePokelikeDatabase(
        @ApplicationContext context: Context,
    ): PokelikeDatabase = Room.databaseBuilder(
        context = context,
        klass = PokelikeDatabase::class.java,
        name = PokelikeDatabase.DATABASE_NAME,
    )
        .addMigrations(*DatabaseMigrations.ALL)
        // Bewusst KEIN fallbackToDestructiveMigration: Das wuerde bei einer
        // vergessenen Migration alle Spielstaende der Nutzerschaft loeschen -
        // erst nach dem Rollout und ohne Weg zurueck. Ein Absturz beim Start
        // faellt in der Entwicklung sofort auf; stiller Datenverlust erst in
        // den Rezensionen im Store.
        .build()

    @Provides
    @Singleton
    fun provideGameStateDao(database: PokelikeDatabase): GameStateDao = database.gameStateDao()
}
