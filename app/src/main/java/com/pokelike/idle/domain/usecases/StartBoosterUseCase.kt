package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Startet einen Booster, ohne etwas abzubuchen.
 *
 * Getrennt vom Kauf, weil ein Booster auf mehreren Wegen anfaellt: gekauft mit
 * Diamanten, verdient durch ein Werbevideo, gewaehrt von einem Event oder aus
 * einem Angebot. Alle diese Wege unterscheiden sich ausschliesslich darin, was
 * vorher passiert; das Starten selbst ist immer dasselbe.
 *
 * Laeuft bereits ein Booster derselben Art, wird dessen Laufzeit verlaengert -
 * siehe [com.pokelike.idle.domain.model.BoosterState.withStarted]. Ihn zu
 * verwerfen waere die schlechtere Antwort: Der Spieler haette bezahlt und
 * nichts bekommen.
 */
@Singleton
class StartBoosterUseCase @Inject constructor() {

    /**
     * @param nowMillis Systemzeit. Booster laufen in Echtzeit ab, deshalb
     *   ausdruecklich nicht die monotone Uhr - diese steht bei beendetem
     *   Prozess nicht zur Verfuegung.
     */
    operator fun invoke(state: GameState, type: BoosterType, nowMillis: Long): GameState =
        state.copy(boosters = state.boosters.withStarted(type, nowMillis))
}
