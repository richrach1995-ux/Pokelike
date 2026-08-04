package com.pokelike.idle.ui.screens.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.usecases.PerformClickUseCase
import com.pokelike.idle.manager.ClickManager
import com.pokelike.idle.manager.GameClock
import com.pokelike.idle.testing.FakeGameRepository
import com.pokelike.idle.testing.FakeRandomProvider
import com.pokelike.idle.testing.MainDispatcherRule
import com.pokelike.idle.testing.TestDispatcherProvider
import com.pokelike.idle.testing.VirtualTimeSource
import com.pokelike.idle.util.NumberFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    /**
     * Ohne diese Regel schlaegt jeder Zugriff auf `viewModelScope` fehl, weil
     * es in einem JVM-Test keinen Android-Main-Dispatcher gibt.
     */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class Fixture(
        val viewModel: HomeViewModel,
        val repository: FakeGameRepository,
    )

    private fun createFixture(
        scope: CoroutineScope,
        scheduler: TestCoroutineScheduler,
    ): Fixture {
        val dispatchers = TestDispatcherProvider(mainDispatcherRule.testDispatcher)
        val timeSource = VirtualTimeSource(scheduler)
        val repository = FakeGameRepository(initialState = GameState.newGame(nowMillis = 0L))
        val clock = GameClock(scope, dispatchers, timeSource)

        return Fixture(
            viewModel = HomeViewModel(
                clickManager = ClickManager(
                    scope = scope,
                    dispatchers = dispatchers,
                    timeSource = timeSource,
                    gameClock = clock,
                    repository = repository,
                    performClick = PerformClickUseCase(FakeRandomProvider.neverHitting()),
                ),
                gameRepository = repository,
                numberFormatter = NumberFormatter(),
                dispatchers = dispatchers,
            ),
            repository = repository,
        )
    }

    @Test
    fun `zeigt die Kontostaende aus dem Spielstand`() = runTest {
        val fixture = createFixture(backgroundScope, testScheduler)

        fixture.viewModel.uiState.test {
            val state = awaitItem()

            // Werte aus GameConfig: Der Spieler startet ohne Muenzen, aber mit
            // einer kleinen Menge Premiumwaehrung.
            assertThat(state.coins).isEqualTo("0")
            assertThat(state.diamonds).isEqualTo("25")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sperrt den Klick-Button bis der Spielstand geladen ist`() = runTest {
        val fixture = createFixture(backgroundScope, testScheduler)

        fixture.viewModel.uiState.test {
            assertThat(awaitItem().isReady).isFalse()

            fixture.repository.load(nowMillis = 0L)
            runCurrent()

            // Ein Klick auf einen ungeladenen Stand wuerde auf dem leeren
            // Ausgangszustand rechnen und den gespeicherten Fortschritt
            // ueberschreiben.
            assertThat(expectMostRecentItem().isReady).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `erhoeht den Muenzstand bei einem Klick`() = runTest {
        val fixture = createFixture(backgroundScope, testScheduler)

        fixture.viewModel.uiState.test {
            awaitItem()

            fixture.viewModel.onClick()
            runCurrent()

            assertThat(expectMostRecentItem().coins)
                .isEqualTo(GameConfig.BASE_COINS_PER_CLICK.toString())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `zeigt die Combo ab dem zweiten Klick`() = runTest {
        val fixture = createFixture(backgroundScope, testScheduler)

        fixture.viewModel.uiState.test {
            awaitItem()

            fixture.viewModel.onClick()
            fixture.viewModel.onClick()
            runCurrent()

            val state = expectMostRecentItem()
            assertThat(state.comboCount).isEqualTo(2)
            assertThat(state.comboMultiplier).isEqualTo("1.02x")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `formatiert den Ertrag mit Vorzeichen fuer den schwebenden Hinweis`() = runTest {
        val fixture = createFixture(backgroundScope, testScheduler)

        val outcome = fixture.viewModel.onClick()

        assertThat(fixture.viewModel.formatEarned(outcome)).isEqualTo("+1")
    }
}
