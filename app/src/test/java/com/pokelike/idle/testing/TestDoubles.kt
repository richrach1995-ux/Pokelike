package com.pokelike.idle.testing

import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler

/**
 * Leitet alle Dispatcher auf denselben Test-Dispatcher um.
 *
 * Dadurch laeuft saemtliche Nebenlaeufigkeit im Test auf einer einzigen,
 * kontrollierten Warteschlange. Ohne diese Umleitung waeren Tests, die
 * Hintergrundarbeit anstossen, von der Laune des Schedulers abhaengig und damit
 * unzuverlaessig.
 */
class TestDispatcherProvider(
    private val dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}

/**
 * Zeitquelle, die an die virtuelle Uhr des Test-Schedulers gekoppelt ist.
 *
 * Damit stimmen die Zeitstempel, die der getestete Code liest, exakt mit der
 * Zeit ueberein, die der Test per `advanceTimeBy` vorspult. Ein Test kann so
 * Stunden Spielzeit in Millisekunden Laufzeit pruefen.
 *
 * @property offsetMillis Zusaetzlicher Versatz auf die virtuelle Zeit. Damit
 *   lassen sich Zeitspruenge nachstellen, wie sie entstehen, wenn das System
 *   den Prozess zwischenzeitlich anhaelt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VirtualTimeSource(
    private val scheduler: TestCoroutineScheduler,
    var offsetMillis: Long = 0L,
) : TimeSource {
    override fun elapsedRealtime(): Long = scheduler.currentTime + offsetMillis
    override fun wallClock(): Long = scheduler.currentTime + offsetMillis
}
