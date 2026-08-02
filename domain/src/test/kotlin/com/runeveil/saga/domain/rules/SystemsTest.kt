package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.TestFixtures
import com.runeveil.saga.domain.model.item.CraftingStation
import com.runeveil.saga.domain.model.item.InventoryStack
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.ItemCategory
import com.runeveil.saga.domain.model.item.Recipe
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.EvolutionPath
import com.runeveil.saga.domain.model.monster.EvolutionTrigger
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.GrowthRate
import com.runeveil.saga.domain.model.monster.LearnsetEntry
import com.runeveil.saga.domain.model.monster.Rarity
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.TalentNode
import com.runeveil.saga.domain.model.monster.World
import com.runeveil.saga.domain.model.story.ObjectiveType
import com.runeveil.saga.domain.model.story.Quest
import com.runeveil.saga.domain.model.story.QuestCategory
import com.runeveil.saga.domain.model.story.QuestObjective
import com.runeveil.saga.domain.model.story.QuestProgress
import com.runeveil.saga.domain.model.story.QuestState
import com.runeveil.saga.domain.model.world.UnlockRequirement
import com.runeveil.saga.domain.util.ScriptedRng
import com.runeveil.saga.domain.util.SeededRng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionRulesTest {

    @Test
    fun `granting experience levels a monster up and reports stat gains`() {
        val monster = TestFixtures.monster(level = 10)
        val needed = ExperienceCurve.totalAt(monster.species.growthRate, 15)

        val result = ProgressionRules.grantExperience(monster.copy(experience = 0), needed)

        assertEquals(15, result.monster.level)
        assertEquals(5, result.levelsGained)
        assertTrue(result.statGains.values.any { it > 0 })
        assertTrue(result.talentPointsGained > 0)
    }

    @Test
    fun `levelling never heals a monster beyond its stat growth`() {
        val monster = TestFixtures.monster(level = 10).let { it.copy(currentHp = 5) }
        val hpBefore = monster.maxHp
        val result = ProgressionRules.grantExperience(
            monster,
            ExperienceCurve.totalAt(monster.species.growthRate, 20),
        )
        val hpGrowth = result.monster.maxHp - hpBefore
        assertEquals(5 + hpGrowth, result.monster.currentHp)
    }

    @Test
    fun `experience stops at level one hundred`() {
        val monster = TestFixtures.monster(level = 99)
        val result = ProgressionRules.grantExperience(monster, Long.MAX_VALUE / 4)
        assertEquals(100, result.monster.level)
    }

    @Test
    fun `level ups report newly learnable moves`() {
        val species = TestFixtures.species(
            learnset = listOf(LearnsetEntry(1, "move_strike"), LearnsetEntry(12, "move_flame")),
        )
        val monster = TestFixtures.monster(species = species, level = 10, moveIds = listOf("move_strike"))
            .let { it.copy(experience = ExperienceCurve.totalAt(species.growthRate, 10)) }

        val result = ProgressionRules.grantExperience(
            monster,
            ExperienceCurve.totalAt(species.growthRate, 13) - monster.experience,
        )

        assertTrue("move_flame" in result.learnableMoveIds)
    }

    @Test
    fun `a monster with four moves must replace one to learn a fifth`() {
        val monster = TestFixtures.monster(moveIds = listOf("m1", "m2", "m3", "m4"))
        assertNull(ProgressionRules.learnMove(monster, "m5", 15))
        val replaced = ProgressionRules.learnMove(monster, "m5", 15, replaceSlotIndex = 1)
        assertNotNull(replaced)
        assertEquals("m5", replaced!!.moves[1].moveId)
    }

    @Test
    fun `talents require points and prerequisites`() {
        val tree = listOf(
            TalentNode("t1", "n1", "d1", tier = 1, cost = 1, statBonus = StatBlock(attack = 5)),
            TalentNode("t2", "n2", "d2", tier = 2, cost = 2, requires = listOf("t1")),
        )
        val monster = TestFixtures.monster(level = 10) // 5 talent points

        assertNull("prerequisite missing", ProgressionRules.unlockTalent(monster, tree[1], tree))

        val first = ProgressionRules.unlockTalent(monster, tree[0], tree)
        assertNotNull(first)
        assertEquals(5, first!!.talentStatBonus.attack)
        assertEquals(4, first.availableTalentPoints)

        val second = ProgressionRules.unlockTalent(first, tree[1], tree)
        assertNotNull(second)
    }

    @Test
    fun `respec refunds every talent point`() {
        val tree = listOf(TalentNode("t1", "n1", "d1", tier = 1, cost = 2, statBonus = StatBlock(attack = 5)))
        val monster = ProgressionRules.unlockTalent(TestFixtures.monster(level = 20), tree[0], tree)!!
        val reset = ProgressionRules.respecTalents(monster)
        assertEquals(0, reset.spentTalentPoints)
        assertEquals(StatBlock.ZERO, reset.talentStatBonus)
        assertEquals(10, reset.availableTalentPoints)
    }

    @Test
    fun `rune binding respects the capacity`() {
        val monster = TestFixtures.monster()
        val once = ProgressionRules.bindRune(monster, "rune_fire", capacity = 1)!!
        assertNull(ProgressionRules.bindRune(once, "rune_ice", capacity = 1))
        assertNotNull(ProgressionRules.bindRune(once, "rune_ice", capacity = 2))
    }

    @Test
    fun `friendship gains shrink as the bond grows`() {
        val early = ProgressionRules.friendshipDelta(ProgressionRules.FriendshipEvent.LEVEL_UP, 50)
        val late = ProgressionRules.friendshipDelta(ProgressionRules.FriendshipEvent.LEVEL_UP, 240)
        assertTrue("early=$early late=$late", early > late)
        assertTrue(ProgressionRules.friendshipDelta(ProgressionRules.FriendshipEvent.FAINTED, 200) < 0)
    }
}

