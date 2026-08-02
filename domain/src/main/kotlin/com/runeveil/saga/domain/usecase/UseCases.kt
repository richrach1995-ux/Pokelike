package com.runeveil.saga.domain.usecase

import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.ItemEffect
import com.runeveil.saga.domain.model.item.Recipe
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.EvolutionTrigger
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.world.Location
import com.runeveil.saga.domain.repository.BestiaryRepository
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.InventoryRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.repository.QuestRepository
import com.runeveil.saga.domain.repository.WorldStateRepository
import com.runeveil.saga.domain.rules.BreedingRules
import com.runeveil.saga.domain.rules.EncounterRules
import com.runeveil.saga.domain.rules.EvolutionRules
import com.runeveil.saga.domain.rules.InventoryRules
import com.runeveil.saga.domain.rules.ProgressionRules
import com.runeveil.saga.domain.rules.QuestRules
import com.runeveil.saga.domain.model.story.QuestProgress
import com.runeveil.saga.domain.model.story.QuestState
import com.runeveil.saga.domain.util.Rng
import javax.inject.Inject

/**
 * Application-level operations that combine several repositories and rule
 * objects. ViewModels depend on these, never on repositories directly, which
 * keeps orchestration logic testable and out of the UI.
 */

/**
 * Rolls a wild encounter for a step taken at [location].
 *
 * Returns null when no encounter triggers — the common case, so the caller can
 * simply continue walking.
 */
class RollEncounterUseCase @Inject constructor(
    private val content: ContentRepository,
    private val worldState: WorldStateRepository,
    private val bestiary: BestiaryRepository,
    private val inventory: InventoryRepository,
    private val rng: Rng,
) {
    suspend operator fun invoke(
        location: Location,
        stepsSinceLastEncounter: Int,
        storyFlags: Set<String>,
        dayPhase: DayPhase,
        floor: Int = 1,
        encounterRateModifier: Double = 1.0,
    ): MonsterInstance? {
        if (!EncounterRules.rollEncounter(location, stepsSinceLastEncounter, encounterRateModifier, rng)) {
            return null
        }
        val table = location.encounterTableId?.let { content.encounterTable(it) } ?: return null
        val weather = worldState.weatherFor(location.regionId).weather
        val entry = EncounterRules.pickEntry(table, dayPhase, weather, floor, storyFlags, rng)
            ?: return null
        val species = content.species(entry.speciesId) ?: return null

        val hasCharm = inventory.countOf(SHINY_CHARM_ITEM_ID) > 0
        val monster = EncounterRules.generateWild(
            entry = entry,
            species = species,
            uid = "wild-${rng.nextInt(Int.MAX_VALUE)}",
            rng = rng,
            movePpResolver = { content.move(it)?.maxPp },
            hasShinyCharm = hasCharm,
        )
        bestiary.markSeen(species.id, location.id)
        return monster
    }

    private companion object {
        const val SHINY_CHARM_ITEM_ID = "werkzeug_gluecksrune"
    }
}

/**
 * Registers a successful capture: stores the monster, updates the bestiary,
 * consumes the orb and advances any matching quest objectives.
 */
class RegisterCaptureUseCase @Inject constructor(
    private val monsters: MonsterRepository,
    private val bestiary: BestiaryRepository,
    private val inventory: InventoryRepository,
    private val questTracker: TrackQuestEventUseCase,
) {
    suspend operator fun invoke(
        captured: MonsterInstance,
        orbItemId: String,
        locationId: String?,
        trainerName: String,
        nowEpochMs: Long,
    ): MonsterInstance {
        val uid = monsters.nextUid()
        val owned = captured.copy(
            uid = uid,
            originalTrainerName = trainerName,
            caughtAtEpochMs = nowEpochMs,
            caughtWithOrbId = orbItemId,
            originLocationId = locationId,
            friendship = 90,
        )
        monsters.insert(owned, toParty = true)
        bestiary.markCaught(owned.species.id, owned.isShiny, locationId)
        inventory.remove(orbItemId, 1)
        questTracker(
            QuestRules.GameEvent.CapturedSpecies(
                speciesId = owned.species.id,
                element = owned.species.primaryElement.name,
            ),
        )
        return owned
    }
}

/**
 * Applies a game event to every active quest and persists the ones that moved.
 */
