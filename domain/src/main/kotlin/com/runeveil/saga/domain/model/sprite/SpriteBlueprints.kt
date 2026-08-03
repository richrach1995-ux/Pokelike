package com.runeveil.saga.domain.model.sprite

import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.Rarity
import com.runeveil.saga.domain.model.monster.SizeClass

/**
 * Turns a species into the shapes and colours it is drawn from.
 *
 * ### Determinism
 *
 * Two seeds are used, and both come from stable identifiers:
 *
 *  * the **family** seed decides everything that makes an evolution line
 *    recognisable — body plan, tail, crest, wings, markings, eye count. All
 *    three stages of a line therefore share a silhouette.
 *  * the **species** seed varies the proportions within that silhouette, so a
 *    stage 3 form is visibly bulkier and more ornamented than its stage 1.
 *
 * Nothing here reads a clock or a random source, so a monster looks identical
 * on every device, in every session, and in screenshots taken a year apart.
 * [SpriteBlueprintsTest] pins that guarantee.
 */
object SpriteBlueprints {

    /**
     * @param shiny rotates the palette far around the colour wheel, the way the
     *   shiny system promises a *visibly* different creature rather than a
     *   slightly different one.
     */
    fun of(species: MonsterSpecies, shiny: Boolean = false): SpriteBlueprint {
        val family = Seeded(species.familyId)
        val self = Seeded(species.id)
        // The palette draws from its own stream so that the shiny variant —
        // which needs one extra number for its hue jitter — cannot shift the
        // proportion stream. A shiny is the same creature in other colours,
        // never a differently shaped one.
        val tint = Seeded(species.id + PALETTE_STREAM)

        val archetype = archetypeFor(species, family)
        val stageGrowth = STAGE_GROWTH.getOrElse(species.stage - 1) { 1.05f }
        val sizeScale = sizeScaleFor(species.sizeClass)
        val bulk = stageGrowth * sizeScale

        val floats = archetype in FLOATING_ARCHETYPES
        val legs = legCountFor(archetype, species.stage)

        return SpriteBlueprint(
            speciesId = species.id,
            archetype = archetype,
            palette = paletteFor(species, shiny, tint),

            bodyWidth = (baseWidthFor(archetype) * bulk * self.range(0.94f, 1.06f))
                .coerceIn(0.20f, 0.66f),
            bodyHeight = (baseHeightFor(archetype) * bulk * self.range(0.94f, 1.06f))
                .coerceIn(0.16f, 0.52f),
            bodyRoundness = roundnessFor(archetype, family),

            headScale = (headScaleFor(archetype) * self.range(0.92f, 1.08f) / stageGrowth)
                .coerceIn(0.28f, 0.95f),
            neckLength = neckLengthFor(archetype) * self.range(0.85f, 1.15f),

            legCount = legs,
            legLength = if (legs == 0) 0f else legLengthFor(archetype) * self.range(0.85f, 1.15f),
            hasArms = hasArmsFor(archetype, species.stage),

            tail = tailFor(archetype, family),
            tailLength = family.range(0.55f, 1.0f) * stageGrowth,

            crest = crestFor(species, archetype, family),
            crestSize = family.range(0.5f, 1.0f) * stageGrowth,

            wings = wingsFor(species, archetype, family),
            wingSpan = family.range(0.7f, 1.15f) * stageGrowth,

            eyeCount = eyeCountFor(archetype, species.primaryElement, family),
            eyeSize = family.range(0.8f, 1.25f),

            markings = markingsFor(species, archetype, family),
            markingCount = family.int(3) + 2,

            auraStrength = auraFor(species.rarity),
            floats = floats,

            idleBob = if (floats) 0.045f else 0.022f,
            idlePeriodMs = 1500 + family.int(900),
        )
    }

    // -----------------------------------------------------------------------
    // Body plan
    // -----------------------------------------------------------------------

