package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.TestFixtures
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Rarity
import com.runeveil.saga.domain.model.monster.SizeClass
import com.runeveil.saga.domain.model.sprite.Archetype
import com.runeveil.saga.domain.model.sprite.MarkingStyle
import com.runeveil.saga.domain.model.sprite.SpriteBlueprints
import com.runeveil.saga.domain.model.sprite.WingShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sprite generator has one property the whole art direction rests on: the
 * same species must always produce the same creature. These tests pin that,
 * plus the readability rules (element drives palette, family drives silhouette,
 * stage drives bulk) that make the roster legible.
 */
class SpriteBlueprintTest {

    @Test
    fun `same species always yields an identical blueprint`() {
        val species = TestFixtures.species(id = "glutwelp")

        assertEquals(SpriteBlueprints.of(species), SpriteBlueprints.of(species))
    }

    @Test
    fun `different species yield different blueprints`() {
        val a = SpriteBlueprints.of(TestFixtures.species(id = "glutwelp"))
        val b = SpriteBlueprints.of(TestFixtures.species(id = "frostmoere"))

        assertNotEquals(a, b)
    }

    @Test
    fun `a family shares its body plan across all stages`() {
        val base = TestFixtures.species(id = "line_a")
        val plans = listOf(base, base.copy(id = "line_b", stage = 2), base.copy(id = "line_c", stage = 3))
            .map { SpriteBlueprints.of(it) }

        // familyId is shared, so the silhouette decisions must agree …
        assertEquals(1, plans.map { it.archetype }.distinct().size)
        assertEquals(1, plans.map { it.tail }.distinct().size)
        assertEquals(1, plans.map { it.crest }.distinct().size)

        // … while the later stages are visibly bulkier.
        assertTrue(plans[2].bodyWidth > plans[0].bodyWidth)
        assertTrue(plans[2].crestSize > plans[0].crestSize)
    }

    @Test
    fun `shiny rotates the palette but keeps the creature`() {
        val species = TestFixtures.species(id = "glutwelp")
        val normal = SpriteBlueprints.of(species, shiny = false)
        val shiny = SpriteBlueprints.of(species, shiny = true)

        assertNotEquals(normal.palette.body, shiny.palette.body)
        assertEquals(normal.archetype, shiny.archetype)
        assertEquals(normal.bodyWidth, shiny.bodyWidth, 0f)
    }

    @Test
    fun `element decides the body plan`() {
        val water = SpriteBlueprints.of(TestFixtures.species(id = "s1", primary = Element.WATER))
        val earth = SpriteBlueprints.of(TestFixtures.species(id = "s2", primary = Element.EARTH))

        assertTrue(water.archetype in setOf(Archetype.AQUATIC, Archetype.SERPENT))
        assertTrue(earth.archetype in setOf(Archetype.GOLEM, Archetype.BEAST))
    }

    @Test
    fun `size class scales the silhouette`() {
        val base = TestFixtures.species(id = "sized")
        val tiny = SpriteBlueprints.of(base.copy(sizeClass = SizeClass.TINY))
        val colossal = SpriteBlueprints.of(base.copy(sizeClass = SizeClass.COLOSSAL))

        assertTrue(colossal.bodyWidth > tiny.bodyWidth)
        assertTrue(colossal.bodyHeight > tiny.bodyHeight)
    }

    @Test
    fun `only legendary and above carry an aura`() {
        val base = TestFixtures.species(id = "aura")

        assertEquals(0f, SpriteBlueprints.of(base.copy(rarity = Rarity.COMMON)).auraStrength, 0f)
        assertEquals(0f, SpriteBlueprints.of(base.copy(rarity = Rarity.RARE)).auraStrength, 0f)
        assertTrue(SpriteBlueprints.of(base.copy(rarity = Rarity.LEGENDARY)).auraStrength > 0f)
        assertEquals(1f, SpriteBlueprints.of(base.copy(rarity = Rarity.DIVINE)).auraStrength, 0f)
    }

    @Test
    fun `rune species are marked with runes`() {
        val runic = TestFixtures.species(id = "runic", primary = Element.RUNE)

        assertEquals(MarkingStyle.RUNES, SpriteBlueprints.of(runic).markings)
    }

    @Test
    fun `divine species always have wings`() {
        val divine = SpriteBlueprints.of(TestFixtures.species(id = "d", primary = Element.DIVINE))

        assertNotEquals(WingShape.NONE, divine.wings)
    }

