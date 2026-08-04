package com.pokelike.idle.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Spielspezifische Farben, die das Material-3-Farbschema nicht abbildet.
 *
 * Material kennt Rollen wie `primary` oder `error`, aber keine Muenzfarbe und
 * keine Seltenheitsstufen. Statt diese Rollen zweckzuentfremden - was jede
 * spaetere Theme-Anpassung zur Ratearbeit machen wuerde - bekommt das Spiel
 * eine eigene, klar benannte Farbschicht.
 *
 * [Immutable] ist wichtig fuer die Performance: Compose darf die Klasse damit
 * als stabil behandeln und Composables ueberspringen, die sie nur weiterreichen.
 */
@Immutable
data class GameColors(
    // Ressourcen
    val coin: Color,
    val diamond: Color,
    val ticket: Color,
    val eventToken: Color,
    val premium: Color,
    // Seltenheit
    val rarityCommon: Color,
    val rarityUncommon: Color,
    val rarityRare: Color,
    val rarityEpic: Color,
    val rarityLegendary: Color,
    // Zustand
    val positive: Color,
    val elevatedSurface: Color,
)

/** Farbschicht fuer den dunklen Modus. */
val DarkGameColors = GameColors(
    coin = ResourceCoin,
    diamond = ResourceDiamond,
    ticket = ResourceTicket,
    eventToken = ResourceEventToken,
    premium = ResourcePremium,
    rarityCommon = RarityCommon,
    rarityUncommon = RarityUncommon,
    rarityRare = RarityRare,
    rarityEpic = RarityEpic,
    rarityLegendary = RarityLegendary,
    positive = SuccessGreen,
    elevatedSurface = SurfaceDarkElevated,
)

/**
 * Farbschicht fuer den hellen Modus.
 *
 * Muenz- und Diamantton sind hier bewusst abgedunkelt: Die hellen Varianten
 * erreichen auf weissem Untergrund keinen ausreichenden Kontrast und waeren
 * fuer Zahlenwerte kaum lesbar.
 */
val LightGameColors = GameColors(
    coin = Color(0xFFB07A00),
    diamond = Color(0xFF0077A8),
    ticket = Color(0xFFC2521F),
    eventToken = Color(0xFF6B3FBF),
    premium = Color(0xFFC01B63),
    rarityCommon = Color(0xFF6E6E6E),
    rarityUncommon = Color(0xFF1F7A4D),
    rarityRare = Color(0xFF1B5FC4),
    rarityEpic = Color(0xFF7B2FC4),
    rarityLegendary = Color(0xFFB56A00),
    positive = Color(0xFF1F7A4D),
    elevatedSurface = SurfaceLightElevated,
)

/**
 * Zugriffspunkt fuer [GameColors] innerhalb der Composition.
 *
 * `staticCompositionLocalOf` statt `compositionLocalOf`, weil sich der Wert
 * praktisch nie aendert (nur beim Themewechsel). Compose spart sich dadurch das
 * Nachverfolgen der Lesezugriffe; beim Wechsel wird stattdessen der gesamte
 * darunterliegende Baum neu aufgebaut - genau das gewuenschte Verhalten.
 */
val LocalGameColors = staticCompositionLocalOf { DarkGameColors }
