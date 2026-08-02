package com.runeveil.saga.data.content

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import dagger.hilt.android.qualifiers.ApplicationContext
import com.runeveil.saga.data.di.IoDispatcher
import android.content.Context
import com.runeveil.saga.domain.battle.BattleContent
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.Recipe
import com.runeveil.saga.domain.model.monster.Ability
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.TalentNode
import com.runeveil.saga.domain.model.npc.Faction
import com.runeveil.saga.domain.model.npc.Npc
import com.runeveil.saga.domain.model.npc.TrainerTeam
import com.runeveil.saga.domain.model.player.Achievement
import com.runeveil.saga.domain.model.player.Title
import com.runeveil.saga.domain.model.story.Cutscene
import com.runeveil.saga.domain.model.story.DialogueTree
import com.runeveil.saga.domain.model.story.Ending
import com.runeveil.saga.domain.model.story.Quest
import com.runeveil.saga.domain.model.story.StoryChapter
import com.runeveil.saga.domain.model.world.EncounterTable
import com.runeveil.saga.domain.model.world.Location
import com.runeveil.saga.domain.model.world.Region
import com.runeveil.saga.domain.model.world.Shop
import com.runeveil.saga.domain.model.world.TreasureChest
import com.runeveil.saga.domain.repository.ContentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the shipped JSON content once and keeps it in memory.
 *
 * The whole content set is roughly 2.3 MiB of JSON and decodes in well under a
 * second on a mid-range device; the files are parsed **in parallel** on
 * [ioDispatcher] and indexed into maps so every later lookup is O(1).
 *
 * [ensureLoaded] is idempotent and safe to call from several coroutines: the
 * mutex guarantees that the parse happens exactly once.
 */
@Singleton
class ContentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ContentRepository, BattleContent {

    private val loadMutex = Mutex()

    @Volatile
    private var cache: ContentCache? = null

    private val loaded: ContentCache
        get() = cache ?: error(
            "Inhalte wurden noch nicht geladen — ensureLoaded() muss vor jedem Zugriff laufen.",
        )

    override suspend fun ensureLoaded() {
        if (cache != null) return
        loadMutex.withLock {
            if (cache != null) return
            cache = withContext(ioDispatcher) { parseAll() }
        }
    }

    private suspend fun parseAll(): ContentCache = coroutineScope {
        val moves = async { decodeList<MoveDto>("moves.json") }
        val abilities = async { decodeList<AbilityDto>("abilities.json") }
        val monsters = async { decodeList<MonsterDto>("monsters.json") }
        val items = async { decodeList<ItemDto>("items.json") }
        val recipes = async { decodeList<RecipeDto>("recipes.json") }
        val talents = async { decodeMap<List<TalentNodeDto>>("talents.json") }
        val titles = async { decodeList<TitleDto>("titles.json") }
        val achievements = async { decodeList<AchievementDto>("achievements.json") }
        val regions = async { decodeList<RegionDto>("regions.json") }
        val encounters = async { decodeList<EncounterTableDto>("encounters.json") }
        val shops = async { decodeList<ShopDto>("shops.json") }
        val chests = async { decodeList<ChestDto>("chests.json") }
        val factions = async { decodeList<FactionDto>("factions.json") }
        val npcs = async { decodeList<NpcDto>("npcs.json") }
        val trainers = async { decodeList<TrainerTeamDto>("trainers.json") }
        val dialogues = async { decodeList<DialogueTreeDto>("dialogues.json") }
        val chapters = async { decodeList<ChapterDto>("chapters.json") }
        val quests = async { decodeList<QuestDto>("quests.json") }
        val cutscenes = async { decodeList<CutsceneDto>("cutscenes.json") }
        val endings = async { decodeList<EndingDto>("endings.json") }

        val speciesList = monsters.await().map { it.toDomain() }
        val regionList = regions.await().map { it.toDomain() }

        ContentCache(
            moves = moves.await().associate { it.id to it.toDomain() },
            abilities = abilities.await().associate { it.id to it.toDomain() },
            species = speciesList.associateBy { it.id },
            speciesByFamily = speciesList.groupBy { it.familyId }
                .mapValues { (_, list) -> list.sortedBy { it.stage } },
            items = items.await().associate { it.id to it.toDomain() },
            recipes = recipes.await().associate { it.id to it.toDomain() },
            talents = talents.await().mapValues { (_, nodes) -> nodes.map { it.toDomain() } },
            titles = titles.await().associate { it.id to it.toDomain() },
            achievements = achievements.await().map { it.toDomain() },
            regions = regionList.associateBy { it.id },
            locations = regionList.flatMap { it.locations }.associateBy { it.id },
            encounters = encounters.await().associate { it.id to it.toDomain() },
            shops = shops.await().associate { it.id to it.toDomain() },
            chests = chests.await().associate { it.id to it.toDomain() },
            factions = factions.await().associate { it.id to it.toDomain() },
            npcs = npcs.await().associate { it.id to it.toDomain() },
            trainers = trainers.await().associate { it.id to it.toDomain() },
            dialogues = dialogues.await().associate { it.id to it.toDomain() },
            chapters = chapters.await().associate { it.number to it.toDomain() },
            quests = quests.await().associate { it.id to it.toDomain() },
            cutscenes = cutscenes.await().associate { it.id to it.toDomain() },
            endings = endings.await().map { it.toDomain() },
        )
    }

