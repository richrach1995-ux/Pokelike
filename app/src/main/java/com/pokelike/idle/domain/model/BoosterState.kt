package com.pokelike.idle.domain.model

/**
 * Ein laufender Booster.
 *
 * **Warum die Systemzeit und nicht die monotone Uhr.** Booster laufen in
 * Echtzeit ab, also auch bei beendetem Prozess - und die monotone Uhr steht
 * dann nicht zur Verfuegung. Dieselbe Zwangslage wie beim Offline-Fortschritt,
 * und dieselbe Antwort: Systemzeit mit einer Obergrenze.
 *
 * Die Obergrenze sitzt in [remainingAt]. Wer die Geraeteuhr zurueckstellt,
 * bekommt hoechstens die Laufzeit zurueck, die er ohnehin gekauft hat - nicht
 * die Zeitspanne, um die er zurueckgestellt hat. Der Missbrauch bleibt damit
 * auf "Booster neu starten" begrenzt, und genau das kann der Spieler auch
 * kaufen.
 *
 * @property type Welcher Booster.
 * @property startedAtMillis Beginn (Systemzeit).
 * @property endsAtMillis Ende (Systemzeit). Bei gestapelten Starts nach hinten
 *   verschoben.
 */
data class ActiveBooster(
    val type: BoosterType,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
) {

    init {
        require(endsAtMillis >= startedAtMillis) {
            "Booster endet vor seinem Beginn: $startedAtMillis bis $endsAtMillis"
        }
    }

    /** Gesamte gewaehrte Laufzeit, einschliesslich gestapelter Starts. */
    val grantedDurationMillis: Long get() = endsAtMillis - startedAtMillis

    /**
     * Verbleibende Laufzeit, nie mehr als die gewaehrte.
     *
     * Die Deckelung greift, wenn die Geraeteuhr zurueckgestellt wurde. Ohne sie
     * liesse sich ein Booster durch Verstellen der Uhr beliebig verlaengern.
     */
    fun remainingAt(nowMillis: Long): Long =
        (endsAtMillis - nowMillis).coerceIn(0L, grantedDurationMillis)

    /** Ob der Booster zu diesem Zeitpunkt noch wirkt. */
    fun isActiveAt(nowMillis: Long): Boolean = remainingAt(nowMillis) > 0L
}

/**
 * Alle laufenden Booster.
 *
 * Hoechstens ein Eintrag je Art: Ein zweiter Start derselben Art verlaengert
 * den bestehenden Eintrag, statt einen weiteren anzulegen. Andernfalls wuerden
 * sich die Faktoren derselben Art multiplizieren, und zwei "doppeltes
 * Einkommen" ergaeben den vierfachen Ertrag - eine Wirkung, die niemand
 * verkauft hat.
 *
 * @property active Laufende Booster je Art.
 */
data class BoosterState(
    val active: Map<BoosterType, ActiveBooster> = emptyMap(),
) {

    /** Ob gerade kein Booster laeuft. */
    val isEmpty: Boolean get() = active.isEmpty()

    /** Der laufende Booster einer Art, oder `null`. */
    operator fun get(type: BoosterType): ActiveBooster? = active[type]

    /** Alle zu diesem Zeitpunkt wirksamen Booster, nach Restlaufzeit sortiert. */
    fun activeAt(nowMillis: Long): List<ActiveBooster> = active.values
        .filter { it.isActiveAt(nowMillis) }
        .sortedBy { it.remainingAt(nowMillis) }

    /**
     * Startet einen Booster oder verlaengert ihn.
     *
     * Laeuft bereits einer derselben Art, wird dessen Ende um die Grundlaufzeit
     * nach hinten geschoben - bis zur Obergrenze aus
     * [BoosterType.maxStackedDurationMillis]. Der Beginn bleibt dabei stehen,
     * damit die Deckelung in [ActiveBooster.remainingAt] die gestapelte
     * Laufzeit kennt.
     */
    fun withStarted(type: BoosterType, nowMillis: Long): BoosterState {
        val running = active[type]?.takeIf { it.isActiveAt(nowMillis) }

        val started = if (running == null) {
            ActiveBooster(
                type = type,
                startedAtMillis = nowMillis,
                endsAtMillis = nowMillis + type.durationMillis,
            )
        } else {
            val extended = running.endsAtMillis + type.durationMillis
            val ceiling = nowMillis + type.maxStackedDurationMillis

            running.copy(endsAtMillis = extended.coerceAtMost(ceiling))
        }

        return copy(active = active + (type to started))
    }

    /**
     * Entfernt abgelaufene Booster.
     *
     * Notwendig und nicht bloss Aufraeumen: Die Modifikatoren werden aus dem
     * Spielstand berechnet und kennen die Uhrzeit nicht. Erst das Entfernen
     * macht das Ablaufen zu einer Aenderung des Spielstands - und damit zu
     * etwas, das die Modifikatoren, die Anzeige und der Autosave mitbekommen.
     */
    fun pruned(nowMillis: Long): BoosterState {
        val remaining = active.filterValues { it.isActiveAt(nowMillis) }
        return if (remaining.size == active.size) this else copy(active = remaining)
    }

    companion object {
        val EMPTY = BoosterState()
    }
}
