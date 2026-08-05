package com.pokelike.idle.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pokelike.idle.data.database.dao.GameStateDao
import com.pokelike.idle.data.database.entity.AchievementEntity
import com.pokelike.idle.data.database.entity.BuildingEntity
import com.pokelike.idle.data.database.entity.GameStateEntity
import com.pokelike.idle.data.database.entity.QuestBaselineEntity
import com.pokelike.idle.data.database.entity.QuestBaselineValueEntity
import com.pokelike.idle.data.database.entity.QuestClaimEntity
import com.pokelike.idle.data.database.entity.ResourceEntity
import com.pokelike.idle.data.database.entity.UpgradeEntity

/**
 * Room-Datenbank der App.
 *
 * `exportSchema` ist eingeschaltet, und der Ablageort ist in der
 * `build.gradle.kts` gesetzt. Die erzeugten JSON-Dateien werden eingecheckt:
 * Sie sind die Grundlage automatisierter Migrationstests und die einzige
 * verlaessliche Auskunft darueber, wie das Schema in einer bereits
 * veroeffentlichten Version aussah.
 *
 * Zur Versionierung: [DATABASE_VERSION] beschreibt den Tabellenaufbau,
 * [com.pokelike.idle.domain.model.GameState.CURRENT_SCHEMA_VERSION] den Inhalt
 * des Spielstands. Beides ist bewusst getrennt. Der weitaus haeufigere Fall ist
 * eine inhaltliche Aenderung - eine neue Ressource, ein neues Statistikfeld -,
 * und die kommt ohne Datenbankmigration aus, weil Ressourcen als Zeilen und
 * nicht als Spalten gespeichert werden.
 */
@Database(
    entities = [
        GameStateEntity::class,
        ResourceEntity::class,
        BuildingEntity::class,
        UpgradeEntity::class,
        AchievementEntity::class,
        QuestBaselineEntity::class,
        QuestBaselineValueEntity::class,
        QuestClaimEntity::class,
    ],
    version = PokelikeDatabase.DATABASE_VERSION,
    exportSchema = true,
)
abstract class PokelikeDatabase : RoomDatabase() {

    abstract fun gameStateDao(): GameStateDao

    companion object {

        /**
         * Version 2 hat die Tabelle `buildings` ergaenzt, Version 3 die
         * Tabelle `upgrades`, Version 4 Achievements, Quests und einen Zaehler
         * in `game_state`. Die zugehoerigen Migrationen stehen in
         * [com.pokelike.idle.data.database.migration.DatabaseMigrations].
         */
        const val DATABASE_VERSION: Int = 4

        /**
         * Dateiname der Datenbank.
         *
         * Muss mit den Ausschluessen in `backup_rules.xml` und
         * `data_extraction_rules.xml` uebereinstimmen, sonst landet der
         * Spielstand doch im Geraete-Backup und liesse sich vervielfaeltigen.
         */
        const val DATABASE_NAME: String = "pokelike-database"
    }
}
