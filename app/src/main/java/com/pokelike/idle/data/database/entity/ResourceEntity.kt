package com.pokelike.idle.data.database.entity

import androidx.room.Entity

/**
 * Verwendungszweck einer gespeicherten Ressourcenzeile.
 *
 * Kontostand und Lebenszeitsummen haben dieselbe Struktur - Ressource und
 * Betrag. Sie in einer Tabelle zu fuehren und ueber diesen Schluessel zu
 * trennen, spart zwei nahezu identische Tabellen und macht jede weitere
 * Sammlung (etwa Ertrag der laufenden Sitzung) zu einem zusaetzlichen
 * Enum-Eintrag statt zu einer Datenbankmigration.
 */
enum class ResourceBucket(val id: String) {
    /** Aktueller Kontostand. */
    CURRENT("current"),

    /** Lebenslange Summe aller Einnahmen. */
    LIFETIME_EARNED("lifetime_earned"),

    /** Lebenslange Summe aller Ausgaben. */
    LIFETIME_SPENT("lifetime_spent");

    companion object {
        private val byId = entries.associateBy { it.id }

        /** Liefert `null` bei unbekanntem Schluessel, statt zu scheitern. */
        fun fromId(id: String): ResourceBucket? = byId[id]
    }
}

/**
 * Ein einzelner Ressourcenbetrag.
 *
 * Mantisse und Exponent werden getrennt gespeichert statt als Zeichenkette:
 * So bleibt der Wert in SQL vergleichbar und sortierbar - noetig fuer spaetere
 * Statistikabfragen -, und es entfaellt jedes Parsen beim Laden.
 *
 * Der zusammengesetzte Primaerschluessel aus Verwendungszweck und Ressource
 * stellt sicher, dass jede Kombination genau einmal vorkommt.
 *
 * @property bucket Schluessel aus [ResourceBucket].
 * @property resourceId Schluessel aus
 *   [com.pokelike.idle.domain.model.ResourceType].
 */
@Entity(tableName = "resources", primaryKeys = ["bucket", "resourceId"])
data class ResourceEntity(
    val bucket: String,
    val resourceId: String,
    val mantissa: Double,
    val exponent: Int,
)
