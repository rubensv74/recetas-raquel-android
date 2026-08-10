package com.rmm.recetasraquel.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository
import com.rmm.recetasraquel.domain.repository.DemoDataController
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.RecipeSafetySummaryResolver
import com.rmm.recetasraquel.domain.usecase.SaveRecipeOperation
import com.rmm.recetasraquel.ui.cooking.CookingModeScreen
import com.rmm.recetasraquel.ui.cooking.CookingModeViewModel
import com.rmm.recetasraquel.ui.customingredient.CustomIngredientEditorEvent
import com.rmm.recetasraquel.ui.customingredient.CustomIngredientEditorScreen
import com.rmm.recetasraquel.ui.customingredient.CustomIngredientEditorViewModel
import com.rmm.recetasraquel.ui.detail.RecipeDetailScreen
import com.rmm.recetasraquel.ui.detail.RecipeDetailViewModel
import com.rmm.recetasraquel.ui.editor.RecipeEditorScreen
import com.rmm.recetasraquel.ui.editor.RecipeEditorViewModel
import com.rmm.recetasraquel.ui.home.HomeScreen
import com.rmm.recetasraquel.ui.home.RecipeCatalogViewModel
import com.rmm.recetasraquel.ui.ingredientlibrary.IngredientLibraryScreen
import com.rmm.recetasraquel.ui.ingredientlibrary.IngredientLibraryViewModel
import com.rmm.recetasraquel.ui.navigation.AppRoute
import com.rmm.recetasraquel.ui.settings.SettingsScreen
import com.rmm.recetasraquel.ui.settings.SettingsViewModel
import com.rmm.recetasraquel.util.IdGenerator

