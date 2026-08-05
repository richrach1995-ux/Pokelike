package com.pokelike.idle.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein freigeschaltetes Achievement.
 *
 * Wie bei den Upgrades wird nur der Schluessel gespeichert. Bedingung und
 * Belohnung stehen im Code und duerfen dort geaendert werden - eine
 * Balancing-Anpassung soll bestehende Spielstaende erreichen.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val achievementId: String,
)

/**
 * Beginn eines Quest-Zeitraums.
 *
 * Eine Zeile je Zeitraum. Die zugehoerigen Ausgangswerte liegen in
 * [QuestBaselineValueEntity] - als Zeilen und nicht als Spalten, damit eine
 * neue Messgroesse keine Datenbankmigration erfordert.
 */
@Entity(tableName = "quest_baselines")
data class QuestBaselineEntity(
    @PrimaryKey
    val period: String,
    val startedAtMillis: Long,
)

/**
 * Ausgangswert einer Messgroesse zu Beginn eines Zeitraums.
 *
 * Mantisse und Exponent getrennt, wie bei den Ressourcen: So bleibt der Wert in
 * SQL vergleichbar, und es entfaellt jedes Parsen beim Laden.
 */
@Entity(tableName = "quest_baseline_values", primaryKeys = ["period", "metric"])
data class QuestBaselineValueEntity(
    val period: String,
    val metric: String,
    val mantissa: Double,
    val exponent: Int,
)

/** Eine bereits abgeholte Quest. */
@Entity(tableName = "quest_claims")
data class QuestClaimEntity(
    @PrimaryKey
    val questId: String,
)
