package com.rmm.recetasraquel.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rmm.recetasraquel.app.RecetasRaquelApp
import com.rmm.recetasraquel.util.UuidIdGenerator
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test

class RecipeCatalogUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyCatalogIsShown() {
        setApp(UiFakeRepository())
        waitForText("Todavía no hay recetas guardadas.")
    }

    @Test
    fun catalogShowsDataAndSearchesByNameAndIngredient() {
        setApp(UiFakeRepository(sampleRecipes()))
        waitForText("Tortilla de patatas")

        composeRule.onNodeWithTag("catalog_search").performTextInput("tarta")
        waitForText("Tarta de queso")
        composeRule.onAllNodesWithText("Tortilla de patatas").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("Limpiar búsqueda").performClick()
        composeRule.onNodeWithTag("catalog_search").performTextInput("cebolla")
        waitForText("Tortilla de patatas")
        composeRule.onAllNodesWithText("Tarta de queso").assertCountEquals(0)
    }

    @Test
    fun favoritesCategoryAndClearFiltersWork() {
        setApp(UiFakeRepository(sampleRecipes()))
        waitForText("Tortilla de patatas")
        composeRule.onNodeWithTag("filter_favorites").performClick()
        waitForText("Tortilla de patatas")
        composeRule.onAllNodesWithText("Tarta de queso").assertCountEquals(0)

        composeRule.onNodeWithTag("category_Principal").performClick()
        composeRule.onNodeWithTag("catalog_search").performTextInput("inexistente")
        waitForText("No se encontraron recetas con estos filtros.")
        composeRule.onNodeWithContentDescription("Limpiar filtros").performClick()
        waitForText("Tarta de queso")
    }

    @Test
    fun opensDetailShowsChildrenAndReturnsToCatalog() {
        setApp(UiFakeRepository(sampleRecipes()))
        waitForText("Tortilla de patatas")
        composeRule.onNodeWithText("Tortilla de patatas").performClick()
        waitForText("Ingredientes")
        composeRule.onNodeWithText("1/2 kg · Patatas").assertIsDisplayed()
        composeRule.onNodeWithText("1. Cortar las patatas.").assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()
        waitForText("Buscar por receta o ingrediente")
    }

    @Test
    fun detailFavoritePersistsInCatalog() {
        setApp(UiFakeRepository(sampleRecipes()))
        waitForText("Tarta de queso")
        composeRule.onNodeWithText("Tarta de queso").performClick()
        waitForText("Ingredientes")
        composeRule.onNodeWithContentDescription("Marcar como favorita").performClick()
        composeRule.onNodeWithText("Volver").performClick()
        waitForText("Tarta de queso")
        composeRule.onNode(
            hasTestTag("favorite_tarta") and hasContentDescription("Quitar de favoritas"),
        ).assertIsDisplayed()
    }

    @Test
    fun detailShowsNotFoundWhenRecipeDisappears() {
        val repository = UiFakeRepository(sampleRecipes())
        setApp(repository)
        waitForText("Tortilla de patatas")
        composeRule.onNodeWithText("Tortilla de patatas").performClick()
        waitForText("Ingredientes")
        repository.remove("tortilla")
        waitForText("La receta ya no está disponible.")
        composeRule.onNodeWithText("Volver al catálogo").performClick()
        waitForText("Tarta de queso")
    }

    private fun setApp(repository: RecipeRepository) {
        composeRule.setContent {
            RecetasRaquelTheme { RecetasRaquelApp(repository, idGenerator = UuidIdGenerator(), demoDataController = null) }
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}

private class UiFakeRepository(initial: List<Recipe> = emptyList()) : RecipeRepository {
    private val recipes = MutableStateFlow(initial)

    fun remove(recipeId: String) {
        recipes.value = recipes.value.filterNot { it.id == recipeId }
    }

    override fun observeRecipes(): Flow<List<Recipe>> = recipes

    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> {
        val normalized = filter.normalized()
        return recipes.map { values ->
            values.filter { recipe ->
                (!normalized.favoritesOnly || recipe.isFavorite) &&
                    (normalized.category == null || recipe.category.equals(normalized.category, true)) &&
                    (normalized.query.isEmpty() || recipe.name.contains(normalized.query, true) ||
                        recipe.category?.contains(normalized.query, true) == true ||
                        recipe.ingredients.any { it.name.contains(normalized.query, true) })
            }.sortedByDescending(Recipe::updatedAt).map { it.toSummary() }
        }
    }

    override fun observeCategories(): Flow<List<String>> = recipes.map { values ->
        values.mapNotNull(Recipe::category).filter(String::isNotBlank).distinct().sorted()
    }

    override fun observeRecipe(recipeId: String): Flow<Recipe?> =
        recipes.map { values -> values.firstOrNull { it.id == recipeId } }

    override suspend fun getRecipe(recipeId: String): Recipe? = recipes.value.firstOrNull { it.id == recipeId }
    override suspend fun createRecipe(input: RecipeDraft): Result<String> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> = Result.failure(UnsupportedOperationException())

    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> {
        recipes.value = recipes.value.map { if (it.id == recipeId) it.copy(isFavorite = isFavorite) else it }
        return Result.success(Unit)
    }
}

private fun sampleRecipes() = listOf(
    Recipe(
        id = "tortilla",
        name = "Tortilla de patatas",
        description = "Jugosa",
        category = "Principal",
        servings = 4,
        preparationMinutes = 10,
        cookingMinutes = 20,
        notes = "Servir templada",
        isFavorite = true,
        coverPhotoPath = null,
        ingredients = listOf(Ingredient("i1", "tortilla", "1/2", "kg", "Patatas", null, 0), Ingredient("i2", "tortilla", "1", null, "Cebolla", null, 1)),
        steps = listOf(RecipeStep("s1", "tortilla", "Cortar las patatas.", null, null, 0), RecipeStep("s2", "tortilla", "Cuajar.", 5, null, 1)),
        createdAt = 1,
        updatedAt = 2,
    ),
    Recipe(
        id = "tarta",
        name = "Tarta de queso",
        description = null,
        category = "Postres",
        servings = null,
        preparationMinutes = null,
        cookingMinutes = 45,
        notes = null,
        isFavorite = false,
        coverPhotoPath = null,
        ingredients = listOf(Ingredient("i3", "tarta", "500", "g", "Queso", null, 0)),
        steps = listOf(RecipeStep("s3", "tarta", "Hornear.", null, null, 0)),
        createdAt = 1,
        updatedAt = 1,
    ),
)

private fun Recipe.toSummary() = RecipeSummary(
    id, name, category, servings, preparationMinutes, cookingMinutes, isFavorite, coverPhotoPath, updatedAt,
)