class BreedingRulesTest {

    private fun parent(
        uid: String,
        gender: Gender,
        eggGroups: List<String> = listOf("beast"),
        world: World = World.MIDGARD,
        genes: StatBlock = StatBlock.uniform(20),
    ) = TestFixtures.monster(
        uid = uid,
        gender = gender,
        genes = genes,
        species = TestFixtures.species(id = "sp_$uid", eggGroups = eggGroups).copy(nativeWorld = world),
    )

    private fun resolver(vararg species: com.runeveil.saga.domain.model.monster.MonsterSpecies) =
        { id: String -> species.firstOrNull { it.id == id } }

    @Test
    fun `same gender pairs cannot breed`() {
        val a = parent("a", Gender.MALE)
        val b = parent("b", Gender.MALE)
        assertEquals(BreedingRules.Compatibility.SameGender, BreedingRules.compatibility(a, b))
    }

    @Test
    fun `different egg groups cannot breed`() {
        val a = parent("a", Gender.MALE, eggGroups = listOf("beast"))
        val b = parent("b", Gender.FEMALE, eggGroups = listOf("spirit"))
        assertEquals(BreedingRules.Compatibility.NoSharedEggGroup, BreedingRules.compatibility(a, b))
    }

    @Test
    fun `legendary monsters never breed`() {
        val a = TestFixtures.monster(
            uid = "a",
            gender = Gender.MALE,
            species = TestFixtures.species(id = "legend", rarity = Rarity.MYTHIC),
        )
        val b = parent("b", Gender.FEMALE)
        assertEquals(BreedingRules.Compatibility.LegendaryLineage, BreedingRules.compatibility(a, b))
    }

    @Test
    fun `a seidr vessel breeds with anything`() {
        val vessel = parent("v", Gender.MALE, eggGroups = listOf(BreedingRules.UNIVERSAL_EGG_GROUP))
        val other = parent("o", Gender.MALE, eggGroups = listOf("beast"))
        assertEquals(BreedingRules.Compatibility.Compatible, BreedingRules.compatibility(vessel, other))
    }

    @Test
    fun `offspring inherits genes from its parents`() {
        val mother = parent("m", Gender.FEMALE, genes = StatBlock.uniform(31))
        val father = parent("f", Gender.MALE, genes = StatBlock.uniform(31))
            .let { it.copy(species = it.species.copy(eggGroups = listOf("beast"))) }
        val childSpecies = mother.species

        val blueprint = BreedingRules.breed(
            mother = mother,
            father = father,
            speciesResolver = resolver(childSpecies),
            familyBaseResolver = { childSpecies.id },
            rng = SeededRng(1234),
        )

        assertNotNull(blueprint)
        // At least the three inherited genes must be perfect.
        val perfectGenes = com.runeveil.saga.domain.model.monster.Stat.entries
            .count { blueprint!!.genes[it] == 31 }
        assertTrue("only $perfectGenes perfect genes", perfectGenes >= BreedingRules.INHERITED_GENES)
    }

