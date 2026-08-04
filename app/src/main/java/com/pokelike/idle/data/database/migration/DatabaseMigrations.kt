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

    /** Alle Migrationen in der Reihenfolge ihrer Versionen. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
