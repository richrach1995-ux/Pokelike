package com.runeveil.saga.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.runeveil.saga.presentation.battle.BattleScreen
import com.runeveil.saga.presentation.bestiary.BestiaryDetailScreen
import com.runeveil.saga.presentation.bestiary.BestiaryScreen
import com.runeveil.saga.presentation.creation.CharacterCreationScreen
import com.runeveil.saga.presentation.crafting.CraftingScreen
import com.runeveil.saga.presentation.cutscene.CutsceneScreen
import com.runeveil.saga.presentation.dialogue.DialogueScreen
import com.runeveil.saga.presentation.inventory.InventoryScreen
import com.runeveil.saga.presentation.party.MonsterDetailScreen
import com.runeveil.saga.presentation.party.PartyScreen
import com.runeveil.saga.presentation.quest.QuestLogScreen
import com.runeveil.saga.presentation.roost.RoostScreen
import com.runeveil.saga.presentation.save.SaveSlotScreen
import com.runeveil.saga.presentation.settings.SettingsScreen
import com.runeveil.saga.presentation.shop.ShopScreen
import com.runeveil.saga.presentation.title.TitleScreen
import com.runeveil.saga.presentation.world.LocationScreen
import com.runeveil.saga.presentation.world.WorldMapScreen

/**
 * Every screen the game can show.
 *
 * Routes are declared as objects with a `route` template plus a typed
 * `create(...)` builder, which keeps argument encoding in one place and makes
 * a typo a compile error rather than a blank screen.
 */
sealed class Destination(val route: String) {

    data object Title : Destination("title")
    data object SaveSlots : Destination("saves?mode={mode}") {
        /** @param mode `load` or `save`. */
        fun create(mode: String = "load") = "saves?mode=$mode"
        const val ARG_MODE = "mode"
    }

    data object CharacterCreation : Destination("creation")
    data object WorldMap : Destination("world")

    data object Location : Destination("location/{locationId}") {
        fun create(locationId: String) = "location/$locationId"
        const val ARG_LOCATION = "locationId"
    }

    data object Battle : Destination("battle/{encounterId}?trainer={trainerId}") {
        /** [encounterId] is either `wild:<speciesId>` or a trainer team id. */
        fun create(encounterId: String, trainerId: String? = null) =
            "battle/$encounterId" + if (trainerId != null) "?trainer=$trainerId" else ""
        const val ARG_ENCOUNTER = "encounterId"
        const val ARG_TRAINER = "trainerId"
    }

    data object Party : Destination("party")

    data object MonsterDetail : Destination("monster/{uid}") {
        fun create(uid: String) = "monster/$uid"
        const val ARG_UID = "uid"
    }

    data object Bestiary : Destination("bestiary")

    data object BestiaryDetail : Destination("bestiary/{speciesId}") {
        fun create(speciesId: String) = "bestiary/$speciesId"
        const val ARG_SPECIES = "speciesId"
    }

    data object Inventory : Destination("inventory")
    data object Quests : Destination("quests")
    data object Settings : Destination("settings")

    data object Shop : Destination("shop/{shopId}") {
        fun create(shopId: String) = "shop/$shopId"
        const val ARG_SHOP = "shopId"
    }

    data object Crafting : Destination("crafting/{station}") {
        fun create(station: String) = "crafting/$station"
        const val ARG_STATION = "station"
    }

    data object Roost : Destination("roost")

    data object Dialogue : Destination("dialogue/{npcId}") {
        fun create(npcId: String) = "dialogue/$npcId"
        const val ARG_NPC = "npcId"
    }

    data object Cutscene : Destination("cutscene/{cutsceneId}") {
        fun create(cutsceneId: String) = "cutscene/$cutsceneId"
        const val ARG_CUTSCENE = "cutsceneId"
    }
}

/** Slide-in/out transitions used by every content screen. */
private const val TRANSITION_MS = 260

