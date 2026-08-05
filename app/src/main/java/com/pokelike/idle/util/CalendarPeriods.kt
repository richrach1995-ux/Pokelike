package com.pokelike.idle.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Bestimmt Kalendertage und -wochen aus einem Zeitstempel.
 *
 * Notwendig fuer taegliche und woechentliche Quests: Ein fester Abstand von 24
 * Stunden waere nicht dasselbe wie ein Kalendertag. Bei Sommerzeitumstellung
 * hat ein Tag 23 oder 25 Stunden, und ein Spieler, der jeden Abend um dieselbe
 * Zeit spielt, bekaeme mit einem festen Abstand mal zwei Zuruecksetzungen an
 * einem Tag und mal keine.
 *
 * Die Zeitzone ist Parameter statt fest verdrahtet - allein schon, damit Tests
 * nicht davon abhaengen, in welcher Zone sie laufen.
 */
object CalendarPeriods {

    /**
     * Fortlaufende Nummer des Kalendertags.
     *
     * Tage seit dem 1. Januar 1970 in der angegebenen Zone. Monoton steigend,
     * dadurch laesst sich mit einem einfachen Vergleich feststellen, ob die Uhr
     * vor- oder zurueckgestellt wurde.
     */
    fun dayIndex(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    /**
     * Fortlaufende Nummer der Kalenderwoche.
     *
     * Bestimmt ueber den Montag der jeweiligen Woche statt ueber eine Division
     * durch sieben: Letztere haenge davon ab, auf welchen Wochentag der
     * 1. Januar 1970 fiel, und die Woche begaenne an einem willkuerlichen Tag.
     */
    fun weekIndex(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(millis)
            .atZone(zone)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toEpochDay()
}
