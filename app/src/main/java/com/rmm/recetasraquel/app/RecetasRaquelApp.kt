package com.rmm.recetasraquel.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rmm.recetasraquel.ui.home.HomeScreen
import com.rmm.recetasraquel.ui.navigation.AppDestination
import com.rmm.recetasraquel.ui.settings.SettingsScreen

/** Root UI and intentionally small navigation container for the Sprint 0 destinations. */
@Composable
fun RecetasRaquelApp() {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }

    BackHandler(enabled = destination != AppDestination.Home) {
        destination = AppDestination.Home
    }

    when (destination) {
        AppDestination.Home -> HomeScreen(onOpenSettings = { destination = AppDestination.Settings })
        AppDestination.Settings -> SettingsScreen(onNavigateBack = { destination = AppDestination.Home })
    }
}
