package com.rmm.recetasraquel.ui

import androidx.lifecycle.SavedStateHandle
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.ui.detail.DetailUiState
import com.rmm.recetasraquel.ui.detail.RecipeDetailViewModel
import com.rmm.recetasraquel.ui.home.RecipeCatalogViewModel
import com.rmm.recetasraquel.ui.navigation.AppRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() = Dispatchers.setMain(dispatcher)

    @After
    fun resetMainDispatcher() = Dispatchers.resetMain()

    @Test
    fun catalogStartsLoadingThenShowsEmptyDatabase() = runTest(dispatcher) {
        val viewModel = RecipeCatalogViewModel(FakeRecipeRepository())
        assertTrue(viewModel.uiState.value.isLoading)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasAnyRecipes)
        job.cancel()
    }

    @Test
    fun catalogMaintainsAndClearsCombinedFilters() = runTest(dispatcher) {
        val viewModel = RecipeCatalogViewModel(FakeRecipeRepository(listOf(recipe())))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.setQuery(" patata ")
        viewModel.toggleFavorites()
        viewModel.selectCategory(" Principal ")
        advanceUntilIdle()
        assertEquals(" patata ", viewModel.uiState.value.filter.query)
        assertTrue(viewModel.uiState.value.filter.favoritesOnly)
        assertEquals("Principal", viewModel.uiState.value.filter.category)

        viewModel.clearFilters()
        advanceUntilIdle()
        assertEquals(RecipeCatalogFilter(), viewModel.uiState.value.filter)
        job.cancel()
    }

    @Test
    fun catalogTransformsResultsAndDistinguishesNoResults() = runTest(dispatcher) {
        val repository = FakeRecipeRepository(listOf(recipe(name = "Tortilla", category = "Principal")))
        val viewModel = RecipeCatalogViewModel(repository)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals("Tortilla", viewModel.uiState.value.recipes.single().name)
        assertTrue(viewModel.uiState.value.hasAnyRecipes)

        viewModel.setQuery("inexistente")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.recipes.isEmpty())
        assertTrue(viewModel.uiState.value.hasAnyRecipes)
        job.cancel()
    }

    @Test
    fun catalogShowsLoadAndFavoriteErrors() = runTest(dispatcher) {
        val failingLoad = FakeRecipeRepository(failReads = true)
        val loadViewModel = RecipeCatalogViewModel(failingLoad)
        val loadJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { loadViewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals("No se pudieron cargar las recetas.", loadViewModel.uiState.value.errorMessage)
        loadJob.cancel()

        val failingFavorite = FakeRecipeRepository(listOf(recipe()), failFavorites = true)
        val favoriteViewModel = RecipeCatalogViewModel(failingFavorite)
        val favoriteJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { favoriteViewModel.uiState.collect {} }
        advanceUntilIdle()
        favoriteViewModel.toggleFavorite(favoriteViewModel.uiState.value.recipes.single())
        advanceUntilIdle()
        assertEquals("No se pudo cambiar el favorito.", favoriteViewModel.uiState.value.actionMessage)
        favoriteJob.cancel()
    }

    @Test
    fun detailLoadsFoundRecipeAndChangesFavorite() = runTest(dispatcher) {
        val repository = FakeRecipeRepository(listOf(recipe()))
        val viewModel = detailViewModel(repository)
        assertEquals(DetailUiState.Loading, viewModel.uiState.value)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DetailUiState.Content)
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value as DetailUiState.Content).recipe.isFavorite)
        job.cancel()
    }

    @Test
    fun detailDistinguishesNotFoundReadErrorAndFavoriteError() = runTest(dispatcher) {
        val missing = detailViewModel(FakeRecipeRepository())
        val missingJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { missing.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(DetailUiState.NotFound, missing.uiState.value)
        missingJob.cancel()

        val broken = detailViewModel(FakeRecipeRepository(failReads = true))
        val brokenJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { broken.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(broken.uiState.value is DetailUiState.Error)
        brokenJob.cancel()

        val favorite = detailViewModel(FakeRecipeRepository(listOf(recipe()), failFavorites = true))
        val favoriteJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { favorite.uiState.collect {} }
        advanceUntilIdle()
        favorite.toggleFavorite()
        advanceUntilIdle()
        assertEquals(
            "No se pudo cambiar el favorito.",
            (favorite.uiState.value as DetailUiState.Content).actionMessage,
        )
        favoriteJob.cancel()
    }

    private fun detailViewModel(repository: RecipeRepository) = RecipeDetailViewModel(
        repository,
        SavedStateHandle(mapOf(AppRoute.RECIPE_ID to "r1")),
    )
}

private class FakeRecipeRepository(
    initialRecipes: List<Recipe> = emptyList(),
    private val failReads: Boolean = false,
    private val failFavorites: Boolean = false,
) : RecipeRepository {
    private val recipes = MutableStateFlow(initialRecipes)

    override fun observeRecipes(): Flow<List<Recipe>> = recipes

    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> {
        if (failReads) return flow { throw IllegalStateException("test") }
        val normalized = filter.normalized()
        return recipes.map { values ->
            values.filter { recipe ->
                (!normalized.favoritesOnly || recipe.isFavorite) &&
                    (normalized.category == null || recipe.category.equals(normalized.category, true)) &&
                    (normalized.query.isEmpty() || listOfNotNull(recipe.name, recipe.category).any {
                        it.contains(normalized.query, true)
                    } || recipe.ingredients.any { it.name.contains(normalized.query, true) })
            }.sortedByDescending { it.updatedAt }.map(Recipe::summary)
        }
    }

    override fun observeCategories(): Flow<List<String>> {
        if (failReads) return flow { throw IllegalStateException("test") }
        return recipes.map { values -> values.mapNotNull { it.category }.distinct().sorted() }
    }

    override fun observeRecipe(recipeId: String): Flow<Recipe?> {
        if (failReads) return flow { throw IllegalStateException("test") }
        return recipes.map { values -> values.firstOrNull { it.id == recipeId } }
    }

    override suspend fun getRecipe(recipeId: String): Recipe? = recipes.value.firstOrNull { it.id == recipeId }
    override suspend fun createRecipe(input: RecipeDraft): Result<String> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> = Result.failure(UnsupportedOperationException())

    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> {
        if (failFavorites) return Result.failure(IllegalStateException("test"))
        recipes.value = recipes.value.map { if (it.id == recipeId) it.copy(isFavorite = isFavorite) else it }
        return Result.success(Unit)
    }
}

private fun recipe(
    name: String = "Tortilla",
    category: String? = "Principal",
    favorite: Boolean = false,
) = Recipe(
    "r1", name, null, category, 4, 10, 20, null, favorite, null, emptyList(), emptyList(), 1, 2,
)

private fun Recipe.summary() = RecipeSummary(
    id, name, category, servings, preparationMinutes, cookingMinutes, isFavorite, coverPhotoPath, updatedAt,
)
