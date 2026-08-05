package com.pokelike.idle.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein laufender Booster.
 *
 * Eine Zeile je Art - mehr kann es nicht geben, weil ein zweiter Start
 * denselben Eintrag verlaengert. Der Schluessel ist deshalb die Art selbst und
 * keine laufende Nummer.
 *
 * Booster laufen in Echtzeit ab und muessen den Neustart der App ueberdauern;
 * eine Restlaufzeit, die beim Beenden verschwindet, waere keine Echtzeit.
 * Gespeichert werden absolute Zeitpunkte der Systemuhr, denn die monotone Uhr
 * steht bei beendetem Prozess nicht zur Verfuegung.
 *
 * @property boosterId Stabiler Schluessel der Art.
 * @property startedAtMillis Beginn (Systemzeit).
 * @property endsAtMillis Ende (Systemzeit).
 */
@Entity(tableName = "active_boosters")
data class ActiveBoosterEntity(
    @PrimaryKey
    val boosterId: String,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
)
