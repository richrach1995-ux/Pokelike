package com.runeveil.saga.data.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of the JSON content shipped in `assets/content`.
 *
 * These DTOs mirror the generator output one-to-one. They are deliberately
 * *separate* from the domain models: content can gain optional fields without
 * touching the rule layer, and the mapping code in `ContentMappers.kt` is the
 * single place where the two representations meet.
 *
 * Every polymorphic hierarchy uses kotlinx.serialization's default `type`
 * discriminator, which is exactly what the generator writes.
 */

// ---------------------------------------------------------------------------
// Shared
// ---------------------------------------------------------------------------

@Serializable
data class StatBlockDto(
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val magic: Int = 0,
    val resistance: Int = 0,
    val speed: Int = 0,
    val luck: Int = 0,
)

@Serializable
data class RangeDto(val first: Int, val last: Int)

@Serializable
sealed interface UnlockRequirementDto {
    @Serializable
    @SerialName("None")
    data object None : UnlockRequirementDto

    @Serializable
    @SerialName("StoryFlag")
    data class StoryFlag(val flag: String) : UnlockRequirementDto

    @Serializable
    @SerialName("Chapter")
    data class Chapter(val chapter: Int) : UnlockRequirementDto

    @Serializable
    @SerialName("KeyItem")
    data class KeyItem(val itemId: String) : UnlockRequirementDto

    @Serializable
    @SerialName("RuneCount")
    data class RuneCount(val count: Int) : UnlockRequirementDto

    @Serializable
    @SerialName("Reputation")
    data class Reputation(val factionId: String, val minimum: Int) : UnlockRequirementDto

    @Serializable
    @SerialName("All")
    data class All(val requirements: List<UnlockRequirementDto>) : UnlockRequirementDto
}

// ---------------------------------------------------------------------------
// Moves and abilities
// ---------------------------------------------------------------------------

@Serializable
data class MoveDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val element: String,
    val category: String,
    val power: Int,
    val accuracy: Int,
    val maxPp: Int,
    val priority: Int = 0,
    val target: String = "SINGLE_OPPONENT",
    val effects: List<MoveEffectDto> = emptyList(),
    val critStageBonus: Int = 0,
    val contact: Boolean = false,
    val alwaysHits: Boolean = false,
    val ignoresProtection: Boolean = false,
    val animationKey: String,
    val soundKey: String,
    val comboTag: String? = null,
    val tier: Int = 1,
    val flavourKey: String? = null,
)

@Serializable
sealed interface MoveEffectDto {
    val chance: Double

    @Serializable @SerialName("InflictStatus")
    data class InflictStatus(
        val condition: String,
        override val chance: Double,
        val durationTurns: Int = 0,
    ) : MoveEffectDto

