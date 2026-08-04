package com.pokelike.idle.domain.model

/**
 * Der Kontostand des Spielers ueber alle Ressourcen.
 *
 * Unveraenderlich: Jede Buchung liefert einen neuen Pool. Das ist bei einem
 * Spielstand, der zugleich gespeichert, angezeigt und weitergerechnet wird,
 * kein Luxus - es schliesst aus, dass eine Buchung mitten im Speichervorgang
 * einen halb aktualisierten Zustand hinterlaesst.
 *
 * Betraege sind immer nicht negativ. Ein Konto kann nicht ins Minus geraten;
 * Abbuchungen laufen ausschliesslich ueber [spend], das vorher prueft.
 */
class ResourcePool private constructor(
    private val amounts: Map<ResourceType, BigNumber>,
) {

    /** Kontostand einer Ressource. Nicht gefuehrte Ressourcen stehen auf null. */
    operator fun get(type: ResourceType): BigNumber = amounts[type] ?: BigNumber.ZERO

    /** Ob ueberhaupt etwas von dieser Ressource vorhanden ist. */
    fun has(type: ResourceType): Boolean = this[type].isPositive

    /**
     * Bucht einen Betrag auf eine Ressource.
     *
     * @throws IllegalArgumentException bei negativem Betrag. Eine Gutschrift
     *   mit negativem Vorzeichen waere eine verdeckte Abbuchung, die die
     *   Pruefung in [spend] umgeht.
     */
    fun grant(type: ResourceType, amount: BigNumber): ResourcePool {
        require(!amount.isNegative) { "Negative Gutschrift fuer ${type.id}: $amount" }
        if (amount.isZero) return this
        return ResourcePool(amounts + (type to (this[type] + amount)))
    }

    /** Bucht ein ganzes Buendel gut, etwa eine Questbelohnung. */
    fun grant(bundle: ResourceBundle): ResourcePool {
        if (bundle.isEmpty) return this
        val updated = amounts.toMutableMap()
        bundle.amounts.forEach { (type, amount) ->
            updated[type] = (updated[type] ?: BigNumber.ZERO) + amount
        }
        return withoutZeroEntries(updated)
    }

    /** Ob der Kontostand fuer diesen Preis reicht. */
    fun canAfford(cost: ResourceBundle): Boolean =
        cost.amounts.all { (type, amount) -> this[type] >= amount }

    /**
     * Bucht einen Preis ab.
     *
     * Liefert `null`, wenn der Kontostand nicht reicht - der Pool bleibt dann
     * unveraendert.
     *
     * Pruefung und Abbuchung liegen bewusst in einer Operation. Waeren sie
     * getrennt, koennte zwischen `canAfford` und der Abbuchung ein anderer
     * Vorgang das Guthaben verringern, und der Kontostand geriete ins Minus.
     */
    fun spend(cost: ResourceBundle): ResourcePool? {
        if (cost.isEmpty) return this
        if (!canAfford(cost)) return null

        val updated = amounts.toMutableMap()
        cost.amounts.forEach { (type, amount) ->
            updated[type] = (updated[type] ?: BigNumber.ZERO) - amount
        }
        return withoutZeroEntries(updated)
    }

    /**
     * Entfernt Eintraege mit Betrag null.
     *
     * Ohne diesen Schritt waere ein leergekauftes Konto nicht gleich einem
     * Konto, das die Ressource nie gefuehrt hat - beide stellen denselben
     * Zustand dar. Da [equals] auf der Map beruht, wuerde der Unterschied sonst
     * ueberall durchschlagen, wo Zustaende verglichen werden: bei
     * `distinctUntilChanged` im UI-Fluss ebenso wie in Tests.
     */
    private fun withoutZeroEntries(candidate: Map<ResourceType, BigNumber>): ResourcePool {
        val cleaned = candidate.filterValues { !it.isZero }
        return if (cleaned.isEmpty()) EMPTY else ResourcePool(cleaned)
    }

    /**
     * Setzt alle Ressourcen zurueck, die ein Prestige-Reset verfallen laesst.
     *
     * Welche das sind, entscheidet ausschliesslich
     * [ResourceType.resetOnPrestige]. Dadurch verhaelt sich eine spaeter
     * ergaenzte Ressource beim Prestige automatisch richtig, ohne dass diese
     * Methode angefasst werden muss.
     */
    fun resetForPrestige(): ResourcePool {
        val retained = amounts.filterKeys { type -> !type.resetOnPrestige }
        return if (retained.size == amounts.size) this else ResourcePool(retained)
    }

    /** Momentaufnahme aller gefuehrten Betraege, etwa zum Speichern. */
    fun asMap(): Map<ResourceType, BigNumber> = amounts

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourcePool) return false
        return amounts == other.amounts
    }

    override fun hashCode(): Int = amounts.hashCode()

    override fun toString(): String =
        amounts.entries.joinToString(prefix = "ResourcePool(", postfix = ")") { (type, amount) ->
            "${type.id}=$amount"
        }

    companion object {

        val EMPTY: ResourcePool = ResourcePool(emptyMap())

        /**
         * Erzeugt einen Pool aus vorhandenen Betraegen.
         *
         * Negative Betraege werden abgewiesen, statt sie stillschweigend auf
         * null zu ziehen: Sie koennen nur aus einem Fehler oder aus einem
         * manipulierten Spielstand stammen, und beides soll auffallen.
         */
        fun of(amounts: Map<ResourceType, BigNumber>): ResourcePool {
            val sanitized = buildMap {
                amounts.forEach { (type, amount) ->
                    require(!amount.isNegative) {
                        "Negativer Kontostand fuer ${type.id}: $amount"
                    }
                    if (!amount.isZero) put(type, amount)
                }
            }
            return if (sanitized.isEmpty()) EMPTY else ResourcePool(sanitized)
        }

        fun of(vararg entries: Pair<ResourceType, BigNumber>): ResourcePool =
            of(entries.toMap())
    }
}
