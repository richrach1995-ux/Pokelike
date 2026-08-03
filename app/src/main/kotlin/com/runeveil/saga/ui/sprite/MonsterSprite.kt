package com.runeveil.saga.ui.sprite

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.sprite.Archetype
import com.runeveil.saga.domain.model.sprite.HeadCrest
import com.runeveil.saga.domain.model.sprite.MarkingStyle
import com.runeveil.saga.domain.model.sprite.SpriteBlueprint
import com.runeveil.saga.domain.model.sprite.SpriteBlueprints
import com.runeveil.saga.domain.model.sprite.TailShape
import com.runeveil.saga.domain.model.sprite.WingShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a monster from its [SpriteBlueprint].
 *
 * Everything is vector work on a [Canvas]: no bitmaps are loaded, nothing is
 * decoded, and the result scales to any density without a mipmap set. A sprite
 * costs a few dozen draw calls, which is what lets the party screen show six of
 * them and the bestiary a whole grid.
 *
 * The composable is deliberately dumb — all shape decisions were made in
 * :domain and are already in the blueprint. This file only knows *how* to put
 * the described creature on a canvas.
 *
 * @param facing which way the creature looks. In battle the opponent looks at
 *   the player and the player's monster looks away, which is what makes the two
 *   halves of the screen read as facing each other.
 * @param motion supplies the current animation state. It is a lambda, not a
 *   value, so the animation is read *inside* the draw scope: a running battle
 *   then repaints without recomposing anything above it.
 */
@Composable
fun MonsterSprite(
    blueprint: SpriteBlueprint,
    modifier: Modifier = Modifier,
    facing: SpriteFacing = SpriteFacing.RIGHT,
    motion: () -> SpriteMotion = { SpriteMotion.Still },
) {
    Canvas(modifier) { drawCreature(blueprint, facing, motion()) }
}

/**
 * Non-animated variant for lists, grids and menus.
 *
 * @param silhouette draws the creature in flat shadow. The bestiary uses it for
 *   species that have not been seen: the outline is already a hint, the colours
 *   would give the element away.
 */
@Composable
fun MonsterSpriteStatic(
    blueprint: SpriteBlueprint,
    modifier: Modifier = Modifier,
    facing: SpriteFacing = SpriteFacing.RIGHT,
    silhouette: Boolean = false,
) {
    Canvas(modifier) { drawCreature(blueprint, facing, SpriteMotion.Still, silhouette) }
}

enum class SpriteFacing { LEFT, RIGHT }

// ---------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------

private fun DrawScope.drawCreature(
    blueprint: SpriteBlueprint,
    facing: SpriteFacing,
    motion: SpriteMotion,
    silhouette: Boolean = false,
) {
    if (motion.alpha <= 0.01f) return

    val direction = if (facing == SpriteFacing.LEFT) -1f else 1f
    val geometry = Geometry.of(this, blueprint)

    val shiftX = (motion.lungeX * direction + motion.shakeX) * size.width
    val shiftY = motion.bobY * size.height + motion.dropY * size.height

    translate(left = shiftX, top = shiftY) {
        rotate(degrees = motion.rotation * direction, pivot = geometry.pivot) {
            scale(
                scaleX = motion.scale * motion.stretch,
                scaleY = motion.scale * motion.squash,
                pivot = geometry.pivot,
            ) {
                val palette = if (silhouette) {
                    shadowPalette(motion)
                } else {
                    blueprint.palette.toColors(motion)
                }

                if (blueprint.auraStrength > 0f && !silhouette) drawAura(geometry, palette, blueprint.auraStrength)
                drawBackWings(blueprint, geometry, palette, direction)
                drawTail(blueprint, geometry, palette, direction)
                drawLegs(blueprint, geometry, palette, behind = true)
                drawBody(blueprint, geometry, palette)
                drawMarkings(blueprint, geometry, palette)
                drawLegs(blueprint, geometry, palette, behind = false)
                if (blueprint.hasArms) drawArms(geometry, palette, direction)
                drawHead(blueprint, geometry, palette, direction)
                drawCrest(blueprint, geometry, palette, direction)
                drawEyes(blueprint, geometry, palette, direction)
            }
        }
    }
}

/**
 * Every anchor point the drawing needs, resolved once from the blueprint and
 * the canvas size. Keeping this out of the draw functions means the body, the
 * head and the tail cannot drift apart.
 */
