package com.runeveil.saga.domain.battle

import com.runeveil.saga.domain.TestFixtures
import com.runeveil.saga.domain.model.battle.BattleAction
import com.runeveil.saga.domain.model.battle.BattleEvent
import com.runeveil.saga.domain.model.battle.BattleOutcome
import com.runeveil.saga.domain.model.battle.BattleSide
import com.runeveil.saga.domain.model.battle.BattleType
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.MoveCategory
import com.runeveil.saga.domain.model.battle.MoveEffect
import com.runeveil.saga.domain.model.battle.MoveTarget
import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.util.ScriptedRng
import com.runeveil.saga.domain.util.SeededRng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end tests of the turn pipeline. Every test drives the engine with a
 * scripted RNG so the outcome is exact, not statistical.
 */
class BattleEngineTest {

    /** Rng that makes every probability check succeed and picks index 0. */
    private fun alwaysRng() = ScriptedRng(doubles = listOf(0.0), ints = listOf(0))

    /** Rng that makes every probability check fail. */
    private fun neverRng() = ScriptedRng(doubles = listOf(0.9999), ints = listOf(0))

    private fun setup(
        playerMoves: List<String> = listOf("move_strike"),
        enemyMoves: List<String> = listOf("move_strike"),
        playerElement: Element = Element.FIRE,
        enemyElement: Element = Element.SPIRIT,
        playerLevel: Int = 50,
        enemyLevel: Int = 50,
        type: BattleType = BattleType.WILD,
    ): com.runeveil.saga.domain.model.battle.BattleState {
        val hero = TestFixtures.battler(
            id = "hero",
            side = BattleSide.PLAYER,
            monster = TestFixtures.monster(
                uid = "hero",
                level = playerLevel,
                species = TestFixtures.species(id = "hero_sp", primary = playerElement),
                moveIds = playerMoves,
            ),
        )
        val foe = TestFixtures.battler(
            id = "foe",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(
                uid = "foe",
                level = enemyLevel,
                species = TestFixtures.species(id = "foe_sp", primary = enemyElement),
                moveIds = enemyMoves,
            ),
        )
        return TestFixtures.battleState(listOf(hero), listOf(foe), type)
    }

    @Test
    fun `a plain attack damages the target and emits a damage event`() {
        val content = TestFixtures.content(moves = listOf(TestFixtures.move()))
        val engine = BattleEngine(content, SeededRng(7))
        val started = engine.start(setup())

        val result = engine.executeTurn(
            started.state,
            listOf(BattleAction.UseMove("hero", 0, listOf("foe"))),
        )

        val damage = result.events.filterIsInstance<BattleEvent.DamageDealt>()
            .firstOrNull { it.actorId == "hero" }
        assertNotNull("expected the hero to deal damage", damage)
        assertTrue(damage!!.amount > 0)
        val foe = result.state.battler("foe")!!
        assertTrue(foe.monster.currentHp < foe.monster.maxHp)
    }

    @Test
    fun `using a move consumes exactly one PP`() {
        val content = TestFixtures.content()
        val engine = BattleEngine(content, SeededRng(1))
        val started = engine.start(setup())
        val before = started.state.battler("hero")!!.monster.moves.first().currentPp

        val result = engine.executeTurn(started.state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))