@Composable
fun RecetasRaquelApp(
    repository: RecipeRepository,
    ingredientCatalogRepository: IngredientCatalogRepository,
    customIngredientRepository: CustomIngredientRepository,
    idGenerator: IdGenerator,
    photoStorage: RecipePhotoStorage,
    saveRecipeUseCase: SaveRecipeOperation,
    demoDataController: DemoDataController?,
    recipeSafetySummaryResolver: RecipeSafetySummaryResolver? = null,
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
                factory = RecipeDetailViewModel.factory(repository, recipeSafetySummaryResolver),
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
        composable(AppRoute.NEW_RECIPE) { backStackEntry ->
            val editorViewModel: RecipeEditorViewModel = viewModel(
                factory = RecipeEditorViewModel.factory(repository, idGenerator, photoStorage, saveRecipeUseCase),
            )
            EditorRoute(
                viewModel = editorViewModel,
                navController = navController,
                backStackEntry = backStackEntry,
            )
        }
        composable(
            route = AppRoute.EDIT_RECIPE,
            arguments = listOf(navArgument(AppRoute.RECIPE_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val editorViewModel: RecipeEditorViewModel = viewModel(
                factory = RecipeEditorViewModel.factory(repository, idGenerator, photoStorage, saveRecipeUseCase),
            )
            EditorRoute(
                viewModel = editorViewModel,
                navController = navController,
                backStackEntry = backStackEntry,
            )
        }
        composable(AppRoute.INGREDIENT_LIBRARY) { libraryBackStackEntry ->
            val libraryViewModel: IngredientLibraryViewModel = viewModel(
                factory = IngredientLibraryViewModel.factory(ingredientCatalogRepository),
            )
            val selectedCustomIngredientId = libraryBackStackEntry.savedStateHandle
                .getStateFlow<String?>(AppRoute.SELECTED_CUSTOM_INGREDIENT_ID, null)
                .collectAsStateWithLifecycle().value
            val selectedCustomIngredientName = libraryBackStackEntry.savedStateHandle
                .getStateFlow<String?>(AppRoute.SELECTED_CUSTOM_INGREDIENT_NAME, null)
                .collectAsStateWithLifecycle().value
            val selectedCustomIngredientUnit = libraryBackStackEntry.savedStateHandle
                .getStateFlow<String?>(AppRoute.SELECTED_CUSTOM_INGREDIENT_UNIT, null)
                .collectAsStateWithLifecycle().value

            LaunchedEffect(
                selectedCustomIngredientId,
                selectedCustomIngredientName,
                selectedCustomIngredientUnit,
            ) {
                val ingredientId = selectedCustomIngredientId
                val ingredientName = selectedCustomIngredientName
                if (ingredientId != null && ingredientName != null) {
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(AppRoute.SELECTED_CUSTOM_INGREDIENT_ID, ingredientId)
                        set(AppRoute.SELECTED_CUSTOM_INGREDIENT_NAME, ingredientName)
                        set(AppRoute.SELECTED_CUSTOM_INGREDIENT_UNIT, selectedCustomIngredientUnit.orEmpty())
                    }
                    libraryBackStackEntry.savedStateHandle[AppRoute.SELECTED_CUSTOM_INGREDIENT_ID] = null
                    libraryBackStackEntry.savedStateHandle[AppRoute.SELECTED_CUSTOM_INGREDIENT_NAME] = null
                    libraryBackStackEntry.savedStateHandle[AppRoute.SELECTED_CUSTOM_INGREDIENT_UNIT] = null
                    navController.popBackStack()
                }
            }

            IngredientLibraryScreen(
                state = libraryViewModel.uiState.collectAsStateWithLifecycle().value,
                onQueryChange = libraryViewModel::setQuery,
                onSelectCategory = libraryViewModel::selectCategory,
                onClearFilters = libraryViewModel::clearFilters,
                onSelectIngredient = { ingredient ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(AppRoute.SELECTED_CATALOG_INGREDIENT_ID, ingredient.id)
                        set(AppRoute.SELECTED_CATALOG_INGREDIENT_NAME, ingredient.canonicalName)
                        set(AppRoute.SELECTED_CATALOG_INGREDIENT_UNIT, ingredient.defaultUnit ?: "")
                    }
                    navController.popBackStack()
                },
                onAddManualIngredient = {
                    navController.navigate(AppRoute.CUSTOM_INGREDIENT) { launchSingleTop = true }
                },
                onNavigateBack = { navController.popBackStack() },
                onRetry = libraryViewModel::retry,
            )
        }
        composable(AppRoute.CUSTOM_INGREDIENT) {
            val customIngredientViewModel: CustomIngredientEditorViewModel = viewModel(
                factory = CustomIngredientEditorViewModel.factory(customIngredientRepository),
            )
            val customIngredientState = customIngredientViewModel.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(Unit) {
                customIngredientViewModel.events.collect { event ->
                    when (event) {
                        is CustomIngredientEditorEvent.IngredientCreated -> {
                            navController.previousBackStackEntry?.savedStateHandle?.apply {
                                set(AppRoute.SELECTED_CUSTOM_INGREDIENT_ID, event.ingredientId)
                                set(AppRoute.SELECTED_CUSTOM_INGREDIENT_NAME, event.name)
                                set(AppRoute.SELECTED_CUSTOM_INGREDIENT_UNIT, event.defaultUnit ?: "")
                            }
                            navController.popBackStack()
                        }
                    }
                }
            }

            CustomIngredientEditorScreen(
                state = customIngredientState,
                onNameChange = customIngredientViewModel::setName,
                onTypeChange = customIngredientViewModel::setType,
                onCategoryChange = customIngredientViewModel::setCategory,
                onDefaultUnitChange = customIngredientViewModel::setDefaultUnit,
                onAliasesChange = customIngredientViewModel::setAliasesText,
                onBrandChange = customIngredientViewModel::setBrand,
                onTradeNameChange = customIngredientViewModel::setTradeName,
                onCompositionKnownChange = customIngredientViewModel::setCompositionKnown,
                onLabelReadAtChange = customIngredientViewModel::setLabelReadAt,
                onNotesChange = customIngredientViewModel::setNotes,
                onAddSafetyRow = customIngredientViewModel::addSafetyRow,
                onRemoveSafetyRow = customIngredientViewModel::removeSafetyRow,
                onSafetyGroupChange = customIngredientViewModel::setSafetyGroup,
                onSafetyRelationTypeChange = customIngredientViewModel::setSafetyRelationType,
                onSafetyEvidenceChange = customIngredientViewModel::setSafetyEvidence,
                onSafetySourceDetailsChange = customIngredientViewModel::setSafetySourceDetails,
                onSafetyNotesChange = customIngredientViewModel::setSafetyNotes,
                onSave = customIngredientViewModel::save,
                onRetry = customIngredientViewModel::retry,
                onNavigateBack = { navController.popBackStack() },
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

@Composable
private fun EditorRoute(
    viewModel: RecipeEditorViewModel,
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val selectedIngredientId = backStackEntry.savedStateHandle
        .getStateFlow<String?>(AppRoute.SELECTED_CATALOG_INGREDIENT_ID, null)
        .collectAsStateWithLifecycle().value
    val selectedIngredientName = backStackEntry.savedStateHandle
        .getStateFlow<String?>(AppRoute.SELECTED_CATALOG_INGREDIENT_NAME, null)
        .collectAsStateWithLifecycle().value
    val selectedIngredientUnit = backStackEntry.savedStateHandle
        .getStateFlow<String?>(AppRoute.SELECTED_CATALOG_INGREDIENT_UNIT, null)
        .collectAsStateWithLifecycle().value
    val selectedCustomIngredientId = backStackEntry.savedStateHandle
        .getStateFlow<String?>(AppRoute.SELECTED_CUSTOM_INGREDIENT_ID, null)
        .collectAsStateWithLifecycle().value
    val selectedCustomIngredientName = backStackEntry.savedStateHandle
        .getStateFlow<String?>(AppRoute.SELECTED_CUSTOM_INGREDIENT_NAME, null)
        .collectAsStateWithLifecycle().value
    val selectedCustomIngredientUnit = backStackEntry.savedStateHandle
        .getStateFlow<String?>(AppRoute.SELECTED_CUSTOM_INGREDIENT_UNIT, null)
        .collectAsStateWithLifecycle().value

    LaunchedEffect(selectedIngredientId, selectedIngredientName, selectedIngredientUnit) {
        val ingredientId = selectedIngredientId
        val ingredientName = selectedIngredientName
        if (ingredientId != null && ingredientName != null) {
            viewModel.addCatalogIngredient(
                catalogIngredientId = ingredientId,
                canonicalName = ingredientName,
                defaultUnit = selectedIngredientUnit?.ifBlank { null },
            )
            backStackEntry.savedStateHandle[AppRoute.SELECTED_CATALOG_INGREDIENT_ID] = null
            backStackEntry.savedStateHandle[AppRoute.SELECTED_CATALOG_INGREDIENT_NAME] = null
            backStackEntry.savedStateHandle[AppRoute.SELECTED_CATALOG_INGREDIENT_UNIT] = null
        }
    }

    LaunchedEffect(
        selectedCustomIngredientId,
        selectedCustomIngredientName,
        selectedCustomIngredientUnit,
    ) {
        val ingredientId = selectedCustomIngredientId
        val ingredientName = selectedCustomIngredientName
        if (ingredientId != null && ingredientName != null) {
            viewModel.addCustomIngredient(
                customIngredientId = ingredientId,
                name = ingredientName,
                defaultUnit = selectedCustomIngredientUnit?.ifBlank { null },
            )
            backStackEntry.savedStateHandle[AppRoute.SELECTED_CUSTOM_INGREDIENT_ID] = null
            backStackEntry.savedStateHandle[AppRoute.SELECTED_CUSTOM_INGREDIENT_NAME] = null
            backStackEntry.savedStateHandle[AppRoute.SELECTED_CUSTOM_INGREDIENT_UNIT] = null
        }
    }

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
        onAddIngredient = { navController.navigate(AppRoute.INGREDIENT_LIBRARY) { launchSingleTop = true } },
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