    @Serializable @SerialName("ModifyStat")
    data class ModifyStat(
        val stat: String,
        val stages: Int,
        val onSelf: Boolean,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("ModifyRatio")
    data class ModifyRatio(
        val kind: String,
        val stages: Int,
        val onSelf: Boolean,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("Drain")
    data class Drain(val fraction: Double, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("Recoil")
    data class Recoil(val fraction: Double, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("Heal")
    data class Heal(
        val fraction: Double,
        val onSelf: Boolean = true,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("MultiHit")
    data class MultiHit(val min: Int, val max: Int, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("SetWeather")
    data class SetWeather(
        val weather: String,
        val turns: Int,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("RaiseShield")
    data class RaiseShield(
        val fraction: Double,
        val turns: Int,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("Protect")
    data class Protect(val turns: Int = 1, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("ClearStatChanges")
    data class ClearStatChanges(
        val positiveOnly: Boolean,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("CureStatus")
    data class CureStatus(val onSelf: Boolean, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("FixedDamage")
    data class FixedDamage(val amount: Int, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("PercentDamage")
    data class PercentDamage(val fraction: Double, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("Recharge")
    data class Recharge(override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("Charge")
    data class Charge(val messageKey: String, override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("AlwaysCritical")
    data class AlwaysCritical(override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("IgnoreDefenceStages")
    data class IgnoreDefenceStages(override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("SwitchOut")
    data class SwitchOut(override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("WeakenForCapture")
    data class WeakenForCapture(
        val multiplier: Double,
        val turns: Int,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("StealItem")
    data class StealItem(override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("MirrorMove")
    data class MirrorMove(override val chance: Double = 1.0) : MoveEffectDto

    @Serializable @SerialName("BonusVersusStatus")
    data class BonusVersusStatus(
        val condition: String,
        val multiplier: Double,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("ScaleWithMissingHp")
    data class ScaleWithMissingHp(
        val maxMultiplier: Double,
        override val chance: Double = 1.0,
    ) : MoveEffectDto

    @Serializable @SerialName("ScaleWithBuffs")
    data class ScaleWithBuffs(val perStage: Double, override val chance: Double = 1.0) : MoveEffectDto
}

@Serializable
data class AbilityDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val trigger: String,
    val effectId: String,
    val magnitude: Double = 1.0,
)

@Serializable
data class TalentNodeDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val tier: Int,
    val cost: Int,
    val requires: List<String> = emptyList(),
    val statBonus: StatBlockDto = StatBlockDto(),
    val grantsAbilityId: String? = null,
    val grantsMoveId: String? = null,
)

// ---------------------------------------------------------------------------
// Monsters
// ---------------------------------------------------------------------------

@Serializable
data class MonsterDto(
    val id: String,
    val dexNumber: Int,
    val nameKey: String,
    val descriptionKey: String,
    val loreKey: String,
    val primaryElement: String,
    val secondaryElement: String? = null,
    val familyId: String,
    val stage: Int,
    val baseStats: StatBlockDto,
    val rarity: String,
    val growthRate: String,
    val catchRate: Int,
    val baseExperience: Int,
    val sizeClass: String,
    val heightCm: Int,
    val weightHg: Int,
    val nativeWorld: String,
    val habitats: List<String> = emptyList(),
    val genderRatio: String = "BALANCED",
    val eggGroups: List<String> = emptyList(),
    val eggCycles: Int = 20,
    val abilityIds: List<String> = emptyList(),
    val hiddenAbilityId: String? = null,
    val talentTreeId: String? = null,
    val learnset: List<LearnsetEntryDto> = emptyList(),
    val tutorMoveIds: List<String> = emptyList(),
    val eggMoveIds: List<String> = emptyList(),
    val evolutions: List<EvolutionDto> = emptyList(),
    val spriteKey: String,
    val cryKey: String,
    val shinyPaletteKey: String? = null,
    val bestiaryFlavourKeys: List<String> = emptyList(),
)

@Serializable
data class LearnsetEntryDto(val level: Int, val moveId: String)

@Serializable
data class EvolutionDto(
    val targetSpeciesId: String,
    val trigger: String,
    val minLevel: Int? = null,
    val requiredItemId: String? = null,
    val consumesItem: Boolean = true,
    val minFriendship: Int? = null,
    val requiredTimeOfDay: String? = null,
    val requiredWorld: String? = null,
    val requiredLocationId: String? = null,
    val requiredStoryFlag: String? = null,
    val requiredRuneId: String? = null,
    val requiredGender: String? = null,
    val requiredHigherStat: String? = null,
    val requiredKnownMoveId: String? = null,
    val requiredWeather: String? = null,
    val descriptionKey: String,
)

// ---------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------

@Serializable
data class ItemDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val category: String,
    val price: Int = 0,
    val sellValueOverride: Int? = null,
    val stackLimit: Int = 99,
    val usableInBattle: Boolean = false,
    val usableOnMonster: Boolean = true,
    val consumedOnUse: Boolean = true,
    val effects: List<ItemEffectDto> = emptyList(),
    val orbSpec: OrbSpecDto? = null,
    val equipmentSpec: EquipmentSpecDto? = null,
    val runeSpec: RuneSpecDto? = null,
    val iconKey: String,
    val rarityTier: Int = 1,
    val questId: String? = null,
)

@Serializable
sealed interface ItemEffectDto {
    @Serializable @SerialName("RestoreHp")
    data class RestoreHp(val amount: Int) : ItemEffectDto

    @Serializable @SerialName("RestoreHpFraction")
    data class RestoreHpFraction(val fraction: Double) : ItemEffectDto

    @Serializable @SerialName("RestorePp")
    data class RestorePp(val amount: Int, val allMoves: Boolean = false) : ItemEffectDto

    @Serializable @SerialName("CureStatus")
    data class CureStatus(val conditions: List<String> = emptyList()) : ItemEffectDto

    @Serializable @SerialName("Revive")
    data class Revive(val hpFraction: Double) : ItemEffectDto

    @Serializable @SerialName("GrantExperience")
    data class GrantExperience(val amount: Long) : ItemEffectDto

    @Serializable @SerialName("GrantTraining")
    data class GrantTraining(val gains: Map<String, Int>) : ItemEffectDto

    @Serializable @SerialName("GrantFriendship")
    data class GrantFriendship(val amount: Int) : ItemEffectDto

    @Serializable @SerialName("BattleStatBoost")
    data class BattleStatBoost(val stat: String, val stages: Int) : ItemEffectDto

    @Serializable @SerialName("TriggerEvolution")
    data class TriggerEvolution(val allowedSpeciesIds: List<String> = emptyList()) : ItemEffectDto

    @Serializable @SerialName("TeachMove")
    data class TeachMove(val moveId: String) : ItemEffectDto

    @Serializable @SerialName("RaiseMaxPp")
    data class RaiseMaxPp(val steps: Int = 1) : ItemEffectDto

    @Serializable @SerialName("EncounterModifier")
    data class EncounterModifier(val multiplier: Double, val steps: Int) : ItemEffectDto

    @Serializable @SerialName("GuaranteedCapture")
    data object GuaranteedCapture : ItemEffectDto
}

@Serializable
data class OrbSpecDto(
    val grade: String,
    val catchMultiplier: Double,
    val conditionalBonus: Double = 1.0,
    val condition: String = "NONE",
    val shakeAnimationKey: String,
)

@Serializable
data class EquipmentSpecDto(
    val slot: String,
    val statBonus: StatBlockDto = StatBlockDto(),
    val elementAffinity: String? = null,
    val affinityBonusPercent: Int = 0,
    val captureBonusPercent: Int = 0,
    val encounterRateModifier: Double = 1.0,
    val setId: String? = null,
    val levelRequirement: Int = 1,
)

@Serializable
data class RuneSpecDto(
    val element: String? = null,
    val statBonus: StatBlockDto = StatBlockDto(),
    val grantsAbilityId: String? = null,
    val bindCost: Int = 0,
    val tier: Int = 1,
)

@Serializable
data class RecipeDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val station: String,
    val ingredients: Map<String, Int>,
    val goldCost: Int = 0,
    val resultItemId: String,
    val resultAmount: Int = 1,
    val successChance: Double = 1.0,
    val unlockRequirement: UnlockRequirementDto = UnlockRequirementDto.None,
    val tier: Int = 1,
    val craftTimeSeconds: Int = 0,
    val byproductItemId: String? = null,
    val byproductChance: Double = 0.0,
)

// ---------------------------------------------------------------------------
// World
// ---------------------------------------------------------------------------

@Serializable
data class RegionDto(
    val id: String,
    val world: String,
    val nameKey: String,
    val descriptionKey: String,
    val loreKey: String,
    val minLevel: Int,
    val maxLevel: Int,
    val dominantElements: List<String> = emptyList(),
    val musicKey: String,
    val nightMusicKey: String? = null,
    val ambienceKey: String? = null,
    val mapAssetKey: String,
    val paletteKey: String,
    val weatherProfile: WeatherProfileDto,
    val locations: List<LocationDto>,
    val links: List<LocationLinkDto>,
    val unlockRequirement: UnlockRequirementDto = UnlockRequirementDto.None,
    val legendaryMonsterIds: List<String> = emptyList(),
    val collectibleIds: List<String> = emptyList(),
    val secretIds: List<String> = emptyList(),
)

@Serializable
data class WeatherProfileDto(
    val weights: Map<String, Int>,
    val changeIntervalMinutes: Int = 12,
    val seasonalOverrides: Map<String, String> = emptyMap(),
)

@Serializable
data class LocationDto(
    val id: String,
    val regionId: String,
    val nameKey: String,
    val descriptionKey: String,
    val type: String,
    val mapX: Float,
    val mapY: Float,
    val backgroundKey: String,
    val musicKeyOverride: String? = null,
    val encounterTableId: String? = null,
    val encounterRate: Double = 0.0,
    val npcIds: List<String> = emptyList(),
    val shopId: String? = null,
    val healerAvailable: Boolean = false,
    val roostAvailable: Boolean = false,
    val forgeAvailable: Boolean = false,
    val storageAvailable: Boolean = false,
    val chestIds: List<String> = emptyList(),
    val bossId: String? = null,
    val floors: Int = 1,
    val unlockRequirement: UnlockRequirementDto = UnlockRequirementDto.None,
    val fastTravel: Boolean = false,
    val secretHint: String? = null,
)

@Serializable
data class LocationLinkDto(
    val fromLocationId: String,
    val toLocationId: String,
    val travelSteps: Int = 30,
    val requirement: UnlockRequirementDto = UnlockRequirementDto.None,
    val bidirectional: Boolean = true,
)

@Serializable
data class EncounterTableDto(val id: String, val entries: List<EncounterEntryDto>)

@Serializable
data class EncounterEntryDto(
    val speciesId: String,
    val minLevel: Int,
    val maxLevel: Int,
    val weight: Int,
    val dayPhases: List<String> = emptyList(),
    val weathers: List<String> = emptyList(),
    val floorRange: RangeDto = RangeDto(1, 99),
    val requiresStoryFlag: String? = null,
    val isRareSpawn: Boolean = false,
)

@Serializable
data class ShopDto(
    val id: String,
    val nameKey: String,
    val ownerNpcId: String? = null,
    val itemIds: List<String>,
    val markup: Double = 1.0,
    val unlockRequirement: UnlockRequirementDto = UnlockRequirementDto.None,
    val restockDaily: Boolean = false,
    val factionId: String? = null,
    val factionDiscountPercent: Int = 0,
)

@Serializable
data class ChestDto(
    val id: String,
    val locationId: String,
    val itemIds: List<String> = emptyList(),
    val goldAmount: Int = 0,
    val requiresKeyItemId: String? = null,
    val isHidden: Boolean = false,
    val respawnsDaily: Boolean = false,
)

// ---------------------------------------------------------------------------
// NPCs and factions
// ---------------------------------------------------------------------------

@Serializable
data class NpcDto(
    val id: String,
    val nameKey: String,
    val titleKey: String? = null,
    val descriptionKey: String,
    val roles: List<String>,
    val locationId: String,
    val regionId: String,
    val portraitKey: String,
    val spriteKey: String,
    val factionId: String? = null,
    val dialogueTreeIds: List<String> = emptyList(),
    val shopId: String? = null,
    val trainerTeamId: String? = null,
    val questIds: List<String> = emptyList(),
    val appearsDuringPhases: List<String> = emptyList(),
    val unlockRequirement: UnlockRequirementDto = UnlockRequirementDto.None,
    val loreKeys: List<String> = emptyList(),
    val isStoryCritical: Boolean = false,
    val rematchable: Boolean = false,
)

@Serializable
data class TrainerTeamDto(
    val id: String,
    val npcId: String,
    val aiProfile: String,
    val members: List<TrainerMonsterDto>,
    val rewardGold: Int,
    val rewardItemIds: List<String> = emptyList(),
    val introDialogueNodeId: String? = null,
    val defeatDialogueNodeId: String? = null,
    val victoryDialogueNodeId: String? = null,
    val rematchLevelBonus: Int = 0,
    val battleMusicKey: String? = null,
)

@Serializable
data class TrainerMonsterDto(
    val speciesId: String,
    val level: Int,
    val moveIds: List<String> = emptyList(),
    val abilityId: String? = null,
    val heldItemId: String? = null,
    val temperamentId: String? = null,
    val geneQuality: Int = 15,
    val isShiny: Boolean = false,
    val nickname: String? = null,
)

@Serializable
data class FactionDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val crestKey: String,
    val colorHex: Long,
    val opposingFactionIds: List<String> = emptyList(),
    val ranks: List<FactionRankDto>,
    val homeRegionId: String? = null,
)

@Serializable
data class FactionRankDto(
    val id: String,
    val nameKey: String,
    val minReputation: Int,
    val unlocksShopId: String? = null,
    val unlocksQuestIds: List<String> = emptyList(),
    val discountPercent: Int = 0,
)

// ---------------------------------------------------------------------------
// Story
// ---------------------------------------------------------------------------

@Serializable
data class QuestDto(
    val id: String,
    val nameKey: String,
    val summaryKey: String,
    val descriptionKey: String,
    val category: String,
    val chapter: Int? = null,
    val giverNpcId: String? = null,
    val turnInNpcId: String? = null,
    val regionId: String? = null,
    val recommendedLevel: Int = 1,
    val objectives: List<QuestObjectiveDto>,
    val rewards: List<QuestRewardDto> = emptyList(),
    val prerequisite: UnlockRequirementDto = UnlockRequirementDto.None,
    val prerequisiteQuestIds: List<String> = emptyList(),
    val grantsStoryFlags: List<String> = emptyList(),
    val repeatable: Boolean = false,
    val timeLimitMinutes: Int? = null,
    val isHidden: Boolean = false,
    val factionId: String? = null,
    val factionReputationDelta: Int = 0,
    val nextQuestId: String? = null,
    val cutsceneIdOnStart: String? = null,
    val cutsceneIdOnComplete: String? = null,
)

@Serializable
data class QuestObjectiveDto(
    val id: String,
    val descriptionKey: String,
    val type: String,
    val targetId: String? = null,
    val requiredCount: Int = 1,
    val optional: Boolean = false,
    val hiddenUntilPrevious: Boolean = false,
)

@Serializable
sealed interface QuestRewardDto {
    @Serializable @SerialName("Gold")
    data class Gold(val amount: Int) : QuestRewardDto

    @Serializable @SerialName("Items")
    data class Items(val itemIds: Map<String, Int>) : QuestRewardDto

    @Serializable @SerialName("Experience")
    data class Experience(val amount: Long) : QuestRewardDto

    @Serializable @SerialName("Monster")
    data class Monster(val speciesId: String, val level: Int) : QuestRewardDto

    @Serializable @SerialName("Reputation")
    data class Reputation(val factionId: String, val amount: Int) : QuestRewardDto

    @Serializable @SerialName("UnlockTitle")
    data class UnlockTitle(val titleId: String) : QuestRewardDto

    @Serializable @SerialName("UnlockRegion")
    data class UnlockRegion(val regionId: String) : QuestRewardDto

    @Serializable @SerialName("UnlockRecipe")
    data class UnlockRecipe(val recipeId: String) : QuestRewardDto

    @Serializable @SerialName("StoryFlag")
    data class StoryFlag(val flag: String) : QuestRewardDto
}

@Serializable
data class ChapterDto(
    val number: Int,
    val titleKey: String,
    val synopsisKey: String,
    val regionId: String,
    val mainQuestIds: List<String> = emptyList(),
    val openingCutsceneId: String? = null,
    val closingCutsceneId: String? = null,
    val unlocksRegionIds: List<String> = emptyList(),
    val bossId: String? = null,
    val musicKey: String? = null,
)

@Serializable
data class CutsceneDto(
    val id: String,
    val titleKey: String,
    val beats: List<CutsceneBeatDto>,
    val musicKey: String? = null,
    val skippable: Boolean = true,
)

@Serializable
sealed interface CutsceneBeatDto {
    @Serializable @SerialName("Narration")
    data class Narration(val textKey: String, val durationMs: Int = 3200) : CutsceneBeatDto

    @Serializable @SerialName("Speech")
    data class Speech(
        val speakerNpcId: String? = null,
        val speakerNameKey: String,
        val textKey: String,
        val portraitKey: String? = null,
        val emotion: String = "neutral",
    ) : CutsceneBeatDto

    @Serializable @SerialName("ShowImage")
    data class ShowImage(val imageKey: String, val durationMs: Int = 2600) : CutsceneBeatDto

    @Serializable @SerialName("PlaySound")
    data class PlaySound(val soundKey: String) : CutsceneBeatDto

    @Serializable @SerialName("ChangeMusic")
    data class ChangeMusic(val musicKey: String? = null, val fadeMs: Int = 900) : CutsceneBeatDto

    @Serializable @SerialName("ScreenEffect")
    data class ScreenEffect(val effect: String, val durationMs: Int = 800) : CutsceneBeatDto

    @Serializable @SerialName("GrantFlag")
    data class GrantFlag(val flag: String) : CutsceneBeatDto

    @Serializable @SerialName("StartBattle")
    data class StartBattle(val encounterId: String) : CutsceneBeatDto
}

@Serializable
data class EndingDto(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val cutsceneId: String,
    val requiredFlags: List<String> = emptyList(),
    val forbiddenFlags: List<String> = emptyList(),
    val minMoralScore: Int? = null,
    val maxMoralScore: Int? = null,
    val minFactionStanding: Map<String, Int> = emptyMap(),
    val requiresAllRunes: Boolean = false,
    val priority: Int = 0,
)

@Serializable
data class DialogueTreeDto(
    val id: String,
    val npcId: String,
    val entryNodeId: String,
    val nodes: List<DialogueNodeDto>,
    val priority: Int = 0,
    val conditions: List<DialogueConditionDto> = emptyList(),
)

@Serializable
data class DialogueNodeDto(
    val id: String,
    val speakerNameKey: String,
    val textKey: String,
    val portraitKey: String? = null,
    val emotion: String = "neutral",
    val choices: List<DialogueChoiceDto> = emptyList(),
    val nextNodeId: String? = null,
    val actions: List<DialogueActionDto> = emptyList(),
    val conditions: List<DialogueConditionDto> = emptyList(),
    val voiceKey: String? = null,
)

@Serializable
data class DialogueChoiceDto(
    val id: String,
    val textKey: String,
    val nextNodeId: String? = null,
    val conditions: List<DialogueConditionDto> = emptyList(),
    val actions: List<DialogueActionDto> = emptyList(),
    val moralWeight: Int = 0,
    val disabledHintKey: String? = null,
)

@Serializable
sealed interface DialogueConditionDto {
    @Serializable @SerialName("HasFlag")
    data class HasFlag(val flag: String) : DialogueConditionDto

    @Serializable @SerialName("MissingFlag")
    data class MissingFlag(val flag: String) : DialogueConditionDto

    @Serializable @SerialName("QuestInState")
    data class QuestInState(val questId: String, val state: String) : DialogueConditionDto

    @Serializable @SerialName("HasItem")
    data class HasItem(val itemId: String, val count: Int = 1) : DialogueConditionDto

    @Serializable @SerialName("HasGold")
    data class HasGold(val amount: Int) : DialogueConditionDto

    @Serializable @SerialName("MinLevel")
    data class MinLevel(val level: Int) : DialogueConditionDto

    @Serializable @SerialName("TimeOfDay")
    data class TimeOfDay(val phase: String) : DialogueConditionDto

    @Serializable @SerialName("MinReputation")
    data class MinReputation(val factionId: String, val amount: Int) : DialogueConditionDto

    @Serializable @SerialName("MaxReputation")
    data class MaxReputation(val factionId: String, val amount: Int) : DialogueConditionDto

    @Serializable @SerialName("HasSpecies")
    data class HasSpecies(val speciesId: String) : DialogueConditionDto

    @Serializable @SerialName("PartySize")
    data class PartySize(val min: Int, val max: Int = 6) : DialogueConditionDto

    @Serializable @SerialName("BestiaryCount")
    data class BestiaryCount(val min: Int) : DialogueConditionDto

    @Serializable @SerialName("ChapterAtLeast")
    data class ChapterAtLeast(val chapter: Int) : DialogueConditionDto
}

@Serializable
sealed interface DialogueActionDto {
    @Serializable @SerialName("GrantFlag")
    data class GrantFlag(val flag: String) : DialogueActionDto

    @Serializable @SerialName("ClearFlag")
    data class ClearFlag(val flag: String) : DialogueActionDto

    @Serializable @SerialName("StartQuest")
    data class StartQuest(val questId: String) : DialogueActionDto

    @Serializable @SerialName("CompleteObjective")
    data class CompleteObjective(
        val questId: String,
        val objectiveId: String,
        val amount: Int = 1,
    ) : DialogueActionDto

    @Serializable @SerialName("GiveItem")
    data class GiveItem(val itemId: String, val count: Int = 1) : DialogueActionDto

    @Serializable @SerialName("TakeItem")
    data class TakeItem(val itemId: String, val count: Int = 1) : DialogueActionDto

    @Serializable @SerialName("GiveGold")
    data class GiveGold(val amount: Int) : DialogueActionDto

    @Serializable @SerialName("TakeGold")
    data class TakeGold(val amount: Int) : DialogueActionDto

    @Serializable @SerialName("GiveMonster")
    data class GiveMonster(val speciesId: String, val level: Int) : DialogueActionDto

    @Serializable @SerialName("ChangeReputation")
    data class ChangeReputation(val factionId: String, val amount: Int) : DialogueActionDto

    @Serializable @SerialName("OpenShop")
    data class OpenShop(val shopId: String) : DialogueActionDto

    @Serializable @SerialName("StartBattle")
    data class StartBattle(val encounterId: String) : DialogueActionDto

    @Serializable @SerialName("PlayCutscene")
    data class PlayCutscene(val cutsceneId: String) : DialogueActionDto

    @Serializable @SerialName("HealParty")
    data class HealParty(val full: Boolean = true) : DialogueActionDto

    @Serializable @SerialName("UnlockFastTravel")
    data class UnlockFastTravel(val locationId: String) : DialogueActionDto

    @Serializable @SerialName("ChangeMoralScore")
    data class ChangeMoralScore(val amount: Int) : DialogueActionDto

    @Serializable @SerialName("EndDialogue")
    data object EndDialogue : DialogueActionDto
}

@Serializable
data class TitleDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val unlockConditionKey: String,
    val captureBonusPercent: Int = 0,
    val goldBonusPercent: Int = 0,
    val experienceBonusPercent: Int = 0,
    val rarityTier: Int = 1,
)

@Serializable
data class AchievementDto(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val iconKey: String,
    val category: String,
    val progressTarget: Int = 1,
    val hidden: Boolean = false,
    val rewardTitleId: String? = null,
    val rewardItemId: String? = null,
    val points: Int = 10,
)

@Serializable
data class ContentManifestDto(
    val schemaVersion: Int,
    val generator: String,
    val counts: Map<String, Int>,
    val stringCount: Int,
    val languages: List<String>,
)
