package com.runeveil.saga.domain.util

import kotlin.random.Random

/**
 * Every random decision in the rule layer goes through this interface.
 *
 * Two reasons:
 *  1. **Testability** — a seeded implementation makes a whole battle replayable
 *     turn by turn, which is what the engine tests rely on.
 *  2. **Save integrity** — the encounter and breeding systems persist their
 *     seed so a reloaded save cannot be re-rolled for a better outcome.
 */
interface Rng {
    /** Uniform integer in `0 until bound`. */
    fun nextInt(bound: Int): Int

    /** Uniform integer in `from..until - 1`. */
    fun nextInt(from: Int, until: Int): Int

    /** Uniform double in `[0, 1)`. */
    fun nextDouble(): Double

    /** True with probability [probability] (clamped to 0…1). */
    fun chance(probability: Double): Boolean = nextDouble() < probability.coerceIn(0.0, 1.0)

    /** Picks a uniformly random element, or null when [items] is empty. */
    fun <T> pick(items: List<T>): T? = if (items.isEmpty()) null else items[nextInt(items.size)]

    /**
     * Weighted pick. Entries with a weight ≤ 0 are ignored.
     * Returns null when no entry has a positive weight.
     */
    fun <T> pickWeighted(items: List<T>, weight: (T) -> Int): T? {
        val total = items.sumOf { weight(it).coerceAtLeast(0) }
        if (total <= 0) return null
        var roll = nextInt(total)
        for (item in items) {
            val w = weight(item).coerceAtLeast(0)
            if (roll < w) return item
            roll -= w
        }
        return items.lastOrNull()
    }

    /** Shuffles a copy of [items]. */
    fun <T> shuffled(items: List<T>): List<T> {
        val result = items.toMutableList()
        for (i in result.indices.reversed()) {
            val j = nextInt(i + 1)
            val tmp = result[i]
            result[i] = result[j]
            result[j] = tmp
        }
        return result
    }
}

/** Production implementation backed by [kotlin.random.Random]. */
class SeededRng(seed: Long) : Rng {
    private val random = Random(seed)

    override fun nextInt(bound: Int): Int = if (bound <= 0) 0 else random.nextInt(bound)
    override fun nextInt(from: Int, until: Int): Int =
        if (until <= from) from else random.nextInt(from, until)

    override fun nextDouble(): Double = random.nextDouble()
}

/** Non-deterministic default used outside of tests. */
class SystemRng : Rng {
    override fun nextInt(bound: Int): Int = if (bound <= 0) 0 else Random.nextInt(bound)
    override fun nextInt(from: Int, until: Int): Int =
        if (until <= from) from else Random.nextInt(from, until)

    override fun nextDouble(): Double = Random.nextDouble()
}

/**
 * Deterministic sequence used by tests to force specific outcomes.
 * [doubles] is cycled; [ints] is cycled independently.
 */
class ScriptedRng(
    private val doubles: List<Double> = listOf(0.5),
    private val ints: List<Int> = listOf(0),
) : Rng {
    private var doubleIndex = 0
    private var intIndex = 0

    override fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        val value = ints[intIndex % ints.size]
        intIndex++
        return value.coerceIn(0, bound - 1)
    }

    override fun nextInt(from: Int, until: Int): Int {
        if (until <= from) return from
        return from + nextInt(until - from)
    }

    override fun nextDouble(): Double {
        val value = doubles[doubleIndex % doubles.size]
        doubleIndex++
        return value.coerceIn(0.0, 0.9999999)
    }
}