@Composable
fun RuneveilNavHost(
    navController: NavHostController,
    startDestination: String = Destination.Title.route,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(tween(TRANSITION_MS)) { it / 6 } + fadeIn(tween(TRANSITION_MS))
        },
        exitTransition = { fadeOut(tween(TRANSITION_MS / 2)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
        popExitTransition = {
            slideOutHorizontally(tween(TRANSITION_MS)) { it / 6 } + fadeOut(tween(TRANSITION_MS))
        },
    ) {
        composable(Destination.Title.route) {
            TitleScreen(
                onNewGame = { navController.navigate(Destination.CharacterCreation.route) },
                onContinue = {
                    navController.navigate(Destination.WorldMap.route) {
                        popUpTo(Destination.Title.route) { inclusive = true }
                    }
                },
                onLoad = { navController.navigate(Destination.SaveSlots.create("load")) },
                onSettings = { navController.navigate(Destination.Settings.route) },
                onBestiary = { navController.navigate(Destination.Bestiary.route) },
            )
        }

        composable(
            route = Destination.SaveSlots.route,
            arguments = listOf(
                navArgument(Destination.SaveSlots.ARG_MODE) {
                    type = NavType.StringType
                    defaultValue = "load"
                },
            ),
        ) { entry ->
            SaveSlotScreen(
                mode = entry.arguments?.getString(Destination.SaveSlots.ARG_MODE) ?: "load",
                onBack = { navController.popBackStack() },
                onLoaded = {
                    navController.navigate(Destination.WorldMap.route) {
                        popUpTo(Destination.Title.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Destination.CharacterCreation.route) {
            CharacterCreationScreen(
                onBack = { navController.popBackStack() },
                onCreated = {
                    navController.navigate(Destination.WorldMap.route) {
                        popUpTo(Destination.Title.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Destination.WorldMap.route) {
            WorldMapScreen(
                onOpenLocation = { navController.navigate(Destination.Location.create(it)) },
                onOpenParty = { navController.navigate(Destination.Party.route) },
                onOpenInventory = { navController.navigate(Destination.Inventory.route) },
                onOpenQuests = { navController.navigate(Destination.Quests.route) },
                onOpenBestiary = { navController.navigate(Destination.Bestiary.route) },
                onOpenSettings = { navController.navigate(Destination.Settings.route) },
                onOpenSaves = { navController.navigate(Destination.SaveSlots.create("save")) },
            )
        }

        composable(
            route = Destination.Location.route,
            arguments = listOf(navArgument(Destination.Location.ARG_LOCATION) { type = NavType.StringType }),
        ) { entry ->
            LocationScreen(
                locationId = entry.arguments?.getString(Destination.Location.ARG_LOCATION).orEmpty(),
                onBack = { navController.popBackStack() },
                onEncounter = { encounterId ->
                    navController.navigate(Destination.Battle.create(encounterId))
                },
                onTrainerBattle = { teamId, trainerId ->
                    navController.navigate(Destination.Battle.create(teamId, trainerId))
                },
                onOpenShop = { navController.navigate(Destination.Shop.create(it)) },
                onOpenForge = { navController.navigate(Destination.Crafting.create(it)) },
                onOpenRoost = { navController.navigate(Destination.Roost.route) },
                onTalkTo = { navController.navigate(Destination.Dialogue.create(it)) },
                onCutscene = { navController.navigate(Destination.Cutscene.create(it)) },
            )
        }

        composable(
            route = Destination.Battle.route,
            arguments = listOf(
                navArgument(Destination.Battle.ARG_ENCOUNTER) { type = NavType.StringType },
                navArgument(Destination.Battle.ARG_TRAINER) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            BattleScreen(onFinished = { navController.popBackStack() })
        }

        composable(Destination.Party.route) {
            PartyScreen(
                onBack = { navController.popBackStack() },
                onOpenMonster = { navController.navigate(Destination.MonsterDetail.create(it)) },
            )
        }

        composable(
            route = Destination.MonsterDetail.route,
            arguments = listOf(navArgument(Destination.MonsterDetail.ARG_UID) { type = NavType.StringType }),
        ) { entry ->
            MonsterDetailScreen(
                uid = entry.arguments?.getString(Destination.MonsterDetail.ARG_UID).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destination.Bestiary.route) {
            BestiaryScreen(
                onBack = { navController.popBackStack() },
                onOpenSpecies = { navController.navigate(Destination.BestiaryDetail.create(it)) },
            )
        }

        composable(
            route = Destination.BestiaryDetail.route,
            arguments = listOf(navArgument(Destination.BestiaryDetail.ARG_SPECIES) { type = NavType.StringType }),
        ) { entry ->
            BestiaryDetailScreen(
                speciesId = entry.arguments?.getString(Destination.BestiaryDetail.ARG_SPECIES).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destination.Inventory.route) {
            InventoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Destination.Quests.route) {
            QuestLogScreen(onBack = { navController.popBackStack() })
        }

        composable(Destination.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Destination.Shop.route,
            arguments = listOf(navArgument(Destination.Shop.ARG_SHOP) { type = NavType.StringType }),
        ) { entry ->
            ShopScreen(
                shopId = entry.arguments?.getString(Destination.Shop.ARG_SHOP).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Destination.Crafting.route,
            arguments = listOf(navArgument(Destination.Crafting.ARG_STATION) { type = NavType.StringType }),
        ) { entry ->
            CraftingScreen(
                station = entry.arguments?.getString(Destination.Crafting.ARG_STATION).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destination.Roost.route) {
            RoostScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Destination.Dialogue.route,
            arguments = listOf(navArgument(Destination.Dialogue.ARG_NPC) { type = NavType.StringType }),
        ) { entry ->
            DialogueScreen(
                npcId = entry.arguments?.getString(Destination.Dialogue.ARG_NPC).orEmpty(),
                onFinished = { navController.popBackStack() },
                onOpenShop = { navController.navigate(Destination.Shop.create(it)) },
                onStartBattle = { navController.navigate(Destination.Battle.create(it)) },
            )
        }

        composable(
            route = Destination.Cutscene.route,
            arguments = listOf(navArgument(Destination.Cutscene.ARG_CUTSCENE) { type = NavType.StringType }),
        ) { entry ->
            CutsceneScreen(
                cutsceneId = entry.arguments?.getString(Destination.Cutscene.ARG_CUTSCENE).orEmpty(),
                onFinished = { navController.popBackStack() },
            )
        }
    }
}
