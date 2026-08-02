package com.runeveil.saga.data.content

import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.battle.MoveCategory
import com.runeveil.saga.domain.model.battle.MoveEffect
import com.runeveil.saga.domain.model.battle.MoveTarget
import com.runeveil.saga.domain.model.battle.RatioKind
import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.item.CraftingStation
import com.runeveil.saga.domain.model.item.EquipmentSlot
import com.runeveil.saga.domain.model.item.EquipmentSpec
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.ItemCategory
import com.runeveil.saga.domain.model.item.ItemEffect
import com.runeveil.saga.domain.model.item.OrbCondition
import com.runeveil.saga.domain.model.item.OrbGrade
import com.runeveil.saga.domain.model.item.OrbSpec
import com.runeveil.saga.domain.model.item.Recipe
import com.runeveil.saga.domain.model.item.RuneSpec
import com.runeveil.saga.domain.model.monster.Ability
import com.runeveil.saga.domain.model.monster.AbilityTrigger
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.EvolutionPath
import com.runeveil.saga.domain.model.monster.EvolutionTrigger
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.GenderRatio
import com.runeveil.saga.domain.model.monster.GrowthRate
import com.runeveil.saga.domain.model.monster.LearnsetEntry
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.Rarity
import com.runeveil.saga.domain.model.monster.SizeClass
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.TalentNode
import com.runeveil.saga.domain.model.monster.World
import com.runeveil.saga.domain.model.npc.Faction
import com.runeveil.saga.domain.model.npc.FactionRank
import com.runeveil.saga.domain.model.npc.Npc
import com.runeveil.saga.domain.model.npc.NpcRole
import com.runeveil.saga.domain.model.npc.TrainerMonster
import com.runeveil.saga.domain.model.npc.TrainerTeam
import com.runeveil.saga.domain.model.player.Achievement
import com.runeveil.saga.domain.model.player.AchievementCategory
import com.runeveil.saga.domain.model.player.Title
import com.runeveil.saga.domain.model.story.Cutscene
import com.runeveil.saga.domain.model.story.CutsceneBeat
import com.runeveil.saga.domain.model.story.DialogueAction
import com.runeveil.saga.domain.model.story.DialogueChoice
import com.runeveil.saga.domain.model.story.DialogueCondition
import com.runeveil.saga.domain.model.story.DialogueNode
import com.runeveil.saga.domain.model.story.DialogueTree
import com.runeveil.saga.domain.model.story.Ending
import com.runeveil.saga.domain.model.story.ObjectiveType
import com.runeveil.saga.domain.model.story.Quest
import com.runeveil.saga.domain.model.story.QuestCategory
import com.runeveil.saga.domain.model.story.QuestObjective
import com.runeveil.saga.domain.model.story.QuestReward
import com.runeveil.saga.domain.model.story.QuestState
import com.runeveil.saga.domain.model.story.StoryChapter
import com.runeveil.saga.domain.model.world.EncounterEntry
import com.runeveil.saga.domain.model.world.EncounterTable
import com.runeveil.saga.domain.model.world.Location
import com.runeveil.saga.domain.model.world.LocationLink
import com.runeveil.saga.domain.model.world.LocationType
import com.runeveil.saga.domain.model.world.Region
import com.runeveil.saga.domain.model.world.Shop
import com.runeveil.saga.domain.model.world.TreasureChest
import com.runeveil.saga.domain.model.world.UnlockRequirement
import com.runeveil.saga.domain.model.world.WeatherProfile
import com.runeveil.saga.domain.battle.AiProfile

/**
 * Pure functions turning content DTOs into domain models.
 *
 * Unknown enum names are a *content bug*, not a runtime condition: the build
 * validator rejects them, so the mappers fail fast with a clear message rather
 * than silently substituting a default.
 */

private inline fun <reified T : Enum<T>> String.toEnum(field: String): T =
    enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: error("Ungültiger Wert '$this' für $field (erwartet: ${enumValues<T>().joinToString { it.name }})")

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { raw -> enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } }

