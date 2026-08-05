package com.pokelike.idle.ui.screens.home

import androidx.compose.runtime.Immutable
import com.pokelike.idle.domain.model.BoosterType

/**
 * Ein laufender Booster in der Anzeige.
 *
 * @property remainingText Restlaufzeit als `mm:ss`.
 * @property progress Anteil der verbleibenden Laufzeit, 0 bis 1. Fuer den
 *   ablaufenden Balken.
 */
@Immutable
data class BoosterRow(
    val type: BoosterType,
    val remainingText: String,
    val progress: Float,
)

/**
 * Ein Booster im Angebot.
 *
 * @property priceText Preis, formatiert.
 * @property durationText Grundlaufzeit, formatiert.
 * @property canBuy Ob der Kauf jetzt gelingen wuerde. Falsch bei zu wenig
 *   Diamanten und bei ausgereizter Laufzeit.
 * @property isAtMaximum Ob die Laufzeit ausgereizt ist. Von [canBuy] getrennt,
 *   damit die Oberflaeche den Grund benennen kann - im einen Fall helfen mehr
 *   Diamanten, im anderen waeren sie vergeblich.
 * @property remainingText Restlaufzeit, falls der Booster gerade laeuft. Der
 *   Spieler soll vor dem Kauf sehen, was er verlaengert.
 */
@Immutable
data class BoosterOffer(
    val type: BoosterType,
    val priceText: String,
    val durationText: String,
    val canBuy: Boolean,
    val isAtMaximum: Boolean,
    val remainingText: String? = null,
)
