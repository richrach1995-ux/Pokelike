package com.pokelike.idle.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Skalare Felder des Spielstands.
 *
 * Es gibt genau eine Zeile. Der feste Primaerschluessel [SINGLE_ROW_ID] setzt
 * das durch: Ein fehlerhafter Einfuegevorgang erzeugt keine zweite Zeile,
 * sondern ersetzt die bestehende. Andernfalls koennten mehrere Spielstaende
 * nebeneinander entstehen, und beim Laden waere nicht bestimmt, welcher gilt.
 *
 * Ressourcenbetraege liegen bewusst in einer eigenen Tabelle
 * ([ResourceEntity]) und nicht als Spalten hier: Eine neue Ressource waere
 * sonst eine Schemaaenderung mit Datenbankmigration.
 *
 * @property signature Pruefsumme ueber diese Zeile *und* alle zugehoerigen
 *   Ressourcenzeilen. Beides gemeinsam zu signieren ist wesentlich - sonst
 *   liesse sich der Muenzstand in der Ressourcentabelle veraendern, ohne die
 *   Pruefung zu beruehren.
 */
@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val lastSeenAtMillis: Long,
    val totalClicks: Long,
    val totalCriticalClicks: Long,
    val totalPlayTimeMillis: Long,
    val sessionCount: Int,
    val prestigeCount: Int,
    val totalBuildingsPurchased: Long,
    val signature: String,
) {
    companion object {
        /** Schluessel der einzigen Zeile. */
        const val SINGLE_ROW_ID: Int = 1
    }
}