private class Geometry(
    val centerX: Float,
    val centerY: Float,
    val bodyW: Float,
    val bodyH: Float,
    val cornerRadius: Float,
    val headX: Float,
    val headY: Float,
    val headR: Float,
    val groundY: Float,
    val legTop: Float,
    val unit: Float,
) {
    val pivot: Offset get() = Offset(centerX, centerY)

    companion object {
        fun of(scope: DrawScope, blueprint: SpriteBlueprint): Geometry {
            val w = scope.size.width
            val h = scope.size.height
            val groundY = h * GROUND_LINE

            val bodyW = w * blueprint.bodyWidth
            val bodyH = h * blueprint.bodyHeight
            val legLength = h * blueprint.legLength

            val centerY = if (blueprint.floats) {
                h * 0.5f
            } else {
                groundY - legLength - bodyH * 0.5f
            }
            // The head sits forward of the torso. The caller mirrors the whole
            // creature for a left-facing sprite, so this is always drawn facing
            // right and no direction term is needed here.
            val headR = bodyH * 0.5f * blueprint.headScale * HEAD_FACTOR

            return Geometry(
                centerX = w * 0.5f,
                centerY = centerY,
                bodyW = bodyW,
                bodyH = bodyH,
                cornerRadius = minOf(bodyW, bodyH) * 0.5f * blueprint.bodyRoundness,
                headX = w * 0.5f + bodyW * 0.5f + headR * 0.15f,
                headY = centerY - bodyH * 0.30f - h * blueprint.neckLength * 0.5f,
                headR = headR,
                groundY = groundY,
                legTop = centerY + bodyH * 0.35f,
                unit = minOf(w, h),
            )
        }

        private const val GROUND_LINE = 0.94f
        private const val HEAD_FACTOR = 1.15f
    }
}

private fun DrawScope.drawAura(geometry: Geometry, palette: Palette, strength: Float) {
    val radius = maxOf(geometry.bodyW, geometry.bodyH) * (0.9f + strength * 0.5f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.glow.copy(alpha = 0.30f * strength),
                palette.glow.copy(alpha = 0.10f * strength),
                Color.Transparent,
            ),
            center = Offset(geometry.centerX, geometry.centerY),
            radius = radius,
        ),
        radius = radius,
        center = Offset(geometry.centerX, geometry.centerY),
    )
}

private fun DrawScope.drawBody(blueprint: SpriteBlueprint, g: Geometry, palette: Palette) {
    when (blueprint.archetype) {
        Archetype.WISP -> {
            drawCircle(palette.body, g.bodyH * 0.5f, Offset(g.centerX, g.centerY))
            drawCircle(
                palette.glow.copy(alpha = 0.5f),
                g.bodyH * 0.72f,
                Offset(g.centerX, g.centerY),
            )
            drawCircle(
                palette.outline,
                g.bodyH * 0.5f,
                Offset(g.centerX, g.centerY),
                style = Stroke(width = g.unit * OUTLINE),
            )
        }

        Archetype.SERPENT -> drawCoil(g, palette)

        else -> {
            val topLeft = Offset(g.centerX - g.bodyW * 0.5f, g.centerY - g.bodyH * 0.5f)
            val boxSize = Size(g.bodyW, g.bodyH)
            drawRoundRect(palette.body, topLeft, boxSize, CornerRadius(g.cornerRadius, g.cornerRadius))
            // Underside shading gives the flat vector shape a sense of volume.
            drawRoundRect(
                color = palette.bodyShade.copy(alpha = 0.55f),
                topLeft = Offset(topLeft.x, g.centerY + g.bodyH * 0.10f),
                size = Size(g.bodyW, g.bodyH * 0.40f),
                cornerRadius = CornerRadius(g.cornerRadius, g.cornerRadius),
            )
            drawOval(
                color = palette.belly,
                topLeft = Offset(g.centerX - g.bodyW * 0.28f, g.centerY - g.bodyH * 0.05f),
                size = Size(g.bodyW * 0.56f, g.bodyH * 0.42f),
            )
            drawRoundRect(
                color = palette.outline,
                topLeft = topLeft,
                size = boxSize,
                cornerRadius = CornerRadius(g.cornerRadius, g.cornerRadius),
                style = Stroke(width = g.unit * OUTLINE),
            )
        }
    }
}

