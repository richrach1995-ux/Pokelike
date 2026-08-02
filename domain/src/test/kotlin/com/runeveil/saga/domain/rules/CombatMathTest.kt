package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.TestFixtures
import com.runeveil.saga.domain.model.battle.BattleSide
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.item.OrbCondition
import com.runeveil.saga.domain.model.item.OrbGrade
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Rarity
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.util.ScriptedRng
import com.runeveil.saga.domain.util.SeededRng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DamageCalculatorTest {

    private val neutralRng = ScriptedRng(doubles = listOf(0.99, 1.0))

    @Test
    fun `super effective hits deal roughly double damage`() {
        val attacker = TestFixtures.battler(
            id = "a",
            monster = TestFixtures.monster(species = TestFixtures.species(primary = Element.FIRE)),
        )
        val natureTarget = TestFixtures.battler(
            id = "d1",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(uid = "m2", species = TestFixtures.species(id = "leaf", primary = Element.NATURE)),
        )
        val neutralTarget = TestFixtures.battler(
            id = "d2",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(uid = "m3", species = TestFixtures.species(id = "spirit", primary = Element.SPIRIT)),
        )
        val move = TestFixtures.move(element = Element.FIRE)

        val superHit = DamageCalculator.compute(
            attacker, natureTarget, move, BattleWeather.CLEAR, DayPhase.DAY,
            ScriptedRng(doubles = listOf(0.99, 0.99)),
        )
        val neutralHit = DamageCalculator.compute(
            attacker, neutralTarget, move, BattleWeather.CLEAR, DayPhase.DAY,
            ScriptedRng(doubles = listOf(0.99, 0.99)),
        )
        assertEquals(2.0, superHit.effectivenessMultiplier, 0.0001)
        assertTrue(superHit.damage > neutralHit.damage * 1.8)
    }

    @Test
    fun `immune defenders take no damage at all`() {
        val attacker = TestFixtures.battler(
            monster = TestFixtures.monster(species = TestFixtures.species(primary = Element.THUNDER)),
        )
        val earth = TestFixtures.battler(
            id = "e",
            side = BattleSide.ENEMY,
            monster = TestFixtures.monster(uid = "m2", species = TestFixtures.species(id = "stone", primary = Element.EARTH)),
        )
        val result = DamageCalculator.compute(
            attacker, earth, TestFixtures.move(element = Element.THUNDER),
            BattleWeather.CLEAR, DayPhase.DAY, neutralRng,
        )
        assertTrue(result.isImmune)
        assertEquals(0, result.damage)
    }

    @Test
    fun `matching element grants the affinity bonus`() {
        val fireMonster = TestFixtures.battler(
            monster = TestFixtures.monster(species = TestFixtures.species(primary = Element.FIRE)),
        )
        assertEquals(
            DamageCalculator.AFFINITY_BONUS,
            DamageCalculator.affinityBonus(fireMonster, Element.FIRE),
            0.0001,
        )
        assertEquals(1.0, DamageCalculator.affinityBonus(fireMonster, Element.WATER), 0.0001)
    }

    @Test
    fun `a bound matching rune raises the affinity bonus further`() {
        val runed = TestFixtures.battler(
            monster = TestFixtures.monster(
                species = TestFixtures.species(primary = Element.FIRE),
            ).copy(boundRuneIds = listOf("rune_greater_fire")),
        )
        assertEquals(
            DamageCalculator.RUNE_AFFINITY_BONUS,
            DamageCalculator.affinityBonus(runed, Element.FIRE),
            0.0001,
        )
    }

    @Test
    fun `critical hits multiply damage`() {
        val attacker = TestFixtures.battler()
        val defender = TestFixtures.battler(
            id = "d", side = BattleSide.ENEMY,
            monster = TestFixtures.monster(uid = "m2", species = TestFixtures.species(id = "target", primary = Element.SPIRIT)),
        )
        val move = TestFixtures.move()
        val normal = DamageCalculator.compute(
            attacker, defender, move, BattleWeather.CLEAR, DayPhase.DAY,
            ScriptedRng(doubles = listOf(0.99, 0.99)), forceCritical = false,
        )
        val crit = DamageCalculator.compute(
            attacker, defender, move, BattleWeather.CLEAR, DayPhase.DAY,
            ScriptedRng(doubles = listOf(0.99, 0.99)), forceCritical = true,
        )
        assertTrue(crit.critical)
        assertTrue(crit.damage > normal.damage)
    }

    @Test
    fun `weather boosts and dampens the right elements`() {
        val attacker = TestFixtures.battler(
            monster = TestFixtures.monster(species = TestFixtures.species(primary = Element.SPIRIT)),
        )
        val defender = TestFixtures.battler(
            id = "d", side = BattleSide.ENEMY,
            monster = TestFixtures.monster(uid = "m2", species = TestFixtures.species(id = "t", primary = Element.SPIRIT)),
        )
        val fireMove = TestFixtures.move(element = Element.FIRE)
        val dry = DamageCalculator.compute(
            attacker, defender, fireMove, BattleWeather.CLEAR, DayPhase.DAY, ScriptedRng(listOf(0.99, 0.99)),
        )
        val rainy = DamageCalculator.compute(
            attacker, defender, fireMove, BattleWeather.RAIN, DayPhase.DAY, ScriptedRng(listOf(0.99, 0.99)),
        )
        assertTrue("rain=${rainy.damage} dry=${dry.damage}", rainy.damage < dry.damage)
    }

    @Test
    fun `shadow moves hit harder at night and light moves by day`() {
        assertTrue(
            DamageCalculator.phaseBonus(Element.SHADOW, DayPhase.NIGHT) >
                DamageCalculator.phaseBonus(Element.SHADOW, DayPhase.DAY),
        )
        assertTrue(
            DamageCalculator.phaseBonus(Element.LIGHT, DayPhase.DAY) >
                DamageCalculator.phaseBonus(Element.LIGHT, DayPhase.NIGHT),
        )
        assertEquals(1.0, DamageCalculator.phaseBonus(Element.EARTH, DayPhase.NIGHT), 0.0001)
    }

    @Test
    fun `damage is never zero against a non-immune target`() {
        val weak = TestFixtures.battler(
            monster = TestFixtures.monster(
                level = 1,
                species = TestFixtures.species(stats = com.runeveil.saga.domain.model.monster.StatBlock.uniform(5)),
            ),
        )
        val tank = TestFixtures.battler(
            id = "d", side = BattleSide.ENEMY,
            monster = TestFixtures.monster(
                uid = "m2", level = 100,
                species = TestFixtures.species(id = "tank", primary = Element.SPIRIT, stats = com.runeveil.saga.domain.model.monster.StatBlock.uniform(190)),
            ),
        )
        val result = DamageCalculator.compute(
            weak, tank, TestFixtures.move(power = 10), BattleWeather.CLEAR, DayPhase.DAY, neutralRng,
        )
        assertTrue(result.damage >= 1)
    }

    @Test
    fun `expected damage sits inside the random damage range`() {
        val attacker = TestFixtures.battler()
        val defender = TestFixtures.battler(
            id = "d", side = BattleSide.ENEMY,
            monster = TestFixtures.monster(uid = "m2", species = TestFixtures.species(id = "t", primary = Element.SPIRIT)),
        )
        val move = TestFixtures.move()
        val expected = DamageCalculator.expectedDamage(attacker, defender, move)
        val rng = SeededRng(42)
        val rolls = (1..200).map {
            DamageCalculator.compute(attacker, defender, move, BattleWeather.CLEAR, DayPhase.DAY, rng).damage
        }
        val nonCritical = rolls.filter { it <= expected * 1.6 }
        assertTrue(nonCritical.min() <= expected)
        assertTrue(nonCritical.max() >= expected)
    }
}

