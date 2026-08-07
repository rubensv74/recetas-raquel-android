package com.rmm.recetasraquel.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.repository.DemoDataController
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.SaveRecipeOperation
import com.rmm.recetasraquel.ui.cooking.CookingModeScreen
import com.rmm.recetasraquel.ui.cooking.CookingModeViewModel
import com.rmm.recetasraquel.ui.detail.RecipeDetailScreen
import com.rmm.recetasraquel.ui.detail.RecipeDetailViewModel
import com.rmm.recetasraquel.ui.editor.RecipeEditorScreen
import com.rmm.recetasraquel.ui.editor.RecipeEditorViewModel
import com.rmm.recetasraquel.ui.home.HomeScreen
import com.rmm.recetasraquel.ui.home.RecipeCatalogViewModel
import com.rmm.recetasraquel.ui.navigation.AppRoute
import com.rmm.recetasraquel.ui.settings.SettingsScreen
import com.rmm.recetasraquel.ui.settings.SettingsViewModel
import com.rmm.recetasraquel.util.IdGenerator

@Composable
fun RecetasRaquelApp(
    repository: RecipeRepository,
    idGenerator: IdGenerator,
    photoStorage: RecipePhotoStorage,
    saveRecipeUseCase: SaveRecipeOperation,
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
                onCreateRecipe = { navController.navigate(AppRoute.NEW_RECIPE) { launchSingleTop = true } },
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
                onEditRecipe = { id -> navController.navigate(AppRoute.editRecipe(id)) { launchSingleTop = true } },
                onStartCooking = { id -> navController.navigate(AppRoute.cookRecipe(id)) { launchSingleTop = true } },
            )
        }
        composable(
            route = AppRoute.COOK_RECIPE,
            arguments = listOf(navArgument(AppRoute.RECIPE_ID) { type = NavType.StringType }),
        ) {
            val cookingViewModel: CookingModeViewModel = viewModel(
                factory = CookingModeViewModel.factory(repository),
            )
            CookingModeScreen(
                state = cookingViewModel.uiState.collectAsStateWithLifecycle().value,
                onPreviousStep = cookingViewModel::previousStep,
                onNextStep = cookingViewModel::nextStep,
                onShowIngredients = cookingViewModel::showIngredients,
                onHideIngredients = cookingViewModel::hideIngredients,
                onFinish = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(AppRoute.NEW_RECIPE) {
            val editorViewModel: RecipeEditorViewModel = viewModel(
                factory = RecipeEditorViewModel.factory(repository, idGenerator, photoStorage, saveRecipeUseCase),
            )
            EditorRoute(viewModel = editorViewModel, navController = navController)
        }
        composable(
            route = AppRoute.EDIT_RECIPE,
            arguments = listOf(navArgument(AppRoute.RECIPE_ID) { type = NavType.StringType }),
        ) {
            val editorViewModel: RecipeEditorViewModel = viewModel(
                factory = RecipeEditorViewModel.factory(repository, idGenerator, photoStorage, saveRecipeUseCase),
            )
            EditorRoute(viewModel = editorViewModel, navController = navController)
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

@Composable
private fun EditorRoute(
    viewModel: RecipeEditorViewModel,
    navController: androidx.navigation.NavHostController,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(Unit) {
        viewModel.navigation.collect { event ->
            when (event) {
                com.rmm.recetasraquel.ui.editor.EditorNavigationEvent.RecipeDeleted -> {
                    navController.popBackStack()
                }
                is com.rmm.recetasraquel.ui.editor.EditorNavigationEvent.RecipeCreated,
                is com.rmm.recetasraquel.ui.editor.EditorNavigationEvent.RecipeUpdated -> {
                    val recipeId = when (event) {
                        is com.rmm.recetasraquel.ui.editor.EditorNavigationEvent.RecipeCreated -> event.recipeId
                        is com.rmm.recetasraquel.ui.editor.EditorNavigationEvent.RecipeUpdated -> event.recipeId
                        else -> return@collect
                    }
                    navController.navigate(AppRoute.recipe(recipeId)) {
                        popUpTo(AppRoute.CATALOG)
                        launchSingleTop = true
                    }
                }
                is com.rmm.recetasraquel.ui.editor.EditorNavigationEvent.ShowMessage -> {}
            }
        }
    }
    RecipeEditorScreen(
        state = state,
        onNameChange = viewModel::updateName,
        onCategoryChange = viewModel::updateCategory,
        onDescriptionChange = viewModel::updateDescription,
        onServingsChange = viewModel::updateServings,
        onPreparationMinutesChange = viewModel::updatePreparationMinutes,
        onCookingMinutesChange = viewModel::updateCookingMinutes,
        onNotesChange = viewModel::updateNotes,
        onAddIngredient = viewModel::addIngredient,
        onIngredientQuantityChange = viewModel::updateIngredientQuantity,
        onIngredientUnitChange = viewModel::updateIngredientUnit,
        onIngredientNameChange = viewModel::updateIngredientName,
        onIngredientNotesChange = viewModel::updateIngredientNotes,
        onRemoveIngredient = viewModel::removeIngredient,
        onMoveIngredientUp = viewModel::moveIngredientUp,
        onMoveIngredientDown = viewModel::moveIngredientDown,
        onAddStep = viewModel::addStep,
        onStepInstructionChange = viewModel::updateStepInstruction,
        onStepTimerChange = viewModel::updateStepTimer,
        onRemoveStep = viewModel::removeStep,
        onMoveStepUp = viewModel::moveStepUp,
        onMoveStepDown = viewModel::moveStepDown,
        onCoverPhotoSelected = viewModel::selectCoverPhoto,
        onRemoveCoverPhoto = viewModel::removeCoverPhoto,
        onStepPhotoSelected = viewModel::selectStepPhoto,
        onRemoveStepPhoto = viewModel::removeStepPhoto,
        onSave = viewModel::save,
        onNavigateBack = viewModel::handleBack,
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onConfirmDiscard = viewModel::discardChanges,
        onCancelDiscard = viewModel::cancelDiscard,
        onDismissSaveError = viewModel::consumeSaveError,
        onDismissPhotoError = viewModel::dismissPhotoError,
    )
}
