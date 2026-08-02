package com.runeveil.saga.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.runeveil.saga.data.content.ContentRepositoryImpl
import com.runeveil.saga.data.database.AchievementDao
import com.runeveil.saga.data.database.BestiaryDao
import com.runeveil.saga.data.database.GameStateDao
import com.runeveil.saga.data.database.InventoryDao
import com.runeveil.saga.data.database.MonsterDao
import com.runeveil.saga.data.database.QuestDao
import com.runeveil.saga.data.database.RuneveilDatabase
import com.runeveil.saga.data.database.WorldStateDao
import com.runeveil.saga.data.repository.AchievementRepositoryImpl
import com.runeveil.saga.data.repository.BestiaryRepositoryImpl
import com.runeveil.saga.data.repository.InventoryRepositoryImpl
import com.runeveil.saga.data.repository.MonsterRepositoryImpl
import com.runeveil.saga.data.repository.PlayerRepositoryImpl
import com.runeveil.saga.data.repository.QuestRepositoryImpl
import com.runeveil.saga.data.repository.SaveRepositoryImpl
import com.runeveil.saga.data.repository.WorldStateRepositoryImpl
import com.runeveil.saga.data.settings.SettingsRepositoryImpl
import com.runeveil.saga.domain.battle.BattleContent
import com.runeveil.saga.domain.repository.AchievementRepository
import com.runeveil.saga.domain.repository.BestiaryRepository
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.InventoryRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.repository.QuestRepository
import com.runeveil.saga.domain.repository.SaveRepository
import com.runeveil.saga.domain.repository.SettingsRepository
import com.runeveil.saga.domain.repository.WorldStateRepository
import com.runeveil.saga.domain.util.Rng
import com.runeveil.saga.domain.util.SystemRng
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the dispatcher used for disk and parsing work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Marks the dispatcher used for CPU-bound rule evaluation. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Application-scoped coroutine scope for fire-and-forget work (autosave). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoreDataModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = false
        allowStructuredMapKeys = false
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    @Provides
    @Singleton
    fun provideRng(): Rng = SystemRng()

    // --- Room ------------------------------------------------------------

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RuneveilDatabase =
        Room.databaseBuilder(context, RuneveilDatabase::class.java, RuneveilDatabase.NAME)
            // No destructive fallback: a lost save is unacceptable, so a missing
            // migration must fail loudly in development instead of wiping data.
            .build()

    @Provides fun provideMonsterDao(db: RuneveilDatabase): MonsterDao = db.monsterDao()
    @Provides fun provideInventoryDao(db: RuneveilDatabase): InventoryDao = db.inventoryDao()
    @Provides fun provideQuestDao(db: RuneveilDatabase): QuestDao = db.questDao()
    @Provides fun provideBestiaryDao(db: RuneveilDatabase): BestiaryDao = db.bestiaryDao()
    @Provides fun provideAchievementDao(db: RuneveilDatabase): AchievementDao = db.achievementDao()
    @Provides fun provideWorldStateDao(db: RuneveilDatabase): WorldStateDao = db.worldStateDao()
    @Provides fun provideGameStateDao(db: RuneveilDatabase): GameStateDao = db.gameStateDao()

    // --- DataStore --------------------------------------------------------

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        context.preferencesDataStoreFile("runeveil_settings")
    }

    // ContentRepositoryImpl and LocalizationRepository are constructor-injected
    // (@Inject + @Singleton), so they need no provider here.
}

/**
 * Binds the repository interfaces declared in :domain to their implementations.
 *
 * Because [ContentRepositoryImpl] satisfies both [ContentRepository] and
 * [BattleContent], the battle engine can be constructed without knowing about
 * the data layer at all.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindBattleContent(impl: ContentRepositoryImpl): BattleContent

    @Binds
    @Singleton
    abstract fun bindMonsterRepository(impl: MonsterRepositoryImpl): MonsterRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository

    @Binds
    @Singleton
    abstract fun bindBestiaryRepository(impl: BestiaryRepositoryImpl): BestiaryRepository

    @Binds
    @Singleton
    abstract fun bindWorldStateRepository(impl: WorldStateRepositoryImpl): WorldStateRepository

    @Binds
    @Singleton
    abstract fun bindSaveRepository(impl: SaveRepositoryImpl): SaveRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
