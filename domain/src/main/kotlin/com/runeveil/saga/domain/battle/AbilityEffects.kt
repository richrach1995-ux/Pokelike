package com.runeveil.saga.domain.battle

import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.battle.MoveCategory
import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.rules.Effectiveness

/**
 * The complete catalogue of passive abilities.
 *
 * Every ability in `assets/content/abilities.json` maps to one of the effect
 * ids below; `ContentValidator` rejects content that references an id which is
 * not handled here, so there is no silent no-op path.
 *
 * The engine calls these hooks at well-defined points of the turn pipeline.
 * All functions are pure and side-effect free.
 */
object AbilityEffects {

    /** Every effect id the engine understands. Used by content validation. */
    val KNOWN_EFFECT_IDS: Set<String> = setOf(
        "ember_hide", "frost_hide", "storm_hide", "stone_hide", "gale_hide",
        "rune_ward", "giant_frame", "bulwark", "spirit_veil",
        "iron_will", "clear_mind", "divine_grace", "chaos_heart",
        "cleansing_light", "regrowth", "sun_drinker", "moon_drinker",
        "storm_born", "snow_walker", "sand_veil", "gale_step",
        "venom_touch", "ember_touch", "frost_touch", "curse_touch",
        "thorn_mail", "soul_siphon", "arcane_focus", "brute_force",
        "berserker", "last_stand", "keen_eye", "rune_siphon",
        "call_rain", "call_snow", "call_sand", "call_heat", "call_aurora",
        "intimidating_roar", "runic_bulwark", "lucky_star", "swift_strike",
    )

    /** Effect on incoming damage. 1.0 = unchanged. */
    fun incomingDamageMultiplier(
        effectId: String?,
        move: Move,
        effectiveness: Effectiveness,
        defender: Battler,
        weather: BattleWeather,
    ): Double = when (effectId) {
        "ember_hide" -> if (move.element == Element.FIRE) 0.5 else 1.0
        "frost_hide" -> if (move.element == Element.ICE) 0.5 else 1.0
        "storm_hide" -> if (move.element == Element.THUNDER) 0.5 else 1.0
        "stone_hide" -> if (move.element == Element.EARTH) 0.5 else 1.0
        "gale_hide" -> if (move.element == Element.WIND) 0.5 else 1.0
        "rune_ward" -> if (effectiveness.isAdvantage) 0.75 else 1.0
        "giant_frame" -> 0.90
        "bulwark" -> if (move.category == MoveCategory.PHYSICAL) 0.75 else 1.0
        "spirit_veil" -> if (move.category == MoveCategory.MAGICAL) 0.75 else 1.0
        "runic_bulwark" -> if (defender.monster.hpFraction >= 1f) 0.5 else 1.0
        "sand_veil" -> if (weather == BattleWeather.SANDSTORM) 0.85 else 1.0
        else -> 1.0
    }

    /** Effect on outgoing damage. 1.0 = unchanged. */
    fun outgoingDamageMultiplier(effectId: String?, move: Move, attacker: Battler): Double =
        when (effectId) {
            "arcane_focus" -> if (move.category == MoveCategory.MAGICAL) 1.15 else 1.0
            "brute_force" -> if (move.category == MoveCategory.PHYSICAL) 1.15 else 1.0
            "berserker" -> if (attacker.monster.hpFraction <= 0.34f) 1.5 else 1.0
            "chaos_heart" -> if (move.element == Element.CHAOS) 1.20 else 1.0
            "swift_strike" -> if (move.priority > 0) 1.20 else 1.0
            else -> 1.0
        }

    /** Multiplier applied to the bearer's speed for initiative purposes. */
    fun speedMultiplier(effectId: String?, weather: BattleWeather): Double = when (effectId) {
        "storm_born" -> if (weather == BattleWeather.RAIN || weather == BattleWeather.THUNDERSTORM) 1.5 else 1.0
        "snow_walker" -> if (weather == BattleWeather.SNOW) 1.3 else 1.0
        else -> 1.0
    }

    /** Multiplier applied to the *attacker's* hit chance against this bearer. */
    fun evadeMultiplier(effectId: String?, weather: BattleWeather): Double = when (effectId) {
        "gale_step" -> 0.90
        "sand_veil" -> if (weather == BattleWeather.SANDSTORM) 0.85 else 1.0
        "snow_walker" -> if (weather == BattleWeather.SNOW) 0.90 else 1.0
        else -> 1.0
    }

