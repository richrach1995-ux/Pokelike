package com.pokelike.idle.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Wartezeit einer Videostelle.
 *
 * Eine Zeile je Stelle. Gespeichert wird der Zeitpunkt, ab dem das naechste
 * Video bereitsteht, und nicht die verbleibende Dauer: Eine Dauer muesste bei
 * jedem Speichern fortgeschrieben werden, und ein vergessener Pfad liesse sie
 * unveraendert stehen.
 *
 * @property placementId Stabiler Schluessel der Stelle.
 * @property availableAtMillis Fruehester Zeitpunkt (Systemzeit).
 */
@Entity(tableName = "ad_cooldowns")
data class AdCooldownEntity(
    @PrimaryKey
    val placementId: String,
    val availableAtMillis: Long,
)