    private inline fun <reified T> decodeList(fileName: String): List<T> =
        json.decodeFromString(readAsset("$CONTENT_PATH/$fileName"))

    private inline fun <reified T> decodeMap(fileName: String): Map<String, T> =
        json.decodeFromString(readAsset("$CONTENT_PATH/$fileName"))

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    // -----------------------------------------------------------------------
    // ContentRepository
    // -----------------------------------------------------------------------

    override fun species(id: String): MonsterSpecies? = loaded.species[id]
    override fun allSpecies(): List<MonsterSpecies> = loaded.species.values.sortedBy { it.dexNumber }
    override fun speciesByFamily(familyId: String): List<MonsterSpecies> =
        loaded.speciesByFamily[familyId].orEmpty()

    override fun baseFormOf(familyId: String): MonsterSpecies? =
        loaded.speciesByFamily[familyId]?.firstOrNull { it.stage == 1 }

    override fun move(id: String): Move? = loaded.moves[id]
    override fun allMoves(): List<Move> = loaded.moves.values.toList()

    override fun ability(id: String): Ability? = loaded.abilities[id]
    override fun talentTree(id: String): List<TalentNode> = loaded.talents[id].orEmpty()

    override fun item(id: String): Item? = loaded.items[id]
    override fun allItems(): List<Item> = loaded.items.values.toList()

    override fun recipe(id: String): Recipe? = loaded.recipes[id]
    override fun allRecipes(): List<Recipe> = loaded.recipes.values.toList()

    override fun region(id: String): Region? = loaded.regions[id]
    override fun allRegions(): List<Region> = loaded.regions.values.sortedBy { it.world.order }
    override fun locationOf(locationId: String): Location? = loaded.locations[locationId]
    override fun encounterTable(id: String): EncounterTable? = loaded.encounters[id]
    override fun shop(id: String): Shop? = loaded.shops[id]
    override fun chest(id: String): TreasureChest? = loaded.chests[id]

    override fun npc(id: String): Npc? = loaded.npcs[id]
    override fun npcsAt(locationId: String): List<Npc> =
        loaded.npcs.values.filter { it.locationId == locationId }

    override fun trainerTeam(id: String): TrainerTeam? = loaded.trainers[id]
    override fun faction(id: String): Faction? = loaded.factions[id]
    override fun allFactions(): List<Faction> = loaded.factions.values.toList()

    override fun quest(id: String): Quest? = loaded.quests[id]
    override fun allQuests(): List<Quest> = loaded.quests.values.toList()
    override fun dialogueTree(id: String): DialogueTree? = loaded.dialogues[id]
    override fun chapter(number: Int): StoryChapter? = loaded.chapters[number]
    override fun allChapters(): List<StoryChapter> = loaded.chapters.values.sortedBy { it.number }
    override fun cutscene(id: String): Cutscene? = loaded.cutscenes[id]
    override fun allEndings(): List<Ending> = loaded.endings

    override fun title(id: String): Title? = loaded.titles[id]
    override fun allTitles(): List<Title> = loaded.titles.values.toList()
    override fun allAchievements(): List<Achievement> = loaded.achievements

    /** Number of species in the bestiary — used for the completion percentage. */
    val speciesCount: Int get() = loaded.species.size

    private companion object {
        const val CONTENT_PATH = "content"
    }
}

/** Immutable, fully indexed snapshot of the shipped content. */
private class ContentCache(
    val moves: Map<String, Move>,
    val abilities: Map<String, Ability>,
    val species: Map<String, MonsterSpecies>,
    val speciesByFamily: Map<String, List<MonsterSpecies>>,
    val items: Map<String, Item>,
    val recipes: Map<String, Recipe>,
    val talents: Map<String, List<TalentNode>>,
    val titles: Map<String, Title>,
    val achievements: List<Achievement>,
    val regions: Map<String, Region>,
    val locations: Map<String, Location>,
    val encounters: Map<String, EncounterTable>,
    val shops: Map<String, Shop>,
    val chests: Map<String, TreasureChest>,
    val factions: Map<String, Faction>,
    val npcs: Map<String, Npc>,
    val trainers: Map<String, TrainerTeam>,
    val dialogues: Map<String, DialogueTree>,
    val chapters: Map<Int, StoryChapter>,
    val quests: Map<String, Quest>,
    val cutscenes: Map<String, Cutscene>,
    val endings: List<Ending>,
)
