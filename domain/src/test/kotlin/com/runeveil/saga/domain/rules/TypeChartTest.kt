package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The type chart is the backbone of every battle. These tests lock in both its
 * mechanics and its *balance*: a future content change that makes one element
 * dominant will fail the build.
 */
class TypeChartTest {

    @Test
    fun `every element is super effective against two to four elements`() {
        for (element in Element.entries) {
            val count = TypeChart.strengthsOf(element).size
            assertTrue(
                "${element.name} has $count offensive strengths, expected 2..4",
                count in 2..4,
            )
        }
    }

    @Test
    fun `every element is hit super effectively by two to four elements`() {
        for (element in Element.entries) {
            val count = TypeChart.weaknessesOf(element).size
            assertTrue(
                "${element.name} has $count weaknesses, expected 2..4",
                count in 2..4,
            )
        }
    }

    @Test
    fun `no element is super effective against itself`() {
        for (element in Element.entries) {
            assertTrue(
                "${element.name} is super effective against itself",
                TypeChart.multiplier(element, element) <= TypeChart.NEUTRAL,
            )
        }
    }

    @Test
    fun `offensive power is spread evenly across the roster`() {
        val scores = Element.entries.map { attacker ->
            Element.entries.sumOf { TypeChart.multiplier(attacker, it) }
        }
        val spread = scores.max() - scores.min()
        assertTrue("Offensive score spread $spread is too wide", spread <= 5.0)
    }

    @Test
    fun `immunities are rare and thematic`() {
        val immunities = Element.entries.flatMap { attacker ->
            Element.entries.filter { TypeChart.multiplier(attacker, it) == TypeChart.IMMUNE }
                .map { attacker to it }
        }
        assertEquals(
            "Unexpected set of immunities: $immunities",
            setOf(
                Element.THUNDER to Element.EARTH,
                Element.SHADOW to Element.DIVINE,
                Element.CHAOS to Element.RUNE,
                Element.METAL to Element.SPIRIT,
            ),
            immunities.toSet(),
        )
    }

    @Test
    fun `dual typing multiplies both defending elements`() {
        // Fire is super effective against Nature and resisted by Water.
        val multiplier = TypeChart.multiplier(Element.FIRE, Element.NATURE, Element.WATER)
        assertEquals(1.0, multiplier, 0.0001)

        // Double weakness.
        val double = TypeChart.multiplier(Element.FIRE, Element.NATURE, Element.ICE)
        assertEquals(4.0, double, 0.0001)
    }

    @Test
    fun `immunity wins over a weakness on the second element`() {
        // Metal cannot touch Spirit at all, even when paired with Ice.
        val multiplier = TypeChart.multiplier(Element.METAL, Element.SPIRIT, Element.ICE)
        assertEquals(0.0, multiplier, 0.0001)
    }

    @Test
    fun `effectiveness buckets map to the right descriptions`() {
        assertEquals(Effectiveness.IMMUNE, TypeChart.describe(0.0))
        assertEquals(Effectiveness.DOUBLE_RESISTED, TypeChart.describe(0.25))
        assertEquals(Effectiveness.RESISTED, TypeChart.describe(0.5))
        assertEquals(Effectiveness.NEUTRAL, TypeChart.describe(1.0))
        assertEquals(Effectiveness.SUPER_EFFECTIVE, TypeChart.describe(2.0))
        assertEquals(Effectiveness.DEVASTATING, TypeChart.describe(4.0))
    }

    @Test
    fun `defensive profile covers every element`() {
        val profile = TypeChart.defensiveProfile(Element.FIRE, Element.METAL)
        assertEquals(Element.entries.size, profile.size)
        // Fire/Metal double-resists Ice (both resist it).
        assertEquals(0.25, profile.getValue(Element.ICE), 0.0001)
    }
}
