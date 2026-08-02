package com.runeveil.saga.domain.usecase

import com.runeveil.saga.domain.battle.AiProfile
import com.runeveil.saga.domain.battle.BattleAi
import com.runeveil.saga.domain.battle.BattleContent
import com.runeveil.saga.domain.battle.BattleEngine
import com.runeveil.saga.domain.model.battle.BattleAction
import com.runeveil.saga.domain.model.battle.BattleEvent
import com.runeveil.saga.domain.model.battle.BattleOutcome
import com.runeveil.saga.domain.model.battle.BattleSide
import com.runeveil.saga.domain.model.battle.BattleState
import com.runeveil.saga.domain.model.battle.BattleType
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.util.Rng

/**
 * Orchestrates one battle from the presentation layer's point of view.
 *
 * The ViewModel owns a session, feeds it the player's chosen action and gets
 * back the new state plus the events to animate. Enemy actions are decided
 * here, so the UI never has to know the AI exists.
 *
 * A session is intentionally *stateful* (it holds the current [BattleState])
 * while the engine underneath stays pure — that split keeps the engine testable
 * and the UI simple.
 */
class BattleSession(
    content: BattleContent,
    private val rng: Rng,
    initialState: BattleState,
    private val enemyProfile: AiProfile,
) {
    private val engine = BattleEngine(content, rng)
    private val ai = BattleAi(content, rng)

    /** Monster uids that took part, for experience distribution. */
    private val participants = mutableSetOf<String>()

    /** The live battle state; replaced after every resolved turn. */
    var state: BattleState
        private set

    /** Events produced by [BattleEngine.start]; consumed once by the UI. */
    val openingEvents: List<BattleEvent>

    init {
        val started = engine.start(initialState)
        state = started.state
        openingEvents = started.events
        state.activePlayers.forEach { participants += it.monster.uid }
    }

    /**
     * Advances one full turn with the player's [playerAction]. Enemy actions
     * are chosen by the AI.
     */
    fun submit(playerAction: BattleAction): BattleEngine.StepResult {
        if (state.isOver) return BattleEngine.StepResult(state, emptyList())

        val enemyActions = state.activeEnemies
            .filter { !it.isFainted }
            .map { ai.decide(state, it, enemyProfile) }

        val result = engine.executeTurn(state, listOf(playerAction) + enemyActions)
        state = result.state
        state.activePlayers.forEach { participants += it.monster.uid }

        return result
    }

    /** Replaces a fainted active monster; returns false when the switch is illegal. */
    fun forceSwitch(toTeamIndex: Int): BattleEngine.StepResult? {
        val active = state.activePlayers.firstOrNull() ?: return null
        val result = engine.executeTurn(state, listOf(BattleAction.Switch(active.id, toTeamIndex)))
        state = result.state
        state.activePlayers.forEach { participants += it.monster.uid }
        return result
    }

    /** Experience/friendship events for a won battle. */
    fun spoils(): List<BattleEvent> =
        if (state.outcome == BattleOutcome.VICTORY) {
            engine.awardVictorySpoils(state, participants)
        } else {
            emptyList()
        }

    /** The player's monsters as they stand now — written back to the repository. */
    fun survivingParty(): List<MonsterInstance> = state.playerTeam.map { it.monster }

    /** The wild monster that was caught, if any. */
    fun capturedMonster(): MonsterInstance? =
        state.capturedMonsterUid?.let { uid ->
            state.enemyTeam.firstOrNull { it.monster.uid == uid }?.monster
        }

    val outcome: BattleOutcome get() = state.outcome
    val participantUids: Set<String> get() = participants.toSet()

    companion object {

        /**
         * Assembles a battle state from a party and an enemy line-up.
         *
         * @param activeCount how many monsters fight at once per side
         *   (1 for ordinary fights, 2–3 for the chapter bosses).
         */
        fun buildState(
            battleId: String,
            type: BattleType,
            party: List<MonsterInstance>,
            enemies: List<MonsterInstance>,
            weather: BattleWeather,
            dayPhase: DayPhase,
            rngSeed: Long,
            locationId: String? = null,
            enemyTrainerId: String? = null,
            activeCount: Int = 1,
        ): BattleState {
            val playerBattlers = party.mapIndexed { index, monster ->
                Battler(
                    id = "p$index",
                    side = BattleSide.PLAYER,
                    slot = index,
                    monster = monster,
                )
            }
            val enemyBattlers = enemies.mapIndexed { index, monster ->
                Battler(
                    id = "e$index",
                    side = BattleSide.ENEMY,
                    slot = index,
                    monster = monster,
                )
            }
            // The first healthy members take the field.
            val playerSlots = playerBattlers.asSequence()
                .withIndex()
                .filter { !it.value.isFainted }
                .take(activeCount)
                .map { it.index }
                .toList()
                .ifEmpty { listOf(0) }
            val enemySlots = enemyBattlers.indices.take(activeCount).toList().ifEmpty { listOf(0) }

            return BattleState(
                battleId = battleId,
                type = type,
                playerTeam = playerBattlers,
                enemyTeam = enemyBattlers,
                activePlayerSlots = playerSlots,
                activeEnemySlots = enemySlots,
                weather = weather,
                weatherTurns = if (weather == BattleWeather.CLEAR) 0 else Int.MAX_VALUE,
                dayPhase = dayPhase,
                rngSeed = rngSeed,
                locationId = locationId,
                enemyTrainerId = enemyTrainerId,
            )
        }
    }
}
