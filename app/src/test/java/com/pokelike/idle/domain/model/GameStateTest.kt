package com.pokelike.idle.domain.model

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import org.junit.Test
import kotlin.test.assertFailsWith

class GameStatisticsTest {

    @Test
    fun `zaehlt Klicks und kritische Treffer getrennt`() {
        val statistics = GameStatistics()
            .withClick(wasCritical = false)
            .withClick(wasCritical = true)
            .withClick(wasCritical = false)

        assertThat(statistics.totalClicks).isEqualTo(3L)
        assertThat(statistics.totalCriticalClicks).isEqualTo(1L)
    }

    @Test
    fun `berechnet die erreichte Kritrate`() {
        val statistics = GameStatistics(totalClicks = 200L, totalCriticalClicks = 50L)

        assertThat(statistics.criticalRate).isWithin(TOLERANCE).of(0.25)
    }

    @Test
    fun `liefert ohne Klicks eine Kritrate von null`() {
        // Die naheliegende Division durch null darf hier nicht auftreten.
        assertThat(GameStatistics().criticalRate).isEqualTo(0.0)
    }

    @Test
    fun `summiert erhaltene und ausgegebene Betraege`() {
        val statistics = GameStatistics()
            .withEarned(ResourceBundle.single(ResourceType.COINS, BigNumber.of(100)))
            .withEarned(ResourceBundle.single(ResourceType.COINS, BigNumber.of(50)))
            .withSpent(ResourceBundle.single(ResourceType.COINS, BigNumber.of(30)))

        assertThat(statistics.lifetimeEarned[ResourceType.COINS]).isEqualTo(BigNumber.of(150))
        assertThat(statistics.lifetimeSpent[ResourceType.COINS]).isEqualTo(BigNumber.of(30))
    }

    @Test
    fun `schreibt die Spielzeit fort`() {
        val statistics = GameStatistics()
            .withPlayTime(1_000L)
            .withPlayTime(500L)

        assertThat(statistics.totalPlayTimeMillis).isEqualTo(1_500L)
    }

    @Test
    fun `weist negative Spielzeit ab`() {
        assertFailsWith<IllegalArgumentException> { GameStatistics().withPlayTime(-1L) }
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}

class GameStateTest {

    @Test
    fun `stattet einen neuen Spielstand nach Konfiguration aus`() {
        val state = GameState.newGame(nowMillis = NOW)

        assertThat(state[ResourceType.COINS])
            .isEqualTo(BigNumber.of(GameConfig.STARTING_COINS))
        assertThat(state[ResourceType.DIAMONDS])
            .isEqualTo(BigNumber.of(GameConfig.STARTING_DIAMONDS))
        assertThat(state.createdAtMillis).isEqualTo(NOW)
        assertThat(state.lastSeenAtMillis).isEqualTo(NOW)
        assertThat(state.statistics.sessionCount).isEqualTo(1)
    }

    @Test
    fun `traegt die aktuelle Schemaversion`() {
        // Ohne dieses Feld liesse sich ein alter Spielstand nach einem Update
        // nicht von einem beschaedigten unterscheiden.
        assertThat(GameState.newGame(NOW).schemaVersion)
            .isEqualTo(GameState.CURRENT_SCHEMA_VERSION)
    }

    @Test
    fun `setzt beim Prestige die weiche Waehrung zurueck`() {
        val state = GameState.newGame(NOW).copy(
            resources = ResourcePool.of(
                ResourceType.COINS to BigNumber.of(1.0, 40),
                ResourceType.DIAMONDS to BigNumber.of(500),
            ),
        )

        val afterPrestige = state.afterPrestige(BigNumber.of(120))

        assertThat(afterPrestige[ResourceType.COINS]).isEqualTo(BigNumber.ZERO)
        assertThat(afterPrestige[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(500))
        assertThat(afterPrestige[ResourceType.PRESTIGE_POINTS]).isEqualTo(BigNumber.of(120))
    }

    @Test
    fun `sammelt Prestige-Punkte ueber mehrere Durchlaeufe an`() {
        val state = GameState.newGame(NOW)
            .afterPrestige(BigNumber.of(100))
            .afterPrestige(BigNumber.of(50))

        assertThat(state[ResourceType.PRESTIGE_POINTS]).isEqualTo(BigNumber.of(150))
        assertThat(state.statistics.prestigeCount).isEqualTo(2)
    }

    @Test
    fun `behaelt die Statistik ueber den Prestige-Reset hinweg`() {
        // Ein Achievement fuer 10.000 Klicks waere wertlos, wenn der Zaehler
        // bei jedem Prestige von vorn begaenne.
        val state = GameState.newGame(NOW).copy(
            statistics = GameStatistics(totalClicks = 10_000L, totalCriticalClicks = 800L),
        )

        val afterPrestige = state.afterPrestige(BigNumber.of(10))

        assertThat(afterPrestige.statistics.totalClicks).isEqualTo(10_000L)
        assertThat(afterPrestige.statistics.totalCriticalClicks).isEqualTo(800L)
    }

    @Test
    fun `verbucht die Prestige-Gutschrift in der Statistik`() {
        val state = GameState.newGame(NOW).afterPrestige(BigNumber.of(120))

        assertThat(state.statistics.lifetimeEarned[ResourceType.PRESTIGE_POINTS])
            .isEqualTo(BigNumber.of(120))
    }

    @Test
    fun `weist eine negative Prestige-Gutschrift ab`() {
        assertFailsWith<IllegalArgumentException> {
            GameState.newGame(NOW).afterPrestige(BigNumber.of(-1))
        }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
