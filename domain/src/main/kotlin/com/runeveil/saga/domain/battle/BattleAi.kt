package com.runeveil.saga.domain.battle

import com.runeveil.saga.domain.model.battle.BattleAction
import com.runeveil.saga.domain.model.battle.BattleSide
import com.runeveil.saga.domain.model.battle.BattleState
import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.battle.MoveEffect
import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.rules.DamageCalculator
import com.runeveil.saga.domain.rules.TypeChart
import com.runeveil.saga.domain.util.Rng

/**
 * How cleverly an opponent fights.
 *
 * The profile is content-driven: wild monsters get [FERAL] or [INSTINCTIVE],
 * ordinary trainers [TACTICAL], gym-equivalent bosses [MASTERFUL], and the
 * legendary guardians of the Nine get [MYTHIC], which additionally reacts to
 * the player's whole party.
 *
 * @property randomness probability of ignoring the best-scored move.
 * @property looksAhead whether the AI simulates the opponent's likely reply.
 * @property switchesOut whether the AI is allowed to switch monsters.
 * @property usesItems whether the AI may spend healing items.
 */
enum class AiProfile(
    val randomness: Double,
    val looksAhead: Boolean,
    val switchesOut: Boolean,
    val usesItems: Boolean,
) {
    /** Attacks at random — cornered animals. */
    FERAL(randomness = 0.75, looksAhead = false, switchesOut = false, usesItems = false),

    /** Prefers effective damage, but nothing more. */
    INSTINCTIVE(randomness = 0.35, looksAhead = false, switchesOut = false, usesItems = false),

    /** Uses status, buffs and type advantage sensibly. */
    TACTICAL(randomness = 0.15, looksAhead = false, switchesOut = true, usesItems = true),

    /** Plans one turn ahead and preserves its win conditions. */
    MASTERFUL(randomness = 0.05, looksAhead = true, switchesOut = true, usesItems = true),

    /** Never wastes a turn; punishes switches and set-up. */
    MYTHIC(randomness = 0.0, looksAhead = true, switchesOut = false, usesItems = false),
}

/**
 * Decision engine for every non-player combatant.
 *
 * The AI scores each legal action with a heuristic that mirrors how a human
 * plays: kill if you can, avoid wasted turns, respect type charts, do not
 * re-apply a status that already sticks, and value set-up only while healthy.
 * It never reads hidden information the player does not have.
 */
