package com.pokelike.idle.ui.screens.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.manager.GameClock
import com.pokelike.idle.testing.MainDispatcherRule
import com.pokelike.idle.testing.TestDispatcherProvider
import com.pokelike.idle.testing.VirtualTimeSource
import com.pokelike.idle.util.DurationFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
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

    private fun createViewModel(
        scope: CoroutineScope,
        scheduler: TestCoroutineScheduler,
    ): Pair<HomeViewModel, GameClock> {
        val dispatchers = TestDispatcherProvider(mainDispatcherRule.testDispatcher)
        val clock = GameClock(
            scope = scope,
            dispatchers = dispatchers,
            timeSource = VirtualTimeSource(scheduler),
        )
        val viewModel = HomeViewModel(
            gameClock = clock,
            durationFormatter = DurationFormatter(),
            dispatchers = dispatchers,
        )
        return viewModel to clock
    }

    @Test
    fun `startet mit pausierter Engine und Nullzeit`() = runTest {
        val (viewModel, _) = createViewModel(backgroundScope, testScheduler)

        val initial = viewModel.uiState.value
        assertThat(initial.isEngineRunning).isFalse()
        assertThat(initial.sessionTime).isEqualTo("00:00")
        assertThat(initial.tickCount).isEqualTo(0L)
    }

    @Test
    fun `bildet den Fortschritt der Uhr auf den Anzeigezustand ab`() = runTest {
        val (viewModel, clock) = createViewModel(backgroundScope, testScheduler)

        viewModel.uiState.test {
            // Ausgangszustand, bevor die Uhr laeuft.
            assertThat(awaitItem().isEngineRunning).isFalse()

            clock.start()
            advanceTimeBy(65_000L)
            runCurrent()

            // Turbine liefert jede Zwischenaktualisierung; interessant ist der
            // Stand nach Ablauf der vorgespulten Zeit.
            val latest = expectMostRecentItem()
            assertThat(latest.isEngineRunning).isTrue()
            assertThat(latest.sessionTime).isEqualTo("01:05")
            assertThat(latest.tickCount).isEqualTo(650L)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `meldet die Engine als pausiert nach dem Anhalten`() = runTest {
        val (viewModel, clock) = createViewModel(backgroundScope, testScheduler)

        // Der Zustand wird durch SharingStarted.WhileSubscribed nur
        // fortgeschrieben, solange jemand zuhoert. Der Test muss deshalb
        // waehrend der gesamten Pruefung sammeln.
        viewModel.uiState.test {
            clock.start()
            advanceTimeBy(1_000L)
            runCurrent()
            assertThat(expectMostRecentItem().isEngineRunning).isTrue()

            clock.stop()
            runCurrent()
            assertThat(expectMostRecentItem().isEngineRunning).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