class TrackQuestEventUseCase @Inject constructor(
    private val quests: QuestRepository,
    private val content: ContentRepository,
) {
    suspend operator fun invoke(event: QuestRules.GameEvent): List<QuestProgress> {
        val active = quests.activeQuests()
        val updated = active.mapNotNull { progress ->
            val quest = content.quest(progress.questId) ?: return@mapNotNull null
            val next = QuestRules.applyEvent(quest, progress, event)
            if (next === progress) null else next
        }
        updated.forEach { quests.upsert(it) }
        return updated
    }
}

/** Starts a quest if its prerequisites are met. */
class StartQuestUseCase @Inject constructor(
    private val quests: QuestRepository,
    private val content: ContentRepository,
    private val player: PlayerRepository,
    private val inventory: InventoryRepository,
) {
    suspend operator fun invoke(questId: String, nowEpochMs: Long): Boolean {
        val quest = content.quest(questId) ?: return false
        val existing = quests.progressFor(questId)
        if (existing != null && existing.state != QuestState.AVAILABLE && !quest.repeatable) return false

        val completed = content.allQuests()
            .mapNotNull { quests.progressFor(it.id) }
            .filter { it.state == QuestState.COMPLETED }
            .map { it.questId }
            .toSet()

        val keyItems = content.allItems().filter { it.isKeyItem }
            .filter { inventory.countOf(it.id) > 0 }
            .map { it.id }
            .toSet()

        val context = QuestRules.AvailabilityContext(
            storyFlags = player.storyFlags(),
            chapter = player.currentChapter(),
            completedQuestIds = completed,
            keyItemIds = keyItems,
            runeCount = content.allItems().count { it.runeSpec != null && inventory.countOf(it.id) > 0 },
            reputation = emptyMap(),
        )
        if (!QuestRules.isAvailable(quest, context)) return false

        quests.upsert(
            QuestProgress(
                questId = questId,
                state = QuestState.ACTIVE,
                startedAtEpochMs = nowEpochMs,
            ),
        )
        return true
    }
}

/** Hands out a quest's rewards and marks it completed. */
class CompleteQuestUseCase @Inject constructor(
    private val quests: QuestRepository,
    private val content: ContentRepository,
    private val player: PlayerRepository,
    private val inventory: InventoryRepository,
    private val monsters: MonsterRepository,
) {
    suspend operator fun invoke(questId: String, nowEpochMs: Long): Boolean {
        val quest = content.quest(questId) ?: return false
        val progress = quests.progressFor(questId) ?: return false
        if (progress.state != QuestState.READY_TO_TURN_IN) return false

        for (reward in quest.rewards) {
            when (reward) {
                is com.runeveil.saga.domain.model.story.QuestReward.Gold ->
                    player.addGold(reward.amount)

                is com.runeveil.saga.domain.model.story.QuestReward.Items ->
                    reward.itemIds.forEach { (itemId, amount) -> inventory.add(itemId, amount) }

                is com.runeveil.saga.domain.model.story.QuestReward.Reputation ->
                    player.changeReputation(reward.factionId, reward.amount)

                is com.runeveil.saga.domain.model.story.QuestReward.StoryFlag ->
                    player.grantFlag(reward.flag)

                is com.runeveil.saga.domain.model.story.QuestReward.UnlockTitle -> {
                    val profile = player.profile()
                    player.update(
                        profile.copy(unlockedTitleIds = profile.unlockedTitleIds + reward.titleId),
                    )
                }

                is com.runeveil.saga.domain.model.story.QuestReward.Monster -> {
                    val species = content.species(reward.speciesId) ?: continue
                    val gift = EncounterRules.generateTrained(
                        species = species,
                        uid = monsters.nextUid(),
                        level = reward.level,
                        moveIds = species.movesAtLevel(reward.level),
                        abilityId = species.abilityIds.firstOrNull(),
                        heldItemId = null,
                        temperament = com.runeveil.saga.domain.model.monster.Temperament.EVEN,
                        geneQuality = 20,
                        isShiny = false,
                        nickname = null,
                        movePpResolver = { content.move(it)?.maxPp },
                    )
                    monsters.insert(gift, toParty = true)
                }

                is com.runeveil.saga.domain.model.story.QuestReward.Experience -> {
                    val party = monsters.party()
                    monsters.updateAll(
                        party.map { ProgressionRules.grantExperience(it, reward.amount).monster },
                    )
                }

                is com.runeveil.saga.domain.model.story.QuestReward.UnlockRegion,
                is com.runeveil.saga.domain.model.story.QuestReward.UnlockRecipe,
                -> Unit // Persisted through story flags by the calling screen.
            }
        }
        quest.grantsStoryFlags.forEach { player.grantFlag(it) }

        quests.upsert(
            progress.copy(state = QuestState.COMPLETED, completedAtEpochMs = nowEpochMs),
        )
        return true
    }
}

