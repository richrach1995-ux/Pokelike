package com.pokelike.idle.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liefert die Coroutine-Dispatcher der App ueber Dependency Injection.
 *
 * Direkte Zugriffe auf [Dispatchers] im Produktionscode machen Klassen
 * praktisch untestbar, weil sich der Dispatcher dann nicht mehr austauschen
 * laesst. Ueber dieses Interface kann jeder Test alle Dispatcher auf einen
 * `TestDispatcher` legen und damit Nebenlaeufigkeit deterministisch machen.
 */
interface DispatcherProvider {

    /** CPU-gebundene Arbeit: Spielberechnungen, Formatierung, Sortierung. */
    val default: CoroutineDispatcher

    /** Blockierende I/O: Datenbank, DataStore, Netzwerk. */
    val io: CoroutineDispatcher

    /** UI-Thread. Nur fuer Arbeit, die zwingend dort laufen muss. */
    val main: CoroutineDispatcher
}

/** Produktionsimplementierung mit den Standard-Dispatchern von kotlinx.coroutines. */
@Singleton
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {

    override val default: CoroutineDispatcher = Dispatchers.Default

    override val io: CoroutineDispatcher = Dispatchers.IO

    override val main: CoroutineDispatcher = Dispatchers.Main.immediate
}