    /**
     * The element decides the plausible body plans; the family seed picks one
     * of them. Two candidates per element keep the roster varied without
     * letting a fire monster come out as a fish.
     */
    private fun archetypeFor(species: MonsterSpecies, family: Seeded): Archetype {
        val candidates = when (species.primaryElement) {
            Element.FIRE -> listOf(Archetype.BEAST, Archetype.DRAKE)
            Element.WATER -> listOf(Archetype.AQUATIC, Archetype.SERPENT)
            Element.ICE -> listOf(Archetype.BEAST, Archetype.AQUATIC)
            Element.WIND -> listOf(Archetype.AVIAN, Archetype.WISP)
            Element.EARTH -> listOf(Archetype.GOLEM, Archetype.BEAST)
            Element.NATURE -> listOf(Archetype.BEAST, Archetype.INSECTOID)
            Element.THUNDER -> listOf(Archetype.BEAST, Archetype.AVIAN)
            Element.LIGHT -> listOf(Archetype.AVIAN, Archetype.WISP)
            Element.SHADOW -> listOf(Archetype.WISP, Archetype.SERPENT)
            Element.SPIRIT -> listOf(Archetype.WISP, Archetype.AVIAN)
            Element.RUNE -> listOf(Archetype.GOLEM, Archetype.WISP)
            Element.CHAOS -> listOf(Archetype.SERPENT, Archetype.INSECTOID)
            Element.METAL -> listOf(Archetype.GOLEM, Archetype.INSECTOID)
            Element.DIVINE -> listOf(Archetype.DRAKE, Archetype.AVIAN)
        }
        return family.pick(candidates)
    }

    private fun legCountFor(archetype: Archetype, stage: Int): Int = when (archetype) {
        Archetype.BEAST -> 4
        Archetype.DRAKE -> if (stage >= 3) 2 else 4
        Archetype.AVIAN -> 2
        Archetype.GOLEM -> 2
        Archetype.INSECTOID -> 6
        Archetype.SERPENT, Archetype.AQUATIC, Archetype.WISP -> 0
    }

    private fun hasArmsFor(archetype: Archetype, stage: Int): Boolean = when (archetype) {
        Archetype.GOLEM -> true
        Archetype.DRAKE -> stage >= 2
        Archetype.INSECTOID -> stage >= 3
        else -> false
    }

    private fun tailFor(archetype: Archetype, family: Seeded): TailShape = when (archetype) {
        Archetype.BEAST -> family.pick(listOf(TailShape.TAPERED, TailShape.TUFTED))
        Archetype.DRAKE -> family.pick(listOf(TailShape.SPIKED, TailShape.TAPERED))
        Archetype.SERPENT -> TailShape.SERPENTINE
        Archetype.AQUATIC -> TailShape.FINNED
        Archetype.AVIAN -> TailShape.TUFTED
        Archetype.INSECTOID -> family.pick(listOf(TailShape.SPIKED, TailShape.NONE))
        Archetype.GOLEM, Archetype.WISP -> TailShape.NONE
    }

    private fun crestFor(species: MonsterSpecies, archetype: Archetype, family: Seeded): HeadCrest {
        if (archetype == Archetype.WISP) return HeadCrest.HALO
        return when (species.primaryElement) {
            Element.DIVINE, Element.LIGHT -> HeadCrest.HALO
            Element.FIRE, Element.CHAOS -> HeadCrest.SPINES
            Element.NATURE -> HeadCrest.ANTLERS
            Element.EARTH, Element.METAL -> HeadCrest.HORNS
            Element.ICE -> family.pick(listOf(HeadCrest.HORNS, HeadCrest.SPINES))
            else -> family.pick(listOf(HeadCrest.NONE, HeadCrest.MANE, HeadCrest.HORNS))
        }
    }

    private fun wingsFor(species: MonsterSpecies, archetype: Archetype, family: Seeded): WingShape =
        when (archetype) {
            Archetype.DRAKE -> WingShape.MEMBRANE
            Archetype.AVIAN -> WingShape.FEATHERED
            Archetype.INSECTOID -> WingShape.INSECT
            Archetype.WISP -> WingShape.ETHEREAL
            else -> if (species.stage >= 2 && species.elements.any { it in AIRBORNE_ELEMENTS }) {
                family.pick(listOf(WingShape.ETHEREAL, WingShape.FEATHERED))
            } else {
                WingShape.NONE
            }
        }

    private fun eyeCountFor(archetype: Archetype, element: Element, family: Seeded): Int = when {
        archetype == Archetype.WISP -> 1
        archetype == Archetype.INSECTOID -> if (family.int(2) == 0) 4 else 2
        element == Element.CHAOS && family.int(3) == 0 -> 3
        else -> 2
    }

    private fun markingsFor(
        species: MonsterSpecies,
        archetype: Archetype,
        family: Seeded,
    ): MarkingStyle = when {
        archetype == Archetype.GOLEM -> MarkingStyle.PLATES
        species.elements.any { it == Element.RUNE || it == Element.DIVINE } -> MarkingStyle.RUNES
        species.rarity.ordinal >= Rarity.EPIC.ordinal -> MarkingStyle.RUNES
        else -> family.pick(
            listOf(MarkingStyle.NONE, MarkingStyle.STRIPES, MarkingStyle.SPOTS, MarkingStyle.STRIPES),
        )
    }