/** A serpent has no torso — it is a thick, curved stroke. */
private fun DrawScope.drawCoil(g: Geometry, palette: Palette) {
    val path = Path()
    val amplitude = g.bodyH * 0.9f
    val left = g.centerX - g.bodyW * 0.75f
    val right = g.centerX + g.bodyW * 0.75f
    val steps = 24

    path.moveTo(left, g.centerY)
    for (step in 1..steps) {
        val t = step / steps.toFloat()
        val x = left + (right - left) * t
        val y = g.centerY + sin(t * 2f * PI.toFloat()) * amplitude * 0.5f
        path.lineTo(x, y)
    }
    drawPath(path, palette.body, style = Stroke(width = g.bodyH * 0.8f))
    drawPath(path, palette.outline, style = Stroke(width = g.unit * OUTLINE))
}

private fun DrawScope.drawLegs(
    blueprint: SpriteBlueprint,
    g: Geometry,
    palette: Palette,
    behind: Boolean,
) {
    if (blueprint.legCount == 0) return
    val pairs = blueprint.legCount / 2
    val thickness = g.bodyW / (blueprint.legCount * 1.6f)
    val color = if (behind) palette.bodyShade else palette.body

    for (pair in 0 until pairs) {
        val spread = if (pairs == 1) 0f else pair / (pairs - 1f) - 0.5f
        val x = g.centerX + spread * g.bodyW * 0.62f
        val offset = if (behind) -thickness * 0.5f else thickness * 0.5f
        drawRoundRect(
            color = color,
            topLeft = Offset(x - thickness * 0.5f + offset, g.legTop),
            size = Size(thickness, g.groundY - g.legTop),
            cornerRadius = CornerRadius(thickness * 0.5f, thickness * 0.5f),
        )
        if (!behind) {
            drawRoundRect(
                color = palette.outline,
                topLeft = Offset(x - thickness * 0.5f + offset, g.legTop),
                size = Size(thickness, g.groundY - g.legTop),
                cornerRadius = CornerRadius(thickness * 0.5f, thickness * 0.5f),
                style = Stroke(width = g.unit * OUTLINE * 0.7f),
            )
        }
    }
}

private fun DrawScope.drawArms(g: Geometry, palette: Palette, direction: Float) {
    val thickness = g.bodyW * 0.14f
    val length = g.bodyH * 0.9f
    val x = g.centerX + direction * g.bodyW * 0.34f
    drawRoundRect(
        color = palette.body,
        topLeft = Offset(x - thickness * 0.5f, g.centerY - g.bodyH * 0.25f),
        size = Size(thickness, length),
        cornerRadius = CornerRadius(thickness * 0.5f, thickness * 0.5f),
    )
    drawCircle(palette.accent, thickness * 0.6f, Offset(x, g.centerY - g.bodyH * 0.25f + length))
}

private fun DrawScope.drawHead(
    blueprint: SpriteBlueprint,
    g: Geometry,
    palette: Palette,
    direction: Float,
) {
    if (blueprint.archetype == Archetype.WISP) return

    // Neck: a tapered link so a long-necked drake does not float its head.
    if (blueprint.neckLength > 0.02f) {
        val neck = Path()
        neck.moveTo(g.centerX + direction * g.bodyW * 0.20f, g.centerY - g.bodyH * 0.15f)
        neck.lineTo(g.centerX + direction * g.bodyW * 0.42f, g.centerY - g.bodyH * 0.30f)
        neck.lineTo(g.headX + direction * g.headR * 0.30f, g.headY + g.headR * 0.55f)
        neck.lineTo(g.headX - direction * g.headR * 0.45f, g.headY + g.headR * 0.75f)
        neck.close()
        drawPath(neck, palette.body)
        drawPath(neck, palette.outline, style = Stroke(width = g.unit * OUTLINE * 0.8f))
    }

    val headCenter = Offset(g.headX, g.headY)
    drawCircle(palette.body, g.headR, headCenter)
    drawCircle(palette.outline, g.headR, headCenter, style = Stroke(width = g.unit * OUTLINE))

    // Snout / beak, pointing the way the creature faces.
    val snout = Path()
    val snoutLength = g.headR * if (blueprint.archetype == Archetype.AVIAN) 1.1f else 0.7f
    snout.moveTo(g.headX + direction * g.headR * 0.35f, g.headY - g.headR * 0.20f)
    snout.lineTo(g.headX + direction * (g.headR * 0.35f + snoutLength), g.headY + g.headR * 0.12f)
    snout.lineTo(g.headX + direction * g.headR * 0.30f, g.headY + g.headR * 0.50f)
    snout.close()
    drawPath(snout, if (blueprint.archetype == Archetype.AVIAN) palette.accent else palette.belly)
    drawPath(snout, palette.outline, style = Stroke(width = g.unit * OUTLINE * 0.8f))
}

