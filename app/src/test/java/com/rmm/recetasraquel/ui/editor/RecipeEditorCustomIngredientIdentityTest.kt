package com.rmm.recetasraquel.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.SaveRecipeInput
import com.rmm.recetasraquel.domain.usecase.SaveRecipeOperation
import com.rmm.recetasraquel.util.IdGenerator
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEditorCustomIngredientIdentityTest {

    @Test
    fun customIngredientKeepsExclusiveIdentityAndCanonicalNameInEditor() {
        val viewModel = createViewModel()

        viewModel.addCustomIngredient(
            customIngredientId = "custom-ingredient-1",
            name = "Salsa de la casa",
            defaultUnit = "g",
        )

        val ingredient = viewModel.uiState.value.ingredients.single()
        assertEquals("custom-ingredient-1", ingredient.customIngredientId)
        assertNull(ingredient.catalogIngredientId)
        assertEquals("Salsa de la casa", ingredient.name)
        assertEquals("g", ingredient.unit)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.updateIngredientName(ingredient.key, "Nombre distinto")

        assertEquals("Salsa de la casa", viewModel.uiState.value.ingredients.single().name)
    }

    private fun createViewModel(): RecipeEditorViewModel = RecipeEditorViewModel(
        repository = NoOpRecipeRepository,
        idGenerator = IdGenerator { "generated-id" },
        photoStorage = NoOpPhotoStorage,
        saveRecipeUseCase = NoOpSaveRecipeOperation,
        savedStateHandle = SavedStateHandle(),
    )
}

private object NoOpRecipeRepository : RecipeRepository {
    override fun observeRecipes(): Flow<List<Recipe>> = emptyFlow()
    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> = emptyFlow()
    override fun observeCategories(): Flow<List<String>> = emptyFlow()
    override fun observeRecipe(recipeId: String): Flow<Recipe?> = emptyFlow()
    override suspend fun getRecipe(recipeId: String): Recipe? = error("Not used")
    override suspend fun createRecipe(input: RecipeDraft): Result<String> = error("Not used")
    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = error("Not used")
    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> = error("Not used")
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = error("Not used")
    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = error("Not used")
}

private object NoOpPhotoStorage : RecipePhotoStorage {
    override suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto> = error("Not used")
    override suspend fun promotePhoto(stagedPhoto: StagedPhoto, destination: PhotoDestination): Result<String> = error("Not used")
    override suspend fun delete(relativePath: String): Result<Unit> = error("Not used")
    override suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit> = error("Not used")
    override suspend fun resolve(relativePath: String): File? = error("Not used")
    override suspend fun cleanStaging(): Result<Unit> = error("Not used")
    override suspend fun getRecipePhotoPaths(recipeId: String): List<String> = error("Not used")
}

private object NoOpSaveRecipeOperation : SaveRecipeOperation {
    override suspend fun create(input: SaveRecipeInput): Result<String> = error("Not used")
    override suspend fun update(recipeId: String, input: SaveRecipeInput): Result<Unit> = error("Not used")
}
