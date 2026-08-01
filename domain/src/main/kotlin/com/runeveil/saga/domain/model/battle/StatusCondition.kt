package com.runeveil.saga.domain.model.battle

import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Stat

/**
 * The eight status conditions of Runeveil.
 *
 * A monster carries at most one condition at a time; a new one only lands if
 * the slot is free (see `StatusRules.canApply`). Conditions are *not* purely
 * cosmetic — every one of them has a mechanical hook implemented in
 * [com.runeveil.saga.domain.battle.BattleEngine].
 *
 * @property blocksAction probability that the bearer loses its turn.
 * @property endOfTurnDamageFraction fraction of max HP lost each turn.
 * @property statPenalty stat halved (0.5×) while the condition lasts.
 * @property defaultDurationTurns 0 means "until cured".
 * @property immuneElements elements that cannot receive this condition.
 */
enum class StatusCondition(
    val displayKey: String,
    val shortKey: String,
    val iconKey: String,
    val colorHex: Long,
    val blocksAction: Double = 0.0,
    val endOfTurnDamageFraction: Double = 0.0,
    val statPenalty: Stat? = null,
    val defaultDurationTurns: Int = 0,
    val immuneElements: Set<Element> = emptySet(),
    val curedByDamage: Boolean = false,
) {
    /** Verbrennung — chips HP and halves physical attack. */
    BURN(
        displayKey = "status_burn",
        shortKey = "status_burn_short",
        iconKey = "ic_status_burn",
        colorHex = 0xFFE2552B,
        endOfTurnDamageFraction = 1.0 / 16.0,
        statPenalty = Stat.ATTACK,
        immuneElements = setOf(Element.FIRE),
    ),

    /** Vergiftung — escalating damage, ignores defensive stats. */
    POISON(
        displayKey = "status_poison",
        shortKey = "status_poison_short",
        iconKey = "ic_status_poison",
        colorHex = 0xFF7B4FA8,
        endOfTurnDamageFraction = 1.0 / 12.0,
        immuneElements = setOf(Element.METAL, Element.DIVINE),
    ),

    /** Schlaf — full action lock for 1–3 turns, cured by damage. */
    SLEEP(
        displayKey = "status_sleep",
        shortKey = "status_sleep_short",
        iconKey = "ic_status_sleep",
        colorHex = 0xFF5B6C9C,
        blocksAction = 1.0,
        defaultDurationTurns = 2,
        immuneElements = setOf(Element.SPIRIT),
        curedByDamage = false,
    ),

    /** Einfrieren — action lock with a thaw check every turn. */
    FREEZE(
        displayKey = "status_freeze",
        shortKey = "status_freeze_short",
        iconKey = "ic_status_freeze",
        colorHex = 0xFF7FD8E8,
        blocksAction = 1.0,
        defaultDurationTurns = 3,
        immuneElements = setOf(Element.ICE, Element.FIRE),
    ),

    /** Verwirrung — 33 % chance to strike oneself instead. */
    CONFUSION(
        displayKey = "status_confusion",
        shortKey = "status_confusion_short",
        iconKey = "ic_status_confusion",
        colorHex = 0xFFD98CC0,
        blocksAction = 0.33,
        defaultDurationTurns = 3,
    ),

    /** Blutung — heavy bleed that worsens on contact moves. */
    BLEED(
        displayKey = "status_bleed",
        shortKey = "status_bleed_short",
        iconKey = "ic_status_bleed",
        colorHex = 0xFFB02A37,
        endOfTurnDamageFraction = 1.0 / 10.0,
        defaultDurationTurns = 4,
        immuneElements = setOf(Element.SPIRIT, Element.METAL, Element.RUNE),
    ),

    /** Fluch — halves magical resistance and blocks healing. */
    CURSE(
        displayKey = "status_curse",
        shortKey = "status_curse_short",
        iconKey = "ic_status_curse",
        colorHex = 0xFF4B2E60,
        endOfTurnDamageFraction = 1.0 / 16.0,
        statPenalty = Stat.RESISTANCE,
        defaultDurationTurns = 5,
        immuneElements = setOf(Element.DIVINE),
    ),

    /** Lähmung — quarter speed and a 25 % chance to freeze up. */
    PARALYSIS(
        displayKey = "status_paralysis",
        shortKey = "status_paralysis_short",
        iconKey = "ic_status_paralysis",
        colorHex = 0xFFF2C43D,
        blocksAction = 0.25,
        statPenalty = Stat.SPEED,
        immuneElements = setOf(Element.THUNDER),
    ),
    ;

    /** Conditions that end the moment the bearer acts successfully. */
    val isVolatile: Boolean get() = this == CONFUSION

    /** True when the status prevents any healing (Fluch). */
    val blocksHealing: Boolean get() = this == CURSE

    /** Multiplier applied to the penalised stat. */
    val statPenaltyMultiplier: Double get() = if (this == PARALYSIS) 0.25 else 0.5

    /** Capture bonus granted while the target suffers from this condition. */
    val captureBonus: Double
        get() = when (this) {
            SLEEP, FREEZE -> 2.50
            PARALYSIS, CONFUSION -> 1.60
            BURN, POISON, BLEED, CURSE -> 1.40
        }

    fun canAfflict(primary: Element, secondary: Element?): Boolean =
        primary !in immuneElements && (secondary == null || secondary !in immuneElements)
}
