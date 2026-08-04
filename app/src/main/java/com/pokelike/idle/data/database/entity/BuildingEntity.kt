package com.pokelike.idle.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Besitzstand eines Gebaeudetyps.
 *
 * Eine Zeile je Gebaeude statt einer Spalte je Gebaeude. Der Unterschied
 * entscheidet darueber, was ein neues Gebaeude kostet: Als Spalte waere es
 * jedes Mal eine Datenbankmigration, als Zeile ist es ein zusaetzlicher Eintrag
 * in [com.pokelike.idle.domain.model.BuildingType] und sonst nichts.
 *
 * @property buildingId Schluessel aus
 *   [com.pokelike.idle.domain.model.BuildingType].
 * @property owned Anzahl im Besitz.
 */
@Entity(tableName = "buildings")
data class BuildingEntity(
    @PrimaryKey
    val buildingId: String,
    val owned: Int,
)
