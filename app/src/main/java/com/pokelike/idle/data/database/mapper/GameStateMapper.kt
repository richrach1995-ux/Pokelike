package com.pokelike.idle.data.database.mapper

import com.pokelike.idle.data.database.entity.BuildingEntity
import com.pokelike.idle.data.database.entity.GameStateEntity
import com.pokelike.idle.data.database.entity.ResourceBucket
import com.pokelike.idle.data.database.entity.ResourceEntity
import com.pokelike.idle.data.security.SaveSignature
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.GameStatistics
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zusammengehoerige Datenbankzeilen eines Spielstands.
 *
 * Bewusst ein eigener Typ statt eines [Pair]: Beide Bestandteile werden immer
 * gemeinsam geschrieben, und ein benannter Typ macht diese Kopplung sichtbar.
 */
data class PersistedGameState(
    val state: GameStateEntity,
    val resources: List<ResourceEntity>,
    val buildings: List<BuildingEntity>,
)

/**
 * Uebersetzt zwischen Domaenenmodell und Datenbankzeilen und sichert den
 * Spielstand gegen Bearbeitung.
 *
 * Die Klasse enthaelt keinerlei Datenbankzugriff - sie rechnet nur um. Damit
 * ist der gesamte heikle Teil der Persistenz ohne Emulator pruefbar: die
 * Umwandlung selbst, die Bildung der Pruefsumme und das Verhalten bei
 * beschaedigten oder manipulierten Daten.
 */