/**
 * Uses an item out of battle. Returns a human-readable result key so the UI
 * can show "Kein Effekt" instead of silently doing nothing.
 */
class UseItemUseCase @Inject constructor(
    private val content: ContentRepository,
    private val inventory: InventoryRepository,
    private val monsters: MonsterRepository,
    private val player: PlayerRepository,
) {
    sealed interface Result {
        data class Applied(val messageKey: String, val monster: MonsterInstance?) : Result
        data class Evolved(val from: MonsterSpecies, val to: MonsterSpecies) : Result
        data object NoEffect : Result
        data object NotOwned : Result
    }

    suspend operator fun invoke(
        itemId: String,
        targetUid: String?,
        dayPhase: DayPhase,
        locationId: String?,
    ): Result {
        val item = content.item(itemId) ?: return Result.NoEffect
        if (inventory.countOf(itemId) <= 0) return Result.NotOwned
        val target = targetUid?.let { monsters.byUid(it) }

        var updated = target
        var applied = false

        for (effect in item.effects) {
            when (effect) {
                is ItemEffect.RestoreHp -> updated = updated?.withHeal(effect.amount)?.also { applied = true }
                is ItemEffect.RestoreHpFraction -> updated = updated?.let {
                    applied = true
                    it.withHeal((it.maxHp * effect.fraction).toInt())
                }
                is ItemEffect.CureStatus -> updated = updated?.let {
                    val condition = it.status
                    if (condition != null && (effect.conditions.isEmpty() || condition in effect.conditions)) {
                        applied = true
                        it.withStatus(null)
                    } else {
                        it
                    }
                }
                is ItemEffect.Revive -> updated = updated?.let {
                    if (it.isFainted) {
                        applied = true
                        it.copy(currentHp = (it.maxHp * effect.hpFraction).toInt().coerceAtLeast(1))
                    } else {
                        it
                    }
                }
                is ItemEffect.RestorePp -> updated = updated?.let { monster ->
                    applied = true
                    monster.copy(
                        moves = monster.moves.map { slot ->
                            if (effect.allMoves || slot == monster.moves.firstOrNull()) {
                                slot.copy(currentPp = (slot.currentPp + effect.amount).coerceAtMost(slot.maxPp))
                            } else {
                                slot
                            }
                        },
                    )
                }
                is ItemEffect.GrantExperience -> updated = updated?.let {
                    applied = true
                    ProgressionRules.grantExperience(it, effect.amount).monster
                }
                is ItemEffect.GrantTraining -> updated = updated?.let {
                    applied = true
                    it.copy(
                        training = com.runeveil.saga.domain.rules.StatCalculator
                            .applyTraining(it.training, effect.gains),
                    )
                }
                is ItemEffect.GrantFriendship -> updated = updated?.withFriendship(effect.amount)
                    ?.also { applied = true }

                is ItemEffect.TriggerEvolution -> {
                    val monster = updated ?: continue
                    val path = EvolutionRules.evaluate(
                        monster,
                        EvolutionRules.Context(
                            dayPhase = dayPhase,
                            world = null,
                            locationId = locationId,
                            weatherId = null,
                            storyFlags = player.storyFlags(),
                            usedItemId = itemId,
                            triggeredBy = EvolutionTrigger.USE_ITEM,
                        ),
                    ) ?: continue
                    val newSpecies = content.species(path.targetSpeciesId) ?: continue
                    val evolved = EvolutionRules.applyEvolution(monster, newSpecies)
                    monsters.update(evolved)
                    inventory.remove(itemId, 1)
                    return Result.Evolved(monster.species, newSpecies)
                }

                is ItemEffect.TeachMove -> {
                    val monster = updated ?: continue
                    val move = content.move(effect.moveId) ?: continue
                    if (!monster.species.tutorMoveIds.contains(move.id)) continue
                    updated = ProgressionRules.learnMove(monster, move.id, move.maxPp) ?: monster
                    applied = true
                }

                is ItemEffect.RaiseMaxPp -> updated = updated?.let { monster ->
                    applied = true
                    monster.copy(
                        moves = monster.moves.mapIndexed { index, slot ->
                            if (index == 0) {
                                slot.copy(
                                    ppUps = (slot.ppUps + effect.steps).coerceAtMost(3),
                                    maxPp = slot.maxPp + slot.maxPp / 5,
                                )
                            } else {
                                slot
                            }
                        },
                    )
                }

                is ItemEffect.BattleStatBoost,
                is ItemEffect.EncounterModifier,
                is ItemEffect.GuaranteedCapture,
                -> Unit // Only meaningful inside a battle.
            }
        }

        if (!applied) return Result.NoEffect
        updated?.let { monsters.update(it) }
        if (item.consumedOnUse) inventory.remove(itemId, 1)
        return Result.Applied("msg_item_used", updated)
    }
}

