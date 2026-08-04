package com.pokelike.idle.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

class UnlockConditionTest {

    private val emptyState = GameState.newGame(nowMillis = 0L)

    @Test
    fun `Always ist immer erfuellt`() {
        assertThat(UnlockCondition.Always.isMet(emptyState)).isTrue()
    }

    @Test
    fun `TotalClicks prueft die Lebenszeitstatistik`() {
        val condition = UnlockCondition.TotalClicks(100L)
        val state = emptyState.copy(statistics = GameStatistics(totalClicks = 99L))

        assertThat(condition.isMet(state)).isFalse()
        assertThat(condition.isMet(state.copy(statistics = GameStatistics(totalClicks = 100L))))
            .isTrue()
    }

    @Test
    fun `BuildingCount prueft eine einzelne Gebaeudeart`() {
        val condition = UnlockCondition.BuildingCount(BuildingType.MINE, 10)

        assertThat(
            condition.isMet(
                emptyState.copy(buildings = BuildingInventory.of(BuildingType.MINE to 9)),
            ),
        ).isFalse()
        assertThat(
            condition.isMet(
                emptyState.copy(buildings = BuildingInventory.of(BuildingType.MINE to 10)),
            ),
        ).isTrue()
    }

    @Test
    fun `BuildingCount zaehlt andere Gebaeude nicht mit`() {
        val condition = UnlockCondition.BuildingCount(BuildingType.MINE, 1)
        val state = emptyState.copy(buildings = BuildingInventory.of(BuildingType.FARM to 100))

        assertThat(condition.isMet(state)).isFalse()
    }

    @Test
    fun `TotalBuildings zaehlt alle Arten zusammen`() {
        val condition = UnlockCondition.TotalBuildings(10)
        val state = emptyState.copy(
            buildings = BuildingInventory.of(
                BuildingType.FINGER to 6,
                BuildingType.CURSOR to 4,
            ),
        )

        assertThat(condition.isMet(state)).isTrue()
    }

    @Test
    fun `LifetimeCoins prueft die Summe und nicht den Kontostand`() {
        // Sonst verschwaende ein freigeschaltetes Upgrade wieder, sobald der
        // Spieler sein Geld ausgibt - genau dann, wenn er es braeuchte.
        val condition = UnlockCondition.LifetimeCoins(BigNumber.of(1_000))

        val richButSpent = emptyState.copy(
            resources = ResourcePool.EMPTY,
            statistics = GameStatistics(
                lifetimeEarned = ResourceBundle.single(
                    ResourceType.COINS,
                    BigNumber.of(5_000),
                ),
            ),
        )

        assertThat(condition.isMet(richButSpent)).isTrue()
    }

    @Test
    fun `All verlangt alle Teilbedingungen`() {
        val condition = UnlockCondition.All(
            listOf(
                UnlockCondition.TotalClicks(10L),
                UnlockCondition.TotalBuildings(1),
            ),
        )

        val onlyClicks = emptyState.copy(statistics = GameStatistics(totalClicks = 50L))
        val both = onlyClicks.copy(
            buildings = BuildingInventory.of(BuildingType.FINGER to 1),
        )

        assertThat(condition.isMet(onlyClicks)).isFalse()
        assertThat(condition.isMet(both)).isTrue()
    }

    @Test
    fun `weist unsinnige Angaben ab`() {
        assertFailsWith<IllegalArgumentException> { UnlockCondition.TotalClicks(-1L) }
        assertFailsWith<IllegalArgumentException> {
            UnlockCondition.BuildingCount(BuildingType.MINE, -1)
        }
        assertFailsWith<IllegalArgumentException> { UnlockCondition.TotalBuildings(-1) }
        assertFailsWith<IllegalArgumentException> {
            UnlockCondition.LifetimeCoins(BigNumber.of(-1))
        }
        assertFailsWith<IllegalArgumentException> { UnlockCondition.All(emptyList()) }
    }
}

class UpgradeCatalogTest {

    @Test
    fun `haelt die Schluessel eindeutig`() {
        assertThat(UpgradeType.entries.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `findet Upgrades ueber ihren Schluessel`() {
        assertThat(UpgradeType.fromId("stronger_fingers"))
            .isEqualTo(UpgradeType.STRONGER_FINGERS)
        assertThat(UpgradeType.fromId("gibt_es_nicht")).isNull()
    }

    @Test
    fun `hat fuer jedes Upgrade einen positiven Preis`() {
        UpgradeType.entries.forEach { upgrade ->
            assertThat(upgrade.price).isGreaterThan(0.0)
            assertThat(upgrade.priceAsBigNumber.isPositive).isTrue()
        }
    }

    @Test
    fun `ist zu Beginn nur teilweise freigeschaltet`() {
        // Alle Upgrades von Anfang an zu zeigen wuerde den Einstieg
        // erschlagen; keines zu zeigen liesse den Bildschirm leer wirken.
        val newGame = GameState.newGame(nowMillis = 0L)

        val unlocked = UpgradeType.entries.count { it.unlockCondition.isMet(newGame) }

        assertThat(unlocked).isLessThan(UpgradeType.entries.size)
    }

    @Test
    fun `deckt jede Wirkungsart mindestens einmal ab`() {
        // Eine Wirkungsart ohne Upgrade waere toter Code in der Auswertung.
        val effectTypes = UpgradeType.entries.map { it.effect::class }.toSet()

        assertThat(effectTypes).hasSize(EXPECTED_EFFECT_TYPES)
    }

    private companion object {
        /** Anzahl der in [UpgradeEffect] definierten Wirkungsarten. */
        const val EXPECTED_EFFECT_TYPES = 8
    }
}
