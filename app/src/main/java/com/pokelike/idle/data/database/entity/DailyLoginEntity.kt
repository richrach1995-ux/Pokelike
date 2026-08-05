package com.pokelike.idle.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stand des taeglichen Bonus.
 *
 * Anders als Gebaeude, Upgrades und Ziele ist das keine Sammlung, sondern eine
 * feste Handvoll Werte - deshalb genau eine Zeile mit festem Schluessel statt
 * einer Zeile je Eintrag.
 *
 * **Warum trotzdem eine eigene Tabelle und keine Spalten in `game_state`.**
 * Die Pruefsumme wird ueber eine Zeichenkette gebildet, an die neue Abschnitte
 * nur angehaengt werden duerfen. Zusaetzliche Spalten in `game_state` waeren
 * bei einem alten Spielstand mit ihrem Standardwert vorhanden und wuerden die
 * Zeichenkette veraendern - jeder Spielstand aus Version 4 wuerde beim naechsten
 * Start als manipuliert gelten. Eine eigene Tabelle ist dort schlicht leer, und
 * ein leerer Abschnitt traegt nichts bei.
 *
 * @property id Fester Schluessel. Es gibt hoechstens eine Zeile.
 * @property lastClaimedAtMillis `null`, solange nie abgeholt wurde.
 */
@Entity(tableName = "daily_login")
data class DailyLoginEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val streak: Int,
    val longestStreak: Int,
    val lastClaimedAtMillis: Long?,
    val protectionCharges: Int,
) {

    companion object {
        /** Schluessel der einzigen Zeile. */
        const val SINGLE_ROW_ID: Int = 0
    }
}