        assertEquals(before - 1, result.state.battler("hero")!!.monster.moves.first().currentPp)
    }

    @Test
    fun `faster monsters act first`() {
        val fast = TestFixtures.battler(
            id = "fast",
            monster = TestFixtures.monster(
                uid = "fast",
                species = TestFixtures.species(id = "fast_sp", stats = StatBlock(80, 90, 70, 85, 65, 200, 40)),
            ),
        )
        val slow = TestFixtures.battler(
            id = "slow",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(
                uid = "slow",
                species = TestFixtures.species(id = "slow_sp", primary = Element.SPIRIT, stats = StatBlock(80, 90, 70, 85, 65, 10, 40)),
            ),
        )
        val engine = BattleEngine(TestFixtures.content(), SeededRng(3))
        val state = engine.start(TestFixtures.battleState(listOf(fast), listOf(slow))).state

        val ordered = engine.orderActions(
            state,
            listOf(
                BattleAction.UseMove("slow", 0, listOf("fast")),
                BattleAction.UseMove("fast", 0, listOf("slow")),
            ),
        )
        assertEquals("fast", ordered.first().actorId)
    }

    @Test
    fun `priority moves jump the queue`() {
        val quickMove = TestFixtures.move(id = "move_quick", priority = 2)
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), quickMove))
        val engine = BattleEngine(content, SeededRng(3))
        val fast = TestFixtures.battler(
            id = "fast",
            monster = TestFixtures.monster(
                uid = "fast",
                species = TestFixtures.species(id = "fast_sp", stats = StatBlock(80, 90, 70, 85, 65, 200, 40)),
            ),
        )
        val slow = TestFixtures.battler(
            id = "slow",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(
                uid = "slow",
                species = TestFixtures.species(id = "slow_sp", primary = Element.SPIRIT, stats = StatBlock(80, 90, 70, 85, 65, 10, 40)),
                moveIds = listOf("move_quick"),
            ),
        )
        val state = engine.start(TestFixtures.battleState(listOf(fast), listOf(slow))).state

        val ordered = engine.orderActions(
            state,
            listOf(
                BattleAction.UseMove("fast", 0, listOf("slow")),
                BattleAction.UseMove("slow", 0, listOf("fast")),
            ),
        )
        assertEquals("slow", ordered.first().actorId)
    }

    @Test
    fun `defeating the last enemy ends the battle in victory`() {
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(power = 250)))
        val engine = BattleEngine(content, SeededRng(11))
        var state = engine.start(setup(enemyLevel = 5)).state

        repeat(6) {
            if (!state.isOver) {
                state = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe")))).state
            }
        }
        assertEquals(BattleOutcome.VICTORY, state.outcome)
    }

    @Test
    fun `status moves inflict conditions and the engine ticks them`() {
        val poisonMove = TestFixtures.move(
            id = "move_venom",
            power = 0,
            category = MoveCategory.SUPPORT,
            effects = listOf(MoveEffect.InflictStatus(StatusCondition.POISON, chance = 1.0)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), poisonMove))
        val engine = BattleEngine(content, alwaysRng())
        val state = engine.start(setup(playerMoves = listOf("move_venom"))).state

        val result = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))

        assertEquals(StatusCondition.POISON, result.state.battler("foe")!!.monster.status)
        assertTrue(result.events.any { it is BattleEvent.StatusInflicted })
        assertTrue(result.events.any { it is BattleEvent.StatusTicked })
    }

    @Test
    fun `elements immune to a status shrug it off`() {
        val burnMove = TestFixtures.move(
            id = "move_scorch",
            power = 0,
            category = MoveCategory.SUPPORT,
            effects = listOf(MoveEffect.InflictStatus(StatusCondition.BURN, chance = 1.0)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), burnMove))
        val engine = BattleEngine(content, alwaysRng())
        // Fire monsters cannot be burned.
        val state = engine.start(
            setup(playerMoves = listOf("move_scorch"), enemyElement = Element.FIRE),
        ).state

        val result = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))

        assertNull(result.state.battler("foe")!!.monster.status)
        assertTrue(result.events.any { it is BattleEvent.StatusResisted })
    }

    @Test
    fun `sleeping monsters lose their turn`() {
        val content = TestFixtures.content()
        val engine = BattleEngine(content, neverRng())
        var state = engine.start(setup()).state
        val sleeping = state.battler("foe")!!.withMonster { it.withStatus(StatusCondition.SLEEP, 3) }
        state = state.withBattler(sleeping)

        val result = engine.executeTurn(
            state,
            listOf(BattleAction.UseMove("foe", 0, listOf("hero"))),
        )

        assertTrue(result.events.any { it is BattleEvent.ActionBlocked })
        assertEquals(
            "hero should be untouched",
            result.state.battler("hero")!!.monster.maxHp,
            result.state.battler("hero")!!.monster.currentHp,
        )
    }

    @Test
    fun `protect blocks the next incoming attack`() {
        val protectMove = TestFixtures.move(
            id = "move_ward",
            power = 0,
            category = MoveCategory.SUPPORT,
            target = MoveTarget.SELF,
            priority = 4,
            effects = listOf(MoveEffect.Protect(turns = 1)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), protectMove))
        val engine = BattleEngine(content, alwaysRng())
        val state = engine.start(setup(playerMoves = listOf("move_ward"))).state

        val result = engine.executeTurn(
            state,
            listOf(
                BattleAction.UseMove("hero", 0, listOf("hero")),
                BattleAction.UseMove("foe", 0, listOf("hero")),
            ),
        )

        assertTrue(result.events.any { it is BattleEvent.Protected })
        val hero = result.state.battler("hero")!!
        assertEquals(hero.monster.maxHp, hero.monster.currentHp)
    }

    @Test
    fun `shields absorb damage before hit points`() {
        val shieldMove = TestFixtures.move(
            id = "move_aegis",
            power = 0,
            category = MoveCategory.SUPPORT,
            target = MoveTarget.SELF,
            effects = listOf(MoveEffect.RaiseShield(fraction = 0.5, turns = 3)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(power = 20), shieldMove))
        val engine = BattleEngine(content, alwaysRng())
        var state = engine.start(setup(playerMoves = listOf("move_aegis"))).state

        state = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("hero")))).state
        val shielded = state.battler("hero")!!
        assertTrue(shielded.shieldHp > 0)

        val result = engine.executeTurn(state, listOf(BattleAction.UseMove("foe", 0, listOf("hero"))))
        val hero = result.state.battler("hero")!!
        // The shield soaked the hit; HP is untouched.
        assertEquals(hero.monster.maxHp, hero.monster.currentHp)
        assertTrue(hero.shieldHp < shielded.shieldHp)
    }

    @Test
    fun `stat buffs raise the resolved stat`() {
        val buffMove = TestFixtures.move(
            id = "move_fury",
            power = 0,
            category = MoveCategory.SUPPORT,
            target = MoveTarget.SELF,
            effects = listOf(MoveEffect.ModifyStat(Stat.ATTACK, stages = 2, onSelf = true)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), buffMove))
        val engine = BattleEngine(content, alwaysRng())
        val state = engine.start(setup(playerMoves = listOf("move_fury"))).state
        val before = state.battler("hero")!!.effectiveStat(Stat.ATTACK)

        val result = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("hero"))))

        val after = result.state.battler("hero")!!.effectiveStat(Stat.ATTACK)
        assertEquals(2, result.state.battler("hero")!!.stages[Stat.ATTACK])
        assertTrue("before=$before after=$after", after > before)
    }

    @Test
    fun `weather moves change the field and expire`() {
        val rainMove = TestFixtures.move(
            id = "move_downpour",
            power = 0,
            category = MoveCategory.SUPPORT,
            target = MoveTarget.FIELD,
            effects = listOf(MoveEffect.SetWeather(BattleWeather.RAIN, turns = 2)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), rainMove))
        val engine = BattleEngine(content, neverRng())
        var state = engine.start(setup(playerMoves = listOf("move_downpour"))).state

        state = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("hero")))).state
        assertEquals(BattleWeather.RAIN, state.weather)

        // One more turn and the rain runs out.
        val result = engine.executeTurn(state, emptyList())
        assertEquals(BattleWeather.CLEAR, result.state.weather)
        assertTrue(result.events.any { it is BattleEvent.WeatherEnded })
    }

    @Test
    fun `snow chips non-immune monsters at the end of the turn`() {
        val content = TestFixtures.content()
        val engine = BattleEngine(content, neverRng())
        var state = engine.start(setup()).state
        state = state.copy(weather = BattleWeather.SNOW, weatherTurns = 5)

        val result = engine.executeTurn(state, emptyList())

        assertTrue(result.events.any { it is BattleEvent.WeatherTicked })
        assertTrue(result.state.battler("hero")!!.monster.currentHp < result.state.battler("hero")!!.monster.maxHp)
    }

    @Test
    fun `multi hit moves strike several times`() {
        val flurry = TestFixtures.move(
            id = "move_flurry",
            power = 15,
            effects = listOf(MoveEffect.MultiHit(min = 3, max = 3)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), flurry))
        val engine = BattleEngine(content, alwaysRng())
        val state = engine.start(setup(playerMoves = listOf("move_flurry"))).state

        val result = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))

        val hits = result.events.filterIsInstance<BattleEvent.DamageDealt>().filter { it.actorId == "hero" }
        assertEquals(3, hits.size)
        assertEquals(3, hits.first().hitCount)
    }

    @Test
    fun `drain moves heal the attacker`() {
        val drain = TestFixtures.move(
            id = "move_siphon",
            power = 60,
            effects = listOf(MoveEffect.Drain(fraction = 0.5)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), drain))
        val engine = BattleEngine(content, alwaysRng())
        var state = engine.start(setup(playerMoves = listOf("move_siphon"))).state
        val hurt = state.battler("hero")!!.withMonster { it.withDamage(it.maxHp / 2) }
        state = state.withBattler(hurt)
        val before = state.battler("hero")!!.monster.currentHp

        val result = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))

        assertTrue(result.state.battler("hero")!!.monster.currentHp > before)
        assertTrue(result.events.any { it is BattleEvent.Healed })
    }

    @Test
    fun `recoil moves hurt the attacker`() {
        val reckless = TestFixtures.move(
            id = "move_reckless",
            power = 120,
            effects = listOf(MoveEffect.Recoil(fraction = 0.33)),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), reckless))
        val engine = BattleEngine(content, alwaysRng())
        val state = engine.start(setup(playerMoves = listOf("move_reckless"))).state

        val result = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))

        val hero = result.state.battler("hero")!!
        assertTrue(hero.monster.currentHp < hero.monster.maxHp)
    }

    @Test
    fun `charge moves take a turn to wind up`() {
        val charged = TestFixtures.move(
            id = "move_skyfall",
            power = 150,
            effects = listOf(MoveEffect.Charge(messageKey = "msg_charging")),
        )
        val content = TestFixtures.content(moves = listOf(TestFixtures.move(), charged))
        val engine = BattleEngine(content, alwaysRng())
        var state = engine.start(setup(playerMoves = listOf("move_skyfall"))).state

        val first = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))
        assertTrue(first.events.any { it is BattleEvent.Charging })
        assertEquals(
            "no damage on the charging turn",
            0,
            first.events.filterIsInstance<BattleEvent.DamageDealt>().count { it.actorId == "hero" },
        )

        state = first.state
        val second = engine.executeTurn(state, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))
        assertTrue(second.events.filterIsInstance<BattleEvent.DamageDealt>().any { it.actorId == "hero" })
    }

    @Test
    fun `abilities can halve incoming elemental damage`() {
        val ability = TestFixtures.ability(id = "ab_ember_hide", effectId = "ember_hide")
        val content = TestFixtures.content(
            moves = listOf(TestFixtures.move(element = Element.FIRE, power = 80)),
            abilities = listOf(ability),
        )
        val plainEngine = BattleEngine(TestFixtures.content(moves = listOf(TestFixtures.move(element = Element.FIRE, power = 80))), ScriptedRng(listOf(0.99)))
        val warded = BattleEngine(content, ScriptedRng(listOf(0.99)))

        val plainState = plainEngine.start(setup()).state
        val plainResult = plainEngine.executeTurn(plainState, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))
        val plainDamage = plainResult.events.filterIsInstance<BattleEvent.DamageDealt>().first().amount

        var wardedState = warded.start(setup()).state
        val protectedFoe = wardedState.battler("foe")!!
            .withMonster { it.copy(abilityId = ability.id) }
            .copy(abilityEffectId = "ember_hide")
        wardedState = wardedState.withBattler(protectedFoe)
        val wardedResult = warded.executeTurn(wardedState, listOf(BattleAction.UseMove("hero", 0, listOf("foe"))))
        val wardedDamage = wardedResult.events.filterIsInstance<BattleEvent.DamageDealt>().first().amount

        assertTrue("plain=$plainDamage warded=$wardedDamage", wardedDamage < plainDamage)
    }

    @Test
    fun `combination strikes fire when two allies share a tag`() {
        val comboMove = TestFixtures.move(id = "move_combo", comboTag = "runic_chain")
        val content = TestFixtures.content(moves = listOf(comboMove))
        val engine = BattleEngine(content, SeededRng(5))
        val a = TestFixtures.battler(
            id = "a",
            monster = TestFixtures.monster(uid = "a", moveIds = listOf("move_combo")),
        )
        val b = TestFixtures.battler(
            id = "b",
            monster = TestFixtures.monster(uid = "b", moveIds = listOf("move_combo")),
        ).copy(slot = 1)
        val foe = TestFixtures.battler(
            id = "foe",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(
                uid = "foe",
                species = TestFixtures.species(id = "foe_sp", primary = Element.SPIRIT),
                moveIds = listOf("move_combo"),
            ),
        )
        val state = TestFixtures.battleState(listOf(a, b), listOf(foe))
            .copy(activePlayerSlots = listOf(0, 1))
        val started = engine.start(state).state

        val result = engine.executeTurn(
            started,
            listOf(
                BattleAction.UseMove("a", 0, listOf("foe")),
                BattleAction.UseMove("b", 0, listOf("foe")),
            ),
        )

        assertTrue(result.events.any { it is BattleEvent.ComboTriggered })
    }

    @Test
    fun `throwing an orb can capture a wild monster`() {
        val orb = TestFixtures.orb(id = "orb_gold", multiplier = 8.0)
        val content = TestFixtures.content(items = listOf(orb))
        val engine = BattleEngine(content, ScriptedRng(doubles = listOf(0.0)))
        var state = engine.start(setup(enemyLevel = 3)).state
        val weakened = state.battler("foe")!!.withMonster {
            it.copy(currentHp = 1, status = StatusCondition.SLEEP)
        }
        state = state.withBattler(weakened)

        val result = engine.executeTurn(state, listOf(BattleAction.ThrowOrb("hero", orb.id, "foe")))

        assertEquals(BattleOutcome.CAPTURED, result.state.outcome)
        assertTrue(result.events.any { it is BattleEvent.CaptureSucceeded })
    }

    @Test
    fun `capture is refused in trainer battles`() {
        val orb = TestFixtures.orb()
        val content = TestFixtures.content(items = listOf(orb))
        val engine = BattleEngine(content, alwaysRng())
        val state = engine.start(setup(type = BattleType.TRAINER)).state

        val result = engine.executeTurn(state, listOf(BattleAction.ThrowOrb("hero", orb.id, "foe")))

        assertFalse(result.state.isOver)
        assertTrue(
            result.events.any { it is BattleEvent.Message && it.messageKey == "msg_capture_forbidden" },
        )
    }

    @Test
    fun `fleeing is impossible in boss battles`() {
        val engine = BattleEngine(TestFixtures.content(), alwaysRng())
        val state = engine.start(setup(type = BattleType.BOSS)).state

        val result = engine.executeTurn(state, listOf(BattleAction.Flee("hero")))

        assertFalse(result.state.isOver)
        assertTrue(result.events.any { it is BattleEvent.Message && it.messageKey == "msg_cannot_flee" })
    }

    @Test
    fun `switching brings in a bench monster and resets its stages`() {
        val engine = BattleEngine(TestFixtures.content(), SeededRng(9))
        val active = TestFixtures.battler(id = "active", monster = TestFixtures.monster(uid = "active"))
        val bench = TestFixtures.battler(id = "bench", monster = TestFixtures.monster(uid = "bench")).copy(slot = 1)
        val foe = TestFixtures.battler(
            id = "foe",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(uid = "foe", species = TestFixtures.species(id = "foe_sp", primary = Element.SPIRIT)),
        )
        val state = engine.start(TestFixtures.battleState(listOf(active, bench), listOf(foe))).state

        val result = engine.executeTurn(state, listOf(BattleAction.Switch("active", 1)))

        assertEquals(listOf(1), result.state.activePlayerSlots)
        assertTrue(result.events.any { it is BattleEvent.SwitchedIn })
    }

    @Test
    fun `the same seed always produces the same battle`() {
        val content = TestFixtures.content()
        fun run(): List<String> {
            val engine = BattleEngine(content, SeededRng(20250801))
            var state = engine.start(setup()).state
            val log = mutableListOf<String>()
            repeat(5) {
                if (state.isOver) return@repeat
                val step = engine.executeTurn(
                    state,
                    listOf(
                        BattleAction.UseMove("hero", 0, listOf("foe")),
                        BattleAction.UseMove("foe", 0, listOf("hero")),
                    ),
                )
                state = step.state
                log += step.events.map { it.toString() }
            }
            return log
        }
        assertEquals(run(), run())
    }

    @Test
    fun `victory spoils grant experience to every participant`() {
        val engine = BattleEngine(TestFixtures.content(), SeededRng(2))
        var state = engine.start(setup()).state
        val fainted = state.battler("foe")!!.withMonster { it.copy(currentHp = 0) }
        state = state.withBattler(fainted)

        val events = engine.awardVictorySpoils(state, setOf("hero"))

        val xp = events.filterIsInstance<BattleEvent.ExperienceGained>()
        assertEquals(1, xp.size)
        assertTrue(xp.first().amount > 0)
    }

    @Test
    fun `a monster with no PP left can still struggle`() {
        val engine = BattleEngine(TestFixtures.content(), alwaysRng())
        var state = engine.start(setup()).state
        val empty = state.battler("hero")!!.withMonster { monster ->
            monster.copy(moves = monster.moves.map { it.copy(currentPp = 0) })
        }
        state = state.withBattler(empty)

        val result = engine.executeTurn(state, listOf(BattleAction.Struggle("hero")))

        val damage = result.events.filterIsInstance<BattleEvent.DamageDealt>()
        assertTrue(damage.any { it.targetId == "foe" })
        assertTrue("struggle should hurt its user", damage.any { it.targetId == "hero" })
    }
}