fun StatBlockDto.toDomain(): StatBlock =
    StatBlock(hp, attack, defense, magic, resistance, speed, luck)

fun Map<String, Int>.toStatBlock(): StatBlock =
    entries.fold(StatBlock.ZERO) { acc, (key, value) ->
        val stat = Stat.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
            ?: when (key.lowercase()) {
                "hp" -> Stat.HP
                "attack" -> Stat.ATTACK
                "defense" -> Stat.DEFENSE
                "magic" -> Stat.MAGIC
                "resistance" -> Stat.RESISTANCE
                "speed" -> Stat.SPEED
                "luck" -> Stat.LUCK
                else -> error("Unbekannter Wert '$key' in einem Wertblock")
            }
        acc.with(stat, acc[stat] + value)
    }

fun UnlockRequirementDto.toDomain(): UnlockRequirement = when (this) {
    is UnlockRequirementDto.None -> UnlockRequirement.None
    is UnlockRequirementDto.StoryFlag -> UnlockRequirement.StoryFlag(flag)
    is UnlockRequirementDto.Chapter -> UnlockRequirement.Chapter(chapter)
    is UnlockRequirementDto.KeyItem -> UnlockRequirement.KeyItem(itemId)
    is UnlockRequirementDto.RuneCount -> UnlockRequirement.RuneCount(count)
    is UnlockRequirementDto.Reputation -> UnlockRequirement.Reputation(factionId, minimum)
    is UnlockRequirementDto.All -> UnlockRequirement.All(requirements.map { it.toDomain() })
}

// ---------------------------------------------------------------------------
// Moves
// ---------------------------------------------------------------------------

fun MoveDto.toDomain(): Move {
    val moveCategory = category.toEnum<MoveCategory>("Attackenkategorie")
    return Move(
        id = id,
        nameKey = nameKey,
        descriptionKey = descriptionKey,
        element = element.toEnum<Element>("Element"),
        category = moveCategory,
        power = power,
        accuracy = accuracy,
        maxPp = maxPp,
        priority = priority,
        target = target.toEnum<MoveTarget>("Ziel"),
        effects = effects.map { it.toDomain() },
        critStageBonus = critStageBonus,
        contact = contact,
        alwaysHits = alwaysHits,
        ignoresProtection = ignoresProtection,
        animationKey = animationKey,
        soundKey = soundKey,
        comboTag = comboTag,
        tier = tier,
        flavourKey = flavourKey,
    )
}

fun MoveEffectDto.toDomain(): MoveEffect = when (this) {
    is MoveEffectDto.InflictStatus -> MoveEffect.InflictStatus(
        condition.toEnum<StatusCondition>("Status"), chance, durationTurns,
    )
    is MoveEffectDto.ModifyStat -> MoveEffect.ModifyStat(
        stat.toEnum<Stat>("Wert"), stages, onSelf, chance,
    )
    is MoveEffectDto.ModifyRatio -> MoveEffect.ModifyRatio(
        kind.toEnum<RatioKind>("Trefferwert"), stages, onSelf, chance,
    )
    is MoveEffectDto.Drain -> MoveEffect.Drain(fraction, chance)
    is MoveEffectDto.Recoil -> MoveEffect.Recoil(fraction, chance)
    is MoveEffectDto.Heal -> MoveEffect.Heal(fraction, onSelf, chance)
    is MoveEffectDto.MultiHit -> MoveEffect.MultiHit(min, max, chance)
    is MoveEffectDto.SetWeather -> MoveEffect.SetWeather(
        weather.toEnum<BattleWeather>("Wetter"), turns, chance,
    )
    is MoveEffectDto.RaiseShield -> MoveEffect.RaiseShield(fraction, turns, chance)
    is MoveEffectDto.Protect -> MoveEffect.Protect(turns, chance)
    is MoveEffectDto.ClearStatChanges -> MoveEffect.ClearStatChanges(positiveOnly, chance)
    is MoveEffectDto.CureStatus -> MoveEffect.CureStatus(onSelf, chance)
    is MoveEffectDto.FixedDamage -> MoveEffect.FixedDamage(amount, chance)
    is MoveEffectDto.PercentDamage -> MoveEffect.PercentDamage(fraction, chance)
    is MoveEffectDto.Recharge -> MoveEffect.Recharge(chance)
    is MoveEffectDto.Charge -> MoveEffect.Charge(messageKey, chance)
    is MoveEffectDto.AlwaysCritical -> MoveEffect.AlwaysCritical(chance)
    is MoveEffectDto.IgnoreDefenceStages -> MoveEffect.IgnoreDefenceStages(chance)
    is MoveEffectDto.SwitchOut -> MoveEffect.SwitchOut(chance)
    is MoveEffectDto.WeakenForCapture -> MoveEffect.WeakenForCapture(multiplier, turns, chance)
    is MoveEffectDto.StealItem -> MoveEffect.StealItem(chance)
    is MoveEffectDto.MirrorMove -> MoveEffect.MirrorMove(chance)
    is MoveEffectDto.BonusVersusStatus -> MoveEffect.BonusVersusStatus(
        condition.toEnum<StatusCondition>("Status"), multiplier, chance,
    )
    is MoveEffectDto.ScaleWithMissingHp -> MoveEffect.ScaleWithMissingHp(maxMultiplier, chance)
    is MoveEffectDto.ScaleWithBuffs -> MoveEffect.ScaleWithBuffs(perStage, chance)
}

