package com.pokelike.idle.manager

import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.model.GameModifiers
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.CalculateModifiersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haelt die wirksamen Spielwerte bereit.
 *
 * Eine einzige Quelle fuer alle Systeme: Klickberechnung, Leerlaufeinkommen,
 * Gebaeudepreise, Offline-Verrechnung und die Oberflaeche lesen dasselbe
 * [GameModifiers]-Objekt. Wuerde jedes System die Upgrades selbst auswerten,
 * waere dieselbe Rechnung fuenfmal vorhanden - und ein neuer Wirkungstyp
 * muesste an fuenf Stellen ergaenzt werden.
 *
 * [distinctUntilChanged] ist hier nicht Feinschliff, sondern notwendig: Der
 * Spielstand aendert sich zehnmal pro Sekunde durch die laufende Gutschrift,
 * die Werte selbst dagegen nur beim Kauf eines Upgrades. Ohne den Filter wuerde
 * jede daran haengende Anzeige zehnmal pro Sekunde neu gezeichnet, und die
 * Auswertung liefe ebenso oft.
 */
@Singleton
class ModifierManager @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
    repository: GameRepository,
    calculateModifiers: CalculateModifiersUseCase,
) {

    val modifiers: StateFlow<GameModifiers> = repository.gameState
        .map { state -> calculateModifiers(state) }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            // Eagerly, weil die Werte auch dann stimmen muessen, wenn gerade
            // kein Bildschirm sie beobachtet - die Tick-Schleifen von Klick und
            // Einkommen lesen sie direkt.
            started = SharingStarted.Eagerly,
            initialValue = GameModifiers.base(),
        )
}
