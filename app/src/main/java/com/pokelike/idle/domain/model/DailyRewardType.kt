package com.pokelike.idle.domain.model

/**
 * Die Tage des Belohnungszyklus.
 *
 * Wie Gebaeude, Upgrades und Ziele rein deklarativ: Ein Tag besteht aus seiner
 * Nummer, einem Muenzanteil und einem festen Zuschlag. Balancing heisst hier,
 * Zahlen in dieser Datei zu aendern, nicht Code.
 *
 * **Warum der Muenzanteil in Minuten angegeben wird.** Ein fester Muenzbetrag
 * waere im Genre wertlos, sobald der Spieler ein paar Gebaeude besitzt: Was am
 * ersten Tag grosszuegig wirkt, ist nach drei Stunden weniger als eine Sekunde
 * Einkommen. Angegeben wird deshalb eine Zeitspanne, und der tatsaechliche
 * Betrag ergibt sich aus dem Einkommen des Spielers. Der Bonus behaelt dadurch
 * ueber die gesamte Spieldauer denselben gefuehlten Wert.
 *
 * Der Diamantenzuschlag bleibt dagegen fest. Diamanten sind Premiumwaehrung;
 * ihr Wert haengt an den Preisen im Angebot, nicht am Fortschritt.
 *
 * @property day Nummer im Zyklus, beginnend bei eins.
 * @property idleMinutes Muenzen in Hoehe des Ertrags dieser Zeitspanne.
 * @property bonus Fester Zuschlag zusaetzlich zu den Muenzen.
 */
enum class DailyRewardType(
    val day: Int,
    val idleMinutes: Int,
    val bonus: ResourceBundle,
) {

    DAY_1(day = 1, idleMinutes = 15, bonus = ResourceBundle.EMPTY),
    DAY_2(day = 2, idleMinutes = 30, bonus = diamonds(3)),
    DAY_3(day = 3, idleMinutes = 45, bonus = ResourceBundle.EMPTY),
    DAY_4(day = 4, idleMinutes = 60, bonus = diamonds(7)),
    DAY_5(day = 5, idleMinutes = 90, bonus = ResourceBundle.EMPTY),
    DAY_6(day = 6, idleMinutes = 120, bonus = diamonds(10)),

    /**
     * Der Abschlusstag.
     *
     * Deutlich groesser als die uebrigen, und das ist der eigentliche Zweck
     * des Zyklus: Wer am fuenften Tag ueberlegt aufzuhoeren, hat ein
     * greifbares Ziel zwei Tage voraus. Ein gleichmaessig verteilter Zyklus
     * haette diesen Zugeffekt nicht.
     */
    DAY_7(day = 7, idleMinutes = 240, bonus = diamonds(30));

    /** Ob dieser Tag den Zyklus abschliesst. Die Oberflaeche hebt ihn hervor. */
    val isFinalDay: Boolean get() = day == entries.size

    companion object {

        /**
         * Laenge des Zyklus in Tagen.
         *
         * Ergibt sich aus der Aufzaehlung selbst und steht bewusst nicht in
         * `GameConfig`: Ein dort getrennt gefuehrter Wert koennte von der Zahl
         * der Tage abweichen, und der Zyklus liefe dann ueber einen Tag, den es
         * nicht gibt. Wer den Zyklus verlaengern will, ergaenzt hier einen
         * Eintrag - mehr ist nicht noetig.
         */
        val cycleLength: Int get() = entries.size

        /**
         * Der Tag, den eine Serie dieser Laenge erreicht.
         *
         * Der Zyklus laeuft endlos weiter: Serie 8 fuehrt wieder zu Tag 1,
         * Serie 15 ebenfalls. Die Serie selbst bleibt dabei erhalten - sie ist
         * die Groesse, an der Achievements und die Anzeige haengen.
         *
         * @param streak Laenge der Serie einschliesslich des Tages, der gerade
         *   abgeholt wird. Werte unter eins ergeben den ersten Tag.
         */
        fun forStreak(streak: Int): DailyRewardType {
            if (streak <= 1) return DAY_1
            val index = (streak - 1) % entries.size
            return entries[index]
        }
    }
}

/**
 * Kurzform fuer einen Diamantenzuschlag.
 *
 * Nur innerhalb dieser Datei sichtbar: Sie existiert allein, damit die
 * Aufzaehlung oben lesbar bleibt.
 */
private fun diamonds(amount: Long): ResourceBundle =
    ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(amount))
