package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.Element

/**
 * The complete elemental effectiveness matrix of Runeveil.
 *
 * Design rules the table is required to satisfy (enforced by
 * `TypeChartBalanceTest`):
 *
 *  * every element is **super effective** against 2–4 elements,
 *  * every element is **hit super effectively** by 2–4 elements,
 *  * no element is super effective against itself,
 *  * immunities are rare and always thematically justified,
 *  * no element is strictly dominant (offensive score spread ≤ 3.0).
 *
 * Multipliers are multiplicative across the (up to two) defending elements, so
 * a dual-type monster can take 4× or 0.25× damage.
 */
object TypeChart {

    /** Damage multiplier applied when the defender resists twice. */
    const val IMMUNE = 0.0
    const val RESIST = 0.5
    const val NEUTRAL = 1.0
    const val SUPER = 2.0

    /**
     * Offensive relations, keyed by the attacking element.
     *
     * `strong`  — the attack deals [SUPER] damage.
     * `weak`    — the attack deals [RESIST] damage.
     * `immune`  — the attack deals [IMMUNE] damage.
     */
    private data class Relation(
        val strong: Set<Element>,
        val weak: Set<Element>,
        val immune: Set<Element> = emptySet(),
    )

    private val relations: Map<Element, Relation> = mapOf(
        Element.FIRE to Relation(
            strong = setOf(Element.ICE, Element.NATURE, Element.METAL),
            weak = setOf(Element.FIRE, Element.WATER, Element.EARTH, Element.DIVINE),
        ),
        Element.WATER to Relation(
            strong = setOf(Element.FIRE, Element.EARTH),
            weak = setOf(Element.WATER, Element.NATURE, Element.ICE, Element.DIVINE),
        ),
        Element.ICE to Relation(
            strong = setOf(Element.NATURE, Element.WIND, Element.EARTH),
            weak = setOf(Element.ICE, Element.FIRE, Element.WATER, Element.METAL),
        ),
        Element.WIND to Relation(
            // A gale scatters a storm front before it can gather.
            strong = setOf(Element.NATURE, Element.SPIRIT, Element.THUNDER),
            weak = setOf(Element.WIND, Element.METAL, Element.EARTH),
        ),
        Element.EARTH to Relation(
            strong = setOf(Element.FIRE, Element.THUNDER, Element.METAL),
            weak = setOf(Element.EARTH, Element.NATURE, Element.WIND),
        ),
        Element.NATURE to Relation(
            strong = setOf(Element.WATER, Element.EARTH),
            weak = setOf(Element.NATURE, Element.FIRE, Element.ICE, Element.WIND, Element.METAL),
        ),
        Element.THUNDER to Relation(
            strong = setOf(Element.WATER, Element.WIND, Element.METAL),
            weak = setOf(Element.THUNDER, Element.NATURE),
            // Lightning is grounded by stone and soil.
            immune = setOf(Element.EARTH),
        ),
        Element.LIGHT to Relation(
            strong = setOf(Element.SHADOW, Element.SPIRIT, Element.CHAOS),
            weak = setOf(Element.LIGHT, Element.RUNE, Element.METAL),
        ),
        Element.SHADOW to Relation(
            strong = setOf(Element.LIGHT, Element.RUNE),
            weak = setOf(Element.SHADOW, Element.METAL),
            // The radiance of the Æsir cannot be dimmed by mere umbra.
            immune = setOf(Element.DIVINE),
        ),
        Element.SPIRIT to Relation(
            strong = setOf(Element.SHADOW, Element.CHAOS),
            weak = setOf(Element.SPIRIT, Element.RUNE, Element.DIVINE),
        ),
        Element.RUNE to Relation(
            // Bound glyphs constrain even the Æsir — the oldest law of the Nine.
            strong = setOf(Element.CHAOS, Element.SPIRIT, Element.METAL, Element.DIVINE),
            weak = setOf(Element.RUNE),
        ),
        Element.CHAOS to Relation(
            strong = setOf(Element.DIVINE, Element.LIGHT),
            weak = setOf(Element.CHAOS, Element.SHADOW),
            // Bound glyphs unmake formless chaos before it lands.
            immune = setOf(Element.RUNE),
        ),
        Element.METAL to Relation(
            strong = setOf(Element.ICE, Element.RUNE, Element.WIND),
            weak = setOf(Element.METAL, Element.FIRE, Element.THUNDER, Element.WATER, Element.EARTH),
            // Wrought iron passes straight through the unbodied.
            immune = setOf(Element.SPIRIT),
        ),
        Element.DIVINE to Relation(
            strong = setOf(Element.CHAOS, Element.SHADOW, Element.SPIRIT),
            weak = setOf(Element.DIVINE, Element.RUNE, Element.METAL),
        ),
    )