fun AbilityDto.toDomain(): Ability = Ability(
    id = id,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    trigger = trigger.toEnum<AbilityTrigger>("Auslöser"),
    effectId = effectId,
    magnitude = magnitude,
)

fun TalentNodeDto.toDomain(): TalentNode = TalentNode(
    id = id,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    tier = tier,
    cost = cost,
    requires = requires,
    statBonus = statBonus.toDomain(),
    grantsAbilityId = grantsAbilityId,
    grantsMoveId = grantsMoveId,
)

// ---------------------------------------------------------------------------
// Monsters
// ---------------------------------------------------------------------------

fun MonsterDto.toDomain(): MonsterSpecies = MonsterSpecies(
    id = id,
    dexNumber = dexNumber,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    loreKey = loreKey,
    primaryElement = primaryElement.toEnum<Element>("Element"),
    secondaryElement = secondaryElement.toEnumOrNull<Element>(),
    familyId = familyId,
    stage = stage,
    baseStats = baseStats.toDomain(),
    rarity = rarity.toEnum<Rarity>("Seltenheit"),
    growthRate = growthRate.toEnum<GrowthRate>("Wachstum"),
    catchRate = catchRate,
    baseExperience = baseExperience,
    sizeClass = sizeClass.toEnum<SizeClass>("Größenklasse"),
    heightCm = heightCm,
    weightHg = weightHg,
    nativeWorld = nativeWorld.toEnum<World>("Welt"),
    habitats = habitats,
    genderRatio = genderRatio.toEnum<GenderRatio>("Geschlechterverteilung"),
    eggGroups = eggGroups,
    eggCycles = eggCycles,
    abilityIds = abilityIds,
    hiddenAbilityId = hiddenAbilityId,
    talentTreeId = talentTreeId,
    learnset = learnset.map { LearnsetEntry(it.level, it.moveId) },
    tutorMoveIds = tutorMoveIds,
    eggMoveIds = eggMoveIds,
    evolutions = evolutions.map { it.toDomain() },
    spriteKey = spriteKey,
    cryKey = cryKey,
    shinyPaletteKey = shinyPaletteKey,
    bestiaryFlavourKeys = bestiaryFlavourKeys,
)

fun EvolutionDto.toDomain(): EvolutionPath = EvolutionPath(
    targetSpeciesId = targetSpeciesId,
    trigger = trigger.toEnum<EvolutionTrigger>("Entwicklungsauslöser"),
    minLevel = minLevel,
    requiredItemId = requiredItemId,
    consumesItem = consumesItem,
    minFriendship = minFriendship,
    requiredTimeOfDay = requiredTimeOfDay.toEnumOrNull<DayPhase>(),
    requiredWorld = requiredWorld.toEnumOrNull<World>(),
    requiredLocationId = requiredLocationId,
    requiredStoryFlag = requiredStoryFlag,
    requiredRuneId = requiredRuneId,
    requiredGender = requiredGender.toEnumOrNull<Gender>(),
    requiredHigherStat = requiredHigherStat.toEnumOrNull<Stat>(),
    requiredKnownMoveId = requiredKnownMoveId,
    requiredWeather = requiredWeather,
    descriptionKey = descriptionKey,
)

