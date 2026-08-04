package com.pokelike.idle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.ResourceValueTextStyle

/**
 * Ein aufsteigender Ertragshinweis ueber dem Klick-Button.
 *
 * @property id Eindeutige Kennung. Compose braucht sie als stabilen
 *   Schluessel: Ohne ihn wuerde beim Entfernen eines Eintrags die Animation
 *   aller nachfolgenden von vorn beginnen.
 * @property text Bereits formatierter Betrag.
 * @property isCritical Ob der Klick ein kritischer Treffer war.
 * @property horizontalBias Seitliche Ablage von -1 bis 1. Ohne Streuung laegen
 *   alle Hinweise exakt uebereinander und waeren bei schnellem Tippen nicht
 *   mehr lesbar.
 */
@Immutable
data class FloatingText(
    val id: Long,
    val text: String,
    val isCritical: Boolean,
    val horizontalBias: Float,
)

/**
 * Zeichnet alle laufenden Ertragshinweise.
 *
 * Die Liste wird vom Bildschirm gefuehrt, weil nur er weiss, wann ein Hinweis
 * ausgelaufen ist. [onFinished] meldet das zurueck, damit der Eintrag entfernt
 * wird - andernfalls wuechse die Liste bei jedem Klick weiter und das Spiel
 * wuerde nach wenigen Minuten spuerbar langsamer.
 */
@Composable
fun FloatingTextLayer(
    texts: List<FloatingText>,
    onFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        texts.forEach { floatingText ->
            key(floatingText.id) {
                FloatingTextItem(
                    floatingText = floatingText,
                    onFinished = onFinished,
                )
            }
        }
    }
}

/**
 * Ein einzelner Hinweis: steigt auf, waechst leicht und blendet aus.
 *
 * Alle drei Eigenschaften werden ueber [graphicsLayer] gesetzt. Damit aendert
 * sich nur die Zeichenebene - Layout und Zusammensetzung laufen nicht erneut.
 * Bei einem Dutzend gleichzeitig sichtbarer Hinweise ist das der Unterschied
 * zwischen fluessiger und stockender Darstellung.
 *
 * Als Erweiterung von [BoxScope], weil `Modifier.align` nur dort verfuegbar ist.
 */
@Composable
private fun BoxScope.FloatingTextItem(
    floatingText: FloatingText,
    onFinished: (Long) -> Unit,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors
    val riseDistancePx = with(LocalDensity.current) { dimens.floatingTextRise.toPx() }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(floatingText.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = DURATION_MS, easing = LinearEasing),
        )
        onFinished(floatingText.id)
    }

    Text(
        text = floatingText.text,
        style = ResourceValueTextStyle.copy(
            fontSize = if (floatingText.isCritical) CRITICAL_FONT_SIZE else NORMAL_FONT_SIZE,
            fontWeight = FontWeight.Bold,
        ),
        color = if (floatingText.isCritical) gameColors.rarityLegendary else gameColors.coin,
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer {
                translationY = -progress.value * riseDistancePx
                translationX = floatingText.horizontalBias * riseDistancePx * HORIZONTAL_SPREAD

                // Erst gegen Ende ausblenden. Wuerde die Deckkraft von Anfang
                // an sinken, waere der Betrag im wichtigsten Moment - direkt
                // nach dem Klick - bereits blass.
                alpha = ((1f - progress.value) / FADE_START).coerceIn(0f, 1f)

                val growth = 1f + progress.value * GROWTH
                scaleX = growth
                scaleY = growth
            },
    )
}

private const val DURATION_MS = 900
private const val FADE_START = 0.45f
private const val GROWTH = 0.35f
private const val HORIZONTAL_SPREAD = 0.5f
private val NORMAL_FONT_SIZE = 24.sp
private val CRITICAL_FONT_SIZE = 34.sp
