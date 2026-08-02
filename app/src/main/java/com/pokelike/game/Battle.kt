package com.pokelike.game

import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

enum class BattleKind { WILD, TRAINER, BOSS }
enum class BattleResult { NONE, WIN, LOSE, CAUGHT, FLED }

sealed class BEvent {
    data class Msg(val text: String) : BEvent()
    data class Hp(val playerSide: Boolean, val to: Int) : BEvent()
    data class Hit(val playerSide: Boolean, val effectiveness: Double) : BEvent()
    data class Faint(val playerSide: Boolean) : BEvent()
    data class SendOut(val playerSide: Boolean) : BEvent()
    data class Ball(val shakes: Int, val caught: Boolean) : BEvent()
    data class Exp(val monsterIndex: Int, val amount: Int) : BEvent()
    data class LevelUp(val monsterIndex: Int, val newLevel: Int) : BEvent()
    data class LearnMove(val monsterIndex: Int, val moveId: String) : BEvent()
    data class LearnPrompt(val monsterIndex: Int, val moveId: String) : BEvent()
    data class Evolve(val monsterIndex: Int, val toId: String) : BEvent()
    object RequestSwitch : BEvent()
    data class End(val result: BattleResult) : BEvent()
}

private class SideState {
    val stages = IntArray(6)     // ATK, DEF, SPA, SPD, SPE, ACC
    var confusion = 0
    var flinched = false
    fun reset() { stages.fill(0); confusion = 0; flinched = false }
}