    @Test
    fun `egg moves known by a parent are passed on`() {
        val childSpecies = TestFixtures.species(id = "child").copy(
            eggMoveIds = listOf("move_heirloom"),
            learnset = listOf(LearnsetEntry(1, "move_strike")),
        )
        val mother = parent("m", Gender.FEMALE).copy(species = childSpecies)
        val father = parent("f", Gender.MALE, eggGroups = listOf("beast"))
            .let { it.copy(moves = listOf(com.runeveil.saga.domain.model.monster.MoveSlot("move_heirloom", 10, 10))) }

        val blueprint = BreedingRules.breed(
            mother, father, resolver(childSpecies), { childSpecies.id }, SeededRng(7),
        )

        assertTrue("move_heirloom" in blueprint!!.inheritedMoveIds)
    }

    @Test
    fun `foreign blood improves the shiny odds`() {
        assertTrue(
            BreedingRules.FOREIGN_BLOOD_SHINY_DENOMINATOR < BreedingRules.BASE_SHINY_DENOMINATOR,
        )
    }

    @Test
    fun `the roost fee grows with level and rarity`() {
        val cheap = BreedingRules.roostFee(TestFixtures.monster(level = 5), TestFixtures.monster(uid = "b", level = 5))
        val pricey = BreedingRules.roostFee(TestFixtures.monster(level = 80), TestFixtures.monster(uid = "b", level = 80))
        assertTrue(pricey > cheap)
    }

    @Test
    fun `a materialised egg starts as an egg at level one`() {
        val species = TestFixtures.species(id = "child")
        val blueprint = BreedingRules.EggBlueprint(
            speciesId = species.id,
            genes = StatBlock.uniform(20),
            temperament = com.runeveil.saga.domain.model.monster.Temperament.EVEN,
            gender = Gender.FEMALE,
            isShiny = false,
            inheritedMoveIds = listOf("move_strike"),
            inheritedTalentIds = emptySet(),
            isHybrid = false,
            hatchSteps = 2560,
            mutatedStats = emptySet(),
        )
        val egg = BreedingRules.materialise(blueprint, species, "egg1", { 20 }, "Tester", 0L)
        assertTrue(egg.isEgg)
        assertEquals(1, egg.level)
        assertEquals(2560, egg.eggHatchStepsRemaining)
        assertEquals(egg.maxHp, egg.currentHp)
    }
}

class EvolutionRulesTest {

    private val context = EvolutionRules.Context(
        dayPhase = DayPhase.NIGHT,
        world = World.NIFLHEIM,
        locationId = "loc_frozen_shrine",
        weatherId = "SNOW",
        storyFlags = setOf("chapter_3_done"),
    )

    private fun speciesWith(vararg paths: EvolutionPath) =
        TestFixtures.species(evolutions = paths.toList())

    @Test
    fun `a level requirement gates the evolution`() {
        val species = speciesWith(
            EvolutionPath(
                targetSpeciesId = "next",
                trigger = EvolutionTrigger.LEVEL_UP,
                minLevel = 30,
                descriptionKey = "evo_level_30",
            ),
        )
        assertNull(EvolutionRules.evaluate(TestFixtures.monster(species = species, level = 29), context))
        assertNotNull(EvolutionRules.evaluate(TestFixtures.monster(species = species, level = 30), context))
    }

    @Test
    fun `every declared condition must hold`() {
        val species = speciesWith(
            EvolutionPath(
                targetSpeciesId = "next",
                trigger = EvolutionTrigger.LEVEL_UP,
                minLevel = 20,
                requiredTimeOfDay = DayPhase.DAY,
                descriptionKey = "evo_day",
            ),
        )
        // It is night in the context, so the path must not fire.
        assertNull(EvolutionRules.evaluate(TestFixtures.monster(species = species, level = 40), context))
    }