    @Test
    fun `every element, size and stage stays inside the drawable box`() {
        // The renderer has no clipping fallback — an out-of-range proportion
        // would run off the frame, so the whole matrix is checked.
        for (element in Element.entries) {
            for (size in SizeClass.entries) {
                for (stage in 1..3) {
                    val species = TestFixtures.species(id = "${element.name}_${size.name}_$stage")
                        .copy(primaryElement = element, sizeClass = size, stage = stage)
                    val plan = SpriteBlueprints.of(species)
                    val where = "${element.name}/${size.name}/stage$stage"

                    assertTrue("$where: bodyWidth ${plan.bodyWidth}", plan.bodyWidth in 0.20f..0.66f)
                    assertTrue("$where: bodyHeight ${plan.bodyHeight}", plan.bodyHeight in 0.16f..0.52f)
                    assertTrue("$where: headScale ${plan.headScale}", plan.headScale in 0.28f..0.95f)
                    assertTrue("$where: legLength", plan.legLength >= 0f)
                    assertTrue("$where: eyeCount", plan.eyeCount >= 1)
                    assertTrue("$where: markingCount", plan.markingCount >= 2)
                    assertTrue("$where: idlePeriod", plan.idlePeriodMs > 0)
                }
            }
        }
    }

    @Test
    fun `legless body plans are never drawn standing`() {
        for (element in Element.entries) {
            val plan = SpriteBlueprints.of(TestFixtures.species(id = "stand_${element.name}", primary = element))
            if (plan.legCount == 0) {
                assertTrue("${plan.archetype} has no legs and must float", plan.floats)
            }
            assertEquals(plan.legCount > 0 && !plan.floats, plan.isGrounded)
        }
    }
}

/**
 * A battle that cannot be fought must never be entered. These tests describe
 * every situation the world screen has to refuse.
 */
class BattleReadinessTest {

    private val healthy = TestFixtures.monster(uid = "ok")

    @Test
    fun `a healthy party is ready`() {
        assertEquals(BattleReadiness.Ready, BattleReadinessRules.forParty(listOf(healthy)))
    }

    @Test
    fun `an empty party is blocked`() {
        assertEquals(
            BattleReadiness.Blocked(BattleReadiness.Reason.NO_MONSTERS),
            BattleReadinessRules.forParty(emptyList()),
        )
    }

    @Test
    fun `a party of eggs only is blocked`() {
        val egg = healthy.copy(uid = "egg", isEgg = true)

        assertEquals(
            BattleReadiness.Blocked(BattleReadiness.Reason.ONLY_EGGS),
            BattleReadinessRules.forParty(listOf(egg)),
        )
    }

    @Test
    fun `a fully fainted party is blocked`() {
        assertEquals(
            BattleReadiness.Blocked(BattleReadiness.Reason.ALL_FAINTED),
            BattleReadinessRules.forParty(listOf(healthy.copy(currentHp = 0))),
        )
    }

    @Test
    fun `one survivor among fainted monsters is enough`() {
        val party = listOf(healthy.copy(uid = "down", currentHp = 0), healthy)

        assertEquals(BattleReadiness.Ready, BattleReadinessRules.forParty(party))
    }

    @Test
    fun `eggs do not block a party that also carries a fighter`() {
        val party = listOf(healthy.copy(uid = "egg", isEgg = true), healthy)

        assertEquals(BattleReadiness.Ready, BattleReadinessRules.forParty(party))
    }

    @Test
    fun `an unresolved opponent is blocked`() {
        assertEquals(
            BattleReadiness.Blocked(BattleReadiness.Reason.UNKNOWN_OPPONENT),
            BattleReadinessRules.check(listOf(healthy), opponents = null),
        )
    }

    @Test
    fun `an empty opponent team is blocked`() {
        assertEquals(
            BattleReadiness.Blocked(BattleReadiness.Reason.OPPONENT_HAS_NO_MONSTERS),
            BattleReadinessRules.check(listOf(healthy), opponents = emptyList()),
        )
    }

    @Test
    fun `the fixable player-side reason is reported first`() {
        // The player can act on "heal your team"; they cannot act on "the
        // content is broken", so the actionable message wins.
        assertEquals(
            BattleReadiness.Blocked(BattleReadiness.Reason.ALL_FAINTED),
            BattleReadinessRules.check(listOf(healthy.copy(currentHp = 0)), opponents = null),
        )
    }

    @Test
    fun `a complete matchup is ready`() {
        assertEquals(
            BattleReadiness.Ready,
            BattleReadinessRules.check(listOf(healthy), listOf(healthy.copy(uid = "foe"))),
        )
    }

    @Test
    fun `canFight mirrors forParty`() {
        assertTrue(BattleReadinessRules.canFight(listOf(healthy)))
        assertTrue(!BattleReadinessRules.canFight(emptyList()))
        assertTrue(!BattleReadinessRules.canFight(listOf(healthy.copy(currentHp = 0))))
    }
}
