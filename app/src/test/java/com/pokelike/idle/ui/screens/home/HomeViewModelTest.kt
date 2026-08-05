package com.pokelike.idle.ui.screens.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.usecases.CalculateIncomeUseCase
import com.pokelike.idle.domain.usecases.GrantAdRewardUseCase
import com.pokelike.idle.domain.usecases.PerformClickUseCase
import com.pokelike.idle.domain.usecases.PurchaseBoosterUseCase
import com.pokelike.idle.domain.usecases.StartBoosterUseCase
import com.pokelike.idle.manager.BoosterManager
import com.pokelike.idle.manager.ClickManager
import com.pokelike.idle.manager.GameClock
import com.pokelike.idle.manager.IdleIncomeManager
import com.pokelike.idle.manager.RewardedAdManager
import com.pokelike.idle.testing.FakeAdSource
import com.pokelike.idle.testing.FakeGameRepository
import com.pokelike.idle.testing.FakeRandomProvider
import com.pokelike.idle.testing.MainDispatcherRule
import com.pokelike.idle.testing.RecordingGameLogger
import com.pokelike.idle.testing.TestDispatcherProvider
import com.pokelike.idle.testing.VirtualTimeSource
import com.pokelike.idle.testing.modifierManagerFor
import com.pokelike.idle.util.DurationFormatter
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
        val modifierManager = modifierManagerFor(scope, repository)

        return Fixture(
            viewModel = HomeViewModel(
                clickManager = ClickManager(
                    scope = scope,
                    dispatchers = dispatchers,
                    timeSource = timeSource,
                    gameClock = clock,
                    repository = repository,
                    modifierManager = modifierManager,
                    performClick = PerformClickUseCase(FakeRandomProvider.neverHitting()),
                ),
                boosterManager = BoosterManager(
                    scope = scope,
                    dispatchers = dispatchers,
                    gameClock = clock,
                    repository = repository,
                    timeSource = timeSource,
                    purchaseBooster = PurchaseBoosterUseCase(StartBoosterUseCase()),
                ),
                rewardedAdManager = RewardedAdManager(
                    scope = scope,
                    dispatchers = dispatchers,
                    gameClock = clock,
                    repository = repository,
                    timeSource = timeSource,
                    adSource = FakeAdSource(),
                    grantAdReward = GrantAdRewardUseCase(StartBoosterUseCase()),
                    logger = RecordingGameLogger(),
                ),
                idleIncomeManager = IdleIncomeManager(
                    scope = scope,
                    dispatchers = dispatchers,
                    gameClock = clock,
                    repository = repository,
                    modifierManager = modifierManager,
                    calculateIncome = CalculateIncomeUseCase(),
                ),
                modifierManager = modifierManager,
                gameRepository = repository,
                numberFormatter = NumberFormatter(),
                durationFormatter = DurationFormatter(),
                dispatchers = dispatchers,
            ),
            repository = repository,
        )
    }

    @Test
    fun `zeigt die Kontostaende aus dem Spielstand`() = runTest {
        val fixture = createFixture(backgroundScope, testScheduler)

        fixture.viewModel.uiState.test {
            // Der erste Wert ist der Ausgangswert des StateFlow und noch leer:
            // Die Zusammenfuehrung der Quellflusse laeuft auf dem
            // Test-Dispatcher und muss erst einmal ausgefuehrt werden. Ohne
            // runCurrent pruefte dieser Test den Ausgangswert statt den
            // Spielstand.
            awaitItem()
            runCurrent()

            val state = expectMostRecentItem()

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
