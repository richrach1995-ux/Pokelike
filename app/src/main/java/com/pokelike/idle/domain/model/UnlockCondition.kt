package com.pokelike.idle.domain.model

/**
 * Bedingung, unter der ein Inhalt sichtbar wird.
 *
 * Warum ein sealed interface mit eigener Pruefmethode und kein Enum mit
 * Zahlenfeldern:
 *
 * - Jede Bedingung traegt genau die Angaben, die sie braucht. Ein Enum
 *   muesste alle denkbaren Felder fuehren, und bei jeder Bedingung waeren die
 *   meisten davon bedeutungslos.
 * - Eine neue Bedingungsart ist eine neue Klasse. Bestehende bleiben
 *   unveraendert, und es gibt keine zentrale Fallunterscheidung, die jemand
 *   zu erweitern vergessen koennte.
 * - Jede Bedingung ist einzeln pruefbar.
 *
 * Ab dem Quest- und Achievement-Schritt werden dieselben Bedingungen dort
 * wiederverwendet; sie sind bewusst nicht auf Upgrades zugeschnitten.
 */
sealed interface UnlockCondition {

    /** Ob die Bedingung im gegebenen Spielstand erfuellt ist. */
    fun isMet(state: GameState): Boolean

    /** Immer erfuellt. Fuer Inhalte, die von Beginn an verfuegbar sind. */
    data object Always : UnlockCondition {
        override fun isMet(state: GameState): Boolean = true
    }

    /**
     * Mindestzahl an Klicks ueber die gesamte Spielzeit.
     *
     * Bezieht sich auf die lebenslange Statistik und ueberdauert damit den
     * Prestige-Reset - ein bereits freigeschaltetes Upgrade soll nicht wieder
     * verschwinden.
     */
    data class TotalClicks(val required: Long) : UnlockCondition {
        init {
            require(required >= 0L) { "Negative Klickanzahl: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.statistics.totalClicks >= required
    }

    /** Mindestzahl eines bestimmten Gebaeudes. */
    data class BuildingCount(
        val building: BuildingType,
        val required: Int,
    ) : UnlockCondition {
        init {
            require(required >= 0) { "Negative Gebaeudeanzahl: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.buildings[building] >= required
    }

    /** Mindestzahl kritischer Treffer ueber die gesamte Spielzeit. */
    data class TotalCriticalClicks(val required: Long) : UnlockCondition {
        init {
            require(required >= 0L) { "Negative Anzahl: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.statistics.totalCriticalClicks >= required
    }

    /**
     * Mindestzahl je gekaufter Gebaeude.
     *
     * Bezieht sich auf den nur steigenden Zaehler, nicht auf den Bestand:
     * Letzterer faellt beim Prestige-Reset auf null.
     */
    data class BuildingsPurchased(val required: Long) : UnlockCondition {
        init {
            require(required >= 0L) { "Negative Anzahl: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.statistics.totalBuildingsPurchased >= required
    }

    /** Mindestzahl gekaufter Upgrades. */
    data class UpgradesOwned(val required: Int) : UnlockCondition {
        init {
            require(required >= 0) { "Negative Anzahl: $required" }
        }

        override fun isMet(state: GameState): Boolean = state.upgrades.count >= required
    }

    /** Mindestzahl abgeschlossener Prestige-Durchlaeufe. */
    data class PrestigeCount(val required: Int) : UnlockCondition {
        init {
            require(required >= 0) { "Negative Anzahl: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.statistics.prestigeCount >= required
    }

    /** Mindestzahl an Gebaeuden insgesamt, unabhaengig von der Art. */
    data class TotalBuildings(val required: Int) : UnlockCondition {
        init {
            require(required >= 0) { "Negative Gebaeudeanzahl: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.buildings.totalCount >= required
    }

    /**
     * Mindestsumme jemals verdienter Muenzen.
     *
     * Bewusst die Lebenszeitsumme und nicht der Kontostand: Andernfalls
     * verschwaende ein freigeschaltetes Upgrade wieder, sobald der Spieler
     * sein Geld ausgibt - genau in dem Moment also, in dem er es braeuchte.
     */
    data class LifetimeCoins(val required: BigNumber) : UnlockCondition {
        init {
            require(!required.isNegative) { "Negative Muenzsumme: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.statistics.lifetimeEarned[ResourceType.COINS] >= required
    }

    /**
     * Mindestlaenge der Anmeldeserie.
     *
     * Geprueft wird der Bestwert und nicht die laufende Serie: Eine spaeter
     * gerissene Serie wuerde sonst ein bereits freigeschaltetes Achievement
     * wieder entziehen - der Spieler haette es erreicht und danach verloren,
     * ohne etwas falsch gemacht zu haben.
     */
    data class LoginStreak(val required: Int) : UnlockCondition {
        init {
            require(required >= 0) { "Negative Serienlaenge: $required" }
        }

        override fun isMet(state: GameState): Boolean =
            state.login.longestStreak >= required
    }

    /**
     * Alle Teilbedingungen muessen erfuellt sein.
     *
     * Erlaubt zusammengesetzte Anforderungen, ohne dafuer eigene
     * Bedingungsarten anzulegen.
     */
    data class All(val conditions: List<UnlockCondition>) : UnlockCondition {
        init {
            require(conditions.isNotEmpty()) { "Leere Bedingungsliste" }
        }

        override fun isMet(state: GameState): Boolean =
            conditions.all { condition -> condition.isMet(state) }
    }
}