    /** True when the ability makes its bearer immune to [condition]. */
    fun preventsStatus(effectId: String?, condition: StatusCondition): Boolean = when (effectId) {
        "iron_will" -> condition == StatusCondition.SLEEP || condition == StatusCondition.CONFUSION
        "clear_mind" -> condition == StatusCondition.CONFUSION
        "divine_grace" -> condition == StatusCondition.CURSE
        "chaos_heart" -> condition == StatusCondition.CURSE
        "keen_eye" -> false
        else -> false
    }

    /** Status the bearer inflicts when *hit by* a contact move, plus its chance. */
    fun contactStatus(effectId: String?): Pair<StatusCondition, Double>? = when (effectId) {
        "venom_touch" -> StatusCondition.POISON to 0.25
        "ember_touch" -> StatusCondition.BURN to 0.25
        "frost_touch" -> StatusCondition.FREEZE to 0.12
        "curse_touch" -> StatusCondition.CURSE to 0.18
        else -> null
    }

    /** Fraction of the damage dealt that is reflected onto a contact attacker. */
    fun contactRecoilFraction(effectId: String?): Double =
        if (effectId == "thorn_mail") 0.125 else 0.0

    /** Fraction of max HP healed at the end of each turn. */
    fun endOfTurnHealFraction(effectId: String?, weather: BattleWeather, isNight: Boolean): Double =
        when (effectId) {
            "regrowth" -> 1.0 / 16.0
            "sun_drinker" -> if (weather == BattleWeather.HEATWAVE) 1.0 / 10.0 else 0.0
            "moon_drinker" -> if (isNight) 1.0 / 12.0 else 0.0
            else -> 0.0
        }

    /** Fraction of the damage dealt that is drained back as HP. */
    fun drainFraction(effectId: String?): Double =
        if (effectId == "soul_siphon") 0.125 else 0.0

    /** Chance to shrug off one's own status condition at end of turn. */
    fun selfCureChance(effectId: String?): Double =
        if (effectId == "cleansing_light") 0.30 else 0.0

    /** Chance to restore a point of PP at end of turn. */
    fun ppRestoreChance(effectId: String?): Double =
        if (effectId == "rune_siphon") 0.25 else 0.0

    /** True when the bearer ignores this weather's chip damage. */
    fun ignoresWeatherChip(effectId: String?, weather: BattleWeather): Boolean = when (effectId) {
        "snow_walker" -> weather == BattleWeather.SNOW
        "sand_veil" -> weather == BattleWeather.SANDSTORM
        "sun_drinker" -> weather == BattleWeather.HEATWAVE
        "giant_frame" -> true
        else -> false
    }

    /** Ability that lets the bearer survive one lethal hit with 1 HP. */
    fun survivesLethal(effectId: String?): Boolean = effectId == "last_stand"

    /** Number of turns subtracted from an incoming status duration. */
    fun statusDurationModifier(effectId: String?): Int =
        if (effectId == "divine_grace") -1 else 0

    /** Additive bonus to the bearer's critical-hit chance. */
    fun critChanceBonus(effectId: String?): Double = when (effectId) {
        "keen_eye" -> 0.05
        "lucky_star" -> 0.03
        else -> 0.0
    }

    /** True when the bearer's accuracy cannot be lowered. */
    fun accuracyCannotDrop(effectId: String?): Boolean = effectId == "keen_eye"

    /** Weather this ability summons when its bearer enters the field. */
    fun summonedWeather(effectId: String?): BattleWeather? = when (effectId) {
        "call_rain" -> BattleWeather.RAIN
        "call_snow" -> BattleWeather.SNOW
        "call_sand" -> BattleWeather.SANDSTORM
        "call_heat" -> BattleWeather.HEATWAVE
        "call_aurora" -> BattleWeather.AURORA
        else -> null
    }

    /** Stat the ability lowers on every opposing battler upon entry. */
    fun entryDebuff(effectId: String?): Pair<Stat, Int>? = when (effectId) {
        "intimidating_roar" -> Stat.ATTACK to -1
        else -> null
    }

    /** Extra multiplier on capture rolls made by the *player* while this
     *  ability's bearer is on the field (Glücksstern). */
    fun captureLuckMultiplier(effectId: String?): Double =
        if (effectId == "lucky_star") 1.15 else 1.0
}