/** Checks the party for pending evolutions after a level-up or story beat. */
class CheckEvolutionUseCase @Inject constructor(
    private val content: ContentRepository,
    private val monsters: MonsterRepository,
    private val player: PlayerRepository,
    private val worldState: WorldStateRepository,
) {
    data class Pending(val monster: MonsterInstance, val target: MonsterSpecies)

    suspend operator fun invoke(
        dayPhase: DayPhase,
        trigger: EvolutionTrigger = EvolutionTrigger.LEVEL_UP,
    ): List<Pending> {
        val profile = player.profile()
        val weather = worldState.weatherFor(profile.currentRegionId).weather
        val context = EvolutionRules.Context(
            dayPhase = dayPhase,
            world = content.region(profile.currentRegionId)?.world,
            locationId = profile.currentLocationId,
            weatherId = weather.name,
            storyFlags = player.storyFlags(),
            triggeredBy = trigger,
        )
        return monsters.party().mapNotNull { monster ->
            val path = EvolutionRules.evaluate(monster, context) ?: return@mapNotNull null
            val target = content.species(path.targetSpeciesId) ?: return@mapNotNull null
            Pending(monster, target)
        }
    }

    suspend fun confirm(pending: Pending): MonsterInstance {
        val evolved = EvolutionRules.applyEvolution(pending.monster, pending.target)
        monsters.update(evolved)
        return evolved
    }
}

/** Produces an egg from the two monsters left at the roost. */
class BreedUseCase @Inject constructor(
    private val content: ContentRepository,
    private val monsters: MonsterRepository,
    private val player: PlayerRepository,
    private val rng: Rng,
) {
    sealed interface Result {
        data class Egg(val monster: MonsterInstance, val quality: Double) : Result
        data class Rejected(val reason: BreedingRules.Compatibility) : Result
        data object NotEnoughGold : Result
    }

    suspend operator fun invoke(motherUid: String, fatherUid: String, nowEpochMs: Long): Result {
        val mother = monsters.byUid(motherUid) ?: return Result.Rejected(BreedingRules.Compatibility.IsEgg)
        val father = monsters.byUid(fatherUid) ?: return Result.Rejected(BreedingRules.Compatibility.IsEgg)

        val compatibility = BreedingRules.compatibility(mother, father)
        if (compatibility != BreedingRules.Compatibility.Compatible) return Result.Rejected(compatibility)

        val fee = BreedingRules.roostFee(mother, father)
        if (!player.spendGold(fee)) return Result.NotEnoughGold

        val blueprint = BreedingRules.breed(
            mother = mother,
            father = father,
            speciesResolver = { content.species(it) },
            familyBaseResolver = { familyId -> content.baseFormOf(familyId)?.id },
            rng = rng,
        ) ?: return Result.Rejected(BreedingRules.Compatibility.NoSharedEggGroup)

        val species = content.species(blueprint.speciesId)
            ?: return Result.Rejected(BreedingRules.Compatibility.NoSharedEggGroup)

        val egg = BreedingRules.materialise(
            blueprint = blueprint,
            species = species,
            uid = monsters.nextUid(),
            moveResolver = { content.move(it)?.maxPp },
            originalTrainerName = player.profile().name,
            nowEpochMs = nowEpochMs,
        )
        monsters.insert(egg, toParty = false)
        return Result.Egg(egg, BreedingRules.eggQuality(blueprint))
    }
}

