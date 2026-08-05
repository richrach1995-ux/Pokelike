package com.pokelike.idle.domain.model

/**
 * Wartezeiten der Belohnungsvideos.
 *
 * **Warum das im Spielstand steht und nicht im Arbeitsspeicher.** Eine
 * Wartezeit, die den Neustart der App nicht ueberdauert, ist keine: Ein
 * Spieler muesste die App nur schliessen und wieder oeffnen, um sich das
 * naechste Video anzusehen - und das ist bei einem Spiel, das ohnehin
 * staendig in den Hintergrund geht, kein Umweg, sondern der Normalfall.
 *
 * Gespeichert wird der Zeitpunkt, ab dem das naechste Video bereitsteht.
 * Dieselbe Ueberlegung wie bei den Boostern: Eine verbleibende Dauer muesste
 * bei jedem Speichern fortgeschrieben werden, und ein vergessener Pfad liesse
 * sie stehen.
 *
 * @property availableAtMillis Fruehester Zeitpunkt (Systemzeit) je Stelle.
 */
data class AdState(
    val availableAtMillis: Map<RewardedAdPlacement, Long> = emptyMap(),
) {

    /** Ob gerade keine Wartezeit laeuft. */
    val isEmpty: Boolean get() = availableAtMillis.isEmpty()

    /**
     * Verbleibende Wartezeit, nie mehr als die Wartezeit der Stelle.
     *
     * Die Deckelung greift, wenn die Geraeteuhr vorgestellt und danach wieder
     * zurueckgestellt wurde. Ohne sie liesse sich das Angebot durch Verstellen
     * der Uhr auf Monate sperren - was zwar niemand absichtlich tut, aber nach
     * einem Zeitzonenwechsel oder einer Uhrkorrektur durchaus vorkommt und den
     * Spieler ohne erkennbaren Grund aussperren wuerde.
     */
    fun remainingAt(placement: RewardedAdPlacement, nowMillis: Long): Long {
        val availableAt = availableAtMillis[placement] ?: return 0L
        return (availableAt - nowMillis).coerceIn(0L, placement.cooldownMillis)
    }

    /** Ob an dieser Stelle jetzt ein Video angeboten werden darf. */
    fun isReadyAt(placement: RewardedAdPlacement, nowMillis: Long): Boolean =
        remainingAt(placement, nowMillis) == 0L

    /** Startet die Wartezeit einer Stelle. */
    fun withCooldownStarted(placement: RewardedAdPlacement, nowMillis: Long): AdState =
        copy(
            availableAtMillis = availableAtMillis +
                (placement to nowMillis + placement.cooldownMillis),
        )

    /**
     * Entfernt abgelaufene Wartezeiten.
     *
     * Haelt den Spielstand klein und macht das Ablaufen - wie bei den
     * Boostern - zu einer Aenderung des Spielstands, die Anzeige und Autosave
     * gleichermassen mitbekommen.
     */
    fun pruned(nowMillis: Long): AdState {
        val remaining = availableAtMillis.filterKeys { placement ->
            !isReadyAt(placement, nowMillis)
        }
        return if (remaining.size == availableAtMillis.size) this else copy(remaining)
    }

    companion object {
        val EMPTY = AdState()
    }
}
