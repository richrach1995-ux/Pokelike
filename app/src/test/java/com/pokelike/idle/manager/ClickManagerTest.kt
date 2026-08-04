package com.pokelike.idle.manager

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.usecases.PerformClickUseCase
import com.pokelike.idle.testing.FakeGameRepository
import com.pokelike.idle.testing.FakeRandomProvider
import com.pokelike.idle.testing.TestDispatcherProvider
import com.pokelike.idle.testing.VirtualTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClickManagerTest {

    private class Fixture(
        val clickManager: ClickManager,
        val repository: FakeGameRepository,
        val clock: GameClock,
    )

    private fun TestScope.createFixture(
        scope: CoroutineScope,
        random: FakeRandomProvider = FakeRandomProvider.neverHitting(),
    ): Fixture {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        val timeSource = VirtualTimeSource(testScheduler)
        val repository = FakeGameRepository(initialState = GameState.newGame(nowMillis = 0L))
        val clock = GameClock(scope, dispatchers, timeSource)

        return Fixture(
            clickManager = ClickManager(
                scope = scope,
                dispatchers = dispatchers,
                timeSource = timeSource,
                gameClock = clock,
                repository = repository,
                performClick = PerformClickUseCase(random),
            ),
            repository = repository,
            clock = clock,
        )
    }

    @Test
    fun `bucht den Ertrag auf den Spielstand`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.clickManager.click()

        assertThat(fixture.repository.gameState.value[ResourceType.COINS])
            .isEqualTo(BigNumber.of(GameConfig.BASE_COINS_PER_CLICK))
    }

    @Test
    fun `zaehlt die Combo ueber mehrere Klicks hoch`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.clickManager.click()
        fixture.clickManager.click()
        fixture.clickManager.click()

        assertThat(fixture.clickManager.combo.value.count).isEqualTo(3)
        assertThat(fixture.clickManager.combo.value.isActive).isTrue()
    }

    @Test
    fun `laesst die Combo nach Ablauf des Fensters verfallen`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.clickManager.start()
        fixture.clock.start()
        runCurrent()

        fixture.clickManager.click()
        assertThat(fixture.clickManager.combo.value.count).isEqualTo(1)

        advanceTimeBy(GameConfig.COMBO_WINDOW_MS + GameConfig.TICK_INTERVAL_MS)
        runCurrent()

        assertThat(fixture.clickManager.combo.value.isActive).isFalse()
    }

    @Test
    fun `zaehlt den Restanteil zwischen den Takten herunter`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.clickManager.start()
        fixture.clock.start()
        runCurrent()

        fixture.clickManager.click()
        fixture.clickManager.click()
        val afterClick = fixture.clickManager.combo.value.remainingFraction

        advanceTimeBy(GameConfig.COMBO_WINDOW_MS / 2)
        runCurrent()

        val laterFraction = fixture.clickManager.combo.value.remainingFraction
        assertThat(laterFraction).isLessThan(afterClick)
        assertThat(laterFraction).isGreaterThan(0f)
    }

    @Test
    fun `beendet die Combo beim Anhalten`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.clickManager.start()
        fixture.clock.start()
        runCurrent()

        fixture.clickManager.click()
        fixture.clickManager.click()
        assertThat(fixture.clickManager.combo.value.isActive).isTrue()

        // Beim Wechsel in den Hintergrund steht die Uhr. Bliebe die Combo
        // erhalten, koennte der Spieler Stunden spaeter eine laengst
        // verfallene Serie fortsetzen.
        fixture.clickManager.stop()
        runCurrent()

        assertThat(fixture.clickManager.combo.value.isActive).isFalse()
    }

    @Test
    fun `meldet kritische Treffer an den Aufrufer zurueck`() = runTest {
        val fixture = createFixture(backgroundScope, FakeRandomProvider.alwaysHitting())

        val outcome = fixture.clickManager.click()

        // Die Oberflaeche braucht diese Angabe fuer Farbe, Groesse und Haptik
        // der Rueckmeldung.
        assertThat(outcome.wasCritical).isTrue()
        assertThat(outcome.earned).isEqualTo(
            BigNumber.of(GameConfig.BASE_COINS_PER_CLICK) * GameConfig.BASE_CRITICAL_MULTIPLIER,
        )
    }

    @Test
    fun `summiert den Ertrag mehrerer Klicks`() = runTest {
        val fixture = createFixture(backgroundScope)

        repeat(10) { fixture.clickManager.click() }

        val coins = fixture.repository.gameState.value[ResourceType.COINS]
        val statistics = fixture.repository.gameState.value.statistics

        assertThat(statistics.totalClicks).isEqualTo(10L)
        // Zehn Klicks mit wachsender Combo liegen ueber zehn Muenzen.
        assertThat(coins).isGreaterThan(BigNumber.of(10))
    }
}
