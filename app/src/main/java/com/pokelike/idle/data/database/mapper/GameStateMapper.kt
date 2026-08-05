package com.pokelike.idle.data.database.mapper

import com.pokelike.idle.data.database.entity.AchievementEntity
import com.pokelike.idle.data.database.entity.BuildingEntity
import com.pokelike.idle.data.database.entity.GameStateEntity
import com.pokelike.idle.data.database.entity.ResourceBucket
import com.pokelike.idle.data.database.entity.QuestBaselineEntity
import com.pokelike.idle.data.database.entity.QuestBaselineValueEntity
import com.pokelike.idle.data.database.entity.QuestClaimEntity
import com.pokelike.idle.data.database.entity.ResourceEntity
import com.pokelike.idle.data.database.entity.UpgradeEntity
import com.pokelike.idle.data.security.SaveSignature
import com.pokelike.idle.domain.model.AchievementInventory
import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.GameStatistics
import com.pokelike.idle.domain.model.QuestBaseline
import com.pokelike.idle.domain.model.QuestMetric
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.domain.model.QuestState
import com.pokelike.idle.domain.model.QuestType
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.UpgradeInventory
import com.pokelike.idle.domain.model.UpgradeType
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
    val upgrades: List<UpgradeEntity>,
    val achievements: List<AchievementEntity>,
    val questBaselines: List<QuestBaselineEntity>,
    val questBaselineValues: List<QuestBaselineValueEntity>,
    val questClaims: List<QuestClaimEntity>,
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
        val upgrades = state.upgrades.asSet().map { UpgradeEntity(upgradeId = it.id) }
        val achievements = state.achievements.asSet()
            .map { AchievementEntity(achievementId = it.id) }
        val questBaselines = state.quests.baselines.map { (period, baseline) ->
            QuestBaselineEntity(period = period.id, startedAtMillis = baseline.startedAtMillis)
        }
        val questBaselineValues = state.quests.baselines.flatMap { (period, baseline) ->
            baseline.values.map { (metric, value) ->
                QuestBaselineValueEntity(
                    period = period.id,
                    metric = metric.id,
                    mantissa = value.mantissa,
                    exponent = value.exponent,
                )
            }
        }
        val questClaims = state.quests.claimed.map { QuestClaimEntity(questId = it.id) }

        val unsigned = GameStateEntity(
            schemaVersion = state.schemaVersion,
            createdAtMillis = state.createdAtMillis,
            lastSeenAtMillis = state.lastSeenAtMillis,
            totalClicks = state.statistics.totalClicks,
            totalCriticalClicks = state.statistics.totalCriticalClicks,
            totalPlayTimeMillis = state.statistics.totalPlayTimeMillis,
            sessionCount = state.statistics.sessionCount,
            prestigeCount = state.statistics.prestigeCount,
            totalBuildingsPurchased = state.statistics.totalBuildingsPurchased,
            signature = "",
        )

        return PersistedGameState(
            state = unsigned.copy(
                signature = saveSignature.sign(
                    canonicalPayload(
                        entity = unsigned,
                        resources = resources,
                        buildings = buildings,
                        upgrades = upgrades,
                        achievements = achievements,
                        questBaselines = questBaselines,
                        questBaselineValues = questBaselineValues,
                        questClaims = questClaims,
                    ),
                ),
            ),
            resources = resources,
            buildings = buildings,
            upgrades = upgrades,
            achievements = achievements,
            questBaselines = questBaselines,
            questBaselineValues = questBaselineValues,
            questClaims = questClaims,
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
        upgrades: List<UpgradeEntity> = emptyList(),
        achievements: List<AchievementEntity> = emptyList(),
        questBaselines: List<QuestBaselineEntity> = emptyList(),
        questBaselineValues: List<QuestBaselineValueEntity> = emptyList(),
        questClaims: List<QuestClaimEntity> = emptyList(),
    ): GameState? {
        val unsigned = entity.copy(signature = "")
        val payload = canonicalPayload(
            entity = unsigned,
            resources = resources,
            buildings = buildings,
            upgrades = upgrades,
            achievements = achievements,
            questBaselines = questBaselines,
            questBaselineValues = questBaselineValues,
            questClaims = questClaims,
        )
        if (!saveSignature.verify(payload, entity.signature)) return null

        return GameState(
            schemaVersion = entity.schemaVersion,
            resources = ResourcePool.of(readBucket(resources, ResourceBucket.CURRENT)),
            buildings = readBuildings(buildings),
            upgrades = readUpgrades(upgrades),
            achievements = readAchievements(achievements),
            quests = readQuests(questBaselines, questBaselineValues, questClaims),
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
                totalBuildingsPurchased = entity.totalBuildingsPurchased,
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

    /**
     * Liest die gekauften Upgrades.
     *
     * Unbekannte Schluessel werden uebergangen - dieselbe Ueberlegung wie bei
     * Ressourcen und Gebaeuden.
     */
    private fun readUpgrades(upgrades: List<UpgradeEntity>): UpgradeInventory =
        UpgradeInventory.of(
            upgrades.mapNotNull { row -> UpgradeType.fromId(row.upgradeId) }.toSet(),
        )

    /** Liest die freigeschalteten Achievements. Unbekannte werden uebergangen. */
    private fun readAchievements(rows: List<AchievementEntity>): AchievementInventory =
        AchievementInventory.of(
            rows.mapNotNull { row -> AchievementType.fromId(row.achievementId) }.toSet(),
        )

    /**
     * Setzt den Quest-Stand zusammen.
     *
     * Ausgangswerte ohne zugehoerigen Zeitraum werden uebergangen: Sie koennen
     * nur aus einer beschaedigten oder haendisch veraenderten Datenbank stammen,
     * und ein Zeitraum ohne Startzeitpunkt waere nicht auswertbar.
     */
    private fun readQuests(
        baselines: List<QuestBaselineEntity>,
        values: List<QuestBaselineValueEntity>,
        claims: List<QuestClaimEntity>,
    ): QuestState {
        val valuesByPeriod = values.groupBy { it.period }

        val restoredBaselines = buildMap {
            baselines.forEach { row ->
                val period = QuestPeriod.entries.firstOrNull { it.id == row.period }
                    ?: return@forEach

                val metricValues = buildMap {
                    valuesByPeriod[row.period].orEmpty().forEach { value ->
                        val metric = QuestMetric.fromId(value.metric) ?: return@forEach
                        put(metric, BigNumber.of(value.mantissa, value.exponent))
                    }
                }

                put(
                    period,
                    QuestBaseline(
                        startedAtMillis = row.startedAtMillis,
                        values = metricValues,
                    ),
                )
            }
        }

        return QuestState(
            baselines = restoredBaselines,
            claimed = claims.mapNotNull { QuestType.fromId(it.questId) }.toSet(),
        )
    }

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
        upgrades: List<UpgradeEntity> = emptyList(),
        achievements: List<AchievementEntity> = emptyList(),
        questBaselines: List<QuestBaselineEntity> = emptyList(),
        questBaselineValues: List<QuestBaselineValueEntity> = emptyList(),
        questClaims: List<QuestClaimEntity> = emptyList(),
    ): String = buildString {
        append("v=").append(entity.schemaVersion)
        append("|created=").append(entity.createdAtMillis)
        append("|seen=").append(entity.lastSeenAtMillis)
        append("|clicks=").append(entity.totalClicks)
        append("|crits=").append(entity.totalCriticalClicks)
        append("|play=").append(entity.totalPlayTimeMillis)
        append("|sessions=").append(entity.sessionCount)
        append("|prestige=").append(entity.prestigeCount)
        append("|bought=").append(entity.totalBuildingsPurchased)

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

        upgrades
            .sortedBy { it.upgradeId }
            .forEach { row ->
                append("|upgrade:").append(row.upgradeId)
            }

        achievements
            .sortedBy { it.achievementId }
            .forEach { row ->
                append("|achievement:").append(row.achievementId)
            }

        questBaselines
            .sortedBy { it.period }
            .forEach { row ->
                append("|questperiod:").append(row.period)
                append('=').append(row.startedAtMillis)
            }

        questBaselineValues
            .sortedWith(compareBy({ it.period }, { it.metric }))
            .forEach { row ->
                append("|questbase:").append(row.period).append(':').append(row.metric)
                append('=').append(row.mantissa).append('^').append(row.exponent)
            }

        questClaims
            .sortedBy { it.questId }
            .forEach { row ->
                append("|questclaim:").append(row.questId)
            }
    }
}
