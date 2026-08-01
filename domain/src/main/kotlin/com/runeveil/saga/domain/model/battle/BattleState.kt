package com.runeveil.saga.domain.model.battle

import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatStages

/**
 * One combatant on the field: a monster plus everything that is true only
 * *during* this battle (stat stages, shields, charge state …).
 *
 * Battlers are immutable; the engine threads new copies through the turn
 * pipeline, which makes the whole battle a pure function of
 * `(initial state, actions, rng seed)`.
 */
data class Battler(
    val id: String,
    val side: BattleSide,
    val slot: Int,
    val monster: MonsterInstance,
    val stages: StatStages = StatStages(),
    val shieldHp: Int = 0,
    val shieldTurns: Int = 0,
    val protectedTurns: Int = 0,
    val mustRecharge: Boolean = false,
    val chargingMoveId: String? = null,
    val lastMoveId: String? = null,
    val consecutiveProtects: Int = 0,
    val captureWeakness: Double = 1.0,
    val captureWeaknessTurns: Int = 0,
    val hasActedThisTurn: Boolean = false,
    val turnsOnField: Int = 0,
    val participatedInKo: Boolean = false,
    /** Set once the "Letzter Stand" ability has saved this battler. */
    val usedLastStand: Boolean = false,
    /** Cached ability effect id, resolved from content when the battler enters. */
    val abilityEffectId: String? = null,
) {
    val isFainted: Boolean get() = monster.isFainted
    val isActive: Boolean get() = !isFainted

    /**
     * Effective value of [stat] after stat stages, status penalties and the
     * shield of the current field. HP is returned unmodified.
     */
    fun effectiveStat(stat: Stat): Int {
        if (stat.isVital) return monster.maxHp
        val base = monster.stats[stat].toDouble()
        val staged = base * stages.multiplierFor(stat)
        val status = monster.status
        val penalised = if (status?.statPenalty == stat) {
            staged * status.statPenaltyMultiplier
        } else {
            staged
        }
        return penalised.toInt().coerceAtLeast(1)
    }

    fun withMonster(update: (MonsterInstance) -> MonsterInstance): Battler =
        copy(monster = update(monster))

    /** Applies [amount] damage, consuming the shield first. Returns the new
     *  battler and the HP actually removed from the monster. */
    fun absorbDamage(amount: Int): Pair<Battler, Int> {
        if (amount <= 0) return this to 0
        val absorbed = minOf(shieldHp, amount)
        val toHp = amount - absorbed
        val updated = copy(
            shieldHp = shieldHp - absorbed,
            monster = monster.withDamage(toHp),
        )
        return updated to toHp
    }

    /** Resets everything that should not survive a switch-out. */
    fun onSwitchOut(): Battler = copy(
        stages = StatStages(),
        shieldHp = 0,
        shieldTurns = 0,
        protectedTurns = 0,
        consecutiveProtects = 0,
        chargingMoveId = null,
        mustRecharge = false,
        captureWeakness = 1.0,
        captureWeaknessTurns = 0,
        turnsOnField = 0,
        monster = if (monster.status?.isVolatile == true) monster.withStatus(null) else monster,
    )
}

/** What kind of fight this is — drives rewards, escape rules and music. */
enum class BattleType(val musicKey: String, val allowsCapture: Boolean, val allowsFlee: Boolean) {
    WILD("bgm_battle_wild", allowsCapture = true, allowsFlee = true),
    TRAINER("bgm_battle_trainer", allowsCapture = false, allowsFlee = false),
    BOSS("bgm_battle_boss", allowsCapture = false, allowsFlee = false),
    LEGENDARY("bgm_battle_legendary", allowsCapture = true, allowsFlee = false),
    TOURNAMENT("bgm_battle_tournament", allowsCapture = false, allowsFlee = false),
    ENDLESS("bgm_battle_endless", allowsCapture = true, allowsFlee = true),
    STORY_SCRIPTED("bgm_battle_story", allowsCapture = false, allowsFlee = false),
}

/** Terminal outcome of a battle. */
enum class BattleOutcome {
    ONGOING,
    VICTORY,
    DEFEAT,
    CAPTURED,
    FLED,
    DRAW,
}

/**
 * The complete, serialisable state of an ongoing battle.
 *
 * Battles support up to three monsters per side. Single battles simply use one
 * active slot; the boss fights of chapters 6 and 9 use two and three.
 */
data class BattleState(
    val battleId: String,
    val type: BattleType,
    val playerTeam: List<Battler>,
    val enemyTeam: List<Battler>,
    val activePlayerSlots: List<Int> = listOf(0),
    val activeEnemySlots: List<Int> = listOf(0),
    val weather: BattleWeather = BattleWeather.CLEAR,
    val weatherTurns: Int = 0,
    val fieldEffects: List<FieldEffect> = emptyList(),
    val dayPhase: DayPhase = DayPhase.DAY,
    val turn: Int = 1,
    val outcome: BattleOutcome = BattleOutcome.ONGOING,
    val escapeAttempts: Int = 0,
    val rngSeed: Long = 0L,
    val enemyTrainerId: String? = null,
    val locationId: String? = null,
    val lastEvents: List<BattleEvent> = emptyList(),
    val capturedMonsterUid: String? = null,
    val pendingSwitchSide: BattleSide? = null,
) {
    val activePlayers: List<Battler>
        get() = activePlayerSlots.mapNotNull { playerTeam.getOrNull(it) }

    val activeEnemies: List<Battler>
        get() = activeEnemySlots.mapNotNull { enemyTeam.getOrNull(it) }

    val allBattlers: List<Battler> get() = playerTeam + enemyTeam

    fun teamOf(side: BattleSide): List<Battler> =
        if (side == BattleSide.PLAYER) playerTeam else enemyTeam

    fun activeOf(side: BattleSide): List<Battler> =
        if (side == BattleSide.PLAYER) activePlayers else activeEnemies

    fun battler(id: String): Battler? = allBattlers.firstOrNull { it.id == id }

    fun isSideWiped(side: BattleSide): Boolean = teamOf(side).all { it.isFainted }

    /** Replaces a battler by id, keeping the team ordering intact. */
    fun withBattler(updated: Battler): BattleState =
        if (updated.side == BattleSide.PLAYER) {
            copy(playerTeam = playerTeam.map { if (it.id == updated.id) updated else it })
        } else {
            copy(enemyTeam = enemyTeam.map { if (it.id == updated.id) updated else it })
        }

    fun withBattlers(updated: Collection<Battler>): BattleState =
        updated.fold(this) { state, battler -> state.withBattler(battler) }

    /** Reserve members that can still fight (used for forced switches). */
    fun benchOf(side: BattleSide): List<Battler> {
        val activeSlots = if (side == BattleSide.PLAYER) activePlayerSlots else activeEnemySlots
        return teamOf(side).filterIndexed { index, b -> index !in activeSlots && !b.isFainted }
    }

    val isOver: Boolean get() = outcome != BattleOutcome.ONGOING
}