private fun DrawScope.drawCrest(
    blueprint: SpriteBlueprint,
    g: Geometry,
    palette: Palette,
    direction: Float,
) {
    val size = g.headR * blueprint.crestSize
    when (blueprint.crest) {
        HeadCrest.NONE -> Unit

        HeadCrest.HORNS -> repeat(2) { index ->
            val x = g.headX + (index - 0.5f) * g.headR * 0.9f
            val horn = Path()
            horn.moveTo(x - size * 0.16f, g.headY - g.headR * 0.7f)
            horn.lineTo(x + size * 0.16f, g.headY - g.headR * 0.7f)
            horn.lineTo(x + direction * size * 0.28f, g.headY - g.headR * 0.7f - size)
            horn.close()
            drawPath(horn, palette.accent)
            drawPath(horn, palette.outline, style = Stroke(width = g.unit * OUTLINE * 0.7f))
        }

        HeadCrest.ANTLERS -> repeat(2) { index ->
            val x = g.headX + (index - 0.5f) * g.headR * 0.8f
            val tip = Offset(x + (index - 0.5f) * size * 0.9f, g.headY - g.headR - size)
            drawLine(palette.accent, Offset(x, g.headY - g.headR * 0.6f), tip, g.unit * 0.012f)
            drawLine(
                palette.accent,
                Offset((x + tip.x) * 0.5f, (g.headY - g.headR * 0.6f + tip.y) * 0.5f),
                Offset(tip.x + (index - 0.5f) * size * 0.5f, tip.y + size * 0.35f),
                g.unit * 0.009f,
            )
        }

        HeadCrest.SPINES -> repeat(blueprint.markingCount) { index ->
            val t = index / (blueprint.markingCount - 1f).coerceAtLeast(1f)
            val x = g.headX - direction * g.headR * (0.2f + t * 1.4f)
            val height = size * (1f - t * 0.5f)
            val spine = Path()
            spine.moveTo(x - size * 0.14f, g.headY - g.headR * 0.5f)
            spine.lineTo(x + size * 0.14f, g.headY - g.headR * 0.5f)
            spine.lineTo(x, g.headY - g.headR * 0.5f - height * 0.7f)
            spine.close()
            drawPath(spine, palette.accent)
        }

        HeadCrest.HALO -> {
            val radius = g.headR * (1.25f + blueprint.crestSize * 0.4f)
            drawCircle(
                color = palette.glow.copy(alpha = 0.85f),
                radius = radius,
                center = Offset(g.headX, g.headY - g.headR * 0.85f),
                style = Stroke(width = g.unit * 0.012f),
            )
        }

        HeadCrest.MANE -> drawCircle(
            color = palette.accent.copy(alpha = 0.8f),
            radius = g.headR * 1.35f,
            center = Offset(g.headX - direction * g.headR * 0.25f, g.headY + g.headR * 0.1f),
            style = Stroke(width = g.headR * 0.45f),
        )
    }
}

private fun DrawScope.drawEyes(
    blueprint: SpriteBlueprint,
    g: Geometry,
    palette: Palette,
    direction: Float,
) {
    val anchorX = if (blueprint.archetype == Archetype.WISP) g.centerX else g.headX
    val anchorY = if (blueprint.archetype == Archetype.WISP) g.centerY else g.headY
    val radius = g.headR * 0.22f * blueprint.eyeSize
    val spread = g.headR * 0.42f

    when (blueprint.eyeCount) {
        1 -> drawEye(Offset(anchorX, anchorY), radius * 1.6f, palette, g)
        else -> repeat(blueprint.eyeCount) { index ->
            val row = index / 2
            val column = index % 2
            drawEye(
                center = Offset(
                    anchorX + direction * (g.headR * 0.20f + column * spread * 0.9f),
                    anchorY - g.headR * 0.18f + row * radius * 2.4f,
                ),
                radius = radius,
                palette = palette,
                g = g,
            )
        }
    }
}