// ---------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------

fun ItemDto.toDomain(): Item = Item(
    id = id,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    category = category.toEnum<ItemCategory>("Gegenstandskategorie"),
    price = price,
    sellValueOverride = sellValueOverride,
    stackLimit = stackLimit,
    usableInBattle = usableInBattle,
    usableOnMonster = usableOnMonster,
    consumedOnUse = consumedOnUse,
    effects = effects.map { it.toDomain() },
    orbSpec = orbSpec?.toDomain(),
    equipmentSpec = equipmentSpec?.toDomain(),
    runeSpec = runeSpec?.toDomain(),
    iconKey = iconKey,
    rarityTier = rarityTier,
    questId = questId,
)

fun ItemEffectDto.toDomain(): ItemEffect = when (this) {
    is ItemEffectDto.RestoreHp -> ItemEffect.RestoreHp(amount)
    is ItemEffectDto.RestoreHpFraction -> ItemEffect.RestoreHpFraction(fraction)
    is ItemEffectDto.RestorePp -> ItemEffect.RestorePp(amount, allMoves)
    is ItemEffectDto.CureStatus -> ItemEffect.CureStatus(
        conditions.map { it.toEnum<StatusCondition>("Status") }.toSet(),
    )
    is ItemEffectDto.Revive -> ItemEffect.Revive(hpFraction)
    is ItemEffectDto.GrantExperience -> ItemEffect.GrantExperience(amount)
    is ItemEffectDto.GrantTraining -> ItemEffect.GrantTraining(gains.toStatBlock())
    is ItemEffectDto.GrantFriendship -> ItemEffect.GrantFriendship(amount)
    is ItemEffectDto.BattleStatBoost -> ItemEffect.BattleStatBoost(
        stat.toEnum<Stat>("Wert"), stages,
    )
    is ItemEffectDto.TriggerEvolution -> ItemEffect.TriggerEvolution(allowedSpeciesIds)
    is ItemEffectDto.TeachMove -> ItemEffect.TeachMove(moveId)
    is ItemEffectDto.RaiseMaxPp -> ItemEffect.RaiseMaxPp(steps)
    is ItemEffectDto.EncounterModifier -> ItemEffect.EncounterModifier(multiplier, steps)
    is ItemEffectDto.GuaranteedCapture -> ItemEffect.GuaranteedCapture
}

fun OrbSpecDto.toDomain(): OrbSpec = OrbSpec(
    grade = grade.toEnum<OrbGrade>("Kugelgüte"),
    catchMultiplier = catchMultiplier,
    conditionalBonus = conditionalBonus,
    condition = condition.toEnum<OrbCondition>("Kugelbedingung"),
    shakeAnimationKey = shakeAnimationKey,
)

fun EquipmentSpecDto.toDomain(): EquipmentSpec = EquipmentSpec(
    slot = slot.toEnum<EquipmentSlot>("Ausrüstungsplatz"),
    statBonus = statBonus.toDomain(),
    elementAffinity = elementAffinity.toEnumOrNull<Element>(),
    affinityBonusPercent = affinityBonusPercent,
    captureBonusPercent = captureBonusPercent,
    encounterRateModifier = encounterRateModifier,
    setId = setId,
    levelRequirement = levelRequirement,
)

fun RuneSpecDto.toDomain(): RuneSpec = RuneSpec(
    element = element.toEnumOrNull<Element>(),
    statBonus = statBonus.toDomain(),
    grantsAbilityId = grantsAbilityId,
    bindCost = bindCost,
    tier = tier,
)

