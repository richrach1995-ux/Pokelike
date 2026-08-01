package com.runeveil.saga.domain.model.battle

import com.runeveil.saga.domain.model.monster.Element

/**
 * Weather is both an overworld state (per region, see
 * `com.runeveil.saga.domain.model.world.WeatherSystem`) and a battle field
 * state. The battle-side effects live here.
 *
 * @property boosted elements whose damage is multiplied by [BOOST].
 * @property dampened elements whose damage is multiplied by [DAMPEN].
 * @property chipDamageFraction fraction of max HP lost per turn by monsters
 *   that are not immune.
 * @property chipImmuneElements elements that ignore the chip damage.
 * @property accuracyModifier multiplier applied to every accuracy roll.
 * @property evasionModifier multiplier applied to every evasion value.
 */
enum class BattleWeather(
    val displayKey: String,
    val particleKey: String,
    val ambienceKey: String?,
    val boosted: Set<Element> = emptySet(),
    val dampened: Set<Element> = emptySet(),
    val chipDamageFraction: Double = 0.0,
    val chipImmuneElements: Set<Element> = emptySet(),
    val accuracyModifier: Double = 1.0,
    val evasionModifier: Double = 1.0,
    val speedModifier: Double = 1.0,
) {
    CLEAR("weather_clear", "fx_none", null),

    /** Regen. */
    RAIN(
        displayKey = "weather_rain",
        particleKey = "fx_rain",
        ambienceKey = "amb_rain",
        boosted = setOf(Element.WATER, Element.NATURE),
        dampened = setOf(Element.FIRE),
    ),

    /** Gewitter. */
    THUNDERSTORM(
        displayKey = "weather_thunderstorm",
        particleKey = "fx_storm",
        ambienceKey = "amb_storm",
        boosted = setOf(Element.THUNDER, Element.WATER),
        dampened = setOf(Element.FIRE, Element.LIGHT),
        accuracyModifier = 0.92,
    ),

    /** Schnee. */
    SNOW(
        displayKey = "weather_snow",
        particleKey = "fx_snow",
        ambienceKey = "amb_snow",
        boosted = setOf(Element.ICE),
        dampened = setOf(Element.FIRE),
        chipDamageFraction = 1.0 / 20.0,
        chipImmuneElements = setOf(Element.ICE, Element.METAL, Element.SPIRIT),
        evasionModifier = 1.08,
    ),

    /** Nebel. */
    FOG(
        displayKey = "weather_fog",
        particleKey = "fx_fog",
        ambienceKey = "amb_fog",
        boosted = setOf(Element.SPIRIT, Element.SHADOW),
        dampened = setOf(Element.LIGHT),
        accuracyModifier = 0.80,
        evasionModifier = 1.15,
    ),

    /** Hitze. */
    HEATWAVE(
        displayKey = "weather_heatwave",
        particleKey = "fx_heat",
        ambienceKey = "amb_heat",
        boosted = setOf(Element.FIRE),
        dampened = setOf(Element.ICE, Element.WATER),
        chipDamageFraction = 1.0 / 24.0,
        chipImmuneElements = setOf(Element.FIRE, Element.EARTH, Element.CHAOS),
    ),

    /** Nordlicht — the aurora of the Bifröst rift. */
    AURORA(
        displayKey = "weather_aurora",
        particleKey = "fx_aurora",
        ambienceKey = "amb_aurora",
        boosted = setOf(Element.LIGHT, Element.RUNE, Element.DIVINE),
        dampened = setOf(Element.SHADOW, Element.CHAOS),
    ),

    /** Sandsturm. */
    SANDSTORM(
        displayKey = "weather_sandstorm",
        particleKey = "fx_sand",
        ambienceKey = "amb_sand",
        boosted = setOf(Element.EARTH, Element.METAL),
        dampened = setOf(Element.WIND),
        chipDamageFraction = 1.0 / 18.0,
        chipImmuneElements = setOf(Element.EARTH, Element.METAL, Element.SPIRIT),
        accuracyModifier = 0.90,
    ),
    ;

    /** Damage multiplier this weather applies to a move of [element]. */
    fun damageMultiplier(element: Element): Double = when (element) {
        in boosted -> BOOST
        in dampened -> DAMPEN
        else -> 1.0
    }

    val causesChipDamage: Boolean get() = chipDamageFraction > 0.0

    companion object {
        const val BOOST = 1.35
        const val DAMPEN = 0.70

        /** Default number of turns a weather-setting move lasts. */
        const val DEFAULT_TURNS = 5
    }
}

/**
 * Persistent field effects that are not weather: barriers, hazards and the
 * Runenkreis (rune circle) laid down by support moves.
 */
data class FieldEffect(
    val id: String,
    val nameKey: String,
    val side: BattleSide,
    val remainingTurns: Int,
    val magnitude: Double = 1.0,
)

/** Which team a battler or effect belongs to. */
enum class BattleSide {
    PLAYER,
    ENEMY,
    ;

    val opposite: BattleSide get() = if (this == PLAYER) ENEMY else PLAYER
}
