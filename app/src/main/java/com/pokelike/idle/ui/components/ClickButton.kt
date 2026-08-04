package com.pokelike.idle.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Der grosse Klick-Button.
 *
 * Das Herzstueck des Spiels - hier entscheidet sich, ob es sich gut anfuehlt.
 * Drei Entscheidungen tragen dazu bei:
 *
 * - **Keine Ripple-Animation.** Die Standardrueckmeldung von Material ist auf
 *   einzelne Betaetigungen ausgelegt. Bei mehreren Klicks pro Sekunde
 *   ueberlagern sich die Wellen zu einem unruhigen Flimmern. Stattdessen
 *   staucht sich der Button kurz - eine Rueckmeldung, die auch bei schneller
 *   Folge lesbar bleibt.
 * - **Feder statt fester Dauer.** Eine Animation mit fester Laufzeit muss
 *   erst enden, bevor die naechste beginnt. Eine Feder wird jederzeit
 *   abgefangen und laeuft von der aktuellen Position weiter, sodass kein Klick
 *   verschluckt wirkt.
 * - **Skalierung ueber [graphicsLayer].** Damit aendert sich nur eine
 *   Eigenschaft der Zeichenebene. Weder Layout noch Zusammensetzung laufen
 *   erneut - bei zehn Klicks pro Sekunde der Unterschied zwischen fluessig und
 *   ruckelig.
 *
 * @param onClick Wird bei jeder Betaetigung aufgerufen.
 * @param enabled Ob der Button reagiert. Im Ladezustand abgeschaltet, damit
 *   kein Klick auf einen noch nicht geladenen Spielstand faellt.
 */
@Composable
fun ClickButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "clickButtonScale",
    )

    Box(
        modifier = modifier
            .size(dimens.clickButtonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        gameColors.coin,
                        gameColors.coin.copy(alpha = GRADIENT_EDGE_ALPHA),
                        MaterialTheme.colorScheme.primaryContainer,
                    ),
                ),
            )
            .clickable(
                interactionSource = interactionSource,
                // Siehe Klassenkommentar: eigene Rueckmeldung statt Ripple.
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = stringResource(R.string.click_button_action),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.click_button_description),
            // Das Symbol bringt eigene Farben mit und darf nicht eingefaerbt werden.
            tint = Color.Unspecified,
            modifier = Modifier.size(dimens.clickButtonSize - ICON_INSET),
        )
    }
}

/** Stauchung im gedrueckten Zustand. */
private const val PRESSED_SCALE = 0.93f

/** Deckkraft am aeusseren Rand des Farbverlaufs. */
private const val GRADIENT_EDGE_ALPHA = 0.65f

/** Abstand des Symbols zum Rand des Buttons. */
private val ICON_INSET = 48.dp