fun RecipeDto.toDomain(): Recipe = Recipe(
    id = id,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    station = station.toEnum<CraftingStation>("Werkstatt"),
    ingredients = ingredients,
    goldCost = goldCost,
    resultItemId = resultItemId,
    resultAmount = resultAmount,
    successChance = successChance,
    unlockRequirement = unlockRequirement.toDomain(),
    tier = tier,
    craftTimeSeconds = craftTimeSeconds,
    byproductItemId = byproductItemId,
    byproductChance = byproductChance,
)

// ---------------------------------------------------------------------------
// World
// ---------------------------------------------------------------------------

fun RegionDto.toDomain(): Region = Region(
    id = id,
    world = world.toEnum<World>("Welt"),
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    loreKey = loreKey,
    recommendedLevelRange = minLevel..maxLevel,
    dominantElements = dominantElements.map { it.toEnum<Element>("Element") },
    musicKey = musicKey,
    nightMusicKey = nightMusicKey,
    ambienceKey = ambienceKey,
    mapAssetKey = mapAssetKey,
    paletteKey = paletteKey,
    weatherProfile = weatherProfile.toDomain(),
    locations = locations.map { it.toDomain() },
    links = links.map { it.toDomain() },
    unlockRequirement = unlockRequirement.toDomain(),
    legendaryMonsterIds = legendaryMonsterIds,
    collectibleIds = collectibleIds,
    secretIds = secretIds,
)

fun WeatherProfileDto.toDomain(): WeatherProfile = WeatherProfile(
    weights = weights.mapKeys { it.key.toEnum<BattleWeather>("Wetter") },
    changeIntervalMinutes = changeIntervalMinutes,
    seasonalOverrides = seasonalOverrides.mapValues { it.value.toEnum<BattleWeather>("Wetter") },
)

fun LocationDto.toDomain(): Location = Location(
    id = id,
    regionId = regionId,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    type = type.toEnum<LocationType>("Ortstyp"),
    mapX = mapX,
    mapY = mapY,
    backgroundKey = backgroundKey,
    musicKeyOverride = musicKeyOverride,
    encounterTableId = encounterTableId,
    encounterRate = encounterRate,
    npcIds = npcIds,
    shopId = shopId,
    healerAvailable = healerAvailable,
    roostAvailable = roostAvailable,
    forgeAvailable = forgeAvailable,
    storageAvailable = storageAvailable,
    chestIds = chestIds,
    bossId = bossId,
    floors = floors,
    unlockRequirement = unlockRequirement.toDomain(),
    fastTravel = fastTravel,
    secretHint = secretHint,
)

fun LocationLinkDto.toDomain(): LocationLink = LocationLink(
    fromLocationId = fromLocationId,
    toLocationId = toLocationId,
    travelSteps = travelSteps,
    requirement = requirement.toDomain(),
    bidirectional = bidirectional,
)

fun EncounterTableDto.toDomain(): EncounterTable = EncounterTable(
    id = id,
    entries = entries.map { entry ->
        EncounterEntry(
            speciesId = entry.speciesId,
            minLevel = entry.minLevel,
            maxLevel = entry.maxLevel,
            weight = entry.weight,
            dayPhases = entry.dayPhases.map { it.toEnum<DayPhase>("Tageszeit") }.toSet(),
            weathers = entry.weathers.map { it.toEnum<BattleWeather>("Wetter") }.toSet(),
            floorRange = entry.floorRange.first..entry.floorRange.last,
            requiresStoryFlag = entry.requiresStoryFlag,
            isRareSpawn = entry.isRareSpawn,
        )
    },
)

fun ShopDto.toDomain(): Shop = Shop(
    id = id,
    nameKey = nameKey,
    ownerNpcId = ownerNpcId,
    itemIds = itemIds,
    markup = markup,
    unlockRequirement = unlockRequirement.toDomain(),
    restockDaily = restockDaily,
    factionId = factionId,
    factionDiscountPercent = factionDiscountPercent,
)

fun ChestDto.toDomain(): TreasureChest = TreasureChest(
    id = id,
    locationId = locationId,
    itemIds = itemIds,
    goldAmount = goldAmount,
    requiresKeyItemId = requiresKeyItemId,
    isHidden = isHidden,
    respawnsDaily = respawnsDaily,
)