class AccuracyCalculatorTest {

    @Test
    fun `always-hit moves ignore evasion entirely`() {
        val attacker = TestFixtures.battler()
        val evasive = TestFixtures.battler(id = "d", side = BattleSide.ENEMY)
            .let { it.copy(stages = it.stages.applyEvasion(6)) }
        val chance = AccuracyCalculator.hitChance(
            attacker, evasive, TestFixtures.move(alwaysHits = true), BattleWeather.CLEAR,
        )
        assertEquals(1.0, chance, 0.0001)
    }

    @Test
    fun `hit chance never drops below the floor`() {
        val attacker = TestFixtures.battler()
            .let { it.copy(stages = it.stages.applyAccuracy(-6)) }
        val evasive = TestFixtures.battler(id = "d", side = BattleSide.ENEMY)
            .let { it.copy(stages = it.stages.applyEvasion(6)) }
        val chance = AccuracyCalculator.hitChance(
            attacker, evasive, TestFixtures.move(accuracy = 50), BattleWeather.FOG,
        )
        assertTrue(chance >= AccuracyCalculator.MIN_HIT_CHANCE)
    }

    @Test
    fun `fog lowers accuracy compared to clear skies`() {
        val attacker = TestFixtures.battler()
        val defender = TestFixtures.battler(id = "d", side = BattleSide.ENEMY)
        val move = TestFixtures.move(accuracy = 90)
        assertTrue(
            AccuracyCalculator.hitChance(attacker, defender, move, BattleWeather.FOG) <
                AccuracyCalculator.hitChance(attacker, defender, move, BattleWeather.CLEAR),
        )
    }

