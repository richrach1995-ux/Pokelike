package com.pokelike.idle.domain.model

/**
 * Eine Menge von Ressourcenbetraegen.
 *
 * Dient gleichermassen als Preis, als Belohnung und als Statistikposten - es
 * ist immer dieselbe Struktur: welche Ressource, wie viel davon.
 *
 * Alle Betraege sind garantiert nicht negativ. Diese Zusicherung ist eine
 * Sicherheitsmassnahme, keine Formalie: Ein Preis mit negativem Betrag wuerde
 * beim Bezahlen Guthaben gutschreiben statt abziehen. Wird ein solcher Wert je
 * aus einer Remote-Config oder einem manipulierten Spielstand gelesen, faellt
 * er hier auf, statt sich als Exploit auszuwirken.
 *
 * Nulleintraege werden beim Erzeugen entfernt, damit zwei inhaltlich gleiche
 * Buendel auch als gleich gelten.
 */
class ResourceBundle private constructor(
    /** Betraege je Ressource. Enthaelt keine Null- und keine negativen Werte. */
    val amounts: Map<ResourceType, BigNumber>,
) {

    val isEmpty: Boolean get() = amounts.isEmpty()

    /** Betrag der Ressource, oder null, wenn sie nicht enthalten ist. */
    operator fun get(type: ResourceType): BigNumber = amounts[type] ?: BigNumber.ZERO

    operator fun contains(type: ResourceType): Boolean = amounts.containsKey(type)

    /**
     * Vereinigt zwei Buendel und addiert dabei gleiche Ressourcen.
     *
     * Gebraucht, um mehrere Belohnungsquellen zu einem einzigen Popup
     * zusammenzufassen - etwa wenn eine Quest und ein Achievement im selben
     * Moment erfuellt werden.
     */
    operator fun plus(other: ResourceBundle): ResourceBundle {
        if (other.isEmpty) return this
        if (isEmpty) return other

        val merged = amounts.toMutableMap()
        other.amounts.forEach { (type, amount) ->
            merged[type] = (merged[type] ?: BigNumber.ZERO) + amount
        }
        return ResourceBundle(merged)
    }

    /** Skaliert alle Betraege, etwa fuer einen Belohnungsmultiplikator. */
    fun scaledBy(factor: Double): ResourceBundle {
        require(factor >= 0.0) { "Negativer Faktor: $factor" }
        if (factor == 1.0 || isEmpty) return this
        if (factor == 0.0) return EMPTY

        return ResourceBundle(amounts.mapValues { (_, amount) -> amount * factor })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceBundle) return false
        return amounts == other.amounts
    }

    override fun hashCode(): Int = amounts.hashCode()

    override fun toString(): String =
        amounts.entries.joinToString(prefix = "ResourceBundle(", postfix = ")") { (type, amount) ->
            "${type.id}=$amount"
        }

    companion object {

        val EMPTY: ResourceBundle = ResourceBundle(emptyMap())

        /**
         * Erzeugt ein Buendel und prueft dabei die Betraege.
         *
         * @throws IllegalArgumentException bei negativen Betraegen.
         */
        fun of(amounts: Map<ResourceType, BigNumber>): ResourceBundle {
            val sanitized = buildMap {
                amounts.forEach { (type, amount) ->
                    require(!amount.isNegative) {
                        "Negativer Betrag fuer ${type.id}: $amount"
                    }
                    if (!amount.isZero) put(type, amount)
                }
            }
            return if (sanitized.isEmpty()) EMPTY else ResourceBundle(sanitized)
        }

        /** Bequemer Weg fuer feste Buendel im Code. */
        fun of(vararg entries: Pair<ResourceType, BigNumber>): ResourceBundle =
            of(entries.toMap())

        /** Buendel mit einer einzigen Ressource. */
        fun single(type: ResourceType, amount: BigNumber): ResourceBundle =
            of(mapOf(type to amount))
    }
}