// ---------------------------------------------------------------------------
// NPCs
// ---------------------------------------------------------------------------

fun NpcDto.toDomain(): Npc = Npc(
    id = id,
    nameKey = nameKey,
    titleKey = titleKey,
    descriptionKey = descriptionKey,
    roles = roles.map { it.toEnum<NpcRole>("NPC-Rolle") }.toSet(),
    locationId = locationId,
    regionId = regionId,
    portraitKey = portraitKey,
    spriteKey = spriteKey,
    factionId = factionId,
    dialogueTreeIds = dialogueTreeIds,
    shopId = shopId,
    trainerTeamId = trainerTeamId,
    questIds = questIds,
    appearsDuringPhases = appearsDuringPhases.map { it.toEnum<DayPhase>("Tageszeit") }.toSet(),
    unlockRequirement = unlockRequirement.toDomain(),
    loreKeys = loreKeys,
    isStoryCritical = isStoryCritical,
    rematchable = rematchable,
)

fun TrainerTeamDto.toDomain(): TrainerTeam = TrainerTeam(
    id = id,
    npcId = npcId,
    aiProfile = aiProfile.toEnum<AiProfile>("KI-Profil"),
    members = members.map {
        TrainerMonster(
            speciesId = it.speciesId,
            level = it.level,
            moveIds = it.moveIds,
            abilityId = it.abilityId,
            heldItemId = it.heldItemId,
            temperamentId = it.temperamentId,
            geneQuality = it.geneQuality,
            isShiny = it.isShiny,
            nickname = it.nickname,
        )
    },
    rewardGold = rewardGold,
    rewardItemIds = rewardItemIds,
    introDialogueNodeId = introDialogueNodeId,
    defeatDialogueNodeId = defeatDialogueNodeId,
    victoryDialogueNodeId = victoryDialogueNodeId,
    rematchLevelBonus = rematchLevelBonus,
    battleMusicKey = battleMusicKey,
)

fun FactionDto.toDomain(): Faction = Faction(
    id = id,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    crestKey = crestKey,
    colorHex = colorHex,
    opposingFactionIds = opposingFactionIds,
    ranks = ranks.map {
        FactionRank(
            id = it.id,
            nameKey = it.nameKey,
            minReputation = it.minReputation,
            unlocksShopId = it.unlocksShopId,
            unlocksQuestIds = it.unlocksQuestIds,
            discountPercent = it.discountPercent,
        )
    },
    homeRegionId = homeRegionId,
)

// ---------------------------------------------------------------------------
// Story
// ---------------------------------------------------------------------------

fun QuestDto.toDomain(): Quest = Quest(
    id = id,
    nameKey = nameKey,
    summaryKey = summaryKey,
    descriptionKey = descriptionKey,
    category = category.toEnum<QuestCategory>("Questkategorie"),
    chapter = chapter,
    giverNpcId = giverNpcId,
    turnInNpcId = turnInNpcId,
    regionId = regionId,
    recommendedLevel = recommendedLevel,
    objectives = objectives.map {
        QuestObjective(
            id = it.id,
            descriptionKey = it.descriptionKey,
            type = it.type.toEnum<ObjectiveType>("Zieltyp"),
            targetId = it.targetId,
            requiredCount = it.requiredCount,
            optional = it.optional,
            hiddenUntilPrevious = it.hiddenUntilPrevious,
        )
    },
    rewards = rewards.map { it.toDomain() },
    prerequisite = prerequisite.toDomain(),
    prerequisiteQuestIds = prerequisiteQuestIds,
    grantsStoryFlags = grantsStoryFlags,
    repeatable = repeatable,
    timeLimitMinutes = timeLimitMinutes,
    isHidden = isHidden,
    factionId = factionId,
    factionReputationDelta = factionReputationDelta,
    nextQuestId = nextQuestId,
    cutsceneIdOnStart = cutsceneIdOnStart,
    cutsceneIdOnComplete = cutsceneIdOnComplete,
)

