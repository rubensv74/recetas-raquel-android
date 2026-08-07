package com.rmm.recetasraquel.ui.cooking

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rmm.recetasraquel.app.RecetasRaquelApp
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.SaveRecipeUseCase
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import com.rmm.recetasraquel.util.UuidIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import java.io.File

class CookingNavigationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun detailOpensCookingNavigatesStepsAndReturnsOnFinish() {
        val repository = CookingNavigationRepository(navigationRecipe())
        val photoStorage = NavigationPhotoStorage()
        composeRule.setContent {
            RecetasRaquelTheme {
                RecetasRaquelApp(
                    repository = repository,
                    idGenerator = UuidIdGenerator(),
                    photoStorage = photoStorage,
                    saveRecipeUseCase = SaveRecipeUseCase(repository, photoStorage),
                    demoDataController = null,
                )
            }
        }

        waitForText("Tortilla de patatas")
        composeRule.onNodeWithTag("recipe_recipe").performClick()
        waitForText("Ingredientes")
        composeRule.onNodeWithTag("detail_start_cooking").performClick()

        waitForText("Paso 1 de 2")
        composeRule.onNodeWithText("Cortar las patatas.").assertIsDisplayed()
        composeRule.onNodeWithTag("cooking_next").performClick()
        waitForText("Paso 2 de 2")
        composeRule.onNodeWithText("Cuajar la tortilla.").assertIsDisplayed()
        composeRule.onNodeWithTag("cooking_finish").performClick()

        waitForText("Ingredientes")
        composeRule.onNodeWithTag("recipe_detail").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

private class CookingNavigationRepository(initial: Recipe) : RecipeRepository {
    private val recipes = MutableStateFlow(listOf(initial))

    override fun observeRecipes(): Flow<List<Recipe>> = recipes

    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> = recipes.map { values ->
        values.map { recipe ->
            RecipeSummary(
                id = recipe.id,
                name = recipe.name,
                category = recipe.category,
                servings = recipe.servings,
                preparationMinutes = recipe.preparationMinutes,
                cookingMinutes = recipe.cookingMinutes,
                isFavorite = recipe.isFavorite,
                coverPhotoPath = recipe.coverPhotoPath,
                updatedAt = recipe.updatedAt,
            )
        }
    }

    override fun observeCategories(): Flow<List<String>> = recipes.map { emptyList() }
    override fun observeRecipe(recipeId: String): Flow<Recipe?> = recipes.map { values -> values.firstOrNull { it.id == recipeId } }
    override suspend fun getRecipe(recipeId: String): Recipe? = recipes.value.firstOrNull { it.id == recipeId }
    override suspend fun createRecipe(input: RecipeDraft): Result<String> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
}

private class NavigationPhotoStorage : RecipePhotoStorage {
    override suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto> =
        Result.success(StagedPhoto(File("fake"), "fake/path.jpg"))

    override suspend fun promotePhoto(stagedPhoto: StagedPhoto, destination: PhotoDestination): Result<String> =
        Result.success("fake/promoted.jpg")

    override suspend fun delete(relativePath: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit> = Result.success(Unit)
    override suspend fun resolve(relativePath: String): File? = null
    override suspend fun cleanStaging(): Result<Unit> = Result.success(Unit)
    override suspend fun getRecipePhotoPaths(recipeId: String): List<String> = emptyList()
}

private fun navigationRecipe() = Recipe(
    id = "recipe",
    name = "Tortilla de patatas",
    description = "Jugosa",
    category = "Principal",
    servings = 4,
    preparationMinutes = 10,
    cookingMinutes = 20,
    notes = null,
    isFavorite = false,
    coverPhotoPath = null,
    ingredients = listOf(Ingredient("i1", "recipe", "1/2", "kg", "Patatas", null, 0)),
    steps = listOf(
        RecipeStep("s1", "recipe", "Cortar las patatas.", null, null, 0),
        RecipeStep("s2", "recipe", "Cuajar la tortilla.", 5, null, 1),
    ),
    createdAt = 1,
    updatedAt = 2,
)
