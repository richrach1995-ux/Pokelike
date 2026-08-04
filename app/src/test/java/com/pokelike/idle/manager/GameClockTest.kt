package com.pokelike.idle.manager

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
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
class GameClockTest {

    /**
     * Baut eine Uhr, die vollstaendig an der virtuellen Testzeit haengt.
     *
     * [scope] ist im Test der `backgroundScope` von `runTest`: Er wird am
     * Testende automatisch abgebrochen. Ohne ihn wuerde die Endlosschleife der
     * Uhr den Test nie beenden lassen.
     */
    private fun TestScope.createClock(
        scope: CoroutineScope,
        timeSource: VirtualTimeSource = VirtualTimeSource(testScheduler),
    ) = GameClock(
        scope = scope,
        dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        timeSource = timeSource,
    ) to timeSource

    @Test
    fun `zaehlt Takte im konfigurierten Intervall`() = runTest {
        val (clock, _) = createClock(backgroundScope)

        clock.start()
        runCurrent()

        advanceTimeBy(1_000L)
        runCurrent()

        // Bei 100 ms Intervall entfallen auf eine Sekunde genau zehn Takte.
        assertThat(clock.tick.value.index).isEqualTo(10L)
        assertThat(clock.tick.value.elapsedMillis).isEqualTo(1_000L)
        assertThat(clock.tick.value.deltaMillis).isEqualTo(GameConfig.TICK_INTERVAL_MS)
    }

    @Test
    fun `meldet den Laufzustand`() = runTest {
        val (clock, _) = createClock(backgroundScope)

        assertThat(clock.isRunning.value).isFalse()

        clock.start()
        runCurrent()
        assertThat(clock.isRunning.value).isTrue()

        clock.stop()
        runCurrent()
        assertThat(clock.isRunning.value).isFalse()
    }

    @Test
    fun `mehrfaches Starten erzeugt keine zweite Schleife`() = runTest {
        val (clock, _) = createClock(backgroundScope)

        clock.start()
        runCurrent()
        // Ein zweiter Start darf den Ertrag nicht verdoppeln. Lifecycle-Events
        // koennen mehrfach eintreffen, etwa bei einer Konfigurationsaenderung.
        clock.start()
        runCurrent()

        advanceTimeBy(1_000L)
        runCurrent()

        assertThat(clock.tick.value.index).isEqualTo(10L)
    }

    @Test
    fun `begrenzt uebergrosse Zeitspruenge`() = runTest {
        val timeSource = VirtualTimeSource(testScheduler)
        val (clock, _) = createClock(backgroundScope, timeSource)

        clock.start()
        runCurrent()

        advanceTimeBy(100L)
        runCurrent()
        assertThat(clock.tick.value.deltaMillis).isEqualTo(100L)

        // Stellt einen angehaltenen Prozess nach: zwischen zwei Takten
        // vergeht eine Minute Echtzeit.
        timeSource.offsetMillis = 60_000L

        advanceTimeBy(100L)
        runCurrent()

        // Ohne Deckel wuerde dieser eine Takt 60,1 Sekunden Ertrag gutschreiben.
        assertThat(clock.tick.value.deltaMillis).isEqualTo(GameConfig.MAX_TICK_DELTA_MS)
    }

    @Test
    fun `behaelt den Stand nach dem Anhalten`() = runTest {
        val (clock, _) = createClock(backgroundScope)

        clock.start()
        runCurrent()
        advanceTimeBy(500L)
        runCurrent()

        val beforeStop = clock.tick.value
        clock.stop()
        runCurrent()

        // Die UI soll beim Wechsel in den Hintergrund nicht auf null springen.
        assertThat(clock.tick.value).isEqualTo(beforeStop)
    }
}