fun QuestRewardDto.toDomain(): QuestReward = when (this) {
    is QuestRewardDto.Gold -> QuestReward.Gold(amount)
    is QuestRewardDto.Items -> QuestReward.Items(itemIds)
    is QuestRewardDto.Experience -> QuestReward.Experience(amount)
    is QuestRewardDto.Monster -> QuestReward.Monster(speciesId, level)
    is QuestRewardDto.Reputation -> QuestReward.Reputation(factionId, amount)
    is QuestRewardDto.UnlockTitle -> QuestReward.UnlockTitle(titleId)
    is QuestRewardDto.UnlockRegion -> QuestReward.UnlockRegion(regionId)
    is QuestRewardDto.UnlockRecipe -> QuestReward.UnlockRecipe(recipeId)
    is QuestRewardDto.StoryFlag -> QuestReward.StoryFlag(flag)
}

fun ChapterDto.toDomain(): StoryChapter = StoryChapter(
    number = number,
    titleKey = titleKey,
    synopsisKey = synopsisKey,
    regionId = regionId,
    mainQuestIds = mainQuestIds,
    openingCutsceneId = openingCutsceneId,
    closingCutsceneId = closingCutsceneId,
    unlocksRegionIds = unlocksRegionIds,
    bossId = bossId,
    musicKey = musicKey,
)

fun CutsceneDto.toDomain(): Cutscene = Cutscene(
    id = id,
    titleKey = titleKey,
    beats = beats.map { it.toDomain() },
    musicKey = musicKey,
    skippable = skippable,
)

fun CutsceneBeatDto.toDomain(): CutsceneBeat = when (this) {
    is CutsceneBeatDto.Narration -> CutsceneBeat.Narration(textKey, durationMs)
    is CutsceneBeatDto.Speech -> CutsceneBeat.Speech(
        speakerNpcId, speakerNameKey, textKey, portraitKey, emotion,
    )
    is CutsceneBeatDto.ShowImage -> CutsceneBeat.ShowImage(imageKey, durationMs)
    is CutsceneBeatDto.PlaySound -> CutsceneBeat.PlaySound(soundKey)
    is CutsceneBeatDto.ChangeMusic -> CutsceneBeat.ChangeMusic(musicKey, fadeMs)
    is CutsceneBeatDto.ScreenEffect -> CutsceneBeat.ScreenEffect(effect, durationMs)
    is CutsceneBeatDto.GrantFlag -> CutsceneBeat.GrantFlag(flag)
    is CutsceneBeatDto.StartBattle -> CutsceneBeat.StartBattle(encounterId)
}

fun EndingDto.toDomain(): Ending = Ending(
    id = id,
    titleKey = titleKey,
    descriptionKey = descriptionKey,
    cutsceneId = cutsceneId,
    requiredFlags = requiredFlags.toSet(),
    forbiddenFlags = forbiddenFlags.toSet(),
    minMoralScore = minMoralScore,
    maxMoralScore = maxMoralScore,
    minFactionStanding = minFactionStanding,
    requiresAllRunes = requiresAllRunes,
    priority = priority,
)

fun DialogueTreeDto.toDomain(): DialogueTree = DialogueTree(
    id = id,
    npcId = npcId,
    entryNodeId = entryNodeId,
    nodes = nodes.map { it.toDomain() },
    priority = priority,
    conditions = conditions.map { it.toDomain() },
)

fun DialogueNodeDto.toDomain(): DialogueNode = DialogueNode(
    id = id,
    speakerNameKey = speakerNameKey,
    textKey = textKey,
    portraitKey = portraitKey,
    emotion = emotion,
    choices = choices.map { choice ->
        DialogueChoice(
            id = choice.id,
            textKey = choice.textKey,
            nextNodeId = choice.nextNodeId,
            conditions = choice.conditions.map { it.toDomain() },
            actions = choice.actions.map { it.toDomain() },
            moralWeight = choice.moralWeight,
            disabledHintKey = choice.disabledHintKey,
        )
    },
    nextNodeId = nextNodeId,
    actions = actions.map { it.toDomain() },
    conditions = conditions.map { it.toDomain() },
    voiceKey = voiceKey,
)

