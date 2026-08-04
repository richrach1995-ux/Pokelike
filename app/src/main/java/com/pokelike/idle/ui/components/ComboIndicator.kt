package com.pokelike.idle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Zeigt die laufende Combo mit ablaufendem Zeitbalken.
 *
 * Der Balken ist keine Zierde. Ohne ihn kann der Spieler nicht einschaetzen,
 * wie viel Zeit ihm bis zum Verfall bleibt, und die Combo wirkt willkuerlich -
 * sie belohnt dann gefuehlt den Zufall statt die Aufmerksamkeit.
 *
 * Die Anzeige erscheint erst ab der zweiten Stufe: Ein einzelner Klick ist
 * keine Serie, und ein bei jedem Klick aufblitzender Hinweis waere Unruhe ohne
 * Information.
 *
 * @param count Anzahl der Klicks in Folge.
 * @param multiplierText Bereits formatierter Faktor, etwa "1.24x".
 * @param remainingFraction Verbleibender Anteil des Zeitfensters, 1.0 bis 0.0.
 */
@Composable
fun ComboIndicator(
    count: Int,
    multiplierText: String,
    remainingFraction: Float,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    AnimatedVisibility(
        visible = count >= MIN_VISIBLE_COUNT,
        enter = fadeIn() + scaleIn(initialScale = ENTER_SCALE),
        exit = fadeOut() + scaleOut(targetScale = EXIT_SCALE),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.combo_label, count, multiplierText),
                style = MaterialTheme.typography.titleMedium,
                color = gameColors.rarityLegendary,
            )

            Box(
                modifier = Modifier
                    .padding(top = dimens.spaceXs)
                    .width(COMBO_BAR_WIDTH)
                    .height(dimens.comboBarHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        // Geklemmt, weil fillMaxWidth Werte ausserhalb von 0..1
                        // mit einer Ausnahme quittiert. Der Anteil kommt aus
                        // einer Zeitdifferenz und koennte bei einem Zeitsprung
                        // kurzzeitig danebenliegen.
                        .fillMaxWidth(remainingFraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(percent = 50))
                        .background(gameColors.rarityLegendary),
                )
            }
        }
    }
}

/** Ab dieser Stufe wird die Combo angezeigt. */
private const val MIN_VISIBLE_COUNT = 2
private const val ENTER_SCALE = 0.8f
private const val EXIT_SCALE = 0.8f
private val COMBO_BAR_WIDTH = 140.dp