class BattleAi(
    private val content: BattleContent,
    private val rng: Rng,
) {

    /** Chooses one action for [actor]. Always returns a legal action. */
    fun decide(state: BattleState, actor: Battler, profile: AiProfile): BattleAction {
        val opponents = state.activeOf(actor.side.opposite).filter { !it.isFainted }
        if (opponents.isEmpty()) return BattleAction.Struggle(actor.id)

        // 1. Emergency switch — badly outmatched and healthy alternatives exist.
        if (profile.switchesOut && shouldSwitch(state, actor, opponents)) {
            bestSwitchIndex(state, actor, opponents)?.let { index ->
                return BattleAction.Switch(actor.id, index)
            }
        }

        // 2. Healing when critically hurt (trainers only).
        if (profile.usesItems && actor.monster.hpFraction < 0.25f) {
            healingItemId(state, actor)?.let { itemId ->
                return BattleAction.UseItem(actor.id, itemId, actor.id)
            }
        }

        // 3. Score every usable move.
        val usable = actor.monster.moves.mapIndexedNotNull { index, slot ->
            if (slot.currentPp <= 0) return@mapIndexedNotNull null
            content.move(slot.moveId)?.let { index to it }
        }
        if (usable.isEmpty()) return BattleAction.Struggle(actor.id)

        val scored = usable.map { (index, move) ->
            val target = bestTargetFor(state, actor, move, opponents)
            Triple(index, move, scoreMove(state, actor, target, move, profile))
        }

        val chosen = if (rng.chance(profile.randomness)) {
            rng.pick(scored) ?: scored.first()
        } else {
            // Softmax-free: pick the max, break ties randomly for variety.
            val best = scored.maxOf { it.third }
            val contenders = scored.filter { it.third >= best - TIE_EPSILON }
            rng.pick(contenders) ?: scored.first()
        }

        val target = bestTargetFor(state, actor, chosen.second, opponents)
        return BattleAction.UseMove(
            actorId = actor.id,
            moveSlotIndex = chosen.first,
            targetIds = listOf(target.id),
        )
    }

    // -----------------------------------------------------------------------
    // Scoring
    // -----------------------------------------------------------------------

    /**
     * Heuristic score of using [move] against [target].
     *
     * Roughly normalised to "expected fraction of the target's HP removed",
     * with bonuses and penalties layered on top so that support moves compete
     * fairly with damage.
     */
    internal fun scoreMove(
        state: BattleState,
        actor: Battler,
        target: Battler,
        move: Move,
        profile: AiProfile,
    ): Double {
        var score = 0.0

        // --- damage component ----------------------------------------------
        if (move.isDamaging) {
            val expected = DamageCalculator.expectedDamage(
                attacker = actor,
                defender = target,
                move = move,
                weather = state.weather,
                dayPhase = state.dayPhase,
            )
            val fraction = expected.toDouble() / target.monster.maxHp.coerceAtLeast(1)
            score += fraction * 100.0
            // A guaranteed knockout dwarfs everything else.
            if (expected >= target.monster.currentHp) score += 120.0
            // Accuracy risk.
            score *= (move.accuracy.coerceIn(1, 100) / 100.0).let { if (move.alwaysHits) 1.0 else it }
            // Multi-hit moves are worth slightly more than their mean roll.
            move.effects.filterIsInstance<MoveEffect.MultiHit>().firstOrNull()?.let {
                score *= (it.min + it.max) / 2.0
            }
        }

        // --- status component ----------------------------------------------
        for (effect in move.effects) {
            when (effect) {
                is MoveEffect.InflictStatus -> {
                    val alreadyAfflicted = target.monster.status != null
                    val immune = !effect.condition.canAfflict(
                        target.monster.primaryElement,
                        target.monster.secondaryElement,
                    )
                    score += when {
                        alreadyAfflicted || immune -> -25.0
                        else -> statusValue(effect.condition) * effect.chance
                    }
                }

                is MoveEffect.ModifyStat -> {
                    val healthy = actor.monster.hpFraction > 0.5f
                    score += if (effect.onSelf) {
                        val current = actor.stages[effect.stat]
                        when {
                            !healthy -> -10.0
                            current >= 4 -> -15.0
                            else -> effect.stages * 9.0 * statWeight(effect.stat, actor)
                        }
                    } else {
                        val current = target.stages[effect.stat]
                        if (current <= -4) -12.0 else -effect.stages * 8.0
                    }
                }

                is MoveEffect.Heal -> {
                    val missing = 1.0 - actor.monster.hpFraction
                    score += if (actor.monster.status?.blocksHealing == true) {
                        -30.0
                    } else {
                        missing * effect.fraction * 130.0 - 10.0
                    }
                }

                is MoveEffect.RaiseShield -> score += (1.0 - actor.monster.hpFraction) * 40.0 + 8.0
                is MoveEffect.Protect ->
                    score += if (actor.consecutiveProtects > 0) -40.0 else 12.0

                is MoveEffect.SetWeather ->
                    score += if (state.weather == effect.weather) -30.0 else weatherValue(actor, effect.weather)

                is MoveEffect.CureStatus ->
                    score += if (actor.monster.status != null) 35.0 else -30.0

                is MoveEffect.ClearStatChanges -> {
                    val enemyBuffs = Stat.BATTLE_STATS.sumOf { target.stages[it].coerceAtLeast(0) }
                    score += enemyBuffs * 12.0 - 8.0
                }

                is MoveEffect.Drain -> score += 10.0
                is MoveEffect.Recoil -> score -= 8.0
                is MoveEffect.Recharge -> score -= 15.0
                is MoveEffect.Charge -> score -= 20.0
                is MoveEffect.WeakenForCapture -> score -= 50.0 // Never useful for the AI.
                else -> Unit
            }
        }

        // --- priority and speed --------------------------------------------
        if (move.priority > 0 && actor.monster.hpFraction < 0.35f) score += 18.0

        // --- look-ahead: how badly will we be punished? ---------------------
        if (profile.looksAhead) {
            score -= retaliationRisk(state, actor, target) * 0.35
        }

        return score
    }

    /** How dangerous the target's best reply is, as % of the actor's HP. */
    private fun retaliationRisk(state: BattleState, actor: Battler, target: Battler): Double {
        val worst = target.monster.moves
            .mapNotNull { content.move(it.moveId) }
            .filter { it.isDamaging }
            .maxOfOrNull { move ->
                DamageCalculator.expectedDamage(target, actor, move, state.weather, state.dayPhase)
            } ?: return 0.0
        return (worst.toDouble() / actor.monster.maxHp.coerceAtLeast(1)) * 100.0
    }

    private fun statusValue(condition: StatusCondition): Double = when (condition) {
        StatusCondition.SLEEP -> 45.0
        StatusCondition.FREEZE -> 42.0
        StatusCondition.PARALYSIS -> 34.0
        StatusCondition.CONFUSION -> 28.0
        StatusCondition.CURSE -> 30.0
        StatusCondition.BURN -> 26.0
        StatusCondition.POISON -> 24.0
        StatusCondition.BLEED -> 27.0
    }

    /** Buffing the stat you actually use is worth more. */
    private fun statWeight(stat: Stat, actor: Battler): Double {
        val stats = actor.monster.stats
        return when (stat) {
            Stat.ATTACK -> if (stats.attack >= stats.magic) 1.2 else 0.6
            Stat.MAGIC -> if (stats.magic > stats.attack) 1.2 else 0.6
            Stat.SPEED -> 1.0
            Stat.DEFENSE, Stat.RESISTANCE -> 0.8
            Stat.LUCK -> 0.4
            Stat.HP -> 0.0
        }
    }

    /** Weather is valuable when it boosts our own elements. */
    private fun weatherValue(
        actor: Battler,
        weather: com.runeveil.saga.domain.model.battle.BattleWeather,
    ): Double {
        val boosts = actor.monster.species.elements.count { it in weather.boosted }
        val hurts = actor.monster.species.elements.count { it in weather.dampened }
        return boosts * 22.0 - hurts * 25.0
    }

    // -----------------------------------------------------------------------
    // Targeting and switching
    // -----------------------------------------------------------------------

    private fun bestTargetFor(
        state: BattleState,
        actor: Battler,
        move: Move,
        opponents: List<Battler>,
    ): Battler {
        if (!move.target.hitsOpponents) return actor
        return opponents.maxByOrNull { candidate ->
            val expected = if (move.isDamaging) {
                DamageCalculator.expectedDamage(actor, candidate, move, state.weather, state.dayPhase)
            } else {
                0
            }
            val lethal = if (expected >= candidate.monster.currentHp) 1000 else 0
            expected + lethal
        } ?: opponents.first()
    }

    /**
     * The AI switches when its current monster is at a severe type
     * disadvantage *and* a bench member is markedly better off.
     */
    private fun shouldSwitch(state: BattleState, actor: Battler, opponents: List<Battler>): Boolean {
        if (state.benchOf(actor.side).isEmpty()) return false
        if (actor.monster.hpFraction < 0.2f) return true
        val worstIncoming = opponents.maxOf { foe ->
            foe.monster.species.elements.maxOf { element ->
                TypeChart.multiplier(
                    element,
                    actor.monster.primaryElement,
                    actor.monster.secondaryElement,
                )
            }
        }
        return worstIncoming >= 2.0 && actor.monster.hpFraction < 0.6f
    }

    private fun bestSwitchIndex(
        state: BattleState,
        actor: Battler,
        opponents: List<Battler>,
    ): Int? {
        val team = state.teamOf(actor.side)
        val activeSlots = if (actor.side == BattleSide.PLAYER) state.activePlayerSlots else state.activeEnemySlots
        var bestIndex: Int? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for ((index, candidate) in team.withIndex()) {
            if (index in activeSlots || candidate.isFainted) continue
            val incoming = opponents.maxOf { foe ->
                foe.monster.species.elements.maxOf { element ->
                    TypeChart.multiplier(
                        element,
                        candidate.monster.primaryElement,
                        candidate.monster.secondaryElement,
                    )
                }
            }
            val outgoing = candidate.monster.species.elements.maxOf { element ->
                opponents.maxOf { foe ->
                    TypeChart.multiplier(
                        element,
                        foe.monster.primaryElement,
                        foe.monster.secondaryElement,
                    )
                }
            }
            val score = outgoing * 2.0 - incoming * 2.5 + candidate.monster.hpFraction
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        // Only switch when the replacement is genuinely better.
        return if (bestScore > 0.5) bestIndex else null
    }

    private fun healingItemId(state: BattleState, actor: Battler): String? {
        val trainerId = state.enemyTrainerId ?: return null
        // Trainer inventories are content-defined; the id convention is
        // "<trainerId>_potion". Missing items simply mean "no healing".
        return content.item("${trainerId}_potion")?.id ?: content.item("potion_greater")?.id
    }

    companion object {
        private const val TIE_EPSILON = 0.5
    }
}
