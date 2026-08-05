package com.pokelike.idle.domain.model

/**
 * Stand des taeglichen Bonus.
 *
 * **Warum hier kein Kalendertag gespeichert wird.** Naheliegend waere, die
 * fortlaufende Tagesnummer der letzten Abholung zu sichern. Sie haengt aber an
 * der Zeitzone: Wer ueber mehrere Zeitzonen reist, haette einen gespeicherten
 * Wert, der in der neuen Zone einen anderen Tag bezeichnet. Gesichert wird
 * deshalb der Zeitpunkt, und der Tag wird bei jedem Vergleich neu bestimmt -
 * dieselbe Entscheidung wie bei den Quest-Ausgangswerten.
 *
 * @property streak Laenge der laufenden Serie in Tagen. Null bedeutet, dass
 *   noch nie abgeholt wurde oder die Serie gerissen ist.
 * @property longestStreak Bestwert. Wird nie zurueckgesetzt und ist damit die
 *   Groesse, an der Achievements haengen - eine gerissene Serie soll ein
 *   erreichtes Achievement nicht wieder entziehen.
 * @property lastClaimedAtMillis Zeitpunkt der letzten Abholung (Systemzeit),
 *   `null`, solange nie abgeholt wurde. Als eigenes Feld und nicht aus
 *   [streak] abgeleitet: Der Spieler kann Serienschutz besitzen, bevor er zum
 *   ersten Mal abholt.
 * @property protectionCharges Vorrat an Serienschutz. Jede Ladung ueberbrueckt
 *   einen ausgelassenen Tag.
 */
data class LoginState(
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val lastClaimedAtMillis: Long? = null,
    val protectionCharges: Int = 0,
) {

    init {
        require(streak >= 0) { "Negative Serie: $streak" }
        require(longestStreak >= 0) { "Negativer Bestwert: $longestStreak" }
        require(protectionCharges >= 0) { "Negativer Serienschutz: $protectionCharges" }
    }

    /** Ob ueberhaupt schon einmal abgeholt wurde. */
    val hasClaimed: Boolean get() = lastClaimedAtMillis != null

    /**
     * Vermerkt eine Abholung.
     *
     * Der Bestwert wird hier mitgefuehrt statt an anderer Stelle nachgezogen:
     * Waeren es zwei Aufrufe, wuerde einer irgendwann vergessen, und ein
     * Achievement auf die laengste Serie liesse sich nie mehr freischalten.
     *
     * @param streak Neue Laenge der Serie.
     * @param claimedAtMillis Zeitpunkt der Abholung (Systemzeit).
     * @param protectionSpent Verbrauchte Ladungen Serienschutz.
     */
    fun withClaim(
        streak: Int,
        claimedAtMillis: Long,
        protectionSpent: Int = 0,
    ): LoginState {
        require(streak >= 1) { "Eine Abholung ergibt mindestens Serie 1: $streak" }
        require(protectionSpent in 0..protectionCharges) {
            "Nicht vorhandener Serienschutz: $protectionSpent von $protectionCharges"
        }

        return copy(
            streak = streak,
            longestStreak = maxOf(longestStreak, streak),
            lastClaimedAtMillis = claimedAtMillis,
            protectionCharges = protectionCharges - protectionSpent,
        )
    }

    /** Legt eine Ladung Serienschutz in den Vorrat. */
    fun withProtectionCharge(): LoginState = copy(protectionCharges = protectionCharges + 1)

    companion object {

        /** Stand eines Spielers, der den Bonus noch nie abgeholt hat. */
        val EMPTY = LoginState()
    }
}