/** Advances every egg in the party by the walked steps and hatches the ready ones. */
class AdvanceEggsUseCase @Inject constructor(
    private val monsters: MonsterRepository,
    private val inventory: InventoryRepository,
) {
    suspend operator fun invoke(steps: Int): List<MonsterInstance> {
        val hasCradle = inventory.countOf(FLAME_CRADLE_ITEM_ID) > 0
        val perStep = BreedingRules.hatchStepsPerMove(hasCradle)
        val hatched = mutableListOf<MonsterInstance>()
        val updates = monsters.party().filter { it.isEgg }.map { egg ->
            val remaining = (egg.eggHatchStepsRemaining - steps * perStep).coerceAtLeast(0)
            if (remaining <= 0) {
                val born = egg.copy(isEgg = false, eggHatchStepsRemaining = 0).fullyRestored()
                hatched += born
                born
            } else {
                egg.copy(eggHatchStepsRemaining = remaining)
            }
        }
        if (updates.isNotEmpty()) monsters.updateAll(updates)
        return hatched
    }

    private companion object {
        const val FLAME_CRADLE_ITEM_ID = "werkzeug_flammenwiege"
    }
}

/** Crafts a recipe at a station. */
class CraftUseCase @Inject constructor(
    private val content: ContentRepository,
    private val inventory: InventoryRepository,
    private val player: PlayerRepository,
    private val rng: Rng,
) {
    sealed interface Result {
        data class Success(val itemId: String, val amount: Int, val byproductId: String?) : Result
        data object Failed : Result
        data object MissingIngredients : Result
        data object NotEnoughGold : Result
    }

    suspend operator fun invoke(recipeId: String): Result {
        val recipe: Recipe = content.recipe(recipeId) ?: return Result.MissingIngredients
        val stacks = inventory.stacks()
        val profile = player.profile()
        if (!InventoryRules.canCraft(stacks, recipe, profile.gold)) {
            return if (profile.gold < recipe.goldCost) Result.NotEnoughGold else Result.MissingIngredients
        }
        val resultItem: Item = content.item(recipe.resultItemId) ?: return Result.MissingIngredients
        val byproduct = recipe.byproductItemId?.let { content.item(it) }

        if (recipe.goldCost > 0 && !player.spendGold(recipe.goldCost)) return Result.NotEnoughGold

        val outcome = InventoryRules.craft(stacks, recipe, resultItem, byproduct, rng)
        // Persist the consumed ingredients regardless of success.
        for ((itemId, amount) in recipe.ingredients) {
            val consumed = if (outcome.success) amount else (amount + 1) / 2
            inventory.remove(itemId, consumed)
        }
        if (!outcome.success) return Result.Failed

        inventory.add(resultItem.id, recipe.resultAmount)
        outcome.byproductItemId?.let { inventory.add(it, 1) }
        return Result.Success(resultItem.id, recipe.resultAmount, outcome.byproductItemId)
    }
}

/** Buys and sells at a shop. */
class TradeUseCase @Inject constructor(
    private val content: ContentRepository,
    private val inventory: InventoryRepository,
    private val player: PlayerRepository,
) {
    suspend fun buy(shopId: String, itemId: String, amount: Int): Boolean {
        val shop = content.shop(shopId) ?: return false
        if (itemId !in shop.itemIds) return false
        val item = content.item(itemId) ?: return false
        val price = InventoryRules.buyPrice(item, shop.markup, shop.factionDiscountPercent) * amount
        if (!player.spendGold(price)) return false
        inventory.add(itemId, amount)
        return true
    }

    suspend fun sell(itemId: String, amount: Int): Boolean {
        val item = content.item(itemId) ?: return false
        if (item.isKeyItem) return false
        if (!inventory.remove(itemId, amount)) return false
        val profile = player.profile()
        val hasMerchantTitle = MERCHANT_TITLE_ID in profile.unlockedTitleIds
        player.addGold(InventoryRules.sellPrice(item, hasMerchantTitle) * amount)
        return true
    }

    private companion object {
        const val MERCHANT_TITLE_ID = "titel_haendler"
    }
}