    private fun auraFor(rarity: Rarity): Float = when (rarity) {
        Rarity.LEGENDARY -> 0.55f
        Rarity.MYTHIC -> 0.8f
        Rarity.DIVINE -> 1.0f
        else -> 0f
    }

    // -----------------------------------------------------------------------
    // Proportions
    // -----------------------------------------------------------------------

    private fun baseWidthFor(archetype: Archetype): Float = when (archetype) {
        Archetype.BEAST -> 0.40f
        Archetype.DRAKE -> 0.44f
        Archetype.SERPENT -> 0.30f
        Archetype.AVIAN -> 0.28f
        Archetype.AQUATIC -> 0.46f
        Archetype.INSECTOID -> 0.38f
        Archetype.GOLEM -> 0.46f
        Archetype.WISP -> 0.30f
    }

    private fun baseHeightFor(archetype: Archetype): Float = when (archetype) {
        Archetype.BEAST -> 0.26f
        Archetype.DRAKE -> 0.30f
        Archetype.SERPENT -> 0.18f
        Archetype.AVIAN -> 0.30f
        Archetype.AQUATIC -> 0.24f
        Archetype.INSECTOID -> 0.20f
        Archetype.GOLEM -> 0.36f
        Archetype.WISP -> 0.30f
    }

    private fun roundnessFor(archetype: Archetype, family: Seeded): Float = when (archetype) {
        Archetype.GOLEM -> family.range(0.05f, 0.25f)
        Archetype.INSECTOID -> family.range(0.35f, 0.6f)
        Archetype.WISP -> 1f
        else -> family.range(0.65f, 1.0f)
    }

    private fun headScaleFor(archetype: Archetype): Float = when (archetype) {
        Archetype.BEAST -> 0.62f
        Archetype.DRAKE -> 0.55f
        Archetype.SERPENT -> 0.50f
        Archetype.AVIAN -> 0.58f
        Archetype.AQUATIC -> 0.52f
        Archetype.INSECTOID -> 0.48f
        Archetype.GOLEM -> 0.50f
        Archetype.WISP -> 0.85f
    }

    private fun neckLengthFor(archetype: Archetype): Float = when (archetype) {
        Archetype.DRAKE -> 0.30f
        Archetype.AVIAN -> 0.22f
        Archetype.SERPENT -> 0.34f
        Archetype.GOLEM -> 0.06f
        Archetype.WISP -> 0f
        else -> 0.14f
    }

    private fun legLengthFor(archetype: Archetype): Float = when (archetype) {
        Archetype.AVIAN -> 0.24f
        Archetype.GOLEM -> 0.16f
        Archetype.INSECTOID -> 0.14f
        else -> 0.18f
    }

    private fun sizeScaleFor(size: SizeClass): Float = when (size) {
        SizeClass.TINY -> 0.66f
        SizeClass.SMALL -> 0.82f
        SizeClass.MEDIUM -> 1.0f
        SizeClass.LARGE -> 1.14f
        SizeClass.HUGE -> 1.26f
        SizeClass.COLOSSAL -> 1.38f
    }

    // -----------------------------------------------------------------------
    // Palette
    // -----------------------------------------------------------------------

    private fun paletteFor(species: MonsterSpecies, shiny: Boolean, self: Seeded): SpritePalette {
        val primary = species.primaryElement.colorHex
        val secondary = species.secondaryElement?.colorHex ?: SpriteColors.shift(primary, 24f)

        // A shiny is a different creature at a glance, not a slightly warmer
        // one: the whole palette turns roughly two thirds around the wheel.
        val rotation = if (shiny) 150f + self.range(-25f, 25f) else 0f
        val body = SpriteColors.shift(primary, rotation)
        val accent = SpriteColors.shift(secondary, rotation)

        return SpritePalette(
            body = body,
            bodyShade = SpriteColors.scale(body, 0.62f),
            belly = SpriteColors.mix(body, SpriteColors.PARCHMENT, 0.58f),
            accent = accent,
            outline = SpriteColors.scale(SpriteColors.mix(body, SpriteColors.NIGHT, 0.7f), 0.9f),
            eye = if (shiny) SpriteColors.GOLD else SpriteColors.mix(accent, SpriteColors.PARCHMENT, 0.7f),
            glow = SpriteColors.mix(accent, SpriteColors.PARCHMENT, 0.35f),
        )
    }