@Singleton
class GameStateMapper @Inject constructor(
    private val saveSignature: SaveSignature,
) {

    /** Wandelt einen Spielstand in Datenbankzeilen um und signiert ihn. */
    fun toPersisted(state: GameState): PersistedGameState {
        val resources = buildResourceRows(state)
        val buildings = buildBuildingRows(state)

        val unsigned = GameStateEntity(
            schemaVersion = state.schemaVersion,
            createdAtMillis = state.createdAtMillis,
            lastSeenAtMillis = state.lastSeenAtMillis,
            totalClicks = state.statistics.totalClicks,
            totalCriticalClicks = state.statistics.totalCriticalClicks,
            totalPlayTimeMillis = state.statistics.totalPlayTimeMillis,
            sessionCount = state.statistics.sessionCount,
            prestigeCount = state.statistics.prestigeCount,
            signature = "",
        )

        return PersistedGameState(
            state = unsigned.copy(
                signature = saveSignature.sign(
                    canonicalPayload(unsigned, resources, buildings),
                ),
            ),
            resources = resources,
            buildings = buildings,
        )
    }

    /**
     * Setzt einen Spielstand aus Datenbankzeilen zusammen.
     *
     * @return `null`, wenn die Pruefsumme nicht passt. Der Aufrufer legt dann
     *   einen neuen Spielstand an. Einen manipulierten Stand teilweise zu
     *   uebernehmen waere die schlechtere Antwort: Der Spieler behielte den
     *   gefaelschten Kontostand, und die Ursache bliebe unerkannt.
     */
    fun toDomain(
        entity: GameStateEntity,
        resources: List<ResourceEntity>,
        buildings: List<BuildingEntity> = emptyList(),
    ): GameState? {
        val unsigned = entity.copy(signature = "")
        val payload = canonicalPayload(unsigned, resources, buildings)
        if (!saveSignature.verify(payload, entity.signature)) return null

        return GameState(
            schemaVersion = entity.schemaVersion,
            resources = ResourcePool.of(readBucket(resources, ResourceBucket.CURRENT)),
            buildings = readBuildings(buildings),
            statistics = GameStatistics(
                totalClicks = entity.totalClicks,
                totalCriticalClicks = entity.totalCriticalClicks,
                lifetimeEarned = ResourceBundle.of(
                    readBucket(resources, ResourceBucket.LIFETIME_EARNED),
                ),
                lifetimeSpent = ResourceBundle.of(
                    readBucket(resources, ResourceBucket.LIFETIME_SPENT),
                ),
                totalPlayTimeMillis = entity.totalPlayTimeMillis,
                sessionCount = entity.sessionCount,
                prestigeCount = entity.prestigeCount,
            ),
            createdAtMillis = entity.createdAtMillis,
            lastSeenAtMillis = entity.lastSeenAtMillis,
        )
    }

    /** Erzeugt die Zeilen aller drei Sammlungen. */
    private fun buildResourceRows(state: GameState): List<ResourceEntity> = buildList {
        addAll(toRows(ResourceBucket.CURRENT, state.resources.asMap()))
        addAll(toRows(ResourceBucket.LIFETIME_EARNED, state.statistics.lifetimeEarned.amounts))
        addAll(toRows(ResourceBucket.LIFETIME_SPENT, state.statistics.lifetimeSpent.amounts))
    }

    /** Erzeugt die Zeilen des Gebaeudebestands. */
    private fun buildBuildingRows(state: GameState): List<BuildingEntity> =
        state.buildings.asMap().map { (type, owned) ->
            BuildingEntity(buildingId = type.id, owned = owned)
        }

    /**
     * Liest den Gebaeudebestand.
     *
     * Unbekannte Schluessel werden uebergangen - dieselbe Ueberlegung wie bei
     * den Ressourcen.
     */
    private fun readBuildings(buildings: List<BuildingEntity>): BuildingInventory =
        BuildingInventory.of(
            buildMap {
                buildings.forEach { row ->
                    val type = BuildingType.fromId(row.buildingId) ?: return@forEach
                    if (row.owned > 0) put(type, row.owned)
                }
            },
        )

    private fun toRows(
        bucket: ResourceBucket,
        amounts: Map<ResourceType, BigNumber>,
    ): List<ResourceEntity> = amounts.map { (type, amount) ->
        ResourceEntity(
            bucket = bucket.id,
            resourceId = type.id,
            mantissa = amount.mantissa,
            exponent = amount.exponent,
        )
    }

    /**
     * Liest die Betraege einer Sammlung.
     *
     * Zeilen mit unbekanntem Schluessel werden uebergangen. Sie entstehen,
     * wenn der Spieler nach einem Update auf eine aeltere App-Version
     * zurueckwechselt: Der Spielstand kennt dann Ressourcen, die es im Code
     * noch nicht gibt. Ihn deswegen zu verwerfen waere unverhaeltnismaessig.
     */
    private fun readBucket(
        resources: List<ResourceEntity>,
        bucket: ResourceBucket,
    ): Map<ResourceType, BigNumber> = buildMap {
        resources.forEach { row ->
            if (row.bucket != bucket.id) return@forEach
            val type = ResourceType.fromId(row.resourceId) ?: return@forEach
            put(type, BigNumber.of(row.mantissa, row.exponent))
        }
    }

    /**
     * Baut die Zeichenkette, ueber die signiert wird.
     *
     * Zwei Eigenschaften sind dabei entscheidend:
     *
     * - Sie umfasst die Ressourcenzeilen. Deckte die Pruefsumme nur die
     *   skalaren Felder ab, liesse sich der Muenzstand in der Ressourcentabelle
     *   beliebig veraendern, ohne sie zu beruehren.
     * - Die Zeilen werden sortiert. Die Datenbank gibt keine Reihenfolge zu,
     *   und dieselben Daten in anderer Reihenfolge muessen dieselbe Pruefsumme
     *   ergeben - sonst schluege die Pruefung zufaellig fehl.
     *
     * `internal` statt `private`, damit Tests einen Zeilenbestand signieren
     * koennen, den der Mapper selbst nicht erzeugt - etwa einen Spielstand mit
     * einer dem Code unbekannten Ressource. Die Alternative waere, diese Regel
     * im Test nachzubauen; dann pruefte der Test seine eigene Kopie statt der
     * echten Umsetzung.
     */
    internal fun canonicalPayload(
        entity: GameStateEntity,
        resources: List<ResourceEntity>,
        buildings: List<BuildingEntity> = emptyList(),
    ): String = buildString {
        append("v=").append(entity.schemaVersion)
        append("|created=").append(entity.createdAtMillis)
        append("|seen=").append(entity.lastSeenAtMillis)
        append("|clicks=").append(entity.totalClicks)
        append("|crits=").append(entity.totalCriticalClicks)
        append("|play=").append(entity.totalPlayTimeMillis)
        append("|sessions=").append(entity.sessionCount)
        append("|prestige=").append(entity.prestigeCount)

        resources
            .sortedWith(compareBy({ it.bucket }, { it.resourceId }))
            .forEach { row ->
                append('|')
                append(row.bucket).append(':').append(row.resourceId)
                append('=').append(row.mantissa).append('^').append(row.exponent)
            }

        // Gebaeude werden angehaengt, nicht eingefuegt. Ein Spielstand ohne
        // Gebaeude ergibt dadurch exakt dieselbe Zeichenkette wie vor
        // Einfuehrung dieses Abschnitts - alte Staende behalten ihre gueltige
        // Pruefsumme und lassen sich weiter lesen.
        //
        // Diese Regel gilt fuer jede kuenftige Erweiterung: nur anhaengen, nie
        // die Reihenfolge des Bestehenden aendern.
        buildings
            .sortedBy { it.buildingId }
            .forEach { row ->
                append("|building:").append(row.buildingId).append('=').append(row.owned)
            }
    }
}
