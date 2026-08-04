package com.pokelike.idle.ui.screens.prestige

import androidx.compose.runtime.Immutable

/**
 * Anzeigezustand des Prestige-Bildschirms.
 *
 * Alle Werte stehen als fertige Zeichenketten hier. Ein Prestige-Reset ist
 * unumkehrbar; die Angaben, auf die der Spieler seine Entscheidung stuetzt,
 * duerfen deshalb nicht erst im Composable entstehen, wo sie schwerer pruefbar
 * waeren.
 *
 * @property currentPoints Vorhandene Punkte, formatiert.
 * @property pointsOnReset Punkte, die ein Reset einbraechte, formatiert.
 * @property currentBonus Wirksamer Bonus, etwa "+24 %".
 * @property bonusAfterReset Bonus nach dem Reset, etwa "+38 %".
 * @property lifetimeCoins Bisher verdiente Muenzen, formatiert.
 * @property coinsForNextPoint Lebenssumme fuer den naechsten Punkt, formatiert.
 * @property progressToNextPoint Fortschritt zum naechsten Punkt, 0 bis 1.
 * @property prestigeCount Bisherige Durchlaeufe.
 * @property canPrestige Ob ein Reset zulaessig ist.
 */
@Immutable
data class PrestigeUiState(
    val currentPoints: String = "0",
    val pointsOnReset: String = "0",
    val currentBonus: String = "+0 %",
    val bonusAfterReset: String = "+0 %",
    val lifetimeCoins: String = "0",
    val coinsForNextPoint: String = "0",
    val progressToNextPoint: Float = 0f,
    val prestigeCount: Int = 0,
    val canPrestige: Boolean = false,
)
