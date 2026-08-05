package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.DailyRewardType
import com.pokelike.idle.domain.model.GameModifiers
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.util.CalendarPeriods
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ein abholbereiter Tagesbonus.
 *
 * Wird nur erzeugt, wenn tatsaechlich etwas abzuholen ist - andernfalls
 * liefert [EvaluateDailyRewardUseCase] `null`. Ein Statusobjekt mit einem
 * Feld "ist abholbar" waere die schlechtere Form: Die Oberflaeche muesste es
 * jedes Mal pruefen, und beim ersten vergessenen Test erschiene der Dialog
 * ohne Inhalt.
 *
 * @property day Tag des Zyklus, der jetzt ansteht.
 * @property streakAfterClaim Laenge der Serie nach der Abholung.
 * @property reward Was die Abholung einbringt, bereits ausgerechnet.
 * @property protectionCharges Vorrat an Serienschutz vor der Abholung.
 * @property protectionUsed Ladungen, die die Abholung verbraucht. Groesser als
 *   null nur, wenn Tage ausgelassen wurden.
 * @property streakBroken Ob die Serie trotz allem neu beginnt. Der Spieler
 *   soll das erfahren, bevor er abholt, und nicht danach raten muessen.
 */
data class DailyRewardStatus(
    val day: DailyRewardType,
    val streakAfterClaim: Int,
    val reward: ResourceBundle,
    val protectionCharges: Int,
    val protectionUsed: Int,
    val streakBroken: Boolean,
)

/**
 * Bestimmt, ob und was der Spieler heute abholen kann.
 *
 * Getrennt vom Abholen selbst, weil beides zu unterschiedlichen Zeitpunkten
 * gebraucht wird: Die Oberflaeche muss den anstehenden Tag anzeigen, bevor der
 * Spieler tippt. Waere die Berechnung Teil der Abholung, muesste die Anzeige
 * sie nachbauen - und zwei Kopien derselben Regel laufen frueher oder spaeter
 * auseinander.
 *
 * **Zur Manipulation der Geraeteuhr.** Dieselben zwei Regeln wie bei den
 * Quests: Ein Zurueckstellen der Uhr bringt nichts, weil vor dem gespeicherten
 * Zeitpunkt nichts abgeholt werden kann. Ein Vorstellen bringt genau eine
 * zusaetzliche Abholung - nicht mehr, als das Abwarten des Tageswechsels
 * ohnehin gebracht haette. Wiederholtes Vorstellen bleibt auf dem Geraet
 * ausnutzbar; verlaesslich waere nur ein Serverzeitstempel, und der Zeitpunkt
 * wird deshalb hereingereicht statt hier abgefragt.
 */
@Singleton
class EvaluateDailyRewardUseCase @Inject constructor(
    private val calculateIncome: CalculateIncomeUseCase,
) {

    /**
     * @param modifiers Wirksame Werte des Spielstands. Notwendig, weil der
     *   Muenzanteil aus dem Einkommen des Spielers folgt.
     * @return `null`, wenn heute nichts abzuholen ist.
     */
    operator fun invoke(
        state: GameState,
        modifiers: GameModifiers,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DailyRewardStatus? {
        val login = state.login
        val lastClaimed = login.lastClaimedAtMillis

        val transition = when {
            // Erste Abholung ueberhaupt.
            lastClaimed == null -> Transition(streak = 1, protectionUsed = 0, broken = false)

            // Uhr zurueckgestellt. Kein Anspruch - andernfalls waere das
            // Zurueckstellen der Uhr ein zweiter Bonus am selben Tag.
            nowMillis < lastClaimed -> return null

            else -> {
                val daysPassed = CalendarPeriods.dayIndex(nowMillis, zone) -
                    CalendarPeriods.dayIndex(lastClaimed, zone)

                // Heute bereits abgeholt.
                if (daysPassed <= 0L) return null

                transitionFor(daysPassed, login.streak, login.protectionCharges)
            }
        }

        val day = DailyRewardType.forStreak(transition.streak)

        return DailyRewardStatus(
            day = day,
            streakAfterClaim = transition.streak,
            reward = rewardFor(day, state, modifiers),
            protectionCharges = login.protectionCharges,
            protectionUsed = transition.protectionUsed,
            streakBroken = transition.broken,
        )
    }

    /**
     * Bestimmt Serie und Schutzverbrauch aus der Zahl vergangener Tage.
     *
     * Ein Tag bedeutet: nahtlos weiter. Bei mehreren Tagen liegen dazwischen
     * ausgelassene Tage, und je ausgelassenem Tag wird eine Ladung
     * Serienschutz verbraucht. Reicht der Vorrat nicht, beginnt die Serie neu -
     * der Vorrat bleibt dann unberuehrt, denn eine halb ueberbrueckte Luecke
     * haette dem Spieler nichts gebracht.
     */
    private fun transitionFor(daysPassed: Long, streak: Int, charges: Int): Transition {
        val missedDays = daysPassed - 1L

        return when {
            missedDays == 0L -> Transition(
                streak = streak + 1,
                protectionUsed = 0,
                broken = false,
            )

            missedDays <= charges.toLong() -> Transition(
                streak = streak + 1,
                protectionUsed = missedDays.toInt(),
                broken = false,
            )

            else -> Transition(streak = 1, protectionUsed = 0, broken = true)
        }
    }

    /**
     * Rechnet den Muenzanteil in einen Betrag um.
     *
     * Grundlage ist das Leerlaufeinkommen, mindestens aber der erwartete
     * Ertrag eines Klicks je Sekunde. Ohne diese Untergrenze bekaeme ein
     * Spieler ohne Gebaeude null Muenzen - der Bonus wuerde ihn also genau in
     * der Phase leer ausgehen lassen, in der er am meisten hilft. Ein Klick pro
     * Sekunde ist dabei kein gegriffener Wert, sondern ungefaehr das, was ein
     * Spieler ohne Gebaeude tatsaechlich erwirtschaftet.
     */
    private fun rewardFor(
        day: DailyRewardType,
        state: GameState,
        modifiers: GameModifiers,
    ): ResourceBundle {
        if (day.idleMinutes <= 0) return day.bonus

        val incomePerSecond = calculateIncome(
            buildings = state.buildings,
            multiplier = modifiers.incomeMultiplier,
            perBuildingMultipliers = modifiers.buildingIncomeMultipliers,
        )
        val perSecond = incomePerSecond.coerceAtLeast(modifiers.click.expectedValuePerClick())
        val coins = perSecond * BigNumber.of(day.idleMinutes.toLong() * SECONDS_PER_MINUTE)

        return day.bonus + ResourceBundle.single(ResourceType.COINS, coins)
    }

    /** Ergebnis der Serienberechnung, bevor daraus ein Status wird. */
    private data class Transition(
        val streak: Int,
        val protectionUsed: Int,
        val broken: Boolean,
    )

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
    }
}
