package com.runeveil.saga.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Rarity
import com.runeveil.saga.ui.theme.LocalMotionSettings
import com.runeveil.saga.ui.theme.RuneBlood
import com.runeveil.saga.ui.theme.RuneEmber
import com.runeveil.saga.ui.theme.RuneGold
import com.runeveil.saga.ui.theme.RuneGoldDim
import com.runeveil.saga.ui.theme.RuneLeaf

/**
 * The shared vocabulary of the interface: panels with carved borders, runic
 * buttons, element badges and the health/experience meters used everywhere.
 */

/**
 * Resolves *content* strings (monster names, move text …) by key.
 *
 * Provided once by [com.runeveil.saga.MainActivity] from the
 * `LocalizationRepository`; screens simply call `contentText("species_x_name")`.
 */
val LocalContentStrings = staticCompositionLocalOf<(String) -> String> { { key -> key } }

@Composable
fun contentText(key: String): String = LocalContentStrings.current(key)

/** Provides the content-string resolver to a subtree. */
@Composable
fun ProvideContentStrings(resolver: (String) -> String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentStrings provides resolver, content = content)
}

/**
 * A panel with the carved double border used for every framed surface.
 * The subtle inner glow is what makes the UI read as "rune stone" rather than
 * as a plain Material card.
 */
@Composable
fun RunePanel(
    modifier: Modifier = Modifier,
    accent: Color = RuneGoldDim,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surface,
        shape = shape,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                ),
            ),
        ) {
            content()
        }
    }
}

/** The primary call-to-action button. */
@Composable
fun RunicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyph: String? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (glyph != null) {
            Text(glyph, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The secondary, outlined variant. */
@Composable
fun RunicOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Coloured badge showing an element's rune glyph and name. */
@Composable
fun ElementBadge(
    element: Element,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val color = Color(element.colorHex)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics { contentDescription = label ?: element.name },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(element.runeGlyph, color = color, style = MaterialTheme.typography.labelLarge)
        if (label != null) {
            Spacer(Modifier.width(5.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/** Small dot indicating rarity, used on bestiary and vault entries. */
@Composable
fun RarityDot(rarity: Rarity, modifier: Modifier = Modifier) {
    val color = when (rarity) {
        Rarity.COMMON -> Color(0xFF9A93A6)
        Rarity.UNCOMMON -> Color(0xFF6FBF73)
        Rarity.RARE -> Color(0xFF5AA9E6)
        Rarity.EPIC -> Color(0xFFB07CE0)
        Rarity.LEGENDARY -> RuneGold
        Rarity.MYTHIC -> Color(0xFFE2552B)
        Rarity.DIVINE -> Color(0xFFF6EFC9)
    }
    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * Animated hit-point bar.
 *
 * The colour shifts green → amber → red as HP drops, which is the fastest
 * readable signal during a battle.
 */
@Composable
fun HealthBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
    showNumbers: Boolean = true,
    contentDescriptionText: String? = null,
) {
    val fraction = if (max <= 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)
    val motion = LocalMotionSettings.current
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = motion.scale(420)),
        label = "hp",
    )
    val barColor by animateColorAsState(
        targetValue = when {
            fraction > 0.5f -> RuneLeaf
            fraction > 0.2f -> RuneEmber
            else -> RuneBlood
        },
        animationSpec = tween(durationMillis = motion.scale(420)),
        label = "hpColor",
    )

    Column(modifier = modifier.semantics { contentDescriptionText?.let { contentDescription = it } }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
        if (showNumbers) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$current / $max",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Thin experience meter shown under the health bar. */
@Composable
fun ExperienceBar(progress: Float, modifier: Modifier = Modifier) {
    val motion = LocalMotionSettings.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = motion.scale(600)),
        label = "xp",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

/** Section heading with the runic divider used across the menus. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = RuneGold,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(RuneGoldDim.copy(alpha = 0.5f)),
        )
    }
}

/** Full-screen loading state used while content and saves are read. */
@Composable
fun LoadingScreen(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = RuneGold)
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Centred placeholder for empty lists. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** A labelled statistic row, used by the monster summary and settings. */
@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) RuneGold else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
