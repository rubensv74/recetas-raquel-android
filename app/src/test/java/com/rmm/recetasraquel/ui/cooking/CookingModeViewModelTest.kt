package com.rmm.recetasraquel.ui.cooking

import androidx.lifecycle.SavedStateHandle
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.ui.navigation.AppRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
class CookingModeViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() = Dispatchers.setMain(dispatcher)

    @After
    fun resetMainDispatcher() = Dispatchers.resetMain()

    @Test
    fun loadsStepsSortedAndStartsAtFirstStep() = runTest(dispatcher) {
        val repository = CookingFakeRepository(sampleRecipe())
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value as CookingUiState.Content
        assertEquals("Cortar", state.currentStep?.instruction)
        assertEquals(0, state.currentStepIndex)
        assertEquals(2, state.totalSteps)
        assertFalse(state.canGoPrevious)
        assertTrue(state.canGoNext)
        assertEquals(0.5f, state.progress)
    }

    @Test
    fun nextAndPreviousRespectBoundaries() = runTest(dispatcher) {
        val viewModel = createViewModel(CookingFakeRepository(sampleRecipe()))
        advanceUntilIdle()

        viewModel.previousStep()
        advanceUntilIdle()
        assertEquals(0, (viewModel.uiState.value as CookingUiState.Content).currentStepIndex)

        viewModel.nextStep()
        advanceUntilIdle()
        var state = viewModel.uiState.value as CookingUiState.Content
        assertEquals(1, state.currentStepIndex)
        assertEquals("Servir", state.currentStep?.instruction)
        assertTrue(state.isLastStep)

        viewModel.nextStep()
        advanceUntilIdle()
        state = viewModel.uiState.value as CookingUiState.Content
        assertEquals(1, state.currentStepIndex)

        viewModel.previousStep()
        advanceUntilIdle()
        assertEquals(0, (viewModel.uiState.value as CookingUiState.Content).currentStepIndex)
    }

    @Test
    fun ingredientsVisibilityIsExplicitUiState() = runTest(dispatcher) {
        val viewModel = createViewModel(CookingFakeRepository(sampleRecipe()))
        advanceUntilIdle()

        viewModel.showIngredients()
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value as CookingUiState.Content).showIngredients)

        viewModel.hideIngredients()
        advanceUntilIdle()
        assertFalse((viewModel.uiState.value as CookingUiState.Content).showIngredients)
    }

    @Test
    fun currentStepIsRestoredFromSavedState() = runTest(dispatcher) {
        val repository = CookingFakeRepository(sampleRecipe())
        val savedState = SavedStateHandle(
            mapOf(
                AppRoute.RECIPE_ID to "recipe",
                "cookingCurrentStepIndex" to 1,
            ),
        )
        val viewModel = CookingModeViewModel(repository, savedState)

        advanceUntilIdle()

        val state = viewModel.uiState.value as CookingUiState.Content
        assertEquals(1, state.currentStepIndex)
        assertEquals("Servir", state.currentStep?.instruction)
    }

    @Test
    fun recipeWithoutStepsProducesContentWithoutCurrentStep() = runTest(dispatcher) {
        val recipe = sampleRecipe().copy(steps = emptyList())
        val viewModel = createViewModel(CookingFakeRepository(recipe))

        advanceUntilIdle()

        val state = viewModel.uiState.value as CookingUiState.Content
        assertEquals(0, state.totalSteps)
        assertEquals(null, state.currentStep)
        assertEquals(0f, state.progress)
    }

    @Test
    fun disappearingRecipeBecomesNotFound() = runTest(dispatcher) {
        val repository = CookingFakeRepository(sampleRecipe())
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        repository.removeRecipe()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CookingUiState.NotFound)
    }

    @Test
    fun repositoryFailureBecomesError() = runTest(dispatcher) {
        val repository = CookingFakeRepository(sampleRecipe(), failObservation = true)
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value as CookingUiState.Error
        assertEquals("No se pudo cargar el modo cocina.", state.message)
    }

    private fun createViewModel(repository: RecipeRepository): CookingModeViewModel = CookingModeViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf(AppRoute.RECIPE_ID to "recipe")),
    )
}

private class CookingFakeRepository(
    initialRecipe: Recipe?,
    private val failObservation: Boolean = false,
) : RecipeRepository {
    private val recipe = MutableStateFlow(initialRecipe)

    fun removeRecipe() {
        recipe.value = null
    }

    override fun observeRecipes(): Flow<List<Recipe>> = flowOf(listOfNotNull(recipe.value))
    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> = flowOf(emptyList())
    override fun observeCategories(): Flow<List<String>> = flowOf(emptyList())
    override fun observeRecipe(recipeId: String): Flow<Recipe?> = if (failObservation) {
        flow { throw IllegalStateException("boom") }
    } else {
        recipe
    }

    override suspend fun getRecipe(recipeId: String): Recipe? = recipe.value
    override suspend fun createRecipe(input: RecipeDraft): Result<String> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = Result.failure(UnsupportedOperationException())
}

private fun sampleRecipe() = Recipe(
    id = "recipe",
    name = "Tortilla",
    description = null,
    category = "Principal",
    servings = 4,
    preparationMinutes = 10,
    cookingMinutes = 20,
    notes = null,
    isFavorite = false,
    coverPhotoPath = null,
    ingredients = listOf(
        Ingredient("i1", "recipe", "1/2", "kg", "Patatas", null, 0),
    ),
    steps = listOf(
        RecipeStep("s2", "recipe", "Servir", 5, null, 1),
        RecipeStep("s1", "recipe", "Cortar", null, null, 0),
    ),
    createdAt = 1,
    updatedAt = 2,
)