    @Test
    fun `priority dominates speed in the initiative score`() {
        val slow = TestFixtures.battler(
            monster = TestFixtures.monster(species = TestFixtures.species(stats = com.runeveil.saga.domain.model.monster.StatBlock.uniform(20))),
        )
        val fast = TestFixtures.battler(
            id = "f",
            monster = TestFixtures.monster(uid = "m2", species = TestFixtures.species(id = "fast", stats = com.runeveil.saga.domain.model.monster.StatBlock.uniform(180))),
        )
        val slowWithPriority = AccuracyCalculator.initiativeScore(slow, priority = 1, BattleWeather.CLEAR)
        val fastNoPriority = AccuracyCalculator.initiativeScore(fast, priority = 0, BattleWeather.CLEAR)
        assertTrue(slowWithPriority > fastNoPriority)
    }

    @Test
    fun `flee chance grows with repeated attempts`() {
        val runner = TestFixtures.battler()
        val chaser = TestFixtures.battler(id = "c", side = BattleSide.ENEMY)
        val first = AccuracyCalculator.fleeChance(runner, chaser, attempts = 0)
        val third = AccuracyCalculator.fleeChance(runner, chaser, attempts = 2)
        assertTrue(third > first)
    }
}

class CaptureCalculatorTest {

    private fun target(
        hpFraction: Float = 1f,
        status: StatusCondition? = null,
        level: Int = 20,
        rarity: Rarity = Rarity.COMMON,
        catchRate: Int = 120,
    ) = TestFixtures.battler(
        id = "wild",
        side = BattleSide.ENEMY,
        monster = TestFixtures.monster(
            uid = "wild",
            level = level,
            species = TestFixtures.species(rarity = rarity, catchRate = catchRate),
        ).let { monster ->
            monster.copy(
                currentHp = (monster.maxHp * hpFraction).toInt().coerceAtLeast(1),
                status = status,
            )
        },
    )

    private fun chanceOf(battler: com.runeveil.saga.domain.model.battle.Battler, orb: com.runeveil.saga.domain.model.item.Item) =
        CaptureCalculator.captureChance(
            target = battler,
            orbItem = orb,
            dayPhase = DayPhase.DAY,
            weather = BattleWeather.CLEAR,
            turnCount = 3,
            currentWorld = null,
            playerCaptureBonusPercent = 0,
        )

    @Test
    fun `hurt monsters are easier to catch`() {
        val orb = TestFixtures.orb()
        assertTrue(chanceOf(target(hpFraction = 0.05f), orb) > chanceOf(target(hpFraction = 1f), orb))
    }

    @Test
    fun `status conditions improve the capture chance`() {
        val orb = TestFixtures.orb()
        val plain = chanceOf(target(hpFraction = 0.4f), orb)
        val asleep = chanceOf(target(hpFraction = 0.4f, status = StatusCondition.SLEEP), orb)
        val burned = chanceOf(target(hpFraction = 0.4f, status = StatusCondition.BURN), orb)
        assertTrue(asleep > burned)
        assertTrue(burned > plain)
    }

    @Test
    fun `better orbs beat worse ones`() {
        val wood = TestFixtures.orb(id = "orb_wood", grade = OrbGrade.WOOD, multiplier = 1.0)
        val gold = TestFixtures.orb(id = "orb_gold", grade = OrbGrade.GOLD, multiplier = 3.0)
        assertTrue(chanceOf(target(hpFraction = 0.5f), gold) > chanceOf(target(hpFraction = 0.5f), wood))
    }