fun DialogueConditionDto.toDomain(): DialogueCondition = when (this) {
    is DialogueConditionDto.HasFlag -> DialogueCondition.HasFlag(flag)
    is DialogueConditionDto.MissingFlag -> DialogueCondition.MissingFlag(flag)
    is DialogueConditionDto.QuestInState -> DialogueCondition.QuestInState(
        questId, state.toEnum<QuestState>("Queststatus"),
    )
    is DialogueConditionDto.HasItem -> DialogueCondition.HasItem(itemId, count)
    is DialogueConditionDto.HasGold -> DialogueCondition.HasGold(amount)
    is DialogueConditionDto.MinLevel -> DialogueCondition.MinLevel(level)
    is DialogueConditionDto.TimeOfDay -> DialogueCondition.TimeOfDay(
        phase.toEnum<DayPhase>("Tageszeit"),
    )
    is DialogueConditionDto.MinReputation -> DialogueCondition.MinReputation(factionId, amount)
    is DialogueConditionDto.MaxReputation -> DialogueCondition.MaxReputation(factionId, amount)
    is DialogueConditionDto.HasSpecies -> DialogueCondition.HasSpecies(speciesId)
    is DialogueConditionDto.PartySize -> DialogueCondition.PartySize(min, max)
    is DialogueConditionDto.BestiaryCount -> DialogueCondition.BestiaryCount(min)
    is DialogueConditionDto.ChapterAtLeast -> DialogueCondition.ChapterAtLeast(chapter)
}

fun DialogueActionDto.toDomain(): DialogueAction = when (this) {
    is DialogueActionDto.GrantFlag -> DialogueAction.GrantFlag(flag)
    is DialogueActionDto.ClearFlag -> DialogueAction.ClearFlag(flag)
    is DialogueActionDto.StartQuest -> DialogueAction.StartQuest(questId)
    is DialogueActionDto.CompleteObjective -> DialogueAction.CompleteObjective(
        questId, objectiveId, amount,
    )
    is DialogueActionDto.GiveItem -> DialogueAction.GiveItem(itemId, count)
    is DialogueActionDto.TakeItem -> DialogueAction.TakeItem(itemId, count)
    is DialogueActionDto.GiveGold -> DialogueAction.GiveGold(amount)
    is DialogueActionDto.TakeGold -> DialogueAction.TakeGold(amount)
    is DialogueActionDto.GiveMonster -> DialogueAction.GiveMonster(speciesId, level)
    is DialogueActionDto.ChangeReputation -> DialogueAction.ChangeReputation(factionId, amount)
    is DialogueActionDto.OpenShop -> DialogueAction.OpenShop(shopId)
    is DialogueActionDto.StartBattle -> DialogueAction.StartBattle(encounterId)
    is DialogueActionDto.PlayCutscene -> DialogueAction.PlayCutscene(cutsceneId)
    is DialogueActionDto.HealParty -> DialogueAction.HealParty(full)
    is DialogueActionDto.UnlockFastTravel -> DialogueAction.UnlockFastTravel(locationId)
    is DialogueActionDto.ChangeMoralScore -> DialogueAction.ChangeMoralScore(amount)
    is DialogueActionDto.EndDialogue -> DialogueAction.EndDialogue
}

fun TitleDto.toDomain(): Title = Title(
    id = id,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    unlockConditionKey = unlockConditionKey,
    captureBonusPercent = captureBonusPercent,
    goldBonusPercent = goldBonusPercent,
    experienceBonusPercent = experienceBonusPercent,
    rarityTier = rarityTier,
)

fun AchievementDto.toDomain(): Achievement = Achievement(
    id = id,
    nameKey = nameKey,
    descriptionKey = descriptionKey,
    iconKey = iconKey,
    category = category.toEnum<AchievementCategory>("Errungenschaftskategorie"),
    progressTarget = progressTarget,
    hidden = hidden,
    rewardTitleId = rewardTitleId,
    rewardItemId = rewardItemId,
    points = points,
)
