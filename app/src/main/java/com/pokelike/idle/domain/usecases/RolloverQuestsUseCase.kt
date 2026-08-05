package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.util.CalendarPeriods
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Setzt abgelaufene Quest-Zeitraeume zurueck.
 *
 * Gerechnet wird in Kalendertagen und -wochen, nicht in festen Abstaenden von
 * 24 beziehungsweise 168 Stunden. Bei Sommerzeitumstellung hat ein Tag 23 oder
 * 25 Stunden; mit einem festen Abstand bekaeme ein Spieler, der jeden Abend zur
 * selben Zeit spielt, mal zwei Zuruecksetzungen an einem Tag und mal keine.
 *
 * **Zur Manipulation der Geraeteuhr.** Der Kalendertag laesst sich nur ueber
 * die Systemzeit bestimmen; eine monotone Uhr kennt keine Tage. Zwei Regeln
 * begrenzen den Schaden:
 *
 * - Ein Zurueckstellen der Uhr loest keinen Reset aus. Der Zeitraum laeuft
 *   weiter, als waere nichts geschehen.
 * - Ein Vorstellen loest genau einen Reset aus - nicht mehr, als das Abwarten
 *   des Tageswechsels ohnehin gebracht haette.
 *
 * Wiederholtes Vorstellen bleibt damit ausnutzbar. Das ist auf dem Geraet nicht
 * abschliessend loesbar, denn der Client gehoert dem Nutzer. Verlaesslich waere
 * nur ein Serverzeitstempel; die Architektur ist darauf vorbereitet, weil der
 * Zeitpunkt hier hereingereicht und nicht selbst abgefragt wird.
 */
@Singleton
class RolloverQuestsUseCase @Inject constructor() {

    operator fun invoke(
        state: GameState,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): GameState {
        var updated = state

        RESETTING_PERIODS.forEach { period ->
            if (needsReset(updated, period, nowMillis, zone)) {
                updated = updated.copy(
                    quests = updated.quests.withPeriodReset(period, updated, nowMillis),
                )
            }
        }

        return updated
    }

    private fun needsReset(
        state: GameState,
        period: QuestPeriod,
        nowMillis: Long,
        zone: ZoneId,
    ): Boolean {
        // Noch kein Ausgangswert: Der Zeitraum beginnt jetzt.
        val baseline = state.quests.baselines[period] ?: return true

        // Uhr zurueckgestellt. Ein Reset waere hier ein Geschenk fuer den
        // Versuch, das System auszutricksen.
        if (nowMillis < baseline.startedAtMillis) return false

        return periodIndex(period, nowMillis, zone) !=
            periodIndex(period, baseline.startedAtMillis, zone)
    }

    private fun periodIndex(period: QuestPeriod, millis: Long, zone: ZoneId): Long =
        when (period) {
            QuestPeriod.DAILY -> CalendarPeriods.dayIndex(millis, zone)
            QuestPeriod.WEEKLY -> CalendarPeriods.weekIndex(millis, zone)
            // Wird nie zurueckgesetzt; der Zweig ist nur da, weil der Compiler
            // die Vollstaendigkeit einfordert.
            QuestPeriod.LIFETIME -> 0L
        }

    private companion object {
        /** Lebenszeitquests laufen ueber die gesamte Spielzeit. */
        val RESETTING_PERIODS = listOf(QuestPeriod.DAILY, QuestPeriod.WEEKLY)
    }
}
