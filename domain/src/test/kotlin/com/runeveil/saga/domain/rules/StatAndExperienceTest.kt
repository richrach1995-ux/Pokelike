package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.TestFixtures
import com.runeveil.saga.domain.model.monster.GrowthRate
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.Temperament
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatCalculatorTest {

    @Test
    fun `level one monsters are weak but never zero`() {
        val stats = StatCalculator.resolve(
            base = StatBlock.uniform(50),
            genes = StatBlock.ZERO,
            training = StatBlock.ZERO,
            level = 1,
            temperament = Temperament.EVEN,
        )
        for (stat in Stat.entries) {
            assertTrue("${stat.name} was ${stats[stat]}", stats[stat] >= 1)
        }
        // HP = floor((50*2+0+0) * 1/100) + 1 + 12 = 1 + 13 = 14
        assertEquals(14, stats.hp)
    }

    @Test
    fun `stat formula matches the documented curve at level 100`() {
        val stats = StatCalculator.resolve(
            base = StatBlock.uniform(100),
            genes = StatBlock.uniform(31),
            training = StatBlock.uniform(252),
            level = 100,
            temperament = Temperament.EVEN,
        )
        // core = (100*2 + 31 + 252/4) * 100/100 = 294
        // HP    = 294 + 100 + 12 = 406
        // other = 294 + 6 = 300
        assertEquals(406, stats.hp)
        assertEquals(300, stats.attack)
    }

    @Test
    fun `temperament raises one stat and lowers another by ten percent`() {
        val base = StatBlock.uniform(100)
        val neutral = StatCalculator.resolve(base, StatBlock.ZERO, StatBlock.ZERO, 100, Temperament.EVEN)
        val bold = StatCalculator.resolve(base, StatBlock.ZERO, StatBlock.ZERO, 100, Temperament.BOLD)

        assertEquals((neutral.attack * 1.1).toInt(), bold.attack)
        assertEquals((neutral.magic * 0.9).toInt(), bold.magic)
        // HP is never affected by temperament.
        assertEquals(neutral.hp, bold.hp)
    }

    @Test
    fun `training respects the per-stat and total caps`() {
        // 7 × 60 = 420, still below the 510 total cap.
        val start = StatBlock.uniform(60)
        val result = StatCalculator.applyTraining(start, StatBlock.uniform(200))
        assertTrue("total ${result.total} exceeded cap", result.total <= StatBlock.MAX_TRAINING_TOTAL)
        assertTrue("nothing was trained", result.total > start.total)
        for (stat in Stat.entries) {
            assertTrue(result[stat] <= StatBlock.MAX_TRAINING_PER_STAT)
        }
    }

    @Test
    fun `training yield targets the two highest base stats`() {
        val defeated = StatBlock(hp = 40, attack = 120, defense = 50, magic = 110, resistance = 45, speed = 30, luck = 20)
        val yield = StatCalculator.trainingYield(defeated, level = 50)
        assertTrue(yield.attack > 0)
        assertTrue(yield.magic > 0)
        assertEquals(0, yield.speed)
    }

    @Test
    fun `genes and training raise the resolved stats`() {
        val poor = TestFixtures.monster(genes = StatBlock.ZERO)
        val perfect = TestFixtures.monster(genes = StatBlock.uniform(31), training = StatBlock.uniform(252))
        assertTrue(perfect.stats.attack > poor.stats.attack)
        assertTrue(perfect.maxHp > poor.maxHp)
    }
}

class ExperienceCurveTest {

    @Test
    fun `total experience increases monotonically`() {
        for (rate in GrowthRate.entries) {
            var previous = -1L
            for (level in 1..ExperienceCurve.MAX_LEVEL) {
                val total = ExperienceCurve.totalAt(rate, level)
                assertTrue("$rate level $level not increasing", total > previous)
                previous = total
            }
        }
    }

    @Test
    fun `levelFor is the inverse of totalAt`() {
        for (rate in GrowthRate.entries) {
            for (level in 1..ExperienceCurve.MAX_LEVEL) {
                val total = ExperienceCurve.totalAt(rate, level)
                assertEquals("$rate level $level", level, ExperienceCurve.levelFor(rate, total))
            }
        }
    }

    @Test
    fun `glacial monsters need more experience than swift ones`() {
        val swift = ExperienceCurve.totalAt(GrowthRate.SWIFT, 100)
        val glacial = ExperienceCurve.totalAt(GrowthRate.GLACIAL, 100)
        assertTrue(glacial > swift * 1.5)
    }

    @Test
    fun `progress within a level stays inside zero to one`() {
        val rate = GrowthRate.STEADY
        val floorXp = ExperienceCurve.totalAt(rate, 30)
        val step = ExperienceCurve.stepFrom(rate, 30)
        assertEquals(0f, ExperienceCurve.progressWithinLevel(rate, 30, floorXp), 0.0001f)
        assertEquals(0.5f, ExperienceCurve.progressWithinLevel(rate, 30, floorXp + step / 2), 0.02f)
        assertEquals(1f, ExperienceCurve.progressWithinLevel(rate, 100, Long.MAX_VALUE / 2), 0.0001f)
    }

    @Test
    fun `defeating a much weaker enemy yields little experience`() {
        val strong = ExperienceCurve.rewardFor(
            baseExperience = 100, enemyLevel = 50, rarityMultiplier = 1.0,
            participants = 1, winnerLevel = 50, isTrainerBattle = false, friendshipBonus = false,
        )
        val weak = ExperienceCurve.rewardFor(
            baseExperience = 100, enemyLevel = 5, rarityMultiplier = 1.0,
            participants = 1, winnerLevel = 50, isTrainerBattle = false, friendshipBonus = false,
        )
        assertTrue("strong=$strong weak=$weak", weak < strong / 10)
    }

    @Test
    fun `trainer battles and friendship increase the reward`() {
        val plain = ExperienceCurve.rewardFor(64, 30, 1.0, 1, 30, isTrainerBattle = false, friendshipBonus = false)
        val bonus = ExperienceCurve.rewardFor(64, 30, 1.0, 1, 30, isTrainerBattle = true, friendshipBonus = true)
        assertTrue(bonus > plain)
    }

    @Test
    fun `experience is split between participants`() {
        val solo = ExperienceCurve.rewardFor(64, 30, 1.0, 1, 30, false, false)
        val shared = ExperienceCurve.rewardFor(64, 30, 1.0, 3, 30, false, false)
        assertTrue(shared < solo)
    }
}
