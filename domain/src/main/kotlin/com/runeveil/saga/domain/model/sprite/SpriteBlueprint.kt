package com.runeveil.saga.domain.model.sprite

/**
 * A creature's *drawing instructions* — everything the renderer needs to put a
 * monster on screen, with no bitmap involved.
 *
 * ### Why procedural instead of image files
 *
 * The game ships 255 species in 106 evolution families. Hand-drawn sprites for
 * all of them do not exist, and shipping 255 placeholder images would be both
 * dishonest and a download the player pays for. A blueprint is derived
 * *deterministically* from the species itself, so:
 *
 *  * every species looks the same on every device and in every session — the
 *    seed is the species id, never a clock or a random source;
 *  * a family shares a silhouette and grows visibly with each stage;
 *  * the elements decide the palette, so a fire/metal hybrid reads as one at a
 *    glance;
 *  * nothing is copied from anywhere — every shape is generated from rules
 *    written here.
 *
 * The blueprint is pure data. It carries no Android types and is fully unit
 * tested; the renderer in the app module only reads it.
 *
 * All colours are sRGB `0xAARRGGBB` packed into a [Long], the same convention
 * [com.runeveil.saga.domain.model.monster.Element.colorHex] already uses.
 */
data class SpriteBlueprint(
    val speciesId: String,
    val archetype: Archetype,
    val palette: SpritePalette,

    /** Torso extent as a fraction of the drawing box (0…1). */
    val bodyWidth: Float,
    val bodyHeight: Float,
    /** How rounded the torso is: 0 = angular block, 1 = perfect ellipse. */
    val bodyRoundness: Float,

    /** Head size relative to the torso, and where it sits. */
    val headScale: Float,
    val neckLength: Float,

    val legCount: Int,
    val legLength: Float,
    val hasArms: Boolean,

    val tail: TailShape,
    val tailLength: Float,

    val crest: HeadCrest,
    val crestSize: Float,

    val wings: WingShape,
    val wingSpan: Float,

    val eyeCount: Int,
    val eyeSize: Float,

    val markings: MarkingStyle,
    val markingCount: Int,

    /** Legendary and above glow; 0 for ordinary monsters. */
    val auraStrength: Float,
    /** Hovering creatures are drawn without ground contact and bob further. */
    val floats: Boolean,

    /** Idle motion, in fractions of the drawing box and milliseconds. */
    val idleBob: Float,
    val idlePeriodMs: Int,
) {
    /** True when the silhouette has no legs to plant on the ground. */
    val isGrounded: Boolean get() = !floats && legCount > 0
}

/** The body plans a species can be drawn from. */
enum class Archetype {
    /** Four-legged, mammalian outline — the default for land species. */
    BEAST,

    /** Heavy-bodied, winged, long-necked. */
    DRAKE,

    /** Legless, coiled, long. */
    SERPENT,

    /** Upright, light, feathered, beaked. */
    AVIAN,

    /** Streamlined with fins instead of limbs. */
    AQUATIC,

    /** Segmented, many-legged, antennae. */
    INSECTOID,

    /** Broad, angular, mineral — carved rather than grown. */
    GOLEM,

    /** Bodiless: a drifting core wrapped in light. */
    WISP,
}

enum class TailShape { NONE, TAPERED, TUFTED, FINNED, SPIKED, SERPENTINE }

enum class HeadCrest { NONE, HORNS, ANTLERS, SPINES, HALO, MANE }

enum class WingShape { NONE, FEATHERED, MEMBRANE, INSECT, ETHEREAL }

enum class MarkingStyle { NONE, STRIPES, SPOTS, RUNES, PLATES }

/**
 * The seven colours a sprite is drawn with. Derived from the species' elements
 * so that the type is readable before the badge is.
 */
data class SpritePalette(
    /** Main body fill. */
    val body: Long,
    /** Shaded underside of the body. */
    val bodyShade: Long,
    /** Belly, chest and inner limbs. */
    val belly: Long,
    /** Horns, fins, markings — the secondary element speaks here. */
    val accent: Long,
    /** Silhouette line. */
    val outline: Long,
    val eye: Long,
    /** Aura and elemental glow. */
    val glow: Long,
)
