package com.rmm.recetasraquel.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.rmm.recetasraquel.app.RecetasRaquelApp
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.SaveRecipeUseCase
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import com.rmm.recetasraquel.util.UuidIdGenerator
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class CustomIngredientNavigationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun manualLibraryPathCreatesCustomMasterAndReturnsItToRecipeEditor() {
        val recipeRepository = NavigationRecipeRepository()
        val customRepository = FakeCustomIngredientRepository()
        val photoStorage = NavigationPhotoStorage()

        composeRule.setContent {
            RecetasRaquelTheme {
                RecetasRaquelApp(
                    repository = recipeRepository,
                    ingredientCatalogRepository = FakeIngredientCatalogRepository,
                    customIngredientRepository = customRepository,
                    idGenerator = UuidIdGenerator(),
                    photoStorage = photoStorage,
                    saveRecipeUseCase = SaveRecipeUseCase(recipeRepository, photoStorage),
                    demoDataController = null,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Nueva receta").performClick()
        composeRule.onNodeWithText("Nueva receta").assertIsDisplayed()

        composeRule.onNodeWithTag("add_ingredient").performScrollTo().performClick()
        composeRule.onNodeWithTag("ingredient_library_manual").performScrollTo().performClick()

        composeRule.onNodeWithText("Nuevo ingrediente personalizado").assertIsDisplayed()
        composeRule.onNodeWithTag("custom_name").performTextInput("Salsa de prueba")
        composeRule.onNodeWithTag("composition_known").performClick()
        composeRule.onNodeWithTag("save_custom_ingredient").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Salsa de prueba").fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Salsa de prueba").assertIsDisplayed()

        composeRule.runOnIdle {
            assertNotNull(customRepository.lastCreated)
            assertEquals("Salsa de prueba", customRepository.lastCreated?.name)
        }
    }
}

private class NavigationRecipeRepository : RecipeRepository {
    private val recipes = MutableStateFlow<List<Recipe>>(emptyList())

    override fun observeRecipes(): Flow<List<Recipe>> = recipes
    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> =
        MutableStateFlow(emptyList())
    override fun observeCategories(): Flow<List<String>> = MutableStateFlow(emptyList())
    override fun observeRecipe(recipeId: String): Flow<Recipe?> = MutableStateFlow(null)
    override suspend fun getRecipe(recipeId: String): Recipe? = null
    override suspend fun createRecipe(input: RecipeDraft): Result<String> = Result.success(input.id ?: "recipe-test")
    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = Result.success(Unit)
    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = Result.success(Unit)
    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
}

private class NavigationPhotoStorage : RecipePhotoStorage {
    override suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto> =
        Result.failure(UnsupportedOperationException("Not used"))
    override suspend fun promotePhoto(stagedPhoto: StagedPhoto, destination: PhotoDestination): Result<String> =
        Result.failure(UnsupportedOperationException("Not used"))
    override suspend fun delete(relativePath: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit> = Result.success(Unit)
    override suspend fun resolve(relativePath: String): File? = null
    override suspend fun cleanStaging(): Result<Unit> = Result.success(Unit)
    override suspend fun getRecipePhotoPaths(recipeId: String): List<String> = emptyList()
}