private fun DrawScope.drawEye(center: Offset, radius: Float, palette: Palette, g: Geometry) {
    drawCircle(palette.eye, radius, center)
    drawCircle(palette.outline, radius * 0.45f, Offset(center.x, center.y + radius * 0.1f))
    drawCircle(palette.outline, radius, center, style = Stroke(width = g.unit * OUTLINE * 0.6f))
}

private fun DrawScope.drawTail(
    blueprint: SpriteBlueprint,
    g: Geometry,
    palette: Palette,
    direction: Float,
) {
    if (blueprint.tail == TailShape.NONE) return
    val rootX = g.centerX - direction * g.bodyW * 0.48f
    val rootY = g.centerY
    val length = g.bodyW * blueprint.tailLength * 0.9f
    val tipX = rootX - direction * length
    val tipY = rootY - g.bodyH * 0.35f

    when (blueprint.tail) {
        TailShape.NONE -> Unit

        TailShape.TAPERED, TailShape.SERPENTINE -> {
            val path = Path()
            path.moveTo(rootX, rootY - g.bodyH * 0.14f)
            path.quadraticTo(
                rootX - direction * length * 0.6f,
                rootY - g.bodyH * 0.6f,
                tipX,
                tipY,
            )
            path.quadraticTo(
                rootX - direction * length * 0.5f,
                rootY - g.bodyH * 0.1f,
                rootX,
                rootY + g.bodyH * 0.14f,
            )
            path.close()
            drawPath(path, palette.body)
            drawPath(path, palette.outline, style = Stroke(width = g.unit * OUTLINE * 0.7f))
        }

        TailShape.TUFTED -> {
            drawLine(palette.body, Offset(rootX, rootY), Offset(tipX, tipY), g.bodyH * 0.18f)
            drawCircle(palette.accent, g.bodyH * 0.28f, Offset(tipX, tipY))
        }

        TailShape.FINNED -> {
            val fin = Path()
            fin.moveTo(rootX, rootY)
            fin.lineTo(tipX, tipY - g.bodyH * 0.5f)
            fin.lineTo(tipX, tipY + g.bodyH * 0.7f)
            fin.close()
            drawPath(fin, palette.accent.copy(alpha = 0.9f))
            drawPath(fin, palette.outline, style = Stroke(width = g.unit * OUTLINE * 0.7f))
        }

        TailShape.SPIKED -> {
            drawLine(palette.body, Offset(rootX, rootY), Offset(tipX, tipY), g.bodyH * 0.20f)
            repeat(3) { index ->
                val t = (index + 1) / 4f
                val x = rootX + (tipX - rootX) * t
                val y = rootY + (tipY - rootY) * t
                val spike = Path()
                spike.moveTo(x, y - g.bodyH * 0.10f)
                spike.lineTo(x - direction * g.bodyH * 0.12f, y - g.bodyH * 0.42f)
                spike.lineTo(x + direction * g.bodyH * 0.10f, y - g.bodyH * 0.12f)
                spike.close()
                drawPath(spike, palette.accent)
            }
        }
    }
}