    @Test
    fun `friendship evolutions need both the trigger and the value`() {
        val species = speciesWith(
            EvolutionPath(
                targetSpeciesId = "next",
                trigger = EvolutionTrigger.FRIENDSHIP,
                minFriendship = 220,
                descriptionKey = "evo_bond",
            ),
        )
        val friendly = TestFixtures.monster(species = species).copy(friendship = 230)
        val distant = TestFixtures.monster(species = species).copy(friendship = 100)
        val friendshipContext = context.copy(triggeredBy = EvolutionTrigger.FRIENDSHIP)

        assertNotNull(EvolutionRules.evaluate(friendly, friendshipContext))
        assertNull(EvolutionRules.evaluate(distant, friendshipContext))
    }

    @Test
    fun `item evolutions require the exact item`() {
        val species = speciesWith(
            EvolutionPath(
                targetSpeciesId = "next",
                trigger = EvolutionTrigger.USE_ITEM,
                requiredItemId = "stone_frost",
                descriptionKey = "evo_item",
            ),
        )
        val monster = TestFixtures.monster(species = species)
        assertNull(
            EvolutionRules.evaluate(
                monster,
                context.copy(triggeredBy = EvolutionTrigger.USE_ITEM, usedItemId = "stone_ember"),
            ),
        )
        assertNotNull(
            EvolutionRules.evaluate(
                monster,
                context.copy(triggeredBy = EvolutionTrigger.USE_ITEM, usedItemId = "stone_frost"),
            ),
        )
    }

    @Test
    fun `evolving keeps identity and scales current hp`() {
        val base = TestFixtures.species(id = "base", stats = StatBlock.uniform(60))
        val evolved = TestFixtures.species(id = "evolved", stats = StatBlock.uniform(110))
        val monster = TestFixtures.monster(species = base, level = 40)
            .let { it.copy(currentHp = it.maxHp / 2, nickname = "Sigrun") }

        val result = EvolutionRules.applyEvolution(monster, evolved)

        assertEquals("evolved", result.species.id)
        assertEquals("Sigrun", result.nickname)
        assertTrue(result.maxHp > monster.maxHp)
        assertEquals(0.5f, result.hpFraction, 0.05f)
    }

    @Test
    fun `eggs never evolve`() {
        val species = speciesWith(
            EvolutionPath("next", EvolutionTrigger.LEVEL_UP, minLevel = 1, descriptionKey = "e"),
        )
        val egg = TestFixtures.monster(species = species).copy(isEgg = true)
        assertNull(EvolutionRules.evaluate(egg, context))
    }
}

class InventoryRulesTest {

    private val potion = Item(
        id = "potion",
        nameKey = "n", descriptionKey = "d",
        category = ItemCategory.HEALING,
        price = 200, stackLimit = 99, iconKey = "ic",
    )

    @Test
    fun `adding beyond the stack limit opens a second stack`() {
        val stacks = InventoryRules.add(emptyList(), potion, 150)
        assertEquals(2, stacks.size)
        assertEquals(150, InventoryRules.countOf(stacks, "potion"))
    }

    @Test
    fun `removing more than owned fails`() {
        val stacks = InventoryRules.add(emptyList(), potion, 5)
        assertNull(InventoryRules.remove(stacks, "potion", 6))
        assertNotNull(InventoryRules.remove(stacks, "potion", 5))
    }

    @Test
    fun `emptied stacks disappear`() {
        val stacks = InventoryRules.add(emptyList(), potion, 3)
        val emptied = InventoryRules.remove(stacks, "potion", 3)!!
        assertTrue(emptied.isEmpty())
    }

    @Test
    fun `faction discounts lower the buy price`() {
        assertEquals(200, InventoryRules.buyPrice(potion, markup = 1.0, discountPercent = 0))
        assertEquals(180, InventoryRules.buyPrice(potion, markup = 1.0, discountPercent = 10))
        assertEquals(240, InventoryRules.buyPrice(potion, markup = 1.2, discountPercent = 0))
    }

    @Test
    fun `crafting consumes ingredients and yields the result`() {
        val ore = Item("ore", "n", "d", ItemCategory.MATERIAL, iconKey = "ic")
        val ingot = Item("ingot", "n", "d", ItemCategory.MATERIAL, iconKey = "ic")
        val recipe = Recipe(
            id = "r", nameKey = "n", descriptionKey = "d",
            station = CraftingStation.FORGE,
            ingredients = mapOf("ore" to 3),
            resultItemId = "ingot",
        )
        val stacks = InventoryRules.add(emptyList(), ore, 5)
        assertTrue(InventoryRules.canCraft(stacks, recipe, gold = 0))

        val result = InventoryRules.craft(stacks, recipe, ingot, null, SeededRng(1))

        assertTrue(result.success)
        assertEquals(2, InventoryRules.countOf(result.stacks, "ore"))
        assertEquals(1, InventoryRules.countOf(result.stacks, "ingot"))
    }

