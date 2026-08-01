package com.runeveil.saga.domain.model.monster

/**
 * The fourteen elemental affinities of the Nine Worlds.
 *
 * Every [MonsterSpecies] carries one or two elements, every
 * [com.runeveil.saga.domain.model.battle.Move] carries exactly one. The
 * interaction matrix lives in [com.runeveil.saga.domain.rules.TypeChart].
 *
 * The ordinal order is *stable* — it is persisted by name, never by ordinal,
 * but UI ordering relies on the declaration order (physical elements first,
 * then the esoteric ones).
 *
 * @property displayKey resource key used by the presentation layer for the
 *   localised name. Keeping the key here avoids Android imports in :domain.
 * @property runeGlyph the Elder-Futhark-inspired glyph drawn on badges.
 * @property colorHex the canonical accent colour of the element (sRGB).
 */
enum class Element(
    val displayKey: String,
    val runeGlyph: String,
    val colorHex: Long,
) {
    FIRE("element_fire", "ᚲ", 0xFFE2552B),
    WATER("element_water", "ᛚ", 0xFF2F7FD6),
    ICE("element_ice", "ᛁ", 0xFF7FD8E8),
    WIND("element_wind", "ᛖ", 0xFF9AD6A5),
    EARTH("element_earth", "ᛃ", 0xFF9C7A4A),
    NATURE("element_nature", "ᛒ", 0xFF4CA64C),
    THUNDER("element_thunder", "ᛋ", 0xFFF2C43D),
    LIGHT("element_light", "ᛞ", 0xFFF6EFC9),
    SHADOW("element_shadow", "ᛦ", 0xFF5B4A78),
    SPIRIT("element_spirit", "ᚦ", 0xFFA9C7C7),
    RUNE("element_rune", "ᚱ", 0xFFC79A4B),
    CHAOS("element_chaos", "ᚾ", 0xFF8E2F4F),
    METAL("element_metal", "ᛏ", 0xFF9AA3AD),
    DIVINE("element_divine", "ᚨ", 0xFFE8D27A),
    ;

    /** True for the six "material" elements used by the crafting system. */
    val isMaterial: Boolean
        get() = this in MATERIAL_ELEMENTS

    companion object {
        private val MATERIAL_ELEMENTS = setOf(FIRE, WATER, ICE, WIND, EARTH, METAL)

        /** Safe lookup used when reading persisted content; never throws. */
        fun fromKeyOrNull(raw: String): Element? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}