    /** Dense lookup table built once at class-init; `matrix[attacker][defender]`. */
    private val matrix: Array<DoubleArray> = Array(Element.entries.size) { attackerIndex ->
        val attacker = Element.entries[attackerIndex]
        val relation = relations.getValue(attacker)
        DoubleArray(Element.entries.size) { defenderIndex ->
            val defender = Element.entries[defenderIndex]
            when (defender) {
                in relation.immune -> IMMUNE
                in relation.strong -> SUPER
                in relation.weak -> RESIST
                else -> NEUTRAL
            }
        }
    }

    /** Multiplier of [attacker] against a single [defender] element. */
    fun multiplier(attacker: Element, defender: Element): Double =
        matrix[attacker.ordinal][defender.ordinal]

    /**
     * Multiplier of [attacker] against a monster whose typing is
     * [primary] (+ optional [secondary]). Multiplicative, so values range from
     * 0.0 to 4.0.
     */
    fun multiplier(attacker: Element, primary: Element, secondary: Element?): Double {
        val base = multiplier(attacker, primary)
        return if (secondary == null || secondary == primary) base
        else base * multiplier(attacker, secondary)
    }

    /** Human-facing bucket used by the bestiary and battle log. */
    fun describe(multiplier: Double): Effectiveness = when {
        multiplier <= 0.0 -> Effectiveness.IMMUNE
        multiplier < 0.5 -> Effectiveness.DOUBLE_RESISTED
        multiplier < 1.0 -> Effectiveness.RESISTED
        multiplier == 1.0 -> Effectiveness.NEUTRAL
        multiplier <= 2.0 -> Effectiveness.SUPER_EFFECTIVE
        else -> Effectiveness.DEVASTATING
    }

    /** Elements this [element] takes super-effective damage from. */
    fun weaknessesOf(element: Element): List<Element> =
        Element.entries.filter { multiplier(it, element) > NEUTRAL }

    /** Elements this [element] resists (including immunities). */
    fun resistancesOf(element: Element): List<Element> =
        Element.entries.filter { multiplier(it, element) in 0.0..<NEUTRAL }

    /** Elements that cannot damage this [element] at all. */
    fun immunitiesOf(element: Element): List<Element> =
        Element.entries.filter { multiplier(it, element) == IMMUNE }

    /** Elements this [element] hits for super-effective damage. */
    fun strengthsOf(element: Element): List<Element> =
        relations.getValue(element).strong.toList()

    /**
     * Aggregated defensive profile of a (dual-)typed monster, used by the
     * bestiary screen and by [com.runeveil.saga.domain.battle.BattleAi].
     */
    fun defensiveProfile(primary: Element, secondary: Element?): Map<Element, Double> =
        Element.entries.associateWith { multiplier(it, primary, secondary) }
}

/** Coarse effectiveness bucket used for battle-log strings and UI colouring. */
enum class Effectiveness(val logKey: String) {
    IMMUNE("effect_immune"),
    DOUBLE_RESISTED("effect_double_resisted"),
    RESISTED("effect_resisted"),
    NEUTRAL("effect_neutral"),
    SUPER_EFFECTIVE("effect_super"),
    DEVASTATING("effect_devastating"),
    ;

    val isAdvantage: Boolean get() = this == SUPER_EFFECTIVE || this == DEVASTATING
}
