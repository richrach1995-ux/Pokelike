package com.runeveil.saga.data.database

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.MoveSlot
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.Temperament
import com.runeveil.saga.domain.model.player.Appearance
import com.runeveil.saga.domain.model.player.PlayerStatistics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Conversions between Room entities and domain models.
 *
 * The JSON columns are decoded with the injected [Json] instance;
 * `ignoreUnknownKeys` (configured in the DI module) means an older save can be
 * read after a field was added.
 */

@Serializable
private data class MoveSlotJson(
    @SerialName("moveId") val moveId: String,
    @SerialName("currentPp") val currentPp: Int,
    @SerialName("maxPp") val maxPp: Int,
    @SerialName("ppUps") val ppUps: Int = 0,
)

@Serializable
data class AppearanceJson(
    val bodyKey: String = "body_01",
    val hairKey: String = "hair_01",
    val hairColorHex: Long = 0xFF3A2A1E,
    val skinToneKey: String = "skin_02",
    val eyeKey: String = "eyes_01",
    val eyeColorHex: Long = 0xFF4A6B8A,
    val outfitKey: String = "outfit_wanderer",
    val markingKey: String? = null,
    val accessoryKey: String? = null,
)

@Serializable
data class StatisticsJson(
    val battlesWon: Int = 0,
    val battlesLost: Int = 0,
    val monstersCaught: Int = 0,
    val monstersSeen: Int = 0,
    val shiniesFound: Int = 0,
    val eggsHatched: Int = 0,
    val evolutionsPerformed: Int = 0,
    val itemsCrafted: Int = 0,
    val questsCompleted: Int = 0,
    val bossesDefeated: Int = 0,
    val criticalHitsLanded: Int = 0,
    val stepsInEachWorld: Map<String, Long> = emptyMap(),
    val deepestEndlessFloor: Int = 0,
    val tournamentsWon: Int = 0,
    val newGamePlusCount: Int = 0,
)

fun StatBlock.toEmbedded(): EmbeddedStats =
    EmbeddedStats(hp, attack, defense, magic, resistance, speed, luck)

fun EmbeddedStats.toStatBlock(): StatBlock =
    StatBlock(hp, attack, defense, magic, resistance, speed, luck)

/**
 * Rebuilds a full [MonsterInstance].
 *
 * @param speciesResolver resolves the species id; returns null for content that
 *   no longer exists, in which case the monster is skipped rather than crashing
 *   the whole party load.
 */
fun MonsterEntity.toDomain(
    json: Json,
    speciesResolver: (String) -> MonsterSpecies?,
): MonsterInstance? {
    val species = speciesResolver(speciesId) ?: return null
    val moves = runCatching {
        json.decodeFromString<List<MoveSlotJson>>(movesJson)
    }.getOrDefault(emptyList()).map { MoveSlot(it.moveId, it.currentPp, it.maxPp, it.ppUps) }

    return MonsterInstance(
        uid = uid,
        species = species,
        nickname = nickname,
        level = level,
        experience = experience,
        genes = genes.toStatBlock(),
        training = training.toStatBlock(),
        temperament = enumValueOrDefault(temperament, Temperament.EVEN),
        gender = enumValueOrDefault(gender, Gender.UNKNOWN),
        isShiny = isShiny,
        abilityId = abilityId,
        moves = moves,
        currentHp = currentHp,
        status = status?.let { enumValueOrNull<StatusCondition>(it) },
        statusTurns = statusTurns,
        friendship = friendship,
        unlockedTalentIds = unlockedTalents.split(",").filter { it.isNotBlank() }.toSet(),
        spentTalentPoints = spentTalentPoints,
        heldItemId = heldItemId,
        boundRuneIds = boundRunes.split(",").filter { it.isNotBlank() },
        originLocationId = originLocationId,
        originalTrainerName = originalTrainerName,
        caughtAtEpochMs = caughtAtEpochMs,
        caughtWithOrbId = caughtWithOrbId,
        eggHatchStepsRemaining = eggHatchStepsRemaining,
        isEgg = isEgg,
        talentStatBonus = talentBonus.toStatBlock(),
    )
}

fun MonsterInstance.toEntity(
    json: Json,
    saveSlot: Int,
    inParty: Boolean,
    partyOrder: Int,
    atRoost: Boolean = false,
): MonsterEntity = MonsterEntity(
    uid = uid,
    saveSlot = saveSlot,
    speciesId = species.id,
    nickname = nickname,
    level = level,
    experience = experience,
    genes = genes.toEmbedded(),
    training = training.toEmbedded(),
    talentBonus = talentStatBonus.toEmbedded(),
    temperament = temperament.name,
    gender = gender.name,
    isShiny = isShiny,
    abilityId = abilityId,
    movesJson = json.encodeToString(
        moves.map { MoveSlotJson(it.moveId, it.currentPp, it.maxPp, it.ppUps) },
    ),
    currentHp = currentHp,
    status = status?.name,
    statusTurns = statusTurns,
    friendship = friendship,
    unlockedTalents = unlockedTalentIds.joinToString(","),
    spentTalentPoints = spentTalentPoints,
    heldItemId = heldItemId,
    boundRunes = boundRuneIds.joinToString(","),
    originLocationId = originLocationId,
    originalTrainerName = originalTrainerName,
    caughtAtEpochMs = caughtAtEpochMs,
    caughtWithOrbId = caughtWithOrbId,
    eggHatchStepsRemaining = eggHatchStepsRemaining,
    isEgg = isEgg,
    inParty = inParty,
    partyOrder = partyOrder,
    atRoost = atRoost,
)

fun AppearanceJson.toDomain(): Appearance = Appearance(
    bodyKey, hairKey, hairColorHex, skinToneKey, eyeKey, eyeColorHex,
    outfitKey, markingKey, accessoryKey,
)

fun Appearance.toJsonModel(): AppearanceJson = AppearanceJson(
    bodyKey, hairKey, hairColorHex, skinToneKey, eyeKey, eyeColorHex,
    outfitKey, markingKey, accessoryKey,
)

fun StatisticsJson.toDomain(): PlayerStatistics = PlayerStatistics(
    battlesWon, battlesLost, monstersCaught, monstersSeen, shiniesFound, eggsHatched,
    evolutionsPerformed, itemsCrafted, questsCompleted, bossesDefeated, criticalHitsLanded,
    stepsInEachWorld, deepestEndlessFloor, tournamentsWon, newGamePlusCount,
)

fun PlayerStatistics.toJsonModel(): StatisticsJson = StatisticsJson(
    battlesWon, battlesLost, monstersCaught, monstersSeen, shiniesFound, eggsHatched,
    evolutionsPerformed, itemsCrafted, questsCompleted, bossesDefeated, criticalHitsLanded,
    stepsInEachWorld, deepestEndlessFloor, tournamentsWon, newGamePlusCount,
)

internal inline fun <reified T : Enum<T>> enumValueOrNull(raw: String): T? =
    enumValues<T>().firstOrNull { it.name == raw }

internal inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, fallback: T): T =
    enumValueOrNull<T>(raw) ?: fallback
