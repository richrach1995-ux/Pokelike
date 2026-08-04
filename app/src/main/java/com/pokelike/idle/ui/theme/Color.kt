package com.pokelike.idle.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Rohe Farbwerte der Marke.
 *
 * Diese Datei enthaelt ausschliesslich Konstanten - keine Zuordnung zu
 * Material-Rollen. Die Zuordnung passiert in [Theme.kt]. Dadurch laesst sich
 * die Palette austauschen (z. B. fuer ein Event-Theme oder einen gekauften
 * Skin), ohne die Rollenlogik anzufassen.
 *
 * Die Palette ist dunkel angelegt: Idle-Spiele werden ueber lange Sitzungen und
 * haeufig abends gespielt, und ein dunkler Untergrund laesst Muenz- und
 * Partikeleffekte deutlich staerker wirken.
 */

// --- Markenfarben ---------------------------------------------------------

/** Primaerton: kraeftiges Violett als Traeger der Marke. */
val BrandViolet80 = Color(0xFFCFBCFF)
val BrandViolet60 = Color(0xFF9A7BFF)
val BrandViolet40 = Color(0xFF6247AA)
val BrandViolet20 = Color(0xFF3A2A6B)

/** Sekundaerton: kuehles Blaugruen als Gegengewicht zum warmen Gold. */
val BrandTeal80 = Color(0xFF8FE3D8)
val BrandTeal40 = Color(0xFF2E7D74)

/** Tertiaerton: warmes Rosa fuer Hervorhebungen und Angebote. */
val BrandPink80 = Color(0xFFFFB1C8)
val BrandPink40 = Color(0xFF8E4A62)

// --- Flaechen -------------------------------------------------------------

val SurfaceDark = Color(0xFF12101B)
val SurfaceDarkElevated = Color(0xFF1D1A2B)
val SurfaceLight = Color(0xFFFDFBFF)
val SurfaceLightElevated = Color(0xFFF2EDFA)

// --- Statusfarben ---------------------------------------------------------

val ErrorRed80 = Color(0xFFFFB4AB)
val ErrorRed40 = Color(0xFFBA1A1A)
val SuccessGreen = Color(0xFF4CC38A)

// --- Ressourcenfarben -----------------------------------------------------
// Jede Waehrung bekommt eine eigene, sofort wiedererkennbare Farbe. Spieler
// lesen im Sekundentakt Zahlen ab; die Farbe traegt dabei mehr Information als
// das Symbol daneben.

val ResourceCoin = Color(0xFFFFC83D)
val ResourceDiamond = Color(0xFF5BD1FF)
val ResourceTicket = Color(0xFFFF8A5B)
val ResourceEventToken = Color(0xFFB78BFF)
val ResourcePremium = Color(0xFFFF5FA2)

// --- Seltenheitsstufen ----------------------------------------------------
// Bewusst an die im Genre etablierte Farbskala angelehnt: Spieler erkennen den
// Wert eines Gegenstands dadurch ohne Text.

val RarityCommon = Color(0xFF9E9E9E)
val RarityUncommon = Color(0xFF4CC38A)
val RarityRare = Color(0xFF4C8DFF)
val RarityEpic = Color(0xFFB05BFF)
val RarityLegendary = Color(0xFFFFA22E)
