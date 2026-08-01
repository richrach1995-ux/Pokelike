package com.runeveil.saga.domain.battle

import com.runeveil.saga.domain.model.battle.BattleAction
import com.runeveil.saga.domain.model.battle.BattleEvent
import com.runeveil.saga.domain.model.battle.BattleOutcome
import com.runeveil.saga.domain.model.battle.BattleSide
import com.runeveil.saga.domain.model.battle.BattleState
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.battle.MoveEffect
import com.runeveil.saga.domain.model.battle.MoveTarget
import com.runeveil.saga.domain.model.battle.RatioKind
import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.item.ItemEffect
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.rules.AccuracyCalculator
import com.runeveil.saga.domain.rules.CaptureCalculator
import com.runeveil.saga.domain.rules.DamageCalculator
import com.runeveil.saga.domain.rules.Effectiveness
import com.runeveil.saga.domain.rules.TypeChart
import com.runeveil.saga.domain.util.Rng
import kotlin.math.roundToInt

/**
 * The deterministic, turn-based battle engine.
 *
 * ### Contract
 * `executeTurn(state, actions)` is a **pure function** of its inputs and of the
 * [Rng] handed to the constructor. Given the same state, the same actions and a
 * seeded RNG it always produces the same result — which is what makes battles
 * replayable, testable and safe to persist mid-fight.
 *
 * ### Turn pipeline
 * ```
 *  1. turn start          → weather/ability start-of-turn hooks
 *  2. ordering            → items & switches first, then moves by
 *                           priority, then by effective speed
 *  3. combo detection     → allies sharing a comboTag get a damage bonus
 *  4. action resolution   → status gate → PP → accuracy → damage → effects
 *  5. end of turn         → weather chip, status ticks, shields, regen
 *  6. faint & outcome     → KO events, forced switches, battle outcome
 * ```
 *
 * Everything the UI needs is emitted as [BattleEvent]s; the engine never
 * formats a string itself (it emits resource keys).
 */
