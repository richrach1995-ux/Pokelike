package com.pokelike.idle.domain.model

/**
 * Ausgangswerte eines Quest-Zeitraums.
 *
 * Der Kern des Zuruecksetzens: Statt je Quest einen eigenen Zaehler zu fuehren,
 * wird beim Beginn eines Zeitraums der Stand aller Messgroessen festgehalten.
 * Der Fortschritt ist dann die Differenz zum aktuellen Stand.
 *
 * Das hat drei Vorteile gegenueber eigenen Zaehlern: Eine neue Quest braucht
 * keinen neuen gespeicherten Wert, das Zuruecksetzen ist ein einziger Vorgang
 * statt eines Durchlaufs ueber alle Quests, und ein Fehler kann nicht dazu
 * fuehren, dass zwei Quests auf derselben Handlung unterschiedlich zaehlen.
 *
 * @property startedAtMillis Beginn des Zeitraums (Systemzeit).
 * @property values Stand aller Messgroessen zu diesem Zeitpunkt.
 */
data class QuestBaseline(
    val startedAtMillis: Long,
    val values: Map<QuestMetric, BigNumber>,
) {

    /** Ausgangswert einer Groesse. Nicht erfasste Groessen stehen auf null. */
    operator fun get(metric: QuestMetric): BigNumber = values[metric] ?: BigNumber.ZERO

    companion object {

        /** Haelt den aktuellen Stand aller Messgroessen fest. */
        fun from(state: GameState, nowMillis: Long): QuestBaseline = QuestBaseline(
            startedAtMillis = nowMillis,
            values = QuestMetric.entries.associateWith { metric -> metric.readFrom(state) },
        )
    }
}

/**
 * Stand des Quest-Systems.
 *
 * @property baselines Ausgangswerte je Zeitraum.
 * @property claimed Bereits abgeholte Quests. Wird beim Zuruecksetzen eines
 *   Zeitraums fuer dessen Quests geleert.
 */
data class QuestState(
    val baselines: Map<QuestPeriod, QuestBaseline> = emptyMap(),
    val claimed: Set<QuestType> = emptySet(),
) {

    /**
     * Fortschritt einer Quest innerhalb ihres Zeitraums.
     *
     * Bei [QuestPeriod.LIFETIME] gibt es keinen Ausgangswert; dort zaehlt der
     * Gesamtstand. Bei den uebrigen ist es die Differenz zum Beginn des
     * Zeitraums.
     *
     * Das Ergebnis wird bei null abgeschnitten. Negativ kann es werden, wenn
     * eine Messgroesse doch einmal faellt - etwa nach einer Balancing-Aenderung
     * oder einem kuenftigen System, das Upgrades zurueckerstattet.
     */
    fun progressOf(quest: QuestType, state: GameState): BigNumber {
        val current = quest.metric.readFrom(state)
        if (quest.period == QuestPeriod.LIFETIME) return current

        val baseline = baselines[quest.period] ?: return BigNumber.ZERO
        return (current - baseline[quest.metric]).coerceAtLeast(BigNumber.ZERO)
    }

    /** Ob das Ziel erreicht ist. */
    fun isComplete(quest: QuestType, state: GameState): Boolean =
        progressOf(quest, state) >= quest.target

    /** Ob die Belohnung bereits abgeholt wurde. */
    fun isClaimed(quest: QuestType): Boolean = quest in claimed

    /** Ob die Belohnung jetzt abgeholt werden kann. */
    fun isClaimable(quest: QuestType, state: GameState): Boolean =
        !isClaimed(quest) && isComplete(quest, state)

    /** Vermerkt eine Quest als abgeholt. */
    fun withClaimed(quest: QuestType): QuestState =
        if (quest in claimed) this else copy(claimed = claimed + quest)

    /**
     * Beginnt einen Zeitraum neu.
     *
     * Setzt den Ausgangswert auf den aktuellen Stand und gibt die Quests dieses
     * Zeitraums wieder frei. Quests anderer Zeitraeume bleiben unberuehrt - ein
     * Tageswechsel darf eine halb fertige Wochenquest nicht zuruecksetzen.
     */
    fun withPeriodReset(
        period: QuestPeriod,
        state: GameState,
        nowMillis: Long,
    ): QuestState = copy(
        baselines = baselines + (period to QuestBaseline.from(state, nowMillis)),
        claimed = claimed.filterNot { it.period == period }.toSet(),
    )

    companion object {
        val EMPTY = QuestState()
    }
}
