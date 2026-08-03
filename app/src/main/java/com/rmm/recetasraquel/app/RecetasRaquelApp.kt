package com.rmm.recetasraquel.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rmm.recetasraquel.domain.repository.DemoDataController
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.ui.detail.RecipeDetailScreen
import com.rmm.recetasraquel.ui.detail.RecipeDetailViewModel
import com.rmm.recetasraquel.ui.home.HomeScreen
import com.rmm.recetasraquel.ui.home.RecipeCatalogViewModel
import com.rmm.recetasraquel.ui.navigation.AppRoute
import com.rmm.recetasraquel.ui.settings.SettingsScreen
import com.rmm.recetasraquel.ui.settings.SettingsViewModel

@Composable
fun RecetasRaquelApp(
    repository: RecipeRepository,
    demoDataController: DemoDataController?,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoute.CATALOG) {
        composable(AppRoute.CATALOG) {
            val catalogViewModel: RecipeCatalogViewModel = viewModel(
                factory = RecipeCatalogViewModel.factory(repository),
            )
            HomeScreen(
                state = catalogViewModel.uiState.collectAsStateWithLifecycle().value,
                onQueryChange = catalogViewModel::setQuery,
                onToggleFavorites = catalogViewModel::toggleFavorites,
                onSelectCategory = catalogViewModel::selectCategory,
                onClearFilters = catalogViewModel::clearFilters,
                onRetry = catalogViewModel::retry,
                onToggleFavorite = catalogViewModel::toggleFavorite,
                onOpenRecipe = { navController.navigate(AppRoute.recipe(it)) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(AppRoute.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(
            route = AppRoute.RECIPE,
            arguments = listOf(navArgument(AppRoute.RECIPE_ID) { type = NavType.StringType }),
        ) {
            val detailViewModel: RecipeDetailViewModel = viewModel(
                factory = RecipeDetailViewModel.factory(repository),
            )
            RecipeDetailScreen(
                state = detailViewModel.uiState.collectAsStateWithLifecycle().value,
                onNavigateBack = { navController.popBackStack() },
                onToggleFavorite = detailViewModel::toggleFavorite,
            )
        }
        composable(AppRoute.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(demoDataController),
            )
            SettingsScreen(
                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                showDevelopmentTools = settingsViewModel.hasDevelopmentTools,
                onNavigateBack = { navController.popBackStack() },
                onLoadDemoData = settingsViewModel::loadDemoData,
                onRemoveDemoData = settingsViewModel::removeDemoData,
            )
        }
    }
}
