package com.pokelike.idle.domain.model

/**
 * Grobe Einordnung einer Ressource.
 *
 * Steuert Verhalten, das fuer eine ganze Gruppe gilt, statt es je Ressource
 * erneut festzulegen - etwa ob ein Zugang serverseitig geprueft werden muss.
 */
enum class ResourceCategory {
    /** Frei erspielbare Hauptwaehrung. Faellt in grossen Mengen an. */
    SOFT,

    /** Premiumwaehrung. Kann mit Echtgeld erworben werden. */
    HARD,

    /** Verbrauchsgueter mit begrenztem Einsatzzweck, etwa Lose. */
    UTILITY,

    /** Nur waehrend eines laufenden Events gueltig. */
    EVENT,

    /** Waehrung oberhalb eines Durchlaufs, ueberlebt den Prestige-Reset. */
    META,
}

/**
 * Alle Ressourcen des Spiels.
 *
 * Warum ein Enum und keine offene Klassenhierarchie:
 *
 * - Der Compiler kennt jede Ressource. Ein vergessener Zweig in einem `when`
 *   faellt beim Uebersetzen auf, nicht im Feld.
 * - Der Name ist ein stabiler Schluessel fuer Spielstand und Analytics.
 * - Eine neue Ressource ist eine einzige zusaetzliche Zeile.
 *
 * Kein Screen und keine Rechenlogik darf eine Ressource fest verdrahten. Alle
 * Anzeigen laufen ueber [ResourcePool] und iterieren ueber die Eintraege
 * dieses Enums.
 *
 * @property id Stabiler Schluessel fuer Persistenz und Analytics. Bewusst
 *   getrennt vom Enum-Namen: Eine Umbenennung im Code darf keinen Spielstand
 *   unlesbar machen.
 * @property category Gruppenzugehoerigkeit, siehe [ResourceCategory].
 * @property resetOnPrestige Ob der Bestand beim Prestige-Reset verfaellt. Die
 *   wichtigste Balancing-Eigenschaft ueberhaupt: Wuerde die Premiumwaehrung
 *   zurueckgesetzt, verloere der Spieler gekaufte Gueter.
 * @property isTradeable Ob die Ressource ueber Echtgeld erworben werden kann
 *   und deshalb serverseitig geprueft werden muss.
 */
enum class ResourceType(
    val id: String,
    val category: ResourceCategory,
    val resetOnPrestige: Boolean,
    val isTradeable: Boolean,
) {
    /** Hauptwaehrung. Faellt durch Klicks und Gebaeude an. */
    COINS(
        id = "coins",
        category = ResourceCategory.SOFT,
        resetOnPrestige = true,
        isTradeable = false,
    ),

    /** Premiumwaehrung aus Kaeufen, Events, Quests und Login-Bonus. */
    DIAMONDS(
        id = "diamonds",
        category = ResourceCategory.HARD,
        resetOnPrestige = false,
        isTradeable = true,
    ),

    /** Lose fuer Glueckrad und Mystery Box. */
    TICKETS(
        id = "tickets",
        category = ResourceCategory.UTILITY,
        resetOnPrestige = false,
        isTradeable = true,
    ),

    /** Waehrung eines laufenden Events, ausserhalb wertlos. */
    EVENT_TOKENS(
        id = "event_tokens",
        category = ResourceCategory.EVENT,
        resetOnPrestige = false,
        isTradeable = false,
    ),

    /**
     * Punkte aus Prestige-Durchlaeufen.
     *
     * Ueberlebt den Reset zwingend - sie sind sein einziger Ertrag.
     */
    PRESTIGE_POINTS(
        id = "prestige_points",
        category = ResourceCategory.META,
        resetOnPrestige = false,
        isTradeable = false,
    );

    companion object {

        /** Nachschlagetabelle, damit [fromId] nicht jedes Mal linear sucht. */
        private val byId: Map<String, ResourceType> = entries.associateBy { it.id }

        /**
         * Sucht eine Ressource anhand ihres stabilen Schluessels.
         *
         * Liefert bewusst `null` statt einer Ausnahme: Ein Spielstand kann
         * Ressourcen aus einer neueren Version enthalten, etwa nachdem der
         * Spieler auf eine aeltere App-Version zurueckgewechselt ist. Ein
         * unbekannter Schluessel wird dann uebergangen, statt den Spielstand
         * unlesbar zu machen.
         */
        fun fromId(id: String): ResourceType? = byId[id]

        /** Ressourcen, die ein Prestige-Reset auf null setzt. */
        val resetOnPrestige: List<ResourceType> = entries.filter { it.resetOnPrestige }
    }
}
