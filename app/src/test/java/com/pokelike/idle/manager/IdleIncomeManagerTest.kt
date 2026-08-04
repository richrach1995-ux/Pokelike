package com.pokelike.idle.manager

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.usecases.CalculateIncomeUseCase
import com.pokelike.idle.testing.FakeGameRepository
import com.pokelike.idle.testing.modifierManagerFor
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
class IdleIncomeManagerTest {

    private class Fixture(
        val incomeManager: IdleIncomeManager,
        val repository: FakeGameRepository,
        val clock: GameClock,
    )

    private fun TestScope.createFixture(
        scope: CoroutineScope,
        buildings: BuildingInventory = BuildingInventory.EMPTY,
    ): Fixture {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        val repository = FakeGameRepository(
            initialState = GameState.newGame(nowMillis = 0L).copy(buildings = buildings),
        )
        val clock = GameClock(scope, dispatchers, VirtualTimeSource(testScheduler))

        return Fixture(
            incomeManager = IdleIncomeManager(
                scope = scope,
                dispatchers = dispatchers,
                gameClock = clock,
                repository = repository,
                modifierManager = modifierManagerFor(scope, repository),
                calculateIncome = CalculateIncomeUseCase(),
            ),
            repository = repository,
            clock = clock,
        )
    }

    @Test
    fun `schreibt den Ertrag ueber die Zeit gut`() = runTest {
        // Zehn Cursor bringen zehn Muenzen pro Sekunde.
        val fixture = createFixture(
            backgroundScope,
            BuildingInventory.of(BuildingType.CURSOR to 10),
        )

        fixture.incomeManager.start()
        fixture.clock.start()
        runCurrent()

        advanceTimeBy(ONE_SECOND)
        runCurrent()

        val coins = fixture.repository.gameState.value[ResourceType.COINS]
        assertThat(coins.toDouble()).isWithin(TOLERANCE).of(10.0)
    }

    @Test
    fun `schreibt ohne Gebaeude nichts gut`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.incomeManager.start()
        fixture.clock.start()
        runCurrent()

        advanceTimeBy(10 * ONE_SECOND)
        runCurrent()

        assertThat(fixture.repository.gameState.value[ResourceType.COINS])
            .isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `haelt mit der Uhr an`() = runTest {
        val fixture = createFixture(
            backgroundScope,
            BuildingInventory.of(BuildingType.CURSOR to 10),
        )

        fixture.incomeManager.start()
        fixture.clock.start()
        runCurrent()
        advanceTimeBy(ONE_SECOND)
        runCurrent()

        val afterOneSecond = fixture.repository.gameState.value[ResourceType.COINS]

        // Im Hintergrund darf nichts anfallen - sonst wuerde die
        // Offline-Berechnung dieselbe Zeit beim naechsten Start ein zweites
        // Mal gutschreiben.
        fixture.clock.stop()
        runCurrent()
        advanceTimeBy(60 * ONE_SECOND)
        runCurrent()

        assertThat(fixture.repository.gameState.value[ResourceType.COINS])
            .isEqualTo(afterOneSecond)
    }

    @Test
    fun `meldet den Ertrag pro Sekunde`() = runTest {
        val fixture = createFixture(
            backgroundScope,
            BuildingInventory.of(BuildingType.MINE to 2),
        )
        runCurrent()

        // Zwei Minen zu je 47 Muenzen pro Sekunde.
        assertThat(fixture.incomeManager.incomePerSecond.value).isEqualTo(BigNumber.of(94.0))
    }

    @Test
    fun `mehrfaches Starten verdoppelt den Ertrag nicht`() = runTest {
        val fixture = createFixture(
            backgroundScope,
            BuildingInventory.of(BuildingType.CURSOR to 10),
        )

        fixture.incomeManager.start()
        fixture.incomeManager.start()
        fixture.clock.start()
        runCurrent()

        advanceTimeBy(ONE_SECOND)
        runCurrent()

        assertThat(fixture.repository.gameState.value[ResourceType.COINS].toDouble())
            .isWithin(TOLERANCE).of(10.0)
    }

    @Test
    fun `schreibt nach dem Anhalten nichts mehr gut`() = runTest {
        val fixture = createFixture(
            backgroundScope,
            BuildingInventory.of(BuildingType.CURSOR to 10),
        )

        fixture.incomeManager.start()
        fixture.clock.start()
        runCurrent()
        fixture.incomeManager.stop()
        runCurrent()

        advanceTimeBy(10 * ONE_SECOND)
        runCurrent()

        assertThat(fixture.repository.gameState.value[ResourceType.COINS])
            .isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `schreibt die Lebenszeitsumme mit fort`() = runTest {
        val fixture = createFixture(
            backgroundScope,
            BuildingInventory.of(BuildingType.CURSOR to 10),
        )

        fixture.incomeManager.start()
        fixture.clock.start()
        runCurrent()
        advanceTimeBy(ONE_SECOND)
        runCurrent()

        // An dieser Summe haengen Achievements und die Freischaltung weiterer
        // Gebaeude; sie darf nicht hinter dem Kontostand zurueckbleiben.
        val state = fixture.repository.gameState.value
        assertThat(state.statistics.lifetimeEarned[ResourceType.COINS])
            .isEqualTo(state[ResourceType.COINS])
    }

    private companion object {
        const val ONE_SECOND = 1_000L
        const val TOLERANCE = 1e-6
    }
}