    @Test
    fun `conditional orbs only pay out when their condition holds`() {
        val nightOrb = TestFixtures.orb(
            id = "orb_night", grade = OrbGrade.SILVER, multiplier = 1.0,
            condition = OrbCondition.AT_NIGHT, conditionalBonus = 3.0,
        )
        val byDay = CaptureCalculator.captureChance(
            target(hpFraction = 0.5f), nightOrb, DayPhase.DAY, BattleWeather.CLEAR, 2, null, 0,
        )
        val byNight = CaptureCalculator.captureChance(
            target(hpFraction = 0.5f), nightOrb, DayPhase.NIGHT, BattleWeather.CLEAR, 2, null, 0,
        )
        assertTrue(byNight > byDay)
    }

    @Test
    fun `legendary rarity resists capture`() {
        val orb = TestFixtures.orb()
        val common = chanceOf(target(hpFraction = 0.3f, rarity = Rarity.COMMON), orb)
        val legendary = chanceOf(target(hpFraction = 0.3f, rarity = Rarity.LEGENDARY), orb)
        assertTrue(legendary < common)
    }

    @Test
    fun `four successful shakes mean a capture`() {
        val alwaysSucceeds = ScriptedRng(doubles = listOf(0.0))
        val result = CaptureCalculator.resolve(
            target = target(hpFraction = 0.1f, status = StatusCondition.SLEEP),
            orbItem = TestFixtures.orb(id = "orb_gold", grade = OrbGrade.GOLD, multiplier = 4.0),
            dayPhase = DayPhase.NIGHT,
            weather = BattleWeather.CLEAR,
            turnCount = 3,
            currentWorld = null,
            playerCaptureBonusPercent = 0,
            rng = alwaysSucceeds,
        )
        assertTrue(result.captured)
        assertEquals(CaptureCalculator.REQUIRED_SHAKES, result.shakes)
    }

    @Test
    fun `a failed throw reports how close it was`() {
        val alwaysFails = ScriptedRng(doubles = listOf(0.9999))
        val result = CaptureCalculator.resolve(
            target = target(hpFraction = 1f, rarity = Rarity.LEGENDARY, catchRate = 3),
            orbItem = TestFixtures.orb(id = "orb_wood", grade = OrbGrade.WOOD, multiplier = 1.0),
            dayPhase = DayPhase.DAY,
            weather = BattleWeather.CLEAR,
            turnCount = 1,
            currentWorld = null,
            playerCaptureBonusPercent = 0,
            rng = alwaysFails,
        )
        assertFalse(result.captured)
        assertTrue(result.shakes < CaptureCalculator.REQUIRED_SHAKES)
    }

    @Test
    fun `capture chance is always a valid probability`() {
        val orb = TestFixtures.orb(id = "orb_legendary", grade = OrbGrade.LEGENDARY, multiplier = 10.0)
        val chance = chanceOf(target(hpFraction = 0.01f, status = StatusCondition.SLEEP, catchRate = 255), orb)
        assertTrue(chance in 0.0..1.0)
    }
}

class StatStagesTest {

    @Test
    fun `stages are clamped to plus minus six`() {
        var stages = com.runeveil.saga.domain.model.monster.StatStages()
        repeat(10) { stages = stages.apply(Stat.ATTACK, 1).first }
        assertEquals(6, stages[Stat.ATTACK])
        val (capped, applied) = stages.apply(Stat.ATTACK, 1)
        assertEquals(0, applied)
        assertEquals(6, capped[Stat.ATTACK])
    }

    @Test
    fun `stage multipliers follow the documented curve`() {
        assertEquals(1.0, com.runeveil.saga.domain.model.monster.StatStages.stageMultiplier(0), 0.0001)
        assertEquals(1.5, com.runeveil.saga.domain.model.monster.StatStages.stageMultiplier(1), 0.0001)
        assertEquals(4.0, com.runeveil.saga.domain.model.monster.StatStages.stageMultiplier(6), 0.0001)
        assertEquals(0.25, com.runeveil.saga.domain.model.monster.StatStages.stageMultiplier(-6), 0.0001)
    }

    @Test
    fun `clearing negative stages keeps the positive ones`() {
        var stages = com.runeveil.saga.domain.model.monster.StatStages()
        stages = stages.apply(Stat.ATTACK, 2).first
        stages = stages.apply(Stat.DEFENSE, -3).first
        val cleaned = stages.clearNegative()
        assertEquals(2, cleaned[Stat.ATTACK])
        assertEquals(0, cleaned[Stat.DEFENSE])
    }
}
