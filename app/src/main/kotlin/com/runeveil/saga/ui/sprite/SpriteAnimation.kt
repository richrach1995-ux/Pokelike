package com.runeveil.saga.ui.sprite

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import com.runeveil.saga.domain.model.sprite.SpriteBlueprint
import com.runeveil.saga.ui.theme.LocalMotionSettings
import kotlin.math.PI
import kotlin.math.sin

/**
 * What the renderer applies on top of the static silhouette.
 *
 * All offsets are fractions of the sprite's own box, never pixels, so the same
 * animation reads identically on a phone and on a tablet.
 */
data class SpriteMotion(
    /** Vertical breathing offset (negative is up). */
    val bobY: Float = 0f,
    /** Lunge along the facing direction, used by attacks. */
    val lungeX: Float = 0f,
    /** Direction-independent jitter, used by hits. */
    val shakeX: Float = 0f,
    /** Downward fall, used when fainting. */
    val dropY: Float = 0f,
    val scale: Float = 1f,
    /** Horizontal and vertical stretch, for squash-and-stretch. */
    val stretch: Float = 1f,
    val squash: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    /** 0 = untinted, 1 = fully [flashColor]. */
    val flash: Float = 0f,
    val flashColor: Color = Color.White,
) {
    companion object {
        val Still = SpriteMotion()
    }
}

/** The one-shot states a sprite can be driven through from the battle screen. */
enum class SpritePose {
    /** Breathing loop; the resting state. */
    IDLE,

    /** Steps into the attack and back. */
    ATTACK,

    /** Recoils and flashes on taking damage. */
    HURT,

    /** Tips over, sinks and fades out. */
    FAINT,

    /** Scales up as the monster is sent out. */
    ENTER,

    /** Shrinks into the orb on a successful capture. */
    CAPTURE,
    ;

    /**
     * True for poses that describe a lasting condition rather than a flourish.
     * A knocked-out monster stays down and a captured one stays gone — neither
     * returns to breathing when the animation ends.
     */
    val isTerminal: Boolean get() = this == FAINT || this == CAPTURE
}

/**
 * Drives [SpriteMotion] for one sprite.
 *
 * Returns a *lambda* rather than a value on purpose: [MonsterSprite] calls it
 * inside its draw scope, so the sixty updates a second repaint the canvas
 * without recomposing the battle screen around it.
 *
 * Accessibility and pacing are honoured here rather than at every call site:
 * with "reduced motion" enabled nothing moves and poses resolve immediately,
 * and the battle-speed preference scales every duration.
 *
 * @param onPoseFinished invoked when a one-shot pose has played out. It fires
 *   even when motion is reduced, so the battle flow never stalls waiting for an
 *   animation that was switched off.
 */
@Composable
fun rememberSpriteMotion(
    blueprint: SpriteBlueprint,
    pose: SpritePose = SpritePose.IDLE,
    onPoseFinished: () -> Unit = {},
): () -> SpriteMotion {
    val settings = LocalMotionSettings.current
    val reduced = settings.reducedMotion
    val finished by rememberUpdatedState(onPoseFinished)

    // Idle breathing: one continuous phase, sampled by the draw scope.
    val transition = rememberInfiniteTransition(label = "sprite-idle")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reduced) IDLE_DISABLED_MS else blueprint.idlePeriodMs,
                easing = LinearEasing,
            ),
        ),
        label = "sprite-idle-phase",
    )

    val progress = remember { Animatable(1f) }

    LaunchedEffect(pose) {
        if (pose == SpritePose.IDLE) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        val duration = (pose.durationMs / settings.battleSpeed.coerceAtLeast(0.25f)).toInt()
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (reduced) 0 else duration,
                easing = FastOutSlowInEasing,
            ),
        )
        finished()
    }

    return {
        val p = progress.value
        val idle = if (reduced) 0f else sin(phase.value * 2f * PI.toFloat())
        when (pose) {
            SpritePose.IDLE -> SpriteMotion(
                bobY = -idle * blueprint.idleBob,
                squash = 1f + idle * 0.02f,
                stretch = 1f - idle * 0.02f,
            )

            SpritePose.ATTACK -> {
                val swing = sin(p * PI.toFloat())
                SpriteMotion(
                    lungeX = swing * ATTACK_REACH,
                    stretch = 1f + swing * 0.08f,
                    squash = 1f - swing * 0.04f,
                )
            }

            SpritePose.HURT -> {
                val decay = 1f - p
                SpriteMotion(
                    shakeX = sin(p * PI.toFloat() * 6f) * HURT_SHAKE * decay,
                    squash = 1f - decay * 0.06f,
                    flash = decay,
                    flashColor = Color.White,
                )
            }

            SpritePose.FAINT -> SpriteMotion(
                dropY = p * 0.26f,
                rotation = p * 78f,
                alpha = 1f - p * 0.9f,
                squash = 1f - p * 0.2f,
            )

            SpritePose.ENTER -> SpriteMotion(
                scale = 0.35f + 0.65f * p,
                alpha = p.coerceIn(0f, 1f),
                bobY = -(1f - p) * 0.08f,
            )

            SpritePose.CAPTURE -> SpriteMotion(
                scale = 1f - p * 0.88f,
                rotation = p * 40f,
                alpha = 1f - p * 0.95f,
                flash = p * 0.6f,
                flashColor = Color(0xFFE8D27A),
            )
        }
    }
}

/** How long each one-shot takes at normal battle speed. */
private val SpritePose.durationMs: Int
    get() = when (this) {
        SpritePose.IDLE -> 0
        SpritePose.ATTACK -> 420
        SpritePose.HURT -> 320
        SpritePose.FAINT -> 700
        SpritePose.ENTER -> 380
        SpritePose.CAPTURE -> 620
    }

/** Fraction of the sprite box an attack steps forward. */
private const val ATTACK_REACH = 0.13f
private const val HURT_SHAKE = 0.028f

/**
 * With motion reduced the idle loop still has to have a duration — a zero would
 * divide by zero inside the animation clock — so it is made long enough to be
 * invisible instead.
 */
private const val IDLE_DISABLED_MS = 600_000