private fun DrawScope.drawBackWings(
    blueprint: SpriteBlueprint,
    g: Geometry,
    palette: Palette,
    direction: Float,
) {
    if (blueprint.wings == WingShape.NONE) return
    val span = g.bodyW * blueprint.wingSpan
    val top = g.centerY - g.bodyH * 0.45f

    when (blueprint.wings) {
        WingShape.NONE -> Unit

        WingShape.MEMBRANE -> {
            val wing = Path()
            wing.moveTo(g.centerX, top)
            wing.lineTo(g.centerX - direction * span, top - g.bodyH * 0.9f)
            wing.lineTo(g.centerX - direction * span * 0.85f, top + g.bodyH * 0.5f)
            wing.lineTo(g.centerX - direction * span * 0.30f, top + g.bodyH * 0.2f)
            wing.close()
            drawPath(wing, palette.accent.copy(alpha = 0.92f))
            drawPath(wing, palette.outline, style = Stroke(width = g.unit * OUTLINE * 0.7f))
        }

        WingShape.FEATHERED -> repeat(4) { index ->
            val t = index / 3f
            drawLine(
                color = palette.accent.copy(alpha = 0.9f - t * 0.25f),
                start = Offset(g.centerX, top + g.bodyH * 0.1f),
                end = Offset(
                    g.centerX - direction * span * (0.5f + t * 0.6f),
                    top - g.bodyH * (0.2f + t * 0.7f),
                ),
                strokeWidth = g.bodyH * 0.16f,
            )
        }

        WingShape.INSECT -> repeat(2) { index ->
            drawOval(
                color = palette.glow.copy(alpha = 0.42f),
                topLeft = Offset(
                    g.centerX - direction * span * (0.35f + index * 0.35f) - span * 0.25f,
                    top - g.bodyH * (0.55f - index * 0.20f),
                ),
                size = Size(span * 0.7f, g.bodyH * 0.75f),
            )
        }

        WingShape.ETHEREAL -> repeat(3) { index ->
            val angle = (index / 3f) * 2f * PI.toFloat()
            drawCircle(
                color = palette.glow.copy(alpha = 0.30f),
                radius = g.bodyH * 0.30f,
                center = Offset(
                    g.centerX + cos(angle) * span * 0.6f,
                    g.centerY + sin(angle) * g.bodyH * 0.6f,
                ),
            )
        }
    }
}