class Battle(
    val playerParty: MutableList<Monster>,
    val foeParty: MutableList<Monster>,
    val kind: BattleKind,
    val trainerName: String = "",
    val rewardMoney: Int = 0,
    val trainerStyle: String = "npc_trainer"
) {
    var playerIndex = 0
    var foeIndex = 0
    var result: BattleResult = BattleResult.NONE
    var over = false
    var caughtMonster: Monster? = null
    var lastItemConsumed = false
    var moneyEarned = 0

    private val pSide = SideState()
    private val fSide = SideState()
    private var runAttempts = 0
    private var trainerPotions = if (kind == BattleKind.BOSS) 2 else if (kind == BattleKind.TRAINER) 1 else 0
    private val ev = mutableListOf<BEvent>()
    private val participants = HashSet<Int>()

    val activePlayer: Monster get() = playerParty[playerIndex]
    val activeFoe: Monster get() = foeParty[foeIndex]
    val isWild: Boolean get() = kind == BattleKind.WILD

    // ------------------------------------------------------------------
    // Start
    // ------------------------------------------------------------------
    fun start(): List<BEvent> {
        ev.clear()
        playerIndex = playerParty.indexOfFirst { !it.isFainted }.coerceAtLeast(0)
        foeIndex = foeParty.indexOfFirst { !it.isFainted }.coerceAtLeast(0)
        participants.add(playerIndex)
        if (isWild) {
            ev.add(BEvent.Msg("Ein wildes ${activeFoe.name} erscheint!"))
        } else {
            ev.add(BEvent.Msg("$trainerName fordert dich heraus!"))
            ev.add(BEvent.Msg("$trainerName setzt ${activeFoe.name} ein!"))
        }
        ev.add(BEvent.SendOut(false))
        ev.add(BEvent.Msg("Los, ${activePlayer.name}!"))
        ev.add(BEvent.SendOut(true))
        return ev.toList()
    }

    // ------------------------------------------------------------------
    // Spieleraktionen
    // ------------------------------------------------------------------
    fun playerMove(moveIndex: Int): List<BEvent> {
        ev.clear()
        val slot = activePlayer.moves.getOrNull(moveIndex)
        if (slot == null || slot.pp <= 0) {
            ev.add(BEvent.Msg("Keine AP mehr fuer diese Attacke!"))
            return ev.toList()
        }
        runTurn(PlayerAction.Fight(moveIndex))
        return ev.toList()
    }

    fun playerItem(itemId: String, targetIndex: Int): List<BEvent> {
        ev.clear()
        runTurn(PlayerAction.UseItem(itemId, targetIndex))
        return ev.toList()
    }

    fun playerSwitch(index: Int): List<BEvent> {
        ev.clear()
        runTurn(PlayerAction.Switch(index))
        return ev.toList()
    }

    fun playerRun(): List<BEvent> {
        ev.clear()
        runTurn(PlayerAction.Run)
        return ev.toList()
    }

    /** Nach einem Kampfunfaehigkeits-Wechsel des Spielers. */
    fun switchAfterFaint(index: Int): List<BEvent> {
        ev.clear()
        playerIndex = index
        participants.add(index)
        pSide.reset()
        ev.add(BEvent.Msg("Los, ${activePlayer.name}!"))
        ev.add(BEvent.SendOut(true))
        return ev.toList()
    }

    private sealed class PlayerAction {
        data class Fight(val moveIndex: Int) : PlayerAction()
        data class UseItem(val itemId: String, val target: Int) : PlayerAction()
        data class Switch(val index: Int) : PlayerAction()
        object Run : PlayerAction()
    }

    // ------------------------------------------------------------------
    // Rundenablauf
    // ------------------------------------------------------------------
    private fun runTurn(action: PlayerAction) {
        pSide.flinched = false
        fSide.flinched = false

        // Nicht-Angriffs-Aktionen des Spielers zuerst
        when (action) {
            is PlayerAction.Run -> {
                if (!isWild) {
                    ev.add(BEvent.Msg("Vor einem Kampf kann man nicht fliehen!"))
                } else if (tryRun()) {
                    ev.add(BEvent.Msg("Du bist entkommen!"))
                    finish(BattleResult.FLED)
                    return
                } else {
                    ev.add(BEvent.Msg("Flucht gescheitert!"))
                    foeTurn()
                    endOfTurn()
                    return
                }
            }
            is PlayerAction.UseItem -> {
                val consumed = useItemInBattle(action.itemId, action.target)
                lastItemConsumed = consumed
                if (over) return
                if (!consumed) return   // Fehlschlag kostet keine Runde
                foeTurn()
                endOfTurn()
                return
            }
            is PlayerAction.Switch -> {
                pSide.reset()
                playerIndex = action.index
                participants.add(action.index)
                ev.add(BEvent.Msg("Komm zurueck! Los, ${activePlayer.name}!"))
                ev.add(BEvent.SendOut(true))
                foeTurn()
                endOfTurn()
                return
            }
            is PlayerAction.Fight -> Unit
        }

        val playerMoveSlot = (action as PlayerAction.Fight).let { activePlayer.moves[it.moveIndex] }
        val foeChoice = chooseFoeAction()

        if (foeChoice is FoeAction.Potion) {
            doFoePotion()
            if (!over) {
                doAttack(true, playerMoveSlot)
                if (!over) endOfTurn()
            }
            return
        }

        val foeMoveSlot = (foeChoice as FoeAction.Fight).slot
        val pPrio = playerMoveSlot.move.priority
        val fPrio = foeMoveSlot.move.priority
        val pSpe = effSpeed(activePlayer, pSide)
        val fSpe = effSpeed(activeFoe, fSide)
        val playerFirst = when {
            pPrio != fPrio -> pPrio > fPrio
            pSpe != fSpe -> pSpe > fSpe
            else -> Random.nextBoolean()
        }

        if (playerFirst) {
            doAttack(true, playerMoveSlot)
            if (!over && !activeFoe.isFainted) doAttack(false, foeMoveSlot)
        } else {
            doAttack(false, foeMoveSlot)
            if (!over && !activePlayer.isFainted) doAttack(true, playerMoveSlot)
        }
        if (!over) endOfTurn()
    }

    private sealed class FoeAction {
        data class Fight(val slot: MoveSlot) : FoeAction()
        object Potion : FoeAction()
    }

    private fun foeTurn() {
        if (over) return
        when (val a = chooseFoeAction()) {
            is FoeAction.Potion -> doFoePotion()
            is FoeAction.Fight -> doAttack(false, a.slot)
        }
    }

    // ------------------------------------------------------------------
    // Angriffe
    // ------------------------------------------------------------------
    private fun doAttack(byPlayer: Boolean, slot: MoveSlot) {
        if (over) return
        val attacker = if (byPlayer) activePlayer else activeFoe
        val defender = if (byPlayer) activeFoe else activePlayer
        val aSide = if (byPlayer) pSide else fSide
        val dSide = if (byPlayer) fSide else pSide
        if (attacker.isFainted || defender.isFainted) return

        val who = nameOf(attacker, byPlayer)

        // Statusbedingte Blockaden
        if (attacker.status == StatusKind.SCHLAF) {
            if (attacker.sleepTurns > 0) attacker.sleepTurns--
            if (attacker.sleepTurns <= 0) {
                attacker.status = StatusKind.NONE
                ev.add(BEvent.Msg("$who wacht auf!"))
            } else {
                ev.add(BEvent.Msg("$who schlaeft tief und fest."))
                return
            }
        }
        if (attacker.status == StatusKind.FROST) {
            if (Random.nextInt(100) < 20) {
                attacker.status = StatusKind.NONE
                ev.add(BEvent.Msg("$who taut auf!"))
            } else {
                ev.add(BEvent.Msg("$who ist eingefroren!"))
                return
            }
        }
        if (aSide.flinched) {
            ev.add(BEvent.Msg("$who ist zurueckgeschreckt!"))
            aSide.flinched = false
            return
        }
        if (attacker.status == StatusKind.PARALYSE && Random.nextInt(100) < 25) {
            ev.add(BEvent.Msg("$who ist paralysiert und kann nicht angreifen!"))
            return
        }
        if (aSide.confusion > 0) {
            aSide.confusion--
            if (aSide.confusion <= 0) {
                ev.add(BEvent.Msg("$who ist nicht mehr verwirrt."))
            } else {
                ev.add(BEvent.Msg("$who ist verwirrt!"))
                if (Random.nextInt(100) < 33) {
                    val dmg = max(1, confusionDamage(attacker))
                    attacker.damage(dmg)
                    ev.add(BEvent.Msg("$who hat sich selbst verletzt!"))
                    ev.add(BEvent.Hp(byPlayer, attacker.currentHp))
                    if (attacker.isFainted) handleFaint(byPlayer)
                    return
                }
            }
        }

        val move = slot.move
        if (slot.pp <= 0) {
            ev.add(BEvent.Msg("$who hat keine AP mehr!"))
            return
        }
        slot.pp--
        ev.add(BEvent.Msg("$who setzt ${move.name} ein!"))

        // Genauigkeit
        if (move.acc > 0) {
            val accMod = stageMultiplier(aSide.stages[5], true)
            val chance = (move.acc * accMod).toInt().coerceIn(5, 100)
            if (Random.nextInt(100) >= chance) {
                ev.add(BEvent.Msg("Die Attacke geht daneben!"))
                return
            }
        }

        if (move.cat == MoveCategory.STATUS) {
            applyStatusMove(byPlayer, move, attacker, defender, aSide, dSide)
            if (defender.isFainted) handleFaint(!byPlayer)
            return
        }

        // Typ-Effektivitaet
        val eff = TypeChart.multiplier(move.type, defender.species.types)
        if (eff == 0.0) {
            ev.add(BEvent.Msg("Es hat keine Wirkung auf ${nameOf(defender, !byPlayer)} ..."))
            return
        }

        val hits = if (move.maxHits > move.minHits)
            Random.nextInt(move.minHits, move.maxHits + 1) else move.minHits
        var totalDmg = 0
        var crit = false
        for (h in 0 until hits) {
            if (defender.isFainted) break
            val r = computeDamage(attacker, defender, aSide, dSide, move, eff)
            crit = crit || r.second
            val dealt = defender.damage(r.first)
            totalDmg += dealt
            ev.add(BEvent.Hit(!byPlayer, eff))
            ev.add(BEvent.Hp(!byPlayer, defender.currentHp))
        }
        if (crit) ev.add(BEvent.Msg("Ein Volltreffer!"))
        TypeChart.effectivenessText(eff)?.let { ev.add(BEvent.Msg(it)) }
        if (hits > 1) ev.add(BEvent.Msg("Getroffen: ${hits}x!"))

        // Absorption / Rueckstoss
        if (move.drainPct > 0 && totalDmg > 0) {
            val healed = attacker.heal(totalDmg * move.drainPct / 100)
            if (healed > 0) {
                ev.add(BEvent.Msg("${nameOf(attacker, byPlayer)} saugt Energie ab!"))
                ev.add(BEvent.Hp(byPlayer, attacker.currentHp))
            }
        }
        if (move.recoilPct > 0 && totalDmg > 0) {
            attacker.damage(max(1, totalDmg * move.recoilPct / 100))
            ev.add(BEvent.Msg("${nameOf(attacker, byPlayer)} wird vom Rueckstoss getroffen!"))
            ev.add(BEvent.Hp(byPlayer, attacker.currentHp))
        }

        // Zusatzeffekte
        if (!defender.isFainted) {
            if (move.status != null && Random.nextInt(100) < move.statusChance) {
                applyStatus(defender, move.status, !byPlayer)
            }
            if (move.confuseChance > 0 && Random.nextInt(100) < move.confuseChance && dSide.confusion == 0) {
                dSide.confusion = Random.nextInt(2, 5)
                ev.add(BEvent.Msg("${nameOf(defender, !byPlayer)} ist verwirrt!"))
            }
            if (move.flinchChance > 0 && Random.nextInt(100) < move.flinchChance) {
                dSide.flinched = true
            }
            if (move.statChanges.isNotEmpty() && Random.nextInt(100) < move.statChance) {
                for (sc in move.statChanges) {
                    val toPlayerSide = if (sc.target == Target.SELF) byPlayer else !byPlayer
                    applyStatChange(toPlayerSide, sc.stat, sc.stages)
                }
            }
        }

        if (attacker.isFainted) handleFaint(byPlayer)
        if (defender.isFainted) handleFaint(!byPlayer)
    }

    private fun applyStatusMove(
        byPlayer: Boolean, move: Move, attacker: Monster, defender: Monster,
        aSide: SideState, dSide: SideState
    ) {
        var did = false
        if (move.healPct > 0) {
            val healed = attacker.heal(attacker.maxHp * move.healPct / 100)
            if (healed > 0) {
                ev.add(BEvent.Msg("${nameOf(attacker, byPlayer)} erholt sich!"))
                ev.add(BEvent.Hp(byPlayer, attacker.currentHp))
                did = true
            }
        }
        if (move.status != null) {
            val eff = TypeChart.multiplier(move.type, defender.species.types)
            if (eff == 0.0) {
                ev.add(BEvent.Msg("Es hat keine Wirkung ..."))
            } else if (applyStatus(defender, move.status, !byPlayer)) {
                did = true
            }
        }
        if (move.confuseChance > 0) {
            if (dSide.confusion == 0) {
                dSide.confusion = Random.nextInt(2, 5)
                ev.add(BEvent.Msg("${nameOf(defender, !byPlayer)} ist verwirrt!"))
                did = true
            } else ev.add(BEvent.Msg("Es passiert nichts."))
        }
        for (sc in move.statChanges) {
            val toPlayerSide = if (sc.target == Target.SELF) byPlayer else !byPlayer
            if (applyStatChange(toPlayerSide, sc.stat, sc.stages)) did = true
        }
        if (!did && move.statChanges.isEmpty() && move.status == null && move.healPct == 0) {
            ev.add(BEvent.Msg("Es passiert nichts."))
        }
    }

    private fun applyStatus(target: Monster, kind: StatusKind, targetIsPlayer: Boolean): Boolean {
        val n = nameOf(target, targetIsPlayer)
        if (target.status != StatusKind.NONE) return false
        // Typ-Immunitaeten
        val types = target.species.types
        if (kind == StatusKind.GIFT && types.contains(Type.GIFT)) return false
        if (kind == StatusKind.BRAND && types.contains(Type.FEUER)) return false
        if (kind == StatusKind.FROST && types.contains(Type.EIS)) return false
        if (kind == StatusKind.PARALYSE && types.contains(Type.ELEKTRO)) return false
        target.status = kind
        if (kind == StatusKind.SCHLAF) target.sleepTurns = Random.nextInt(1, 4)
        ev.add(BEvent.Msg("$n ist jetzt ${kind.deName}!"))
        return true
    }

    private fun applyStatChange(onPlayerSide: Boolean, stat: Stat, stages: Int): Boolean {
        val side = if (onPlayerSide) pSide else fSide
        val mon = if (onPlayerSide) activePlayer else activeFoe
        val idx = statIndex(stat)
        val before = side.stages[idx]
        val after = (before + stages).coerceIn(-6, 6)
        val n = nameOf(mon, onPlayerSide)
        if (after == before) {
            ev.add(BEvent.Msg(
                if (stages > 0) "${stat.deName} von $n kann nicht weiter steigen!"
                else "${stat.deName} von $n kann nicht weiter sinken!"
            ))
            return false
        }
        side.stages[idx] = after
        val word = when {
            stages >= 2 -> "steigt stark"
            stages == 1 -> "steigt"
            stages == -1 -> "sinkt"
            else -> "sinkt stark"
        }
        ev.add(BEvent.Msg("${stat.deName} von $n $word!"))
        return true
    }

    private fun statIndex(s: Stat) = when (s) {
        Stat.ATK -> 0; Stat.DEF -> 1; Stat.SPA -> 2; Stat.SPD -> 3; Stat.SPE -> 4; else -> 5
    }

    private fun stageMultiplier(stage: Int, accuracy: Boolean = false): Double {
        val n = stage.coerceIn(-6, 6)
        return if (accuracy) {
            if (n >= 0) (3.0 + n) / 3.0 else 3.0 / (3.0 - n)
        } else {
            if (n >= 0) (2.0 + n) / 2.0 else 2.0 / (2.0 - n)
        }
    }

    private fun effSpeed(m: Monster, s: SideState): Int {
        var v = m.stat(Stat.SPE) * stageMultiplier(s.stages[4])
        if (m.status == StatusKind.PARALYSE) v *= 0.25
        return v.toInt()
    }

    private fun confusionDamage(m: Monster): Int {
        val a = m.stat(Stat.ATK)
        val d = m.stat(Stat.DEF)
        return ((2.0 * m.level / 5 + 2) * 40 * a / d / 50 + 2).toInt()
    }

    /** Liefert Schaden und ob es ein Volltreffer war. */
    private fun computeDamage(
        attacker: Monster, defender: Monster,
        aSide: SideState, dSide: SideState,
        move: Move, eff: Double
    ): Pair<Int, Boolean> {
        val physical = move.cat == MoveCategory.PHYSISCH
        val critChance = if (move.highCrit) 8 else 16
        val crit = Random.nextInt(critChance) == 0

        var atk = (if (physical) attacker.stat(Stat.ATK) else attacker.stat(Stat.SPA)).toDouble()
        var def = (if (physical) defender.stat(Stat.DEF) else defender.stat(Stat.SPD)).toDouble()
        val aStage = if (physical) aSide.stages[0] else aSide.stages[2]
        val dStage = if (physical) dSide.stages[1] else dSide.stages[3]
        // Volltreffer ignorieren nachteilige Veraenderungen
        atk *= stageMultiplier(if (crit && aStage < 0) 0 else aStage)
        def *= stageMultiplier(if (crit && dStage > 0) 0 else dStage)
        if (physical && attacker.status == StatusKind.BRAND) atk *= 0.5

        var dmg = (2.0 * attacker.level / 5.0 + 2.0) * move.power * atk / def / 50.0 + 2.0
        if (crit) dmg *= 1.5
        if (move.type in attacker.species.types) dmg *= 1.5     // STAB-Bonus
        dmg *= eff
        dmg *= Random.nextDouble(0.85, 1.0)
        return Pair(max(1, dmg.toInt()), crit)
    }

    // ------------------------------------------------------------------
    // Gegner-KI
    // ------------------------------------------------------------------
    private fun chooseFoeAction(): FoeAction {
        val foe = activeFoe
        if (kind != BattleKind.WILD && trainerPotions > 0 && foe.hpRatio < 0.28 && Random.nextInt(100) < 65) {
            return FoeAction.Potion
        }
        val usable = foe.moves.filter { it.pp > 0 }
        if (usable.isEmpty()) return FoeAction.Fight(MoveSlot.of("tackle"))
        if (kind == BattleKind.WILD) {
            // Wilde Monster greifen weitgehend zufaellig an, bevorzugen aber Schaden
            val damaging = usable.filter { it.move.power > 0 }
            val pick = if (damaging.isNotEmpty() && Random.nextInt(100) < 75)
                damaging.random() else usable.random()
            return FoeAction.Fight(pick)
        }
        // Trainer und Bosse waehlen die staerkste Attacke
        var best: MoveSlot = usable[0]
        var bestScore = -1.0
        for (slot in usable) {
            val m = slot.move
            var score: Double
            if (m.cat == MoveCategory.STATUS) {
                score = 12.0
                if (m.status != null && activePlayer.status == StatusKind.NONE) score += 22.0
                if (m.statChanges.any { it.target == Target.SELF && it.stages > 0 }) score += 14.0
                if (m.healPct > 0 && foe.hpRatio < 0.5) score += 40.0
                if (kind == BattleKind.BOSS) score *= 1.15
                score *= Random.nextDouble(0.6, 1.2)
            } else {
                val eff = TypeChart.multiplier(m.type, activePlayer.species.types)
                val stab = if (m.type in foe.species.types) 1.5 else 1.0
                val atk = if (m.cat == MoveCategory.PHYSISCH) foe.stat(Stat.ATK) else foe.stat(Stat.SPA)
                val def = if (m.cat == MoveCategory.PHYSISCH) activePlayer.stat(Stat.DEF) else activePlayer.stat(Stat.SPD)
                score = m.power * eff * stab * atk / max(1, def) * (m.acc.coerceAtLeast(1) / 100.0)
                score *= if (kind == BattleKind.BOSS) Random.nextDouble(0.92, 1.08)
                else Random.nextDouble(0.7, 1.3)
            }
            if (score > bestScore) { bestScore = score; best = slot }
        }
        return FoeAction.Fight(best)
    }

    private fun doFoePotion() {
        trainerPotions--
        val foe = activeFoe
        val healed = foe.heal(foe.maxHp / 2)
        ev.add(BEvent.Msg("$trainerName setzt einen Hypertrank ein!"))
        ev.add(BEvent.Hp(false, foe.currentHp))
        if (healed <= 0) ev.add(BEvent.Msg("Es hatte keine Wirkung."))
    }

    // ------------------------------------------------------------------
    // Items im Kampf
    // ------------------------------------------------------------------
    private fun useItemInBattle(itemId: String, targetIndex: Int): Boolean {
        val item = Items.get(itemId)
        if (item.ballRate > 0.0) {
            if (!isWild) {
                ev.add(BEvent.Msg("Du kannst keine fremden Monster fangen!"))
                return false
            }
            throwBall(item)
            return true
        }
        if (item.battleStat != null) {
            ev.add(BEvent.Msg("Du setzt ${item.name} ein!"))
            applyStatChange(true, item.battleStat, item.battleStages)
            return true
        }
        val target = playerParty.getOrNull(targetIndex) ?: return false
        val msg = applyHealingItem(item, target)
        if (msg == null) {
            ev.add(BEvent.Msg("Es wuerde keine Wirkung haben."))
            return false
        }
        ev.add(BEvent.Msg("Du setzt ${item.name} ein!"))
        ev.add(BEvent.Msg(msg))
        if (targetIndex == playerIndex) ev.add(BEvent.Hp(true, activePlayer.currentHp))
        return true
    }

    private fun throwBall(item: Item) {
        val foe = activeFoe
        ev.add(BEvent.Msg("Du wirfst einen ${item.name}!"))
        val statusBonus = when (foe.status) {
            StatusKind.SCHLAF, StatusKind.FROST -> 2.5
            StatusKind.PARALYSE, StatusKind.GIFT, StatusKind.BRAND -> 1.5
            else -> 1.0
        }
        val a = ((3.0 * foe.maxHp - 2.0 * foe.currentHp) * foe.species.catchRate * item.ballRate * statusBonus) /
                (3.0 * foe.maxHp)
        var shakes = 0
        var caught = false
        if (a >= 255.0) {
            shakes = 3; caught = true
        } else {
            val b = 65536.0 / (255.0 / a).pow(0.1875)
            caught = true
            for (i in 0 until 4) {
                if (Random.nextInt(65536) < b) shakes++
                else { caught = false; break }
            }
            if (shakes > 3) shakes = 3
        }
        ev.add(BEvent.Ball(shakes, caught))
        if (caught) {
            ev.add(BEvent.Msg("Gefangen! ${foe.name} wurde gefangen!"))
            caughtMonster = foe
            finish(BattleResult.CAUGHT)
        } else {
            val text = when (shakes) {
                0 -> "Oh nein! Das Monster hat sich sofort befreit!"
                1 -> "Mist! Es war so nah dran!"
                2 -> "Argh! Fast haettest du es geschafft!"
                else -> "So ein Pech! Es hat sich befreit!"
            }
            ev.add(BEvent.Msg(text))
            foeTurn()
            endOfTurn()
        }
    }

    // ------------------------------------------------------------------
    // Rundenende, Ohnmacht, Erfahrung
    // ------------------------------------------------------------------
    private fun endOfTurn() {
        if (over) return
        residual(true)
        if (over) return
        residual(false)
    }

    private fun residual(playerSide: Boolean) {
        val m = if (playerSide) activePlayer else activeFoe
        if (m.isFainted) return
        when (m.status) {
            StatusKind.GIFT -> {
                val d = max(1, m.maxHp / 8)
                m.damage(d)
                ev.add(BEvent.Msg("${nameOf(m, playerSide)} leidet unter der Vergiftung!"))
                ev.add(BEvent.Hp(playerSide, m.currentHp))
            }
            StatusKind.BRAND -> {
                val d = max(1, m.maxHp / 16)
                m.damage(d)
                ev.add(BEvent.Msg("${nameOf(m, playerSide)} leidet unter der Verbrennung!"))
                ev.add(BEvent.Hp(playerSide, m.currentHp))
            }
            else -> Unit
        }
        if (m.isFainted) handleFaint(playerSide)
    }

    private fun handleFaint(playerSide: Boolean) {
        if (over) return
        val m = if (playerSide) activePlayer else activeFoe
        if (!m.isFainted) return
        ev.add(BEvent.Msg("${nameOf(m, playerSide)} wurde besiegt!"))
        ev.add(BEvent.Faint(playerSide))
        if (playerSide) {
            pSide.reset()
            if (playerParty.none { !it.isFainted }) {
                ev.add(BEvent.Msg("Du hast keine kampffaehigen Monster mehr!"))
                finish(BattleResult.LOSE)
            } else {
                ev.add(BEvent.RequestSwitch)
            }
        } else {
            grantExp(m)
            fSide.reset()
            val next = foeParty.indexOfFirst { !it.isFainted }
            if (next < 0) {
                if (!isWild) {
                    moneyEarned = rewardMoney
                    ev.add(BEvent.Msg("Du hast $trainerName besiegt!"))
                    if (rewardMoney > 0) ev.add(BEvent.Msg("Du erhaeltst $rewardMoney Muenzen!"))
                }
                finish(BattleResult.WIN)
            } else {
                foeIndex = next
                ev.add(BEvent.Msg("$trainerName setzt ${activeFoe.name} ein!"))
                ev.add(BEvent.SendOut(false))
            }
        }
    }

    private fun grantExp(defeated: Monster) {
        val base = defeated.species.baseExp * defeated.level / 7
        val trainerBonus = if (isWild) 1.0 else 1.5
        for (i in playerParty.indices) {
            val m = playerParty[i]
            if (m.isFainted || m.level >= Monster.MAX_LEVEL) continue
            val share = if (i == playerIndex) 1.0 else if (participants.contains(i)) 0.5 else 0.3
            val amount = max(1, (base * trainerBonus * share).toInt())
            ev.add(BEvent.Exp(i, amount))
            val levels = m.gainExp(amount)
            for (lv in levels) {
                ev.add(BEvent.LevelUp(i, lv))
                for (mv in m.movesLearnedAt(lv)) {
                    if (m.moves.size < 4) {
                        m.learnMove(mv)
                        ev.add(BEvent.LearnMove(i, mv))
                    } else {
                        ev.add(BEvent.LearnPrompt(i, mv))
                    }
                }
            }
        }
    }

    private fun tryRun(): Boolean {
        runAttempts++
        val p = effSpeed(activePlayer, pSide)
        val f = max(1, effSpeed(activeFoe, fSide))
        if (p > f) return true
        val odds = (p * 128 / f + 30 * runAttempts) % 256
        return Random.nextInt(256) < odds
    }

    private fun finish(res: BattleResult) {
        if (over) return
        result = res
        over = true
        if (res == BattleResult.WIN || res == BattleResult.CAUGHT) checkEvolutions()
        ev.add(BEvent.End(res))
    }

    private fun checkEvolutions() {
        for (i in playerParty.indices) {
            val target = playerParty[i].evolutionTarget()
            if (target != null) ev.add(BEvent.Evolve(i, target))
        }
    }

    private fun nameOf(m: Monster, playerSide: Boolean): String =
        if (playerSide) m.name else if (isWild) "Wildes ${m.name}" else "Gegn. ${m.name}"

    companion object {
        /** Heil-/Statusitems ausserhalb und innerhalb des Kampfes. Gibt Meldung oder null zurueck. */
        fun applyHealingItem(item: Item, target: Monster): String? {
            if (item.revivePct > 0) {
                if (!target.isFainted) return null
                target.currentHp = max(1, target.maxHp * item.revivePct / 100)
                target.status = StatusKind.NONE
                return "${target.name} wurde wiederbelebt!"
            }
            if (target.isFainted && (item.healHp != 0 || item.healStatus.isNotEmpty() || item.healAllStatus))
                return null
            if (item.healHp != 0) {
                val amount = if (item.healHp < 0) target.maxHp else item.healHp
                val healed = target.heal(amount)
                if (healed <= 0) return null
                return "${target.name} erhaelt $healed KP zurueck!"
            }
            if (item.healAllStatus) {
                if (target.status == StatusKind.NONE) return null
                target.status = StatusKind.NONE
                target.sleepTurns = 0
                return "${target.name} geht es wieder gut!"
            }
            if (item.healStatus.isNotEmpty()) {
                if (!item.healStatus.contains(target.status)) return null
                target.status = StatusKind.NONE
                target.sleepTurns = 0
                return "${target.name} geht es wieder gut!"
            }
            if (item.restorePp) {
                var did = false
                target.moves.forEach { if (it.pp < it.maxPp) { it.pp = it.maxPp; did = true } }
                return if (did) "Die AP von ${target.name} wurden aufgefuellt!" else null
            }
            return null
        }
    }
}