/** Applies post-battle experience, training and friendship to the party. */
class ApplyBattleResultsUseCase @Inject constructor(
    private val monsters: MonsterRepository,
    private val content: ContentRepository,
    private val questTracker: TrackQuestEventUseCase,
) {
    data class LevelUp(
        val monster: MonsterInstance,
        val newLevel: Int,
        val statGains: Map<Stat, Int>,
        val learnableMoveIds: List<String>,
    )

    suspend operator fun invoke(
        party: List<MonsterInstance>,
        experienceByUid: Map<String, Long>,
        defeatedSpecies: List<MonsterSpecies>,
        defeatedLevels: List<Int>,
        won: Boolean,
    ): List<LevelUp> {
        val levelUps = mutableListOf<LevelUp>()
        val updated = party.map { monster ->
            var working = monster
            defeatedSpecies.forEachIndexed { index, species ->
                working = ProgressionRules.applyBattleTraining(
                    working, species, defeatedLevels.getOrElse(index) { 1 },
                )
            }
            val gained = experienceByUid[monster.uid] ?: 0L
            if (gained > 0) {
                val result = ProgressionRules.grantExperience(working, gained)
                working = result.monster
                if (result.levelsGained > 0) {
                    levelUps += LevelUp(
                        monster = working,
                        newLevel = working.level,
                        statGains = result.statGains,
                        learnableMoveIds = result.learnableMoveIds,
                    )
                    working = working.withFriendship(
                        ProgressionRules.friendshipDelta(
                            ProgressionRules.FriendshipEvent.LEVEL_UP, working.friendship,
                        ),
                    )
                }
            }
            if (won && !working.isFainted) {
                working = working.withFriendship(
                    ProgressionRules.friendshipDelta(
                        ProgressionRules.FriendshipEvent.WON_BATTLE, working.friendship,
                    ),
                )
            }
            if (working.isFainted) {
                working = working.withFriendship(
                    ProgressionRules.friendshipDelta(
                        ProgressionRules.FriendshipEvent.FAINTED, working.friendship,
                    ),
                )
            }
            working
        }
        monsters.updateAll(updated)

        if (won) {
            questTracker(QuestRules.GameEvent.WonBattle)
            defeatedSpecies.forEach { questTracker(QuestRules.GameEvent.DefeatedSpecies(it.id)) }
        }
        return levelUps
    }
}

/** Rolls fresh weather for every region whose spell has run out. */
class AdvanceWorldClockUseCase @Inject constructor(
    private val worldState: WorldStateRepository,
) {
    suspend operator fun invoke(elapsedRealSeconds: Long, nowEpochMs: Long): Long {
        val minutes = elapsedRealSeconds /
            com.runeveil.saga.domain.model.save.GameSave.SECONDS_PER_GAME_MINUTE
        if (minutes > 0) worldState.advanceTime(minutes)
        worldState.advanceWeather(nowEpochMs)
        return minutes
    }
}

/** Picks the ending that matches the player's choices at the end of chapter 9. */
class ResolveEndingUseCase @Inject constructor(
    private val content: ContentRepository,
    private val player: PlayerRepository,
) {
    suspend operator fun invoke(runeCount: Int, totalRunes: Int): com.runeveil.saga.domain.model.story.Ending? {
        val profile = player.profile()
        val flags = player.storyFlags()
        return content.allEndings()
            .filter { ending ->
                ending.requiredFlags.all { it in flags } &&
                    ending.forbiddenFlags.none { it in flags } &&
                    (ending.minMoralScore == null || profile.moralScore >= ending.minMoralScore!!) &&
                    (ending.maxMoralScore == null || profile.moralScore <= ending.maxMoralScore!!) &&
                    (!ending.requiresAllRunes || runeCount >= totalRunes)
            }
            .maxByOrNull { it.priority }
    }
}

/** Weather helper shared by the overworld and battle screens. */
class CurrentWeatherUseCase @Inject constructor(
    private val worldState: WorldStateRepository,
) {
    suspend operator fun invoke(regionId: String): BattleWeather =
        worldState.weatherFor(regionId).weather
}