private fun DrawScope.drawMarkings(blueprint: SpriteBlueprint, g: Geometry, palette: Palette) {
    if (blueprint.markings == MarkingStyle.NONE) return
    val count = blueprint.markingCount

    when (blueprint.markings) {
        MarkingStyle.NONE -> Unit

        MarkingStyle.STRIPES -> repeat(count) { index ->
            val t = (index + 1) / (count + 1f)
            val x = g.centerX - g.bodyW * 0.5f + g.bodyW * t
            drawLine(
                color = palette.accent.copy(alpha = 0.65f),
                start = Offset(x, g.centerY - g.bodyH * 0.36f),
                end = Offset(x, g.centerY + g.bodyH * 0.30f),
                strokeWidth = g.bodyW * 0.05f,
            )
        }

        MarkingStyle.SPOTS -> repeat(count) { index ->
            val t = (index + 1) / (count + 1f)
            drawCircle(
                color = palette.accent.copy(alpha = 0.6f),
                radius = g.bodyH * 0.11f,
                center = Offset(
                    g.centerX - g.bodyW * 0.34f + g.bodyW * 0.68f * t,
                    g.centerY - g.bodyH * 0.18f + (index % 2) * g.bodyH * 0.30f,
                ),
            )
        }

        MarkingStyle.RUNES -> repeat(count) { index ->
            val t = (index + 1) / (count + 1f)
            val x = g.centerX - g.bodyW * 0.32f + g.bodyW * 0.64f * t
            val y = g.centerY - g.bodyH * 0.05f
            val arm = g.bodyH * 0.16f
            drawLine(palette.glow, Offset(x, y - arm), Offset(x, y + arm), g.unit * 0.008f)
            drawLine(
                color = palette.glow,
                start = Offset(x, y - arm * 0.4f),
                end = Offset(x + arm * 0.7f, y - arm),
                strokeWidth = g.unit * 0.008f,
            )
        }

        MarkingStyle.PLATES -> repeat(count) { index ->
            val t = index / count.toFloat()
            drawRoundRect(
                color = palette.bodyShade.copy(alpha = 0.75f),
                topLeft = Offset(
                    g.centerX - g.bodyW * 0.42f + g.bodyW * 0.84f * t,
                    g.centerY - g.bodyH * 0.34f,
                ),
                size = Size(g.bodyW * 0.72f / count, g.bodyH * 0.66f),
                cornerRadius = CornerRadius(g.unit * 0.008f, g.unit * 0.008f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------

private class Palette(
    val body: Color,
    val bodyShade: Color,
    val belly: Color,
    val accent: Color,
    val outline: Color,
    val eye: Color,
    val glow: Color,
)

/**
 * Converts the blueprint's packed colours into Compose colours and applies the
 * damage flash — tinting here rather than drawing an overlay keeps the flash
 * inside the silhouette instead of a rectangle around it.
 */
private fun com.runeveil.saga.domain.model.sprite.SpritePalette.toColors(
    motion: SpriteMotion,
): Palette {
    // The blueprint stores 0xAARRGGBB in a Long; the low 32 bits are exactly
    // the packed Int that Compose's Color(Int) expects.
    fun convert(packed: Long): Color = blend(Color(packed.toInt()), motion)
    return Palette(
        body = convert(body),
        bodyShade = convert(bodyShade),
        belly = convert(belly),
        accent = convert(accent),
        outline = convert(outline),
        eye = convert(eye),
        glow = convert(glow),
    )
}

/** Flat shadow used for species the player has not met yet. */
private fun shadowPalette(motion: SpriteMotion): Palette {
    val shadow = Color(0xFF241C33)
    val edge = Color(0xFF3A2F52)
    return Palette(
        body = blend(shadow, motion),
        bodyShade = blend(shadow, motion),
        belly = blend(shadow, motion),
        accent = blend(shadow, motion),
        outline = blend(edge, motion),
        eye = blend(edge, motion),
        glow = blend(edge, motion),
    )
}

private fun blend(color: Color, motion: SpriteMotion): Color {
    val tinted = if (motion.flash > 0f) {
        Color(
            red = color.red + (motion.flashColor.red - color.red) * motion.flash,
            green = color.green + (motion.flashColor.green - color.green) * motion.flash,
            blue = color.blue + (motion.flashColor.blue - color.blue) * motion.flash,
            alpha = color.alpha,
        )
    } else {
        color
    }
    return tinted.copy(alpha = tinted.alpha * motion.alpha)
}

/** Silhouette line width as a fraction of the sprite's smaller dimension. */
private const val OUTLINE = 0.014f

// ---------------------------------------------------------------------------
// Convenience
// ---------------------------------------------------------------------------

/**
 * Caches the blueprint for a species. Generating one is cheap, but a bestiary
 * grid scrolls through dozens per frame, so it is keyed and remembered.
 */
@Composable
fun rememberBlueprint(species: MonsterSpecies, shiny: Boolean = false): SpriteBlueprint =
    remember(species.id, shiny) { SpriteBlueprints.of(species, shiny) }

/**
 * A monster as it appears in lists, menus and the bestiary: its own species,
 * its own shiny state, no animation.
 */
@Composable
fun MonsterPortrait(
    monster: MonsterInstance,
    modifier: Modifier = Modifier,
    facing: SpriteFacing = SpriteFacing.RIGHT,
) {
    MonsterSpriteStatic(
        blueprint = rememberBlueprint(monster.species, monster.isShiny),
        modifier = modifier,
        facing = facing,
    )
}

/**
 * An unhatched egg. Deliberately anonymous — the species inside is a surprise
 * the roost screen is not allowed to give away.
 */
@Composable
fun EggPortrait(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val centre = Offset(size.width * 0.5f, size.height * 0.55f)
        val radius = minOf(size.width, size.height) * 0.34f
        drawOval(
            color = Color(0xFFF3E9D2),
            topLeft = Offset(centre.x - radius * 0.82f, centre.y - radius * 1.15f),
            size = Size(radius * 1.64f, radius * 2.20f),
        )
        drawOval(
            color = Color(0xFFC79A4B),
            topLeft = Offset(centre.x - radius * 0.82f, centre.y - radius * 1.15f),
            size = Size(radius * 1.64f, radius * 2.20f),
            style = Stroke(width = radius * 0.10f),
        )
        repeat(3) { index ->
            val y = centre.y - radius * 0.45f + index * radius * 0.52f
            drawLine(
                color = Color(0xFFC79A4B).copy(alpha = 0.55f),
                start = Offset(centre.x - radius * 0.52f, y),
                end = Offset(centre.x + radius * 0.52f, y),
                strokeWidth = radius * 0.09f,
            )
        }
    }
}

/**
 * A species as the bestiary shows it: full colour once seen, flat shadow while
 * it is only a rumour.
 */
@Composable
fun SpeciesPortrait(
    species: MonsterSpecies,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    shiny: Boolean = false,
    facing: SpriteFacing = SpriteFacing.RIGHT,
) {
    MonsterSpriteStatic(
        blueprint = rememberBlueprint(species, shiny && revealed),
        modifier = modifier,
        facing = facing,
        silhouette = !revealed,
    )
}
