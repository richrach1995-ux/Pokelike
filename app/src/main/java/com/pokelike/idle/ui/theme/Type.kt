package com.pokelike.idle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Typografie der App.
 *
 * Bewusst auf der Systemschrift aufgebaut statt auf einer eingebetteten
 * Schriftdatei: Das spart APK-Groesse, respektiert die Systemeinstellungen zur
 * Lesbarkeit und vermeidet Lizenzfragen. Eine Markenschrift laesst sich spaeter
 * einfuehren, indem hier eine [FontFamily] gesetzt wird - kein Screen muss
 * dafuer angefasst werden.
 */
val PokelikeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Stil fuer grosse Zahlenwerte (Kontostaende, Ertrag pro Sekunde).
 *
 * [FontFamily.Monospace] ist hier kein gestalterischer Einfall, sondern
 * funktional: Bei einer Proportionalschrift haben Ziffern unterschiedliche
 * Breiten, wodurch ein im Sekundentakt hochzaehlender Kontostand sichtbar
 * zappelt. Monospace haelt die Breite konstant und die Zahl ruhig.
 */
val ResourceValueTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    textAlign = TextAlign.Center,
)
