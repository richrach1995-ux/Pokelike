package com.pokelike.idle.ui.screens.buildings

import androidx.compose.runtime.Immutable
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.usecases.BuyAmount

/**
 * Eine Zeile der Gebaeudeliste.
 *
 * Alle Zahlen stehen bereits als fertige Zeichenketten hier. Bei zehn Zeilen,
 * die sich zehnmal pro Sekunde aendern koennen, waere Formatierung waehrend der
 * Zusammensetzung hundert Zeichenketten pro Sekunde auf dem UI-Thread.
 *
 * @property type Gebaeudeart. Wird fuer Symbol und Beschriftung gebraucht.
 * @property owned Anzahl im Besitz.
 * @property price Preis fuer die gewaehlte Kaufmenge, formatiert.
 * @property purchasableCount Tatsaechliche Kaufmenge. Bei [BuyAmount.MAX]
 *   haengt sie vom Kontostand ab und muss deshalb angezeigt werden - sonst
 *   waere fuer den Spieler nicht erkennbar, was er kauft.
 * @property incomePerSecond Ertrag aller Exemplare dieser Art, formatiert.
 * @property isAffordable Ob der Kauf moeglich ist.
 */
@Immutable
data class BuildingRow(
    val type: BuildingType,
    val owned: Int,
    val price: String,
    val purchasableCount: Int,
    val incomePerSecond: String,
    val isAffordable: Boolean,
)

/**
 * Anzeigezustand der Gebaeudeliste.
 *
 * @property rows Sichtbare Gebaeude in fester Reihenfolge.
 * @property buyAmount Gewaehlte Kaufmenge.
 * @property totalIncomePerSecond Gesamtertrag, formatiert.
 * @property coins Muenzstand, formatiert.
 */
@Immutable
data class BuildingsUiState(
    val rows: List<BuildingRow> = emptyList(),
    val buyAmount: BuyAmount = BuyAmount.ONE,
    val totalIncomePerSecond: String = "0",
    val coins: String = "0",
)
