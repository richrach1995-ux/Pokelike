package com.runeveil.saga.domain

import com.runeveil.saga.domain.battle.MapBattleContent
import com.runeveil.saga.domain.model.battle.BattleSide
import com.runeveil.saga.domain.model.battle.BattleState
import com.runeveil.saga.domain.model.battle.BattleType
import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.battle.MoveCategory
import com.runeveil.saga.domain.model.battle.MoveEffect
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.ItemCategory
import com.runeveil.saga.domain.model.item.OrbCondition
import com.runeveil.saga.domain.model.item.OrbGrade
import com.runeveil.saga.domain.model.item.OrbSpec
import com.runeveil.saga.domain.model.monster.Ability
import com.runeveil.saga.domain.model.monster.AbilityTrigger
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.GrowthRate
import com.runeveil.saga.domain.model.monster.LearnsetEntry
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.MoveSlot
import com.runeveil.saga.domain.model.monster.Rarity
import com.runeveil.saga.domain.model.monster.SizeClass
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.Temperament
import com.runeveil.saga.domain.model.monster.World

/**
 * Hand-built content used by the rule tests.
 *
 * Deliberately small and explicit: every number here is chosen so that the
 * expected outcome of a test can be computed by hand.
 */
object TestFixtures {

    fun species(
        id: String = "test_ember",
        primary: Element = Element.FIRE,
        secondary: Element? = null,
        stats: StatBlock = StatBlock(hp = 80, attack = 90, defense = 70, magic = 85, resistance = 65, speed = 75, luck = 40),
        rarity: Rarity = Rarity.COMMON,
        catchRate: Int = 120,
        growthRate: GrowthRate = GrowthRate.STEADY,
        learnset: List<LearnsetEntry> = listOf(LearnsetEntry(1, "move_strike"), LearnsetEntry(10, "move_flame")),
        evolutions: List<com.runeveil.saga.domain.model.monster.EvolutionPath> = emptyList(),
        eggGroups: List<String> = listOf("beast"),
        abilities: List<String> = emptyList(),
    ) = MonsterSpecies(
        id = id,
        dexNumber = 1,
        nameKey = "species_$id",
        descriptionKey = "species_${id}_desc",
        loreKey = "species_${id}_lore",
        primaryElement = primary,
        secondaryElement = secondary,
        familyId = "${id}_family",
        stage = 1,
        baseStats = stats,
        rarity = rarity,
        growthRate = growthRate,
        catchRate = catchRate,
        baseExperience = 64,
        sizeClass = SizeClass.MEDIUM,
        heightCm = 110,
        weightHg = 340,
        nativeWorld = World.MIDGARD,
        eggGroups = eggGroups,
        abilityIds = abilities,
        learnset = learnset,
        evolutions = evolutions,
        spriteKey = "spr_$id",
        cryKey = "cry_$id",
    )

    fun monster(
        uid: String = "m1",
        species: MonsterSpecies = species(),
        level: Int = 50,
        genes: StatBlock = StatBlock.uniform(15),
        training: StatBlock = StatBlock.ZERO,
        temperament: Temperament = Temperament.EVEN,
        moveIds: List<String> = listOf("move_strike"),
        abilityId: String? = null,
        gender: Gender = Gender.MALE,
    ): MonsterInstance {
        val instance = MonsterInstance(
            uid = uid,
            species = species,
            level = level,
            genes = genes,
            training = training,
            temperament = temperament,
            gender = gender,
            abilityId = abilityId,
            moves = moveIds.map { MoveSlot(it, 20, 20) },
        )
        return instance.copy(currentHp = instance.maxHp)
    }

    fun battler(
        id: String = "b1",
        side: BattleSide = BattleSide.PLAYER,
        monster: MonsterInstance = monster(),
        abilityEffectId: String? = null,
    ) = Battler(id = id, side = side, slot = 0, monster = monster, abilityEffectId = abilityEffectId)

    fun move(
        id: String = "move_strike",
        element: Element = Element.FIRE,
        category: MoveCategory = MoveCategory.PHYSICAL,
        power: Int = 60,
        accuracy: Int = 100,
        pp: Int = 20,
        priority: Int = 0,
        effects: List<MoveEffect> = emptyList(),
        alwaysHits: Boolean = false,
        comboTag: String? = null,
        target: com.runeveil.saga.domain.model.battle.MoveTarget =
            com.runeveil.saga.domain.model.battle.MoveTarget.SINGLE_OPPONENT,
    ) = Move(
        id = id,
        nameKey = "move_${id}_name",
        descriptionKey = "move_${id}_desc",
        element = element,
        category = category,
        power = power,
        accuracy = accuracy,
        maxPp = pp,
        priority = priority,
        target = target,
        effects = effects,
        alwaysHits = alwaysHits,
        animationKey = "anim_$id",
        soundKey = "sfx_$id",
        comboTag = comboTag,
    )

    fun ability(id: String = "ab_test", effectId: String = "regrowth") = Ability(
        id = id,
        nameKey = "ability_$id",
        descriptionKey = "ability_${id}_desc",
        trigger = AbilityTrigger.ON_TURN_END,
        effectId = effectId,
    )

    fun orb(
        id: String = "orb_iron",
        grade: OrbGrade = OrbGrade.IRON,
        multiplier: Double = 1.5,
        condition: OrbCondition = OrbCondition.NONE,
        conditionalBonus: Double = 1.0,
    ) = Item(
        id = id,
        nameKey = "item_$id",
        descriptionKey = "item_${id}_desc",
        category = ItemCategory.RUNE_ORB,
        price = 300,
        usableInBattle = true,
        orbSpec = OrbSpec(
            grade = grade,
            catchMultiplier = multiplier,
            condition = condition,
            conditionalBonus = conditionalBonus,
            shakeAnimationKey = "anim_shake",
        ),
        iconKey = "ic_$id",
    )

    fun content(
        moves: List<Move> = listOf(move()),
        abilities: List<Ability> = emptyList(),
        items: List<Item> = listOf(orb()),
    ) = MapBattleContent(
        moves = moves.associateBy { it.id },
        abilities = abilities.associateBy { it.id },
        items = items.associateBy { it.id },
    )

    fun battleState(
        player: List<Battler>,
        enemy: List<Battler>,
        type: BattleType = BattleType.WILD,
    ) = BattleState(
        battleId = "test_battle",
        type = type,
        playerTeam = player,
        enemyTeam = enemy,
    )
}
