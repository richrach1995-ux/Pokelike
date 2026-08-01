package com.runeveil.saga.domain.model.battle

import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.rules.Effectiveness

/**
 * A command issued by a player or by the AI for one battler in one turn.
 * The engine collects one action per active battler, orders them and resolves
 * them (see `TurnResolver`).
 */
sealed interface BattleAction {
    val actorId: String

    data class UseMove(
        override val actorId: String,
        val moveSlotIndex: Int,
        val targetIds: List<String>,
    ) : BattleAction

    data class UseItem(
        override val actorId: String,
        val itemId: String,
        val targetId: String?,
    ) : BattleAction

    data class ThrowOrb(
        override val actorId: String,
        val orbItemId: String,
        val targetId: String,
    ) : BattleAction

    data class Switch(
        override val actorId: String,
        val toTeamIndex: Int,
    ) : BattleAction

    data class Flee(override val actorId: String) : BattleAction

    /** Skipping is only legal when every move is out of PP. */
    data class Struggle(override val actorId: String) : BattleAction
}

/**
 * Everything the engine emits while resolving a turn.
 *
 * The presentation layer consumes this stream to drive animations, the battle
 * log and haptics; it never inspects [BattleState] deltas directly. Each event
 * carries enough information to be rendered on its own.
 */
sealed interface BattleEvent {
    data class BattleStarted(val type: BattleType, val enemyNameKeys: List<String>) : BattleEvent
    data class TurnStarted(val turn: Int) : BattleEvent
    data class MoveDeclared(val actorId: String, val moveId: String, val moveNameKey: String) : BattleEvent
    data class MoveMissed(val actorId: String, val targetId: String) : BattleEvent
    data class MoveHadNoEffect(val targetId: String) : BattleEvent

    data class DamageDealt(
        val actorId: String,
        val targetId: String,
        val amount: Int,
        val effectiveness: Effectiveness,
        val critical: Boolean,
        val remainingHp: Int,
        val maxHp: Int,
        val shieldAbsorbed: Int = 0,
        val hitIndex: Int = 1,
        val hitCount: Int = 1,
    ) : BattleEvent

    data class Healed(val targetId: String, val amount: Int, val remainingHp: Int) : BattleEvent
    data class HealBlocked(val targetId: String) : BattleEvent
    data class ShieldRaised(val targetId: String, val amount: Int, val turns: Int) : BattleEvent
    data class ShieldBroken(val targetId: String) : BattleEvent
    data class Protected(val targetId: String) : BattleEvent

    data class StatusInflicted(val targetId: String, val condition: StatusCondition) : BattleEvent
    data class StatusResisted(val targetId: String, val condition: StatusCondition) : BattleEvent
    data class StatusCured(val targetId: String, val condition: StatusCondition) : BattleEvent
    data class StatusTicked(val targetId: String, val condition: StatusCondition, val damage: Int) : BattleEvent
    data class ActionBlocked(val actorId: String, val condition: StatusCondition) : BattleEvent
    data class ConfusionSelfHit(val actorId: String, val damage: Int) : BattleEvent

    data class StatChanged(
        val targetId: String,
        val stat: Stat,
        val stages: Int,
        val applied: Int,
    ) : BattleEvent

    data class RatioChanged(val targetId: String, val kind: RatioKind, val applied: Int) : BattleEvent

    data class WeatherChanged(val weather: BattleWeather, val turns: Int) : BattleEvent
    data class WeatherTicked(val weather: BattleWeather, val targetId: String, val damage: Int) : BattleEvent
    data class WeatherEnded(val weather: BattleWeather) : BattleEvent

    data class AbilityTriggered(val battlerId: String, val abilityId: String, val nameKey: String) : BattleEvent
    data class ComboTriggered(val actorIds: List<String>, val comboTag: String, val bonusPercent: Int) : BattleEvent

    data class Charging(val actorId: String, val messageKey: String) : BattleEvent
    data class Recharging(val actorId: String) : BattleEvent

    data class Fainted(val battlerId: String, val nameKey: String) : BattleEvent
    data class SwitchedIn(val battlerId: String, val nameKey: String, val side: BattleSide) : BattleEvent
    data class SwitchedOut(val battlerId: String, val nameKey: String) : BattleEvent

    data class ItemUsed(val actorId: String, val itemId: String) : BattleEvent
    data class OrbThrown(val orbItemId: String, val targetId: String) : BattleEvent
    data class OrbShake(val shakeIndex: Int, val totalNeeded: Int) : BattleEvent
    data class CaptureSucceeded(val targetId: String, val monsterUid: String) : BattleEvent
    data class CaptureFailed(val targetId: String, val shakes: Int) : BattleEvent

    data class FleeSucceeded(val actorId: String) : BattleEvent
    data class FleeFailed(val actorId: String) : BattleEvent

    data class ExperienceGained(val monsterUid: String, val amount: Long) : BattleEvent
    data class LevelledUp(val monsterUid: String, val newLevel: Int, val statGains: Map<Stat, Int>) : BattleEvent
    data class MoveLearnable(val monsterUid: String, val moveId: String) : BattleEvent
    data class FriendshipChanged(val monsterUid: String, val delta: Int) : BattleEvent

    data class BattleEnded(val outcome: BattleOutcome) : BattleEvent
    data class Message(val messageKey: String, val args: List<String> = emptyList()) : BattleEvent
}