    /** Suffix that forks a second, independent stream from the species id. */
    private const val PALETTE_STREAM = "#palette"

    private val STAGE_GROWTH = floatArrayOf(0.80f, 0.94f, 1.06f)
    private val FLOATING_ARCHETYPES = setOf(Archetype.WISP, Archetype.AQUATIC, Archetype.SERPENT)
    private val AIRBORNE_ELEMENTS = setOf(Element.WIND, Element.LIGHT, Element.DIVINE, Element.SPIRIT)
}

/**
 * A small, fully specified pseudo-random stream.
 *
 * [kotlin.random.Random] is not used because its algorithm is an implementation
 * detail that may change between Kotlin versions — a sprite must never change
 * because a dependency was upgraded. SplitMix64 is a published, fixed
 * algorithm, and [String.hashCode] is specified by the Java language, so the
 * whole chain from species id to silhouette is pinned.
 */
internal class Seeded(seedSource: String) {

    private var state: Long = seedSource.hashCode().toLong() * GOLDEN + SALT

    private fun next(): Long {
        state += GAMMA
        var z = state
        z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
        z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
        return z xor (z ushr 31)
    }

    /** Uniform in `0 until bound`. */
    fun int(bound: Int): Int {
        require(bound > 0) { "bound muss positiv sein" }
        return ((next() ushr 1) % bound).toInt()
    }

    /** Uniform in `[0, 1)`. */
    fun float(): Float = ((next() ushr 11).toDouble() / (1L shl 53).toDouble()).toFloat()

    /** Uniform in `[min, max)`. */
    fun range(min: Float, max: Float): Float = min + float() * (max - min)

    fun <T> pick(values: List<T>): T = values[int(values.size)]

    private companion object {
        const val GOLDEN = -0x61c8_8646_80b5_83e5L
        const val GAMMA = -0x61c8_8646_80b5_83e5L
        const val SALT = 0x2545_F491_4F6C_DD1DL
    }
}

/**
 * Colour arithmetic on packed `0xAARRGGBB` values.
 *
 * Kept in :domain so the blueprint is complete on its own and testable without
 * an Android runtime.
 */
internal object SpriteColors {

    const val PARCHMENT = 0xFFF3E9D2L
    const val NIGHT = 0xFF0E0B14L
    const val GOLD = 0xFFE8D27AL

    /** Multiplies the RGB channels, keeping alpha — used for shading. */
    fun scale(color: Long, factor: Float): Long {
        val r = ((color shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        return pack(alphaOf(color), r, g, b)
    }

    /** Linear blend; [t] = 0 returns [a], 1 returns [b]. */
    fun mix(a: Long, b: Long, t: Float): Long {
        val clamped = t.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val from = (a shr shift and 0xFF).toInt()
            val to = (b shr shift and 0xFF).toInt()
            return (from + (to - from) * clamped).toInt().coerceIn(0, 255)
        }
        return pack(alphaOf(a), channel(16), channel(8), channel(0))
    }

    /** Rotates the hue by [degrees], preserving saturation and value. */
    fun shift(color: Long, degrees: Float): Long {
        if (degrees == 0f) return color
        val r = (color shr 16 and 0xFF).toInt() / 255f
        val g = (color shr 8 and 0xFF).toInt() / 255f
        val b = (color and 0xFF).toInt() / 255f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        var hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        hue = ((hue + degrees) % 360f + 360f) % 360f

        val saturation = if (max == 0f) 0f else delta / max
        val chroma = max * saturation
        val second = chroma * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
        val offset = max - chroma

        val (nr, ng, nb) = when ((hue / 60f).toInt()) {
            0 -> Triple(chroma, second, 0f)
            1 -> Triple(second, chroma, 0f)
            2 -> Triple(0f, chroma, second)
            3 -> Triple(0f, second, chroma)
            4 -> Triple(second, 0f, chroma)
            else -> Triple(chroma, 0f, second)
        }
        return pack(
            alphaOf(color),
            ((nr + offset) * 255f).toInt().coerceIn(0, 255),
            ((ng + offset) * 255f).toInt().coerceIn(0, 255),
            ((nb + offset) * 255f).toInt().coerceIn(0, 255),
        )
    }

    private fun alphaOf(color: Long): Int = (color shr 24 and 0xFF).toInt()

    private fun pack(a: Int, r: Int, g: Int, b: Int): Long =
        (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}
