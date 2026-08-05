package com.pokelike.idle.domain.model

/**
 * Lebenslange Kennzahlen eines Spielers.
 *
 * Die Werte ueberdauern den Prestige-Reset. Das ist Absicht: Sie speisen den
 * Statistikbildschirm und die Fortschrittspruefung der Achievements, und ein
 * Achievement fuer "10.000 Klicks" waere wertlos, wenn der Zaehler bei jedem
 * Prestige von vorn begaenne.
 *
 * Zaehler fuer Ereignisse sind [Long] statt [BigNumber]: Ein Spieler erreicht
 * auch in tausend Stunden keine neun Trillionen Klicks. Betraege dagegen
 * wachsen exponentiell und brauchen den grossen Zahlentyp.
 *
 * @property totalClicks Alle je ausgefuehrten Klicks.
 * @property totalCriticalClicks Davon kritische Treffer. Zusammen mit
 *   [totalClicks] ergibt sich die tatsaechlich erreichte Kritrate - der
 *   verlaesslichste Weg, die Balance der Kritwerte zu pruefen.
 * @property lifetimeEarned Summe aller je erhaltenen Betraege je Ressource.
 * @property lifetimeSpent Summe aller je ausgegebenen Betraege je Ressource.
 * @property totalPlayTimeMillis Aktive Spielzeit, ohne Hintergrundzeit.
 * @property sessionCount Anzahl der Sitzungen. Grundlage der Retentionsanalyse.
 * @property prestigeCount Abgeschlossene Prestige-Durchlaeufe.
 * @property totalBuildingsPurchased Je gekaufte Gebaeude.
 *
 *   Bewusst ein eigener, nur steigender Zaehler und nicht der aktuelle
 *   Bestand: Der faellt beim Prestige-Reset auf null zurueck. Ein Ziel wie
 *   "kaufe hundert Gebaeude" waere damit nach jedem Reset wieder offen.
 */
data class GameStatistics(
    val totalClicks: Long = 0L,
    val totalCriticalClicks: Long = 0L,
    val lifetimeEarned: ResourceBundle = ResourceBundle.EMPTY,
    val lifetimeSpent: ResourceBundle = ResourceBundle.EMPTY,
    val totalPlayTimeMillis: Long = 0L,
    val sessionCount: Int = 0,
    val prestigeCount: Int = 0,
    val totalBuildingsPurchased: Long = 0L,
) {

    /**
     * Anteil kritischer Treffer an allen Klicks, zwischen 0 und 1.
     *
     * Liefert 0, solange noch nicht geklickt wurde - eine Division durch null
     * waere hier der naheliegende Fehler.
     */
    val criticalRate: Double
        get() = if (totalClicks == 0L) 0.0 else totalCriticalClicks.toDouble() / totalClicks

    /** Verbucht einen Klick. */
    fun withClick(wasCritical: Boolean): GameStatistics = copy(
        totalClicks = totalClicks + 1L,
        totalCriticalClicks = totalCriticalClicks + if (wasCritical) 1L else 0L,
    )

    /** Verbucht erhaltene Ressourcen. */
    fun withEarned(bundle: ResourceBundle): GameStatistics =
        if (bundle.isEmpty) this else copy(lifetimeEarned = lifetimeEarned + bundle)

    /** Verbucht ausgegebene Ressourcen. */
    fun withSpent(bundle: ResourceBundle): GameStatistics =
        if (bundle.isEmpty) this else copy(lifetimeSpent = lifetimeSpent + bundle)

    /** Verbucht gekaufte Gebaeude. */
    fun withBuildingsPurchased(count: Int): GameStatistics {
        require(count >= 0) { "Negative Anzahl: $count" }
        return if (count == 0) {
            this
        } else {
            copy(totalBuildingsPurchased = totalBuildingsPurchased + count)
        }
    }

    /** Schreibt die aktive Spielzeit fort. */
    fun withPlayTime(additionalMillis: Long): GameStatistics {
        require(additionalMillis >= 0L) { "Negative Spielzeit: $additionalMillis" }
        return if (additionalMillis == 0L) {
            this
        } else {
            copy(totalPlayTimeMillis = totalPlayTimeMillis + additionalMillis)
        }
    }
}
