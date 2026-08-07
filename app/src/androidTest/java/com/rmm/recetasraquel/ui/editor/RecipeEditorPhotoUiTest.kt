package com.rmm.recetasraquel.ui.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.SaveRecipeUseCase
import com.rmm.recetasraquel.ui.navigation.AppRoute
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import com.rmm.recetasraquel.util.UuidIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.junit.Rule
import org.junit.Test
import java.io.File

class RecipeEditorPhotoUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createModeShowsCoverPhotoButton() {
        setApp()
        composeRule.onNodeWithTag("select_cover_photo").assertIsDisplayed()
    }

    @Test
    fun editorHasPhotoSection() {
        setApp()
        composeRule.onNodeWithTag("editor_content").assertIsDisplayed()
        composeRule.onNodeWithTag("select_cover_photo").assertIsDisplayed()
    }

    private fun setApp() {
        val repository = EditorFakeRepository()
        val photoStorage = FakeEditorPhotoStorage()
        val saveRecipeUseCase = SaveRecipeUseCase(repository, photoStorage)
        composeRule.setContent {
            RecetasRaquelTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = AppRoute.NEW_RECIPE) {
                    composable(AppRoute.NEW_RECIPE) {
                        val editorViewModel: RecipeEditorViewModel = viewModel(
                            factory = RecipeEditorViewModel.factory(repository, UuidIdGenerator(), photoStorage, saveRecipeUseCase),
                        )
                        val state = editorViewModel.uiState.collectAsStateWithLifecycle().value
                        RecipeEditorScreen(
                            state = state,
                            onNameChange = editorViewModel::updateName,
                            onCategoryChange = editorViewModel::updateCategory,
                            onDescriptionChange = editorViewModel::updateDescription,
                            onServingsChange = editorViewModel::updateServings,
                            onPreparationMinutesChange = editorViewModel::updatePreparationMinutes,
                            onCookingMinutesChange = editorViewModel::updateCookingMinutes,
                            onNotesChange = editorViewModel::updateNotes,
                            onAddIngredient = editorViewModel::addIngredient,
                            onIngredientQuantityChange = editorViewModel::updateIngredientQuantity,
                            onIngredientUnitChange = editorViewModel::updateIngredientUnit,
                            onIngredientNameChange = editorViewModel::updateIngredientName,
                            onIngredientNotesChange = editorViewModel::updateIngredientNotes,
                            onRemoveIngredient = editorViewModel::removeIngredient,
                            onMoveIngredientUp = editorViewModel::moveIngredientUp,
                            onMoveIngredientDown = editorViewModel::moveIngredientDown,
                            onAddStep = editorViewModel::addStep,
                            onStepInstructionChange = editorViewModel::updateStepInstruction,
                            onStepTimerChange = editorViewModel::updateStepTimer,
                            onRemoveStep = editorViewModel::removeStep,
                            onMoveStepUp = editorViewModel::moveStepUp,
                            onMoveStepDown = editorViewModel::moveStepDown,
                            onCoverPhotoSelected = editorViewModel::selectCoverPhoto,
                            onRemoveCoverPhoto = editorViewModel::removeCoverPhoto,
                            onStepPhotoSelected = editorViewModel::selectStepPhoto,
                            onRemoveStepPhoto = editorViewModel::removeStepPhoto,
                            onSave = editorViewModel::save,
                            onNavigateBack = { },
                            onDelete = editorViewModel::requestDelete,
                            onConfirmDelete = editorViewModel::confirmDelete,
                            onCancelDelete = editorViewModel::cancelDelete,
                            onConfirmDiscard = editorViewModel::discardChanges,
                            onCancelDiscard = editorViewModel::cancelDiscard,
                            onDismissSaveError = editorViewModel::consumeSaveError,
                            onDismissPhotoError = editorViewModel::dismissPhotoError,
                        )
                    }
                }
            }
        }
    }
}

private class FakeEditorPhotoStorage : RecipePhotoStorage {
    override suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto> = Result.success(
        StagedPhoto(stagedFile = File("fake/photo.jpg"), relativePath = "recipe_photos/fake_photo.jpg"),
    )
    override suspend fun promotePhoto(stagedPhoto: StagedPhoto, destination: PhotoDestination): Result<String> =
        Result.success("recipe_photos/promoted.jpg")
    override suspend fun delete(relativePath: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit> = Result.success(Unit)
    override suspend fun resolve(relativePath: String): File? = null
    override suspend fun cleanStaging(): Result<Unit> = Result.success(Unit)
    override suspend fun getRecipePhotoPaths(recipeId: String): List<String> = emptyList()
}

private class EditorFakeRepository : RecipeRepository {
    private val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    override fun observeRecipes(): Flow<List<Recipe>> = recipes
    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> = recipes.map { emptyList() }
    override fun observeCategories(): Flow<List<String>> = recipes.map { emptyList() }
    override fun observeRecipe(recipeId: String): Flow<Recipe?> = recipes.map { null }
    override suspend fun getRecipe(recipeId: String): Recipe? = null
    override suspend fun createRecipe(input: RecipeDraft): Result<String> = Result.success("new_id")
    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = Result.success(Unit)
    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = Result.success(Unit)
    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
}
