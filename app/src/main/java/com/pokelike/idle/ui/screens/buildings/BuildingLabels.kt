package com.pokelike.idle.ui.screens.buildings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.BuildingType

/**
 * Verbindet Gebaeudearten mit ihrer Darstellung.
 *
 * Bewusst in der UI-Schicht: [BuildingType] bleibt dadurch frei von
 * Android-Typen und in einer reinen JVM-Umgebung uebersetzbar. Ein
 * Ressourcenverweis im Domaenenmodell wuerde genau das verhindern.
 *
 * Als `when` ohne `else` geschrieben. Kommt ein Gebaeude hinzu, meldet der
 * Compiler den fehlenden Zweig - andernfalls erschiene es im Spiel ohne Namen.
 */
@get:StringRes
val BuildingType.nameRes: Int
    get() = when (this) {
        BuildingType.FINGER -> R.string.building_finger
        BuildingType.CURSOR -> R.string.building_cursor
        BuildingType.MOUSE -> R.string.building_mouse
        BuildingType.MINE -> R.string.building_mine
        BuildingType.FARM -> R.string.building_farm
        BuildingType.FACTORY -> R.string.building_factory
        BuildingType.BANK -> R.string.building_bank
        BuildingType.LAB -> R.string.building_lab
        BuildingType.SPACESHIP -> R.string.building_spaceship
        BuildingType.TIME_MACHINE -> R.string.building_time_machine
    }

@get:StringRes
val BuildingType.descriptionRes: Int
    get() = when (this) {
        BuildingType.FINGER -> R.string.building_finger_description
        BuildingType.CURSOR -> R.string.building_cursor_description
        BuildingType.MOUSE -> R.string.building_mouse_description
        BuildingType.MINE -> R.string.building_mine_description
        BuildingType.FARM -> R.string.building_farm_description
        BuildingType.FACTORY -> R.string.building_factory_description
        BuildingType.BANK -> R.string.building_bank_description
        BuildingType.LAB -> R.string.building_lab_description
        BuildingType.SPACESHIP -> R.string.building_spaceship_description
        BuildingType.TIME_MACHINE -> R.string.building_time_machine_description
    }

val BuildingType.icon: ImageVector
    get() = when (this) {
        BuildingType.FINGER -> Icons.Filled.PanTool
        BuildingType.CURSOR -> Icons.Filled.Hardware
        BuildingType.MOUSE -> Icons.Filled.Mouse
        BuildingType.MINE -> Icons.Filled.Terrain
        BuildingType.FARM -> Icons.Filled.Agriculture
        BuildingType.FACTORY -> Icons.Filled.Factory
        BuildingType.BANK -> Icons.Filled.AccountBalance
        BuildingType.LAB -> Icons.Filled.Science
        BuildingType.SPACESHIP -> Icons.Filled.Rocket
        BuildingType.TIME_MACHINE -> Icons.Filled.Timer
    }
