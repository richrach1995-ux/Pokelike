package com.pokelike.idle.ui.screens.upgrades

import androidx.compose.runtime.Immutable
import com.pokelike.idle.domain.model.UpgradeType

/**
 * Eine Zeile der Upgrade-Liste.
 *
 * @property type Upgrade-Art. Liefert Beschriftung, Symbol und Wirkungstext.
 * @property price Preis, formatiert.
 * @property isOwned Ob bereits gekauft.
 * @property isAffordable Ob der Kontostand reicht.
 */
@Immutable
data class UpgradeRow(
    val type: UpgradeType,
    val price: String,
    val isOwned: Boolean,
    val isAffordable: Boolean,
)

/**
 * Anzeigezustand der Upgrade-Liste.
 *
 * Gekaufte Upgrades bleiben bewusst sichtbar und werden nur abgesetzt
 * dargestellt. Sie verschwinden zu lassen waere die naheliegende Loesung, nimmt
 * dem Spieler aber die Uebersicht darueber, was er bereits erreicht hat - und
 * genau diese Uebersicht traegt einen guten Teil der Motivation.
 *
 * @property rows Sichtbare Upgrades, gekaufte zuletzt.
 * @property coins Muenzstand, formatiert.
 * @property ownedCount Anzahl gekaufter Upgrades.
 * @property totalCount Gesamtzahl aller Upgrades im Spiel.
 */
@Immutable
data class UpgradesUiState(
    val rows: List<UpgradeRow> = emptyList(),
    val coins: String = "0",
    val ownedCount: Int = 0,
    val totalCount: Int = 0,
)