    @Test
    fun `a failed legendary craft consumes half the ingredients`() {
        val ore = Item("ore", "n", "d", ItemCategory.MATERIAL, iconKey = "ic")
        val relic = Item("relic", "n", "d", ItemCategory.TREASURE, iconKey = "ic")
        val recipe = Recipe(
            id = "r", nameKey = "n", descriptionKey = "d",
            station = CraftingStation.ALTAR,
            ingredients = mapOf("ore" to 4),
            resultItemId = "relic",
            successChance = 0.5,
        )
        val stacks = InventoryRules.add(emptyList(), ore, 4)
        val alwaysFails = ScriptedRng(doubles = listOf(0.99))

        val result = InventoryRules.craft(stacks, recipe, relic, null, alwaysFails)

        assertFalse(result.success)
        assertEquals(2, InventoryRules.countOf(result.stacks, "ore"))
        assertEquals(0, InventoryRules.countOf(result.stacks, "relic"))
    }

    @Test
    fun `stack counting sums across stacks`() {
        val stacks = listOf(InventoryStack("potion", 40), InventoryStack("potion", 30))
        assertEquals(70, InventoryRules.countOf(stacks, "potion"))
    }
}

class QuestRulesTest {

    private val quest = Quest(
        id = "q_hunt",
        nameKey = "n", summaryKey = "s", descriptionKey = "d",
        category = QuestCategory.SIDE,
        objectives = listOf(
            QuestObjective("o_kill", "d", ObjectiveType.DEFEAT_SPECIES, targetId = "wolf", requiredCount = 3),
            QuestObjective("o_talk", "d", ObjectiveType.TALK_TO_NPC, targetId = "npc_elder"),
        ),
        prerequisite = UnlockRequirement.Chapter(2),
    )

    private fun active() = QuestProgress(quest.id, QuestState.ACTIVE)

    @Test
    fun `objectives count up and cap at the requirement`() {
        var progress = active()
        repeat(5) {
            progress = QuestRules.applyEvent(quest, progress, QuestRules.GameEvent.DefeatedSpecies("wolf"))
        }
        assertEquals(3, progress.countFor("o_kill"))
    }

    @Test
    fun `unrelated events are ignored`() {
        val progress = QuestRules.applyEvent(quest, active(), QuestRules.GameEvent.DefeatedSpecies("bear"))
        assertEquals(0, progress.countFor("o_kill"))
    }

    @Test
    fun `a quest becomes turn-in-able once every objective is done`() {
        var progress = active()
        repeat(3) {
            progress = QuestRules.applyEvent(quest, progress, QuestRules.GameEvent.DefeatedSpecies("wolf"))
        }
        assertEquals(QuestState.ACTIVE, progress.state)
        progress = QuestRules.applyEvent(quest, progress, QuestRules.GameEvent.TalkedToNpc("npc_elder"))
        assertEquals(QuestState.READY_TO_TURN_IN, progress.state)
    }

    @Test
    fun `progress is reported as a fraction`() {
        var progress = active()
        repeat(3) {
            progress = QuestRules.applyEvent(quest, progress, QuestRules.GameEvent.DefeatedSpecies("wolf"))
        }
        assertEquals(0.5f, progress.progress(quest), 0.001f)
    }

    @Test
    fun `chapter prerequisites gate availability`() {
        val early = QuestRules.AvailabilityContext(
            storyFlags = emptySet(), chapter = 1, completedQuestIds = emptySet(),
            keyItemIds = emptySet(), runeCount = 0, reputation = emptyMap(),
        )
        assertFalse(QuestRules.isAvailable(quest, early))
        assertTrue(QuestRules.isAvailable(quest, early.copy(chapter = 2)))
    }

