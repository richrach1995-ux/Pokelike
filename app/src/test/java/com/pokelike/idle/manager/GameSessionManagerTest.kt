package com.pokelike.idle.manager

import com.google.common.truth.Truth.assertThat
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

/**
 * Hinweis zur Testtechnik: Hier wird bewusst mit `runCurrent` und
 * `advanceTimeBy` gearbeitet und nicht mit `advanceUntilIdle`. Seit
 * kotlinx-coroutines 1.7 zaehlt Arbeit im `backgroundScope` nicht als
 * ausstehend - `advanceUntilIdle` kehrt sofort zurueck, ohne sie ausgefuehrt zu
 * haben, und die Zusicherungen liefen ins Leere. Ohne den `backgroundScope`
 * wiederum wuerde die endlose Tick-Schleife der Uhr den Test nie beenden.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameSessionManagerTest {

    private class Fixture(
        val session: GameSessionManager,
        val repository: FakeGameRepository,
        val clock: GameClock,
    )

    private fun TestScope.createFixture(
        scope: CoroutineScope,
        repository: FakeGameRepository = FakeGameRepository(),
    ): Fixture {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        val timeSource = VirtualTimeSource(testScheduler)
        val clock = GameClock(scope, dispatchers, timeSource)
        val autosave = AutosaveManager(scope, dispatchers, clock, repository)
        val clickManager = ClickManager(
            scope = scope,
            dispatchers = dispatchers,
            timeSource = timeSource,
            gameClock = clock,
            repository = repository,
            performClick = PerformClickUseCase(FakeRandomProvider.neverHitting()),
        )

        return Fixture(
            session = GameSessionManager(
                scope = scope,
                dispatchers = dispatchers,
                timeSource = timeSource,
                repository = repository,
                gameClock = clock,
                autosaveManager = autosave,
                clickManager = clickManager,
            ),
            repository = repository,
            clock = clock,
        )
    }

    @Test
    fun `laedt den Spielstand und startet die Uhr`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()

        assertThat(fixture.repository.loadCount).isEqualTo(1)
        assertThat(fixture.repository.isLoaded.value).isTrue()
        assertThat(fixture.clock.isRunning.value).isTrue()
    }

    @Test
    fun `laedt bei erneutem Wechsel in den Vordergrund nicht noch einmal`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()
        fixture.session.onEnterBackground()
        runCurrent()
        fixture.session.onEnterForeground()
        runCurrent()

        // Ein zweites Laden wuerde den Fortschritt der laufenden Sitzung
        // verwerfen und auf den zuletzt gespeicherten Stand zuruecksetzen.
        assertThat(fixture.repository.loadCount).isEqualTo(1)
        assertThat(fixture.clock.isRunning.value).isTrue()
    }

    @Test
    fun `haelt die Uhr an und speichert beim Wechsel in den Hintergrund`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()

        fixture.session.onEnterBackground()
        runCurrent()

        assertThat(fixture.clock.isRunning.value).isFalse()
        assertThat(fixture.repository.saveCount).isAtLeast(1)
    }

    @Test
    fun `schreibt den Zeitstempel fuer die Offline-Berechnung fort`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()
        val beforeBackground = fixture.repository.gameState.value.lastSeenAtMillis

        // Spielzeit vergehen lassen, damit sich der Zeitstempel messbar
        // veraendert. Die Zeitquelle haengt an der virtuellen Uhr des Tests.
        advanceTimeBy(60_000L)
        runCurrent()

        fixture.session.onEnterBackground()
        runCurrent()

        // Ohne diesen Zeitstempel gaebe es beim naechsten Start keinen
        // Bezugspunkt fuer den Offline-Fortschritt.
        assertThat(fixture.repository.gameState.value.lastSeenAtMillis)
            .isGreaterThan(beforeBackground)
    }

    @Test
    fun `speichert nicht, solange nichts geladen wurde`() = runTest {
        val fixture = createFixture(backgroundScope)

        // Kann eintreten, wenn der Prozess sofort wieder in den Hintergrund
        // geht. Ein Schreibvorgang wuerde hier den leeren Ausgangszustand
        // ueber den gespeicherten Spielstand legen.
        fixture.session.onEnterBackground()
        runCurrent()

        assertThat(fixture.repository.saveCount).isEqualTo(0)
    }
}
