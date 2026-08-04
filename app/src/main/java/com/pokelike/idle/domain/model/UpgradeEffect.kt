package com.pokelike.idle.domain.model

/**
 * Wirkung eines Upgrades.
 *
 * Jede Wirkungsart hat genau eine Auswertungsstelle - in
 * [com.pokelike.idle.domain.usecases.CalculateModifiersUseCase]. Von dort
 * fliessen die Werte in [GameModifiers] und werden von Klickberechnung,
 * Einkommensberechnung, Preisberechnung und Offline-Berechnung gelesen.
 *
 * Dieser Umweg ist Absicht: Ohne ihn muesste jede dieser Berechnungen die
 * Upgrade-Liste selbst durchgehen, und eine neue Wirkungsart waere eine
 * Aenderung an vier Stellen statt an einer.
 *
 * Es gibt bewusst nur Wirkungsarten, die auch ausgewertet werden. Eine
 * Wirkung ohne Auswertungsstelle waere ein Upgrade, das der Spieler kauft und
 * das nichts tut.
 */
sealed interface UpgradeEffect {

    /**
     * Vervielfacht den Ertrag pro Klick.
     *
     * Faktoren mehrerer Upgrades werden multipliziert: Zweimal "doppelt"
     * ergibt vierfach. Additiv waeren spaete Upgrades wirkungslos.
     */
    data class ClickMultiplier(val factor: Double) : UpgradeEffect {
        init {
            require(factor > 0.0) { "Faktor muss positiv sein: $factor" }
        }
    }

    /**
     * Erhoeht den Grundertrag pro Klick um einen festen Betrag.
     *
     * Wird vor allen Faktoren addiert und dadurch von ihnen miterfasst -
     * andernfalls waeren flache Zuschlaege im spaeten Spiel wertlos.
     */
    data class ClickFlatBonus(val amount: BigNumber) : UpgradeEffect {
        init {
            require(!amount.isNegative) { "Negativer Zuschlag: $amount" }
        }
    }

    /** Erhoeht die Wahrscheinlichkeit eines kritischen Treffers. */
    data class CriticalChanceBonus(val amount: Double) : UpgradeEffect {
        init {
            require(amount >= 0.0) { "Negativer Zuschlag: $amount" }
        }
    }

    /** Erhoeht den Faktor eines kritischen Treffers. */
    data class CriticalMultiplierBonus(val amount: Double) : UpgradeEffect {
        init {
            require(amount >= 0.0) { "Negativer Zuschlag: $amount" }
        }
    }

    /** Vervielfacht den Ertrag aller Gebaeude. */
    data class IncomeMultiplier(val factor: Double) : UpgradeEffect {
        init {
            require(factor > 0.0) { "Faktor muss positiv sein: $factor" }
        }
    }

    /**
     * Vervielfacht den Ertrag einer einzelnen Gebaeudeart.
     *
     * Die im Genre wirksamste Upgrade-Art: Sie gibt fruehen Gebaeuden auch
     * spaeter noch einen Grund und verhindert, dass die ersten Stufen zu
     * totem Inventar werden.
     */
    data class BuildingIncomeMultiplier(
        val building: BuildingType,
        val factor: Double,
    ) : UpgradeEffect {
        init {
            require(factor > 0.0) { "Faktor muss positiv sein: $factor" }
        }
    }

    /**
     * Senkt die Gebaeudepreise.
     *
     * Nachlaesse mehrerer Upgrades werden addiert und gedeckelt. Additiv,
     * weil der Spieler "5 % plus 10 %" als 15 % liest; multiplikativ ergaebe
     * es 14,5 % und wirkte wie ein Fehler.
     */
    data class BuildingDiscount(val fraction: Double) : UpgradeEffect {
        init {
            require(fraction in 0.0..1.0) { "Nachlass ausserhalb von 0..1: $fraction" }
        }
    }

    /** Erhoeht den Anteil des Einkommens, der offline anfaellt. */
    data class OfflineEfficiencyBonus(val amount: Double) : UpgradeEffect {
        init {
            require(amount >= 0.0) { "Negativer Zuschlag: $amount" }
        }
    }
}