    @Test
    fun `combined requirements need every part`() {
        val requirement = UnlockRequirement.All(
            listOf(
                UnlockRequirement.Chapter(3),
                UnlockRequirement.StoryFlag("met_the_seeress"),
                UnlockRequirement.Reputation("runewardens", 200),
            ),
        )
        val context = QuestRules.AvailabilityContext(
            storyFlags = setOf("met_the_seeress"), chapter = 3, completedQuestIds = emptySet(),
            keyItemIds = emptySet(), runeCount = 0, reputation = mapOf("runewardens" to 150),
        )
        assertFalse(QuestRules.meets(requirement, context))
        assertTrue(QuestRules.meets(requirement, context.copy(reputation = mapOf("runewardens" to 400))))
    }

    @Test
    fun `dailies reset across the four o'clock boundary`() {
        val day = 24L * 60 * 60 * 1000
        val monday10 = 10L * 60 * 60 * 1000
        assertFalse(QuestRules.shouldResetDaily(monday10, monday10 + 3 * 60 * 60 * 1000))
        assertTrue(QuestRules.shouldResetDaily(monday10, monday10 + day))
    }
}

class EncounterRulesTest {

    private val location = com.runeveil.saga.domain.model.world.Location(
        id = "loc_route",
        regionId = "midgard",
        nameKey = "n", descriptionKey = "d",
        type = com.runeveil.saga.domain.model.world.LocationType.ROUTE,
        mapX = 0f, mapY = 0f,
        backgroundKey = "bg",
        encounterTableId = "tbl",
        encounterRate = 0.12,
    )

    @Test
    fun `repels block encounters entirely`() {
        val always = ScriptedRng(doubles = listOf(0.0))
        assertFalse(EncounterRules.rollEncounter(location, 50, repelMultiplier = 0.0, rng = always))
    }

    @Test
    fun `encounters need a minimum number of steps`() {
        val always = ScriptedRng(doubles = listOf(0.0))
        assertFalse(EncounterRules.rollEncounter(location, 1, 1.0, always))
        assertTrue(EncounterRules.rollEncounter(location, 20, 1.0, always))
    }

    @Test
    fun `encounter tables honour the day phase`() {
        val table = com.runeveil.saga.domain.model.world.EncounterTable(
            id = "tbl",
            entries = listOf(
                com.runeveil.saga.domain.model.world.EncounterEntry(
                    speciesId = "day_beast", minLevel = 3, maxLevel = 5, weight = 100,
                    dayPhases = setOf(DayPhase.DAY),
                ),
                com.runeveil.saga.domain.model.world.EncounterEntry(
                    speciesId = "night_wraith", minLevel = 3, maxLevel = 5, weight = 100,
                    dayPhases = setOf(DayPhase.NIGHT),
                ),
            ),
        )
        val picked = EncounterRules.pickEntry(
            table, DayPhase.NIGHT, com.runeveil.saga.domain.model.battle.BattleWeather.CLEAR,
            floor = 1, storyFlags = emptySet(), rng = SeededRng(1),
        )
        assertEquals("night_wraith", picked!!.speciesId)
    }

    @Test
    fun `generated wild monsters are level-appropriate and fully healed`() {
        val species = TestFixtures.species(growthRate = GrowthRate.STEADY)
        val entry = com.runeveil.saga.domain.model.world.EncounterEntry(
            speciesId = species.id, minLevel = 10, maxLevel = 14, weight = 10,
        )
        val monster = EncounterRules.generateWild(entry, species, "uid", SeededRng(99), { 20 })

        assertTrue(monster.level in 10..(14 + EncounterRules.RARE_SPAWN_LEVEL_BONUS))
        assertEquals(monster.maxHp, monster.currentHp)
        assertTrue(monster.moves.isNotEmpty())
    }

    @Test
    fun `trainer monsters use their configured moves and genes`() {
        val species = TestFixtures.species()
        val monster = EncounterRules.generateTrained(
            species = species, uid = "t1", level = 40,
            moveIds = listOf("move_a", "move_b"),
            abilityId = null, heldItemId = "potion",
            temperament = com.runeveil.saga.domain.model.monster.Temperament.BOLD,
            geneQuality = 25, isShiny = false, nickname = "Wache",
            movePpResolver = { 15 },
        )
        assertEquals(40, monster.level)
        assertEquals(2, monster.moves.size)
        assertEquals(25, monster.genes.attack)
        assertEquals("Wache", monster.nickname)
    }
}
