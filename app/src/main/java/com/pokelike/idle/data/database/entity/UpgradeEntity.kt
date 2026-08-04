package com.pokelike.idle.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein gekauftes Upgrade.
 *
 * Nur der Schluessel wird gespeichert. Preis, Wirkung und Freischaltbedingung
 * stehen im Code und duerfen dort auch geaendert werden - eine Balancing-
 * Anpassung soll bestehende Spielstaende erreichen, nicht an ihnen
 * vorbeilaufen. Waeren die Werte mitgespeichert, behielte jeder Altbestand
 * dauerhaft die alten Zahlen.
 *
 * @property upgradeId Schluessel aus
 *   [com.pokelike.idle.domain.model.UpgradeType].
 */
@Entity(tableName = "upgrades")
data class UpgradeEntity(
    @PrimaryKey
    val upgradeId: String,
)
