package com.pokelike.idle.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Alle Datenbankmigrationen.
 *
 * Bewusst von Hand geschrieben und nicht als automatische Migration: Eine
 * automatische Migration wird aus den exportierten Schemadateien abgeleitet und
 * ist damit nur so verlaesslich wie deren Pflege. Eine ausformulierte Migration
 * steht im Code, laesst sich lesen und pruefen, und ihr Verhalten aendert sich
 * nicht, wenn jemand vergisst, ein Schema einzuchecken.
 *
 * Regel fuer alle kuenftigen Migrationen: Eine bereits veroeffentlichte
 * Migration wird nie mehr geaendert. Wer sie anpasst, veraendert rueckwirkend
 * die Datenbank derjenigen, die sie noch nicht durchlaufen haben.
 */
object DatabaseMigrations {

    /**
     * Version 1 auf 2: Gebaeude.
     *
     * Legt lediglich eine Tabelle an. Bestehende Spielstaende bleiben
     * unveraendert und starten mit einem leeren Gebaeudebestand - genau das
     * richtige Verhalten, denn vor dieser Version gab es keine Gebaeude.
     *
     * Die Anweisung muss dem entsprechen, was Room aus der Entity erzeugt. Room
     * prueft das Schema beim Oeffnen und bricht bei einer Abweichung ab; ein
     * Fehler faellt deshalb sofort auf und nicht erst beim Schreiben.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `buildings` " +
                    "(`buildingId` TEXT NOT NULL, `owned` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`buildingId`))",
            )
        }
    }

    /**
     * Version 2 auf 3: Upgrades.
     *
     * Wie bei den Gebaeuden nur eine zusaetzliche Tabelle. Bestehende
     * Spielstaende starten ohne Upgrades - vor dieser Version gab es keine.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `upgrades` " +
                    "(`upgradeId` TEXT NOT NULL, PRIMARY KEY(`upgradeId`))",
            )
        }
    }

    /**
     * Version 3 auf 4: Achievements und Quests.
     *
     * Die einzige bisherige Migration, die eine bestehende Tabelle anfasst:
     * `game_state` bekommt einen Zaehler fuer je gekaufte Gebaeude. Er wird mit
     * null vorbelegt - fuer Altbestaende ist das die einzig moegliche Antwort,
     * denn die Zahl laesst sich rueckwirkend nicht ermitteln. Der aktuelle
     * Bestand waere ein falscher Ersatz: Er faellt beim Prestige auf null.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `game_state` ADD COLUMN `totalBuildingsPurchased` " +
                    "INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `achievements` " +
                    "(`achievementId` TEXT NOT NULL, PRIMARY KEY(`achievementId`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `quest_baselines` " +
                    "(`period` TEXT NOT NULL, `startedAtMillis` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`period`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `quest_baseline_values` " +
                    "(`period` TEXT NOT NULL, `metric` TEXT NOT NULL, " +
                    "`mantissa` REAL NOT NULL, `exponent` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`period`, `metric`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `quest_claims` " +
                    "(`questId` TEXT NOT NULL, PRIMARY KEY(`questId`))",
            )
        }
    }

    /**
     * Ergaenzt die Tabelle des taeglichen Bonus.
     *
     * `lastClaimedAtMillis` ist als einzige Spalte nullbar: `null` bedeutet,
     * dass noch nie abgeholt wurde. Ein Ersatzwert wie null Millisekunden
     * waere der 1. Januar 1970 und damit ein gueltiger Zeitpunkt - die
     * Unterscheidung "nie" gegen "vor sehr langer Zeit" ginge verloren.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `daily_login` " +
                    "(`id` INTEGER NOT NULL, `streak` INTEGER NOT NULL, " +
                    "`longestStreak` INTEGER NOT NULL, " +
                    "`lastClaimedAtMillis` INTEGER, " +
                    "`protectionCharges` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    /** Alle Migrationen in der Reihenfolge ihrer Versionen. */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
    )
}
