package com.runeveil.saga.domain.model.monster

/**
 * The seven persistent statistics every monster owns.
 *
 * [LCK] (Glück) is a soft stat: it nudges critical hits, capture rolls,
 * status resistance and rare-drop chances rather than damage directly.
 */
enum class Stat(val displayKey: String, val abbreviationKey: String) {
    HP("stat_hp", "stat_hp_short"),
    ATTACK("stat_attack", "stat_attack_short"),
    DEFENSE("stat_defense", "stat_defense_short"),
    MAGIC("stat_magic", "stat_magic_short"),
    RESISTANCE("stat_resistance", "stat_resistance_short"),
    SPEED("stat_speed", "stat_speed_short"),
    LUCK("stat_luck", "stat_luck_short"),
    ;

    /** HP behaves differently in every formula, so it is worth a fast check. */
    val isVital: Boolean get() = this == HP

    companion object {
        /** Every stat except [HP] — the set that can be buffed/debuffed in battle. */
        val BATTLE_STATS: List<Stat> = entries.filterNot { it.isVital }
    }
}

/**
 * An immutable set of seven values, one per [Stat].
 *
 * Used for three different concepts, all of which share the same shape:
 *  * **base stats** of a species (5 … 190),
 *  * **genes** of an individual (0 … 31, the inherited potential),
 *  * **training** values earned through battle (0 … 255 per stat, 510 total).
 */
data class StatBlock(
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val magic: Int = 0,
    val resistance: Int = 0,
    val speed: Int = 0,
    val luck: Int = 0,
) {
    operator fun get(stat: Stat): Int = when (stat) {
        Stat.HP -> hp
        Stat.ATTACK -> attack
        Stat.DEFENSE -> defense
        Stat.MAGIC -> magic
        Stat.RESISTANCE -> resistance
        Stat.SPEED -> speed
        Stat.LUCK -> luck
    }

    /** Returns a copy with [stat] replaced by [value]. */
    fun with(stat: Stat, value: Int): StatBlock = when (stat) {
        Stat.HP -> copy(hp = value)
        Stat.ATTACK -> copy(attack = value)
        Stat.DEFENSE -> copy(defense = value)
        Stat.MAGIC -> copy(magic = value)
        Stat.RESISTANCE -> copy(resistance = value)
        Stat.SPEED -> copy(speed = value)
        Stat.LUCK -> copy(luck = value)
    }

    /** Element-wise transformation, handy for clamping or scaling. */
    inline fun map(transform: (Stat, Int) -> Int): StatBlock =
        Stat.entries.fold(this) { acc, stat -> acc.with(stat, transform(stat, this[stat])) }

    operator fun plus(other: StatBlock): StatBlock = map { stat, value -> value + other[stat] }

    /** Sum of all seven values — the "base stat total" shown in the bestiary. */
    val total: Int get() = hp + attack + defense + magic + resistance + speed + luck

    /** Clamps every value into [min]..[max]. */
    fun coerce(min: Int, max: Int): StatBlock = map { _, value -> value.coerceIn(min, max) }

    fun asMap(): Map<Stat, Int> = Stat.entries.associateWith { this[it] }

    companion object {
        val ZERO = StatBlock()

        /** Upper bound for a single gene value. */
        const val MAX_GENE = 31

        /** Upper bound for a single training value. */
        const val MAX_TRAINING_PER_STAT = 255

        /** Upper bound for the sum of all training values. */
        const val MAX_TRAINING_TOTAL = 510

        fun uniform(value: Int) = StatBlock(value, value, value, value, value, value, value)

        fun of(values: Map<Stat, Int>): StatBlock =
            Stat.entries.fold(ZERO) { acc, stat -> acc.with(stat, values[stat] ?: 0) }
    }
}

/**
 * Stat *stages* used inside battle: −6 … +6 per stat, plus the two derived
 * combat ratings (accuracy and evasion) that only exist while fighting.
 */
data class StatStages(
    private val stages: Map<Stat, Int> = emptyMap(),
    val accuracy: Int = 0,
    val evasion: Int = 0,
) {
    operator fun get(stat: Stat): Int = stages[stat] ?: 0

    /**
     * Applies [delta] stages to [stat], clamped to ±[MAX_STAGE].
     * Returns the new stages plus how much was actually applied (0 when the
     * stat was already capped — the battle log needs that distinction).
     */
    fun apply(stat: Stat, delta: Int): Pair<StatStages, Int> {
        val current = this[stat]
        val next = (current + delta).coerceIn(-MAX_STAGE, MAX_STAGE)
        return copy(stages = stages + (stat to next)) to (next - current)
    }

    fun applyAccuracy(delta: Int): StatStages =
        copy(accuracy = (accuracy + delta).coerceIn(-MAX_STAGE, MAX_STAGE))

    fun applyEvasion(delta: Int): StatStages =
        copy(evasion = (evasion + delta).coerceIn(-MAX_STAGE, MAX_STAGE))

    /** Drops every negative stage (used by cleansing effects). */
    fun clearNegative(): StatStages = copy(
        stages = stages.filterValues { it > 0 },
        accuracy = accuracy.coerceAtLeast(0),
        evasion = evasion.coerceAtLeast(0),
    )

    /** Drops every positive stage (used by "Runenbruch" style effects). */
    fun clearPositive(): StatStages = copy(
        stages = stages.filterValues { it < 0 },
        accuracy = accuracy.coerceAtMost(0),
        evasion = evasion.coerceAtMost(0),
    )

    fun reset(): StatStages = StatStages()

    val activeStages: Map<Stat, Int> get() = stages.filterValues { it != 0 }

    /**
     * Multiplier for offensive/defensive stats.
     * `+n` → `(2 + n) / 2`, `−n` → `2 / (2 + n)`; capped at 4× / 0.25×.
     */
    fun multiplierFor(stat: Stat): Double = stageMultiplier(this[stat])

    /** Accuracy/evasion use a gentler 3-based curve. */
    val accuracyMultiplier: Double get() = ratioMultiplier(accuracy)
    val evasionMultiplier: Double get() = ratioMultiplier(evasion)

    companion object {
        const val MAX_STAGE = 6

        fun stageMultiplier(stage: Int): Double {
            val clamped = stage.coerceIn(-MAX_STAGE, MAX_STAGE)
            return if (clamped >= 0) (2.0 + clamped) / 2.0 else 2.0 / (2.0 - clamped)
        }

        fun ratioMultiplier(stage: Int): Double {
            val clamped = stage.coerceIn(-MAX_STAGE, MAX_STAGE)
            return if (clamped >= 0) (3.0 + clamped) / 3.0 else 3.0 / (3.0 - clamped)
        }
    }
}