class BattleEngine(
    private val content: BattleContent,
    private val rng: Rng,
) {

    /** Result of one engine step: the new state plus the events it produced. */
    data class StepResult(val state: BattleState, val events: List<BattleEvent>)

    // -----------------------------------------------------------------------
    // Battle start
    // -----------------------------------------------------------------------

    /**
     * Prepares a freshly constructed [BattleState]: resolves ability effect
     * ids, fires entry abilities and emits the opening events.
     */
    fun start(state: BattleState): StepResult {
        val events = mutableListOf<BattleEvent>()
        var working = state.copy(
            playerTeam = state.playerTeam.map { it.copy(abilityEffectId = resolveAbility(it)) },
            enemyTeam = state.enemyTeam.map { it.copy(abilityEffectId = resolveAbility(it)) },
        )
        events += BattleEvent.BattleStarted(
            type = working.type,
            enemyNameKeys = working.activeEnemies.map { it.monster.displayNameKey },
        )
        for (battler in working.activePlayers + working.activeEnemies) {
            val result = applyEntryAbilities(working, battler)
            working = result.state
            events += result.events
        }
        return StepResult(working, events)
    }

    private fun resolveAbility(battler: Battler): String? =
        content.abilityEffectId(battler.monster.abilityId)

    /** Weather summons and entry debuffs. */
    private fun applyEntryAbilities(state: BattleState, battler: Battler): StepResult {
        val events = mutableListOf<BattleEvent>()
        var working = state
        val effectId = battler.abilityEffectId ?: return StepResult(working, events)
        val ability = battler.monster.abilityId?.let { content.ability(it) }

        AbilityEffects.summonedWeather(effectId)?.let { weather ->
            if (working.weather != weather) {
                working = working.copy(weather = weather, weatherTurns = BattleWeather.DEFAULT_TURNS)
                ability?.let { events += BattleEvent.AbilityTriggered(battler.id, it.id, it.nameKey) }
                events += BattleEvent.WeatherChanged(weather, BattleWeather.DEFAULT_TURNS)
            }
        }
        AbilityEffects.entryDebuff(effectId)?.let { (stat, stages) ->
            ability?.let { events += BattleEvent.AbilityTriggered(battler.id, it.id, it.nameKey) }
            for (foe in working.activeOf(battler.side.opposite)) {
                val (newStages, applied) = foe.stages.apply(stat, stages)
                if (applied != 0) {
                    working = working.withBattler(foe.copy(stages = newStages))
                    events += BattleEvent.StatChanged(foe.id, stat, stages, applied)
                }
            }
        }
        return StepResult(working, events)
    }

    // -----------------------------------------------------------------------
    // Turn execution
    // -----------------------------------------------------------------------

    /**
     * Executes one full turn.
     *
     * @param actions one action per active battler. Missing actions are simply
     *   skipped, which is how "the monster is still charging" is expressed.
     */
    fun executeTurn(state: BattleState, actions: List<BattleAction>): StepResult {
        if (state.isOver) return StepResult(state, emptyList())

        val ctx = TurnContext(state)
        ctx.emit(BattleEvent.TurnStarted(state.turn))

        val ordered = orderActions(ctx.state, actions)
        val comboBonuses = detectCombos(ctx.state, ordered, ctx)

        for (action in ordered) {
            if (ctx.state.isOver) break
            val actor = ctx.state.battler(action.actorId) ?: continue
            if (actor.isFainted) continue
            resolveAction(ctx, action, comboBonuses[action.actorId] ?: 1.0)
            ctx.checkFaints()
        }

        if (!ctx.state.isOver) {
            endOfTurn(ctx)
            ctx.checkFaints()
        }

        ctx.evaluateOutcome()
        if (!ctx.state.isOver) {
            ctx.state = ctx.state.copy(
                turn = ctx.state.turn + 1,
                playerTeam = ctx.state.playerTeam.map { it.copy(hasActedThisTurn = false) },
                enemyTeam = ctx.state.enemyTeam.map { it.copy(hasActedThisTurn = false) },
            )
        }
        val finalState = ctx.state.copy(lastEvents = ctx.events.toList())
        return StepResult(finalState, ctx.events.toList())
    }

    // -----------------------------------------------------------------------
    // Ordering
    // -----------------------------------------------------------------------

    /**
     * Fleeing and item use always come first (they are "free" actions),
     * then switches, then moves ordered by priority and effective speed.
     * Ties are broken by a coin flip, keeping mirror matches unpredictable.
     */
    internal fun orderActions(state: BattleState, actions: List<BattleAction>): List<BattleAction> {
        data class Scored(val action: BattleAction, val bucket: Int, val score: Double, val tiebreak: Int)

        val scored = actions.mapNotNull { action ->
            val actor = state.battler(action.actorId) ?: return@mapNotNull null
            val bucket = when (action) {
                is BattleAction.Flee -> 0
                is BattleAction.UseItem, is BattleAction.ThrowOrb -> 1
                is BattleAction.Switch -> 2
                else -> 3
            }
            val priority = when (action) {
                is BattleAction.UseMove -> moveFor(actor, action.moveSlotIndex)?.priority ?: 0
                else -> 0
            }
            val abilitySpeed = AbilityEffects.speedMultiplier(actor.abilityEffectId, state.weather)
            val score = AccuracyCalculator.initiativeScore(actor, priority, state.weather) * abilitySpeed
            Scored(action, bucket, score, rng.nextInt(1024))
        }
        return scored
            .sortedWith(
                compareBy<Scored> { it.bucket }
                    .thenByDescending { it.score }
                    .thenByDescending { it.tiebreak },
            )
            .map { it.action }
    }

    /**
     * Two allies using moves that share a [Move.comboTag] in the same turn
     * perform a Combination Strike: both moves gain [COMBO_BONUS].
     */
    internal fun detectCombos(
        state: BattleState,
        actions: List<BattleAction>,
        ctx: TurnContext,
    ): Map<String, Double> {
        val byTag = mutableMapOf<String, MutableList<Pair<String, BattleSide>>>()
        for (action in actions) {
            if (action !is BattleAction.UseMove) continue
            val actor = state.battler(action.actorId) ?: continue
            val tag = moveFor(actor, action.moveSlotIndex)?.comboTag ?: continue
            byTag.getOrPut(tag) { mutableListOf() } += action.actorId to actor.side
        }
        val bonuses = mutableMapOf<String, Double>()
        for ((tag, participants) in byTag) {
            val perSide = participants.groupBy { it.second }
            for ((_, group) in perSide) {
                if (group.size < 2) continue
                val ids = group.map { it.first }
                ids.forEach { bonuses[it] = COMBO_BONUS }
                ctx.emit(
                    BattleEvent.ComboTriggered(
                        actorIds = ids,
                        comboTag = tag,
                        bonusPercent = ((COMBO_BONUS - 1.0) * 100).roundToInt(),
                    ),
                )
            }
        }
        return bonuses
    }

    // -----------------------------------------------------------------------
    // Action resolution
    // -----------------------------------------------------------------------

    private fun resolveAction(ctx: TurnContext, action: BattleAction, comboBonus: Double) {
        when (action) {
            is BattleAction.UseMove -> resolveMove(ctx, action, comboBonus)
            is BattleAction.Struggle -> resolveDesperation(ctx, action)
            is BattleAction.UseItem -> resolveItem(ctx, action)
            is BattleAction.ThrowOrb -> resolveOrb(ctx, action)
            is BattleAction.Switch -> resolveSwitch(ctx, action)
            is BattleAction.Flee -> resolveFlee(ctx, action)
        }
    }

    private fun resolveMove(ctx: TurnContext, action: BattleAction.UseMove, comboBonus: Double) {
        var actor = ctx.state.battler(action.actorId) ?: return

        // --- recharge -------------------------------------------------------
        if (actor.mustRecharge) {
            ctx.emit(BattleEvent.Recharging(actor.id))
            ctx.update(actor.copy(mustRecharge = false))
            return
        }

        // --- resolve which move is being used (charged or fresh) ------------
        val chargedMove = actor.chargingMoveId?.let { content.move(it) }
        val move = chargedMove ?: moveFor(actor, action.moveSlotIndex) ?: return

        // --- status gate ----------------------------------------------------
        if (!passesStatusGate(ctx, actor, move)) return
        actor = ctx.state.battler(action.actorId) ?: return

        // --- charge turn ----------------------------------------------------
        val chargeEffect = move.effects.filterIsInstance<MoveEffect.Charge>().firstOrNull()
        if (chargeEffect != null && chargedMove == null) {
            ctx.update(actor.copy(chargingMoveId = move.id, lastMoveId = move.id))
            ctx.emit(BattleEvent.MoveDeclared(actor.id, move.id, move.nameKey))
            ctx.emit(BattleEvent.Charging(actor.id, chargeEffect.messageKey))
            spendPp(ctx, actor.id, action.moveSlotIndex)
            return
        }
        if (chargedMove != null) {
            actor = actor.copy(chargingMoveId = null)
            ctx.update(actor)
        } else {
            spendPp(ctx, actor.id, action.moveSlotIndex)
            actor = ctx.state.battler(action.actorId) ?: return
        }

        ctx.emit(BattleEvent.MoveDeclared(actor.id, move.id, move.nameKey))
        ctx.update(actor.copy(lastMoveId = move.id, hasActedThisTurn = true))

        // --- targeting ------------------------------------------------------
        val targets = resolveTargets(ctx.state, actor, move, action.targetIds)
        if (targets.isEmpty()) {
            ctx.emit(BattleEvent.MoveHadNoEffect(actor.id))
            return
        }

        for (targetId in targets) {
            if (ctx.state.isOver) break
            applyMoveToTarget(ctx, actor.id, targetId, move, comboBonus)
        }

        // --- post-move self effects ----------------------------------------
        if (move.effects.any { it is MoveEffect.Recharge }) {
            ctx.state.battler(actor.id)?.let { ctx.update(it.copy(mustRecharge = true)) }
        }
        applySelfOnlyEffects(ctx, actor.id, move)
    }

    /**
     * Sleep / freeze / paralysis / confusion gate.
     * @return false when the actor loses its turn.
     */
    private fun passesStatusGate(ctx: TurnContext, battler: Battler, move: Move): Boolean {
        val status = battler.monster.status ?: return true
        when (status) {
            StatusCondition.SLEEP -> {
                val remaining = battler.monster.statusTurns - 1
                if (remaining <= 0) {
                    ctx.update(battler.withMonster { it.withStatus(null) })
                    ctx.emit(BattleEvent.StatusCured(battler.id, StatusCondition.SLEEP))
                    return true
                }
                ctx.update(battler.withMonster { it.copy(statusTurns = remaining) })
                ctx.emit(BattleEvent.ActionBlocked(battler.id, StatusCondition.SLEEP))
                return false
            }

            StatusCondition.FREEZE -> {
                // 25 % thaw chance per turn; fire moves always thaw the user.
                val thaws = rng.chance(0.25) ||
                    move.element == com.runeveil.saga.domain.model.monster.Element.FIRE
                if (thaws) {
                    ctx.update(battler.withMonster { it.withStatus(null) })
                    ctx.emit(BattleEvent.StatusCured(battler.id, StatusCondition.FREEZE))
                    return true
                }
                ctx.emit(BattleEvent.ActionBlocked(battler.id, StatusCondition.FREEZE))
                return false
            }

            StatusCondition.PARALYSIS -> {
                if (rng.chance(status.blocksAction)) {
                    ctx.emit(BattleEvent.ActionBlocked(battler.id, StatusCondition.PARALYSIS))
                    return false
                }
                return true
            }

            StatusCondition.CONFUSION -> {
                val remaining = battler.monster.statusTurns - 1
                if (remaining <= 0) {
                    ctx.update(battler.withMonster { it.withStatus(null) })
                    ctx.emit(BattleEvent.StatusCured(battler.id, StatusCondition.CONFUSION))
                    return true
                }
                ctx.update(battler.withMonster { it.copy(statusTurns = remaining) })
                if (rng.chance(status.blocksAction)) {
                    val self = ctx.state.battler(battler.id) ?: return false
                    val damage = DamageCalculator.confusionSelfDamage(self, rng)
                    val (hurt, applied) = self.absorbDamage(damage)
                    ctx.update(hurt)
                    ctx.emit(BattleEvent.ConfusionSelfHit(battler.id, applied))
                    return false
                }
                return true
            }

            else -> return true
        }
    }

    /** Consumes one PP of the given slot, if the slot exists. */
    private fun spendPp(ctx: TurnContext, battlerId: String, slotIndex: Int) {
        val battler = ctx.state.battler(battlerId) ?: return
        ctx.update(battler.withMonster { it.withPpSpent(slotIndex) })
    }

    private fun moveFor(battler: Battler, slotIndex: Int): Move? =
        battler.monster.moves.getOrNull(slotIndex)?.let { content.move(it.moveId) }

    internal fun resolveTargets(
        state: BattleState,
        actor: Battler,
        move: Move,
        requested: List<String>,
    ): List<String> {
        val opponents = state.activeOf(actor.side.opposite).filter { !it.isFainted }
        val allies = state.activeOf(actor.side).filter { !it.isFainted }
        return when (move.target) {
            MoveTarget.SELF -> listOf(actor.id)
            MoveTarget.SINGLE_ALLY ->
                requested.filter { id -> allies.any { it.id == id } }.ifEmpty { listOf(actor.id) }
            MoveTarget.ALL_ALLIES -> allies.map { it.id }
            MoveTarget.SINGLE_OPPONENT ->
                requested.filter { id -> opponents.any { it.id == id } }
                    .ifEmpty { listOfNotNull(opponents.firstOrNull()?.id) }
            MoveTarget.ALL_OPPONENTS -> opponents.map { it.id }
            MoveTarget.EVERYONE -> (opponents + allies).map { it.id }
            MoveTarget.RANDOM_OPPONENT -> listOfNotNull(rng.pick(opponents)?.id)
            MoveTarget.FIELD -> listOf(actor.id)
        }
    }

    private fun applyMoveToTarget(
        ctx: TurnContext,
        actorId: String,
        targetId: String,
        move: Move,
        comboBonus: Double,
    ) {
        val actor = ctx.state.battler(actorId) ?: return
        var target = ctx.state.battler(targetId) ?: return
        if (target.isFainted) return

        // --- protection -----------------------------------------------------
        if (target.protectedTurns > 0 && !move.ignoresProtection && target.id != actor.id) {
            ctx.emit(BattleEvent.Protected(target.id))
            return
        }

        // --- accuracy -------------------------------------------------------
        val hits = move.alwaysHits || target.id == actor.id || run {
            val evade = AbilityEffects.evadeMultiplier(target.abilityEffectId, ctx.state.weather)
            val chance = AccuracyCalculator.hitChance(actor, target, move, ctx.state.weather) * evade
            rng.chance(chance)
        }
        if (!hits) {
            ctx.emit(BattleEvent.MoveMissed(actor.id, target.id))
            return
        }

        // --- damage ---------------------------------------------------------
        var totalDamage = 0
        var lastEffectiveness = Effectiveness.NEUTRAL
        if (move.isDamaging || move.effects.any { it is MoveEffect.FixedDamage || it is MoveEffect.PercentDamage }) {
            val hitCount = move.effects.filterIsInstance<MoveEffect.MultiHit>().firstOrNull()
                ?.let { rng.nextInt(it.min, it.max + 1) } ?: 1

            for (hitIndex in 1..hitCount) {
                target = ctx.state.battler(targetId) ?: return
                if (target.isFainted) break
                val outcome = computeHit(ctx, actor, target, move, comboBonus)
                lastEffectiveness = outcome.effectiveness
                if (outcome.immune) {
                    ctx.emit(BattleEvent.MoveHadNoEffect(target.id))
                    return
                }
                val (damaged, hpLost) = target.absorbDamage(outcome.damage)
                val shieldAbsorbed = outcome.damage - hpLost
                var updatedTarget = damaged
                // "Letzter Stand" — survive one lethal hit with 1 HP.
                if (updatedTarget.monster.isFainted &&
                    AbilityEffects.survivesLethal(updatedTarget.abilityEffectId) &&
                    !updatedTarget.usedLastStand
                ) {
                    updatedTarget = updatedTarget
                        .withMonster { it.copy(currentHp = 1) }
                        .copy(usedLastStand = true)
                    updatedTarget.monster.abilityId?.let { id ->
                        content.ability(id)?.let {
                            ctx.emit(BattleEvent.AbilityTriggered(updatedTarget.id, it.id, it.nameKey))
                        }
                    }
                }
                ctx.update(updatedTarget)
                if (shieldAbsorbed > 0 && updatedTarget.shieldHp == 0) {
                    ctx.emit(BattleEvent.ShieldBroken(updatedTarget.id))
                }
                totalDamage += outcome.damage
                ctx.emit(
                    BattleEvent.DamageDealt(
                        actorId = actor.id,
                        targetId = updatedTarget.id,
                        amount = outcome.damage,
                        effectiveness = outcome.effectiveness,
                        critical = outcome.critical,
                        remainingHp = updatedTarget.monster.currentHp,
                        maxHp = updatedTarget.monster.maxHp,
                        shieldAbsorbed = shieldAbsorbed,
                        hitIndex = hitIndex,
                        hitCount = hitCount,
                    ),
                )
                // Sleep is broken by damage only when content says so.
                if (updatedTarget.monster.status?.curedByDamage == true) {
                    val cured = updatedTarget.withMonster { it.withStatus(null) }
                    ctx.update(cured)
                    ctx.emit(BattleEvent.StatusCured(cured.id, StatusCondition.SLEEP))
                }
            }

            if (totalDamage > 0) {
                applyContactReactions(ctx, actorId, targetId, move, totalDamage)
                applyDrainAndRecoil(ctx, actorId, move, totalDamage)
            }
        }

        // --- secondary effects ---------------------------------------------
        applyTargetEffects(ctx, actorId, targetId, move, totalDamage, lastEffectiveness)
    }

    private data class HitOutcome(
        val damage: Int,
        val critical: Boolean,
        val effectiveness: Effectiveness,
        val immune: Boolean,
    )

    private fun computeHit(
        ctx: TurnContext,
        actor: Battler,
        target: Battler,
        move: Move,
        comboBonus: Double,
    ): HitOutcome {
        // Fixed and percentage damage bypass the whole formula.
        move.effects.filterIsInstance<MoveEffect.FixedDamage>().firstOrNull()?.let {
            val immune = TypeChart.multiplier(
                move.element,
                target.monster.primaryElement,
                target.monster.secondaryElement,
            ) <= 0.0
            return HitOutcome(it.amount, false, Effectiveness.NEUTRAL, immune)
        }
        move.effects.filterIsInstance<MoveEffect.PercentDamage>().firstOrNull()?.let {
            val amount = (target.monster.currentHp * it.fraction).roundToInt().coerceAtLeast(1)
            return HitOutcome(amount, false, Effectiveness.NEUTRAL, false)
        }

        val forceCrit = move.effects.any { it is MoveEffect.AlwaysCritical } ||
            rng.chance(AbilityEffects.critChanceBonus(actor.abilityEffectId))
        val ignoreDef = move.effects.any { it is MoveEffect.IgnoreDefenceStages }

        var extra = comboBonus
        extra *= AbilityEffects.outgoingDamageMultiplier(actor.abilityEffectId, move, actor)
        move.effects.filterIsInstance<MoveEffect.BonusVersusStatus>().firstOrNull()?.let {
            if (target.monster.status == it.condition) extra *= it.multiplier
        }
        move.effects.filterIsInstance<MoveEffect.ScaleWithMissingHp>().firstOrNull()?.let {
            val missing = 1.0 - actor.monster.hpFraction
            extra *= 1.0 + missing * (it.maxMultiplier - 1.0)
        }
        move.effects.filterIsInstance<MoveEffect.ScaleWithBuffs>().firstOrNull()?.let { effect ->
            val positive = Stat.BATTLE_STATS.sumOf { actor.stages[it].coerceAtLeast(0) }
            extra *= 1.0 + positive * effect.perStage
        }

        val result = DamageCalculator.compute(
            attacker = actor,
            defender = target,
            move = move,
            weather = ctx.state.weather,
            dayPhase = ctx.state.dayPhase,
            rng = rng,
            comboBonus = 1.0,
            forceCritical = forceCrit,
            ignoreDefenceStages = ignoreDef,
            extraMultiplier = extra,
        )
        if (result.isImmune) {
            return HitOutcome(0, false, result.effectiveness, immune = true)
        }
        val incoming = AbilityEffects.incomingDamageMultiplier(
            effectId = target.abilityEffectId,
            move = move,
            effectiveness = result.effectiveness,
            defender = target,
            weather = ctx.state.weather,
        )
        val finalDamage = (result.damage * incoming).roundToInt().coerceAtLeast(1)
        return HitOutcome(finalDamage, result.critical, result.effectiveness, immune = false)
    }

    /** Thorn mail and contact-status abilities of the *defender*. */
    private fun applyContactReactions(
        ctx: TurnContext,
        actorId: String,
        targetId: String,
        move: Move,
        damage: Int,
    ) {
        if (!move.contact) return
        val target = ctx.state.battler(targetId) ?: return
        val actor = ctx.state.battler(actorId) ?: return
        if (actor.isFainted) return

        val recoilFraction = AbilityEffects.contactRecoilFraction(target.abilityEffectId)
        if (recoilFraction > 0.0) {
            val recoil = (damage * recoilFraction).roundToInt().coerceAtLeast(1)
            val (hurt, applied) = actor.absorbDamage(recoil)
            ctx.update(hurt)
            ctx.emit(
                BattleEvent.DamageDealt(
                    actorId = target.id,
                    targetId = actor.id,
                    amount = applied,
                    effectiveness = Effectiveness.NEUTRAL,
                    critical = false,
                    remainingHp = hurt.monster.currentHp,
                    maxHp = hurt.monster.maxHp,
                ),
            )
        }

        AbilityEffects.contactStatus(target.abilityEffectId)?.let { (condition, chance) ->
            if (rng.chance(chance)) {
                tryInflictStatus(ctx, actorId, condition, condition.defaultDurationTurns)
            }
        }
    }

    private fun applyDrainAndRecoil(ctx: TurnContext, actorId: String, move: Move, damage: Int) {
        val actor = ctx.state.battler(actorId) ?: return
        var working = actor

        val drainFraction = move.effects.filterIsInstance<MoveEffect.Drain>().firstOrNull()?.fraction
            ?: AbilityEffects.drainFraction(actor.abilityEffectId).takeIf { it > 0.0 }
        if (drainFraction != null && drainFraction > 0.0) {
            if (working.monster.status?.blocksHealing == true) {
                ctx.emit(BattleEvent.HealBlocked(working.id))
            } else {
                val healed = (damage * drainFraction).roundToInt().coerceAtLeast(1)
                working = working.withMonster { it.withHeal(healed) }
                ctx.update(working)
                ctx.emit(BattleEvent.Healed(working.id, healed, working.monster.currentHp))
            }
        }

        move.effects.filterIsInstance<MoveEffect.Recoil>().firstOrNull()?.let { recoil ->
            val amount = (damage * recoil.fraction).roundToInt().coerceAtLeast(1)
            working = ctx.state.battler(actorId) ?: working
            val (hurt, applied) = working.absorbDamage(amount)
            ctx.update(hurt)
            ctx.emit(
                BattleEvent.DamageDealt(
                    actorId = hurt.id,
                    targetId = hurt.id,
                    amount = applied,
                    effectiveness = Effectiveness.NEUTRAL,
                    critical = false,
                    remainingHp = hurt.monster.currentHp,
                    maxHp = hurt.monster.maxHp,
                ),
            )
        }
    }

    /** Effects that land on the move's target. */
    private fun applyTargetEffects(
        ctx: TurnContext,
        actorId: String,
        targetId: String,
        move: Move,
        damageDealt: Int,
        effectiveness: Effectiveness,
    ) {
        for (effect in move.effects) {
            if (!rng.chance(effect.chance)) continue
            when (effect) {
                is MoveEffect.InflictStatus -> {
                    val duration = if (effect.durationTurns > 0) {
                        effect.durationTurns
                    } else {
                        effect.condition.defaultDurationTurns
                    }
                    tryInflictStatus(ctx, targetId, effect.condition, duration)
                }

                is MoveEffect.ModifyStat -> {
                    val victimId = if (effect.onSelf) actorId else targetId
                    val victim = ctx.state.battler(victimId) ?: continue
                    val (stages, applied) = victim.stages.apply(effect.stat, effect.stages)
                    ctx.update(victim.copy(stages = stages))
                    ctx.emit(BattleEvent.StatChanged(victimId, effect.stat, effect.stages, applied))
                }

                is MoveEffect.ModifyRatio -> {
                    val victimId = if (effect.onSelf) actorId else targetId
                    val victim = ctx.state.battler(victimId) ?: continue
                    if (effect.kind == RatioKind.ACCURACY &&
                        effect.stages < 0 &&
                        AbilityEffects.accuracyCannotDrop(victim.abilityEffectId)
                    ) {
                        continue
                    }
                    val before = if (effect.kind == RatioKind.ACCURACY) victim.stages.accuracy else victim.stages.evasion
                    val updated = if (effect.kind == RatioKind.ACCURACY) {
                        victim.stages.applyAccuracy(effect.stages)
                    } else {
                        victim.stages.applyEvasion(effect.stages)
                    }
                    val after = if (effect.kind == RatioKind.ACCURACY) updated.accuracy else updated.evasion
                    ctx.update(victim.copy(stages = updated))
                    ctx.emit(BattleEvent.RatioChanged(victimId, effect.kind, after - before))
                }

                is MoveEffect.Heal -> {
                    val victimId = if (effect.onSelf) actorId else targetId
                    val victim = ctx.state.battler(victimId) ?: continue
                    if (victim.monster.status?.blocksHealing == true) {
                        ctx.emit(BattleEvent.HealBlocked(victimId))
                        continue
                    }
                    val amount = (victim.monster.maxHp * effect.fraction).roundToInt().coerceAtLeast(1)
                    val healed = victim.withMonster { it.withHeal(amount) }
                    ctx.update(healed)
                    ctx.emit(BattleEvent.Healed(victimId, amount, healed.monster.currentHp))
                }

                is MoveEffect.SetWeather -> {
                    ctx.state = ctx.state.copy(weather = effect.weather, weatherTurns = effect.turns)
                    ctx.emit(BattleEvent.WeatherChanged(effect.weather, effect.turns))
                }

                is MoveEffect.RaiseShield -> {
                    val victim = ctx.state.battler(actorId) ?: continue
                    val amount = (victim.monster.maxHp * effect.fraction).roundToInt().coerceAtLeast(1)
                    ctx.update(victim.copy(shieldHp = victim.shieldHp + amount, shieldTurns = effect.turns))
                    ctx.emit(BattleEvent.ShieldRaised(victim.id, amount, effect.turns))
                }

                is MoveEffect.Protect -> {
                    val victim = ctx.state.battler(actorId) ?: continue
                    // Consecutive protects get progressively less reliable.
                    val successChance = 1.0 / (1 shl victim.consecutiveProtects.coerceIn(0, 4))
                    if (rng.chance(successChance)) {
                        ctx.update(
                            victim.copy(
                                protectedTurns = effect.turns,
                                consecutiveProtects = victim.consecutiveProtects + 1,
                            ),
                        )
                        ctx.emit(BattleEvent.Protected(victim.id))
                    } else {
                        ctx.update(victim.copy(consecutiveProtects = 0))
                        ctx.emit(BattleEvent.MoveHadNoEffect(victim.id))
                    }
                }

                is MoveEffect.ClearStatChanges -> {
                    val victim = ctx.state.battler(targetId) ?: continue
                    val cleared = if (effect.positiveOnly) {
                        victim.stages.clearPositive()
                    } else {
                        victim.stages.reset()
                    }
                    ctx.update(victim.copy(stages = cleared))
                    ctx.emit(BattleEvent.Message("msg_stat_changes_cleared", listOf(victim.id)))
                }

                is MoveEffect.CureStatus -> {
                    val victimId = if (effect.onSelf) actorId else targetId
                    val victim = ctx.state.battler(victimId) ?: continue
                    val condition = victim.monster.status ?: continue
                    ctx.update(victim.withMonster { it.withStatus(null) })
                    ctx.emit(BattleEvent.StatusCured(victimId, condition))
                }

                is MoveEffect.WeakenForCapture -> {
                    val victim = ctx.state.battler(targetId) ?: continue
                    ctx.update(
                        victim.copy(
                            captureWeakness = effect.multiplier,
                            captureWeaknessTurns = effect.turns,
                        ),
                    )
                    ctx.emit(BattleEvent.Message("msg_capture_weakened", listOf(victim.id)))
                }

                is MoveEffect.StealItem -> {
                    val thief = ctx.state.battler(actorId) ?: continue
                    val victim = ctx.state.battler(targetId) ?: continue
                    val loot = victim.monster.heldItemId
                    if (thief.monster.heldItemId == null && loot != null) {
                        ctx.update(thief.withMonster { it.copy(heldItemId = loot) })
                        ctx.update(victim.withMonster { it.copy(heldItemId = null) })
                        ctx.emit(BattleEvent.Message("msg_item_stolen", listOf(thief.id, loot)))
                    }
                }

                is MoveEffect.MirrorMove -> {
                    val victim = ctx.state.battler(targetId) ?: continue
                    val copied = victim.lastMoveId ?: continue
                    val thief = ctx.state.battler(actorId) ?: continue
                    ctx.update(thief.copy(lastMoveId = copied))
                    ctx.emit(BattleEvent.Message("msg_move_mirrored", listOf(copied)))
                }

                is MoveEffect.SwitchOut -> {
                    val actor = ctx.state.battler(actorId) ?: continue
                    ctx.state = ctx.state.copy(pendingSwitchSide = actor.side)
                }

                // Handled inside computeHit / applyDrainAndRecoil.
                is MoveEffect.Drain,
                is MoveEffect.Recoil,
                is MoveEffect.MultiHit,
                is MoveEffect.FixedDamage,
                is MoveEffect.PercentDamage,
                is MoveEffect.Recharge,
                is MoveEffect.Charge,
                is MoveEffect.AlwaysCritical,
                is MoveEffect.IgnoreDefenceStages,
                is MoveEffect.BonusVersusStatus,
                is MoveEffect.ScaleWithMissingHp,
                is MoveEffect.ScaleWithBuffs,
                -> Unit
            }
        }
    }

    /** Effects declared with `onSelf = true` on moves that target opponents. */
    private fun applySelfOnlyEffects(ctx: TurnContext, actorId: String, move: Move) {
        if (move.target != MoveTarget.SELF && move.target != MoveTarget.FIELD) return
        // Self/field moves were already resolved through applyTargetEffects with
        // the actor as target; nothing further to do. The hook exists so future
        // effects that must run exactly once per move have a home.
    }

    /** Attempts to apply [condition]; respects immunity, occupancy and abilities. */
    private fun tryInflictStatus(
        ctx: TurnContext,
        targetId: String,
        condition: StatusCondition,
        durationTurns: Int,
    ) {
        val target = ctx.state.battler(targetId) ?: return
        if (target.isFainted) return
        if (target.monster.status != null) {
            ctx.emit(BattleEvent.StatusResisted(targetId, condition))
            return
        }
        if (!condition.canAfflict(target.monster.primaryElement, target.monster.secondaryElement)) {
            ctx.emit(BattleEvent.StatusResisted(targetId, condition))
            return
        }
        if (AbilityEffects.preventsStatus(target.abilityEffectId, condition)) {
            target.monster.abilityId?.let { id ->
                content.ability(id)?.let { ctx.emit(BattleEvent.AbilityTriggered(targetId, it.id, it.nameKey)) }
            }
            ctx.emit(BattleEvent.StatusResisted(targetId, condition))
            return
        }
        val modifier = AbilityEffects.statusDurationModifier(target.abilityEffectId)
        val turns = (durationTurns + modifier).coerceAtLeast(if (durationTurns > 0) 1 else 0)
        ctx.update(target.withMonster { it.withStatus(condition, turns) })
        ctx.emit(BattleEvent.StatusInflicted(targetId, condition))
    }

    // -----------------------------------------------------------------------
    // Items, orbs, switching, fleeing
    // -----------------------------------------------------------------------

    private fun resolveItem(ctx: TurnContext, action: BattleAction.UseItem) {
        val item = content.item(action.itemId) ?: return
        val targetId = action.targetId ?: action.actorId
        val target = ctx.state.battler(targetId) ?: return
        ctx.emit(BattleEvent.ItemUsed(action.actorId, item.id))

        for (effect in item.effects) {
            when (effect) {
                is ItemEffect.RestoreHp -> healBattler(ctx, targetId, effect.amount)
                is ItemEffect.RestoreHpFraction ->
                    healBattler(ctx, targetId, (target.monster.maxHp * effect.fraction).roundToInt())

                is ItemEffect.CureStatus -> {
                    val condition = target.monster.status
                    if (condition != null && (effect.conditions.isEmpty() || condition in effect.conditions)) {
                        ctx.update(target.withMonster { it.withStatus(null) })
                        ctx.emit(BattleEvent.StatusCured(targetId, condition))
                    }
                }

                is ItemEffect.Revive -> {
                    if (target.isFainted) {
                        val hp = (target.monster.maxHp * effect.hpFraction).roundToInt().coerceAtLeast(1)
                        ctx.update(target.withMonster { it.copy(currentHp = hp, status = null, statusTurns = 0) })
                        ctx.emit(BattleEvent.Healed(targetId, hp, hp))
                    }
                }

                is ItemEffect.RestorePp -> {
                    val restored = target.withMonster { monster ->
                        val slots = monster.moves.mapIndexed { index, slot ->
                            if (effect.allMoves || index == 0) {
                                slot.copy(currentPp = (slot.currentPp + effect.amount).coerceAtMost(slot.maxPp))
                            } else {
                                slot
                            }
                        }
                        monster.copy(moves = slots)
                    }
                    ctx.update(restored)
                }

                is ItemEffect.BattleStatBoost -> {
                    val (stages, applied) = target.stages.apply(effect.stat, effect.stages)
                    ctx.update(target.copy(stages = stages))
                    ctx.emit(BattleEvent.StatChanged(targetId, effect.stat, effect.stages, applied))
                }

                else -> Unit // Out-of-battle effects are handled by UseItemUseCase.
            }
        }
    }

    private fun healBattler(ctx: TurnContext, battlerId: String, amount: Int) {
        val battler = ctx.state.battler(battlerId) ?: return
        if (battler.monster.status?.blocksHealing == true) {
            ctx.emit(BattleEvent.HealBlocked(battlerId))
            return
        }
        val healed = battler.withMonster { it.withHeal(amount) }
        ctx.update(healed)
        ctx.emit(BattleEvent.Healed(battlerId, amount, healed.monster.currentHp))
    }

    private fun resolveOrb(ctx: TurnContext, action: BattleAction.ThrowOrb) {
        val orb = content.item(action.orbItemId) ?: return
        val target = ctx.state.battler(action.targetId) ?: return
        ctx.emit(BattleEvent.OrbThrown(orb.id, target.id))

        if (!ctx.state.type.allowsCapture) {
            ctx.emit(BattleEvent.Message("msg_capture_forbidden"))
            return
        }

        val luck = ctx.state.activePlayers.maxOfOrNull {
            AbilityEffects.captureLuckMultiplier(it.abilityEffectId)
        } ?: 1.0
        val result = CaptureCalculator.resolve(
            target = target,
            orbItem = orb,
            dayPhase = ctx.state.dayPhase,
            weather = ctx.state.weather,
            turnCount = ctx.state.turn,
            currentWorld = target.monster.species.nativeWorld,
            playerCaptureBonusPercent = ((luck - 1.0) * 100).roundToInt(),
            rng = rng,
        )
        for (shake in 1..result.shakes) {
            ctx.emit(BattleEvent.OrbShake(shake, CaptureCalculator.REQUIRED_SHAKES))
        }
        if (result.captured) {
            ctx.emit(BattleEvent.CaptureSucceeded(target.id, target.monster.uid))
            ctx.state = ctx.state.copy(
                outcome = BattleOutcome.CAPTURED,
                capturedMonsterUid = target.monster.uid,
            )
            ctx.emit(BattleEvent.BattleEnded(BattleOutcome.CAPTURED))
        } else {
            ctx.emit(BattleEvent.CaptureFailed(target.id, result.shakes))
        }
    }

    private fun resolveSwitch(ctx: TurnContext, action: BattleAction.Switch) {
        val actor = ctx.state.battler(action.actorId) ?: return
        val team = ctx.state.teamOf(actor.side)
        val incoming = team.getOrNull(action.toTeamIndex) ?: return
        if (incoming.isFainted || incoming.id == actor.id) return

        ctx.update(actor.onSwitchOut())
        ctx.emit(BattleEvent.SwitchedOut(actor.id, actor.monster.displayNameKey))

        val slots = if (actor.side == BattleSide.PLAYER) {
            ctx.state.activePlayerSlots
        } else {
            ctx.state.activeEnemySlots
        }
        val position = slots.indexOf(team.indexOfFirst { it.id == actor.id })
        if (position >= 0) {
            val updatedSlots = slots.toMutableList().apply { this[position] = action.toTeamIndex }
            ctx.state = if (actor.side == BattleSide.PLAYER) {
                ctx.state.copy(activePlayerSlots = updatedSlots)
            } else {
                ctx.state.copy(activeEnemySlots = updatedSlots)
            }
        }
        ctx.emit(BattleEvent.SwitchedIn(incoming.id, incoming.monster.displayNameKey, incoming.side))
        val entry = applyEntryAbilities(ctx.state, incoming)
        ctx.state = entry.state
        entry.events.forEach(ctx::emit)
    }

    private fun resolveFlee(ctx: TurnContext, action: BattleAction.Flee) {
        val actor = ctx.state.battler(action.actorId) ?: return
        if (!ctx.state.type.allowsFlee) {
            ctx.emit(BattleEvent.Message("msg_cannot_flee"))
            return
        }
        val fastestEnemy = ctx.state.activeOf(actor.side.opposite)
            .maxByOrNull { it.effectiveStat(Stat.SPEED) } ?: return
        val attempts = ctx.state.escapeAttempts
        val chance = AccuracyCalculator.fleeChance(actor, fastestEnemy, attempts)
        ctx.state = ctx.state.copy(escapeAttempts = attempts + 1)
        if (rng.chance(chance)) {
            ctx.emit(BattleEvent.FleeSucceeded(actor.id))
            ctx.state = ctx.state.copy(outcome = BattleOutcome.FLED)
            ctx.emit(BattleEvent.BattleEnded(BattleOutcome.FLED))
        } else {
            ctx.emit(BattleEvent.FleeFailed(actor.id))
        }
    }

    /** Fallback attack used when every move is out of PP. */
    private fun resolveDesperation(ctx: TurnContext, action: BattleAction.Struggle) {
        val actor = ctx.state.battler(action.actorId) ?: return
        val target = ctx.state.activeOf(actor.side.opposite).firstOrNull { !it.isFainted } ?: return
        ctx.emit(BattleEvent.Message("msg_desperation", listOf(actor.id)))
        val damage = DamageCalculator.desperationDamage(actor, target, rng)
        val (hurt, applied) = target.absorbDamage(damage)
        ctx.update(hurt)
        ctx.emit(
            BattleEvent.DamageDealt(
                actorId = actor.id,
                targetId = target.id,
                amount = applied,
                effectiveness = Effectiveness.NEUTRAL,
                critical = false,
                remainingHp = hurt.monster.currentHp,
                maxHp = hurt.monster.maxHp,
            ),
        )
        val recoil = (damage * 0.25).roundToInt().coerceAtLeast(1)
        val self = ctx.state.battler(actor.id) ?: return
        val (hurtSelf, selfApplied) = self.absorbDamage(recoil)
        ctx.update(hurtSelf)
        ctx.emit(
            BattleEvent.DamageDealt(
                actorId = actor.id,
                targetId = actor.id,
                amount = selfApplied,
                effectiveness = Effectiveness.NEUTRAL,
                critical = false,
                remainingHp = hurtSelf.monster.currentHp,
                maxHp = hurtSelf.monster.maxHp,
            ),
        )
    }

    // -----------------------------------------------------------------------
    // End of turn
    // -----------------------------------------------------------------------

    private fun endOfTurn(ctx: TurnContext) {
        // 1. weather chip damage
        val weather = ctx.state.weather
        if (weather.causesChipDamage) {
            for (battler in ctx.state.activePlayers + ctx.state.activeEnemies) {
                if (battler.isFainted) continue
                val elements = battler.monster.species.elements
                val immuneByType = elements.any { it in weather.chipImmuneElements }
                if (immuneByType || AbilityEffects.ignoresWeatherChip(battler.abilityEffectId, weather)) continue
                val damage = (battler.monster.maxHp * weather.chipDamageFraction).roundToInt().coerceAtLeast(1)
                val (hurt, applied) = battler.absorbDamage(damage)
                ctx.update(hurt)
                ctx.emit(BattleEvent.WeatherTicked(weather, battler.id, applied))
            }
        }

        // 2. status ticks
        for (battler in ctx.state.activePlayers + ctx.state.activeEnemies) {
            val current = ctx.state.battler(battler.id) ?: continue
            if (current.isFainted) continue
            val status = current.monster.status ?: continue
            if (status.endOfTurnDamageFraction > 0.0) {
                val damage = (current.monster.maxHp * status.endOfTurnDamageFraction)
                    .roundToInt().coerceAtLeast(1)
                val (hurt, applied) = current.absorbDamage(damage)
                ctx.update(hurt)
                ctx.emit(BattleEvent.StatusTicked(current.id, status, applied))
            }
            // Timed conditions other than sleep/confusion count down here.
            if (status.defaultDurationTurns > 0 &&
                status != StatusCondition.SLEEP &&
                status != StatusCondition.CONFUSION
            ) {
                val after = ctx.state.battler(battler.id) ?: continue
                val remaining = after.monster.statusTurns - 1
                if (remaining <= 0) {
                    ctx.update(after.withMonster { it.withStatus(null) })
                    ctx.emit(BattleEvent.StatusCured(after.id, status))
                } else {
                    ctx.update(after.withMonster { it.copy(statusTurns = remaining) })
                }
            }
        }

        // 3. ability regeneration & self-cure
        for (battler in ctx.state.activePlayers + ctx.state.activeEnemies) {
            val current = ctx.state.battler(battler.id) ?: continue
            if (current.isFainted) continue
            val effectId = current.abilityEffectId
            val healFraction = AbilityEffects.endOfTurnHealFraction(
                effectId,
                ctx.state.weather,
                ctx.state.dayPhase.isNight,
            )
            if (healFraction > 0.0 && current.monster.currentHp < current.monster.maxHp &&
                current.monster.status?.blocksHealing != true
            ) {
                val amount = (current.monster.maxHp * healFraction).roundToInt().coerceAtLeast(1)
                val healed = current.withMonster { it.withHeal(amount) }
                ctx.update(healed)
                ctx.emit(BattleEvent.Healed(healed.id, amount, healed.monster.currentHp))
            }
            val cureChance = AbilityEffects.selfCureChance(effectId)
            if (cureChance > 0.0 && current.monster.status != null && rng.chance(cureChance)) {
                val condition = current.monster.status!!
                val cured = ctx.state.battler(battler.id)!!.withMonster { it.withStatus(null) }
                ctx.update(cured)
                ctx.emit(BattleEvent.StatusCured(cured.id, condition))
            }
            val ppChance = AbilityEffects.ppRestoreChance(effectId)
            if (ppChance > 0.0 && rng.chance(ppChance)) {
                val holder = ctx.state.battler(battler.id) ?: continue
                val index = holder.monster.moves.indexOfFirst { it.currentPp < it.maxPp }
                if (index >= 0) {
                    ctx.update(
                        holder.withMonster { monster ->
                            val slots = monster.moves.toMutableList()
                            val slot = slots[index]
                            slots[index] = slot.copy(currentPp = slot.currentPp + 1)
                            monster.copy(moves = slots)
                        },
                    )
                }
            }
        }

        // 4. decay timers
        ctx.state = ctx.state.copy(
            playerTeam = ctx.state.playerTeam.map(::decayTimers),
            enemyTeam = ctx.state.enemyTeam.map(::decayTimers),
        )

        // 5. weather countdown
        if (ctx.state.weather != BattleWeather.CLEAR && ctx.state.weatherTurns > 0) {
            val remaining = ctx.state.weatherTurns - 1
            if (remaining <= 0) {
                val ended = ctx.state.weather
                ctx.state = ctx.state.copy(weather = BattleWeather.CLEAR, weatherTurns = 0)
                ctx.emit(BattleEvent.WeatherEnded(ended))
            } else {
                ctx.state = ctx.state.copy(weatherTurns = remaining)
            }
        }

        // 6. field effects countdown
        ctx.state = ctx.state.copy(
            fieldEffects = ctx.state.fieldEffects
                .map { it.copy(remainingTurns = it.remainingTurns - 1) }
                .filter { it.remainingTurns > 0 },
        )
    }

    private fun decayTimers(battler: Battler): Battler {
        var updated = battler
        if (updated.protectedTurns > 0) updated = updated.copy(protectedTurns = updated.protectedTurns - 1)
        if (updated.protectedTurns == 0 && !updated.hasActedThisTurn) {
            updated = updated.copy(consecutiveProtects = 0)
        }
        if (updated.shieldTurns > 0) {
            val turns = updated.shieldTurns - 1
            updated = if (turns <= 0) updated.copy(shieldTurns = 0, shieldHp = 0) else updated.copy(shieldTurns = turns)
        }
        if (updated.captureWeaknessTurns > 0) {
            val turns = updated.captureWeaknessTurns - 1
            updated = if (turns <= 0) {
                updated.copy(captureWeaknessTurns = 0, captureWeakness = 1.0)
            } else {
                updated.copy(captureWeaknessTurns = turns)
            }
        }
        return updated.copy(turnsOnField = updated.turnsOnField + 1)
    }

    // -----------------------------------------------------------------------
    // Turn context
    // -----------------------------------------------------------------------

    /** Mutable scratch pad for one turn; keeps the resolution code readable. */
    internal inner class TurnContext(var state: BattleState) {
        val events = mutableListOf<BattleEvent>()
        private val announcedFaints = mutableSetOf<String>()

        fun emit(event: BattleEvent) {
            events += event
        }

        fun update(battler: Battler) {
            state = state.withBattler(battler)
        }

        /** Emits a [BattleEvent.Fainted] for every newly downed battler. */
        fun checkFaints() {
            for (battler in state.allBattlers) {
                if (battler.isFainted && announcedFaints.add(battler.id)) {
                    emit(BattleEvent.Fainted(battler.id, battler.monster.displayNameKey))
                }
            }
        }

        fun evaluateOutcome() {
            if (state.isOver) return
            val playerWiped = state.isSideWiped(BattleSide.PLAYER)
            val enemyWiped = state.isSideWiped(BattleSide.ENEMY)
            val outcome = when {
                playerWiped && enemyWiped -> BattleOutcome.DRAW
                enemyWiped -> BattleOutcome.VICTORY
                playerWiped -> BattleOutcome.DEFEAT
                else -> BattleOutcome.ONGOING
            }
            if (outcome != BattleOutcome.ONGOING) {
                state = state.copy(outcome = outcome)
                emit(BattleEvent.BattleEnded(outcome))
            }
        }
    }

    /**
     * Applies post-battle rewards (experience, training values, friendship) to
     * the surviving party. Kept in the engine so the reward maths sits next to
     * the battle maths, but callable independently by the tournament mode.
     */
    fun awardVictorySpoils(
        state: BattleState,
        participants: Set<String>,
    ): List<BattleEvent> {
        val events = mutableListOf<BattleEvent>()
        val defeated = state.enemyTeam.filter { it.isFainted }
        if (defeated.isEmpty()) return events
        val eligible = state.playerTeam.filter { !it.isFainted && it.monster.uid in participants }
        if (eligible.isEmpty()) return events

        for (enemy in defeated) {
            val species = enemy.monster.species
            for (member in eligible) {
                val xp = com.runeveil.saga.domain.rules.ExperienceCurve.rewardFor(
                    baseExperience = species.baseExperience,
                    enemyLevel = enemy.monster.level,
                    rarityMultiplier = species.rarity.experienceMultiplier,
                    participants = eligible.size,
                    winnerLevel = member.monster.level,
                    isTrainerBattle = state.type == com.runeveil.saga.domain.model.battle.BattleType.TRAINER,
                    friendshipBonus = member.monster.friendship >= MonsterInstance.HIGH_FRIENDSHIP,
                )
                events += BattleEvent.ExperienceGained(member.monster.uid, xp)
            }
        }
        return events
    }

    companion object {
        /** Damage bonus granted by a Combination Strike. */
        const val COMBO_BONUS = 1.25
    }
}
