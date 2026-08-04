package com.rmm.recetasraquel.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.ui.navigation.AppRoute
import com.rmm.recetasraquel.util.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditorViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private var nextId = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        nextId = 0
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun idGenerator(): IdGenerator = IdGenerator { "gen-${nextId++}" }

    private fun createViewModel(
        repository: FakeEditorRepository,
        recipeId: String? = null,
    ): RecipeEditorViewModel {
        val handle = if (recipeId != null) {
            SavedStateHandle(mapOf(AppRoute.RECIPE_ID to recipeId))
        } else {
            SavedStateHandle(emptyMap())
        }
        return RecipeEditorViewModel(repository, idGenerator(), handle)
    }

    // ── Create mode ────────────────────────────────────────────

    @Test
    fun createModeStartsWithEmptyFields() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.mode is EditorMode.Create)
        assertEquals("", state.name)
        assertFalse(state.isLoading)
        assertNull(state.loadError)
        job.cancel()
    }

    @Test
    fun createValidationRequiresName() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.nameError)
        assertEquals("El nombre es obligatorio", vm.uiState.value.nameError)
        job.cancel()
    }

    @Test
    fun createWithOnlyNameSucceeds() = runTest(dispatcher) {
        val repo = FakeEditorRepository()
        val vm = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val navJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.navigation.collect {} }
        vm.updateName("Sopa")
        vm.save()
        advanceUntilIdle()
        assertTrue(repo.createdDrafts.isNotEmpty())
        assertEquals("Sopa", repo.createdDrafts.first().name)
        navJob.cancel()
        job.cancel()
    }

    @Test
    fun createFullRecipeSucceeds() = runTest(dispatcher) {
        val repo = FakeEditorRepository()
        val vm = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Tortilla")
        vm.updateCategory("Principal")
        vm.updateDescription("Jugosa")
        vm.updateServings("4")
        vm.updatePreparationMinutes("10")
        vm.updateCookingMinutes("20")
        vm.updateNotes("Servir templada")
        vm.addIngredient()
        advanceUntilIdle()
        val ingKey = vm.uiState.value.ingredients.first().key
        vm.updateIngredientQuantity(ingKey, "1")
        vm.updateIngredientUnit(ingKey, "kg")
        vm.updateIngredientName(ingKey, "Patatas")
        vm.updateIngredientNotes(ingKey, "Medianas")
        vm.addStep()
        advanceUntilIdle()
        val stepKey = vm.uiState.value.steps.first().key
        vm.updateStepInstruction(stepKey, "Cortar patatas")
        vm.updateStepTimer(stepKey, "5")
        vm.save()
        advanceUntilIdle()
        val draft = repo.createdDrafts.first()
        assertEquals("Tortilla", draft.name)
        assertEquals("Principal", draft.category)
        assertEquals("Jugosa", draft.description)
        assertEquals(4, draft.servings)
        assertEquals(10, draft.preparationMinutes)
        assertEquals(20, draft.cookingMinutes)
        assertEquals("Servir templada", draft.notes)
        assertEquals(1, draft.ingredients.size)
        assertEquals("Patatas", draft.ingredients[0].name)
        assertEquals("1", draft.ingredients[0].quantity)
        assertEquals("kg", draft.ingredients[0].unit)
        assertEquals("Medianas", draft.ingredients[0].notes)
        assertEquals(1, draft.steps.size)
        assertEquals("Cortar patatas", draft.steps[0].instruction)
        assertEquals(5, draft.steps[0].timerMinutes)
        job.cancel()
    }

    // ── Edit mode ──────────────────────────────────────────────

    @Test
    fun editModeLoadsExistingRecipe() = runTest(dispatcher) {
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to existingRecipe()))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.mode is EditorMode.Edit)
        assertEquals("r1", state.recipeId)
        assertEquals("Tortilla", state.name)
        assertEquals("Principal", state.category)
        assertEquals(1, state.ingredients.size)
        assertEquals("Patatas", state.ingredients[0].name)
        assertEquals("i1", state.ingredients[0].id)
        assertEquals(1, state.steps.size)
        assertEquals("Cortar", state.steps[0].instruction)
        assertEquals("s1", state.steps[0].id)
        job.cancel()
    }

    @Test
    fun editModeShowsNotFoundForMissingRecipe() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository(), recipeId = "missing")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadError != null)
        job.cancel()
    }

    // ── Validation ─────────────────────────────────────────────

    @Test
    fun partialIngredientWithQuantityButNoNameFails() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addIngredient()
        advanceUntilIdle()
        val key = vm.uiState.value.ingredients.first().key
        vm.updateIngredientQuantity(key, "2")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.ingredientErrors.containsKey(key))
        job.cancel()
    }

    @Test
    fun partialStepWithTimerButNoInstructionFails() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addStep()
        advanceUntilIdle()
        val key = vm.uiState.value.steps.first().key
        vm.updateStepTimer(key, "10")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.stepErrors.containsKey(key))
        job.cancel()
    }

    // ── Reorder ────────────────────────────────────────────────

    @Test
    fun reorderIngredientsSwapsCorrectly() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addIngredient()
        vm.addIngredient()
        advanceUntilIdle()
        val keys = vm.uiState.value.ingredients.map { it.key }
        vm.updateIngredientName(keys[0], "A")
        vm.updateIngredientName(keys[1], "B")
        advanceUntilIdle()
        assertEquals(listOf("A", "B"), vm.uiState.value.ingredients.map { it.name })
        vm.moveIngredientDown(keys[0])
        advanceUntilIdle()
        assertEquals(listOf("B", "A"), vm.uiState.value.ingredients.map { it.name })
        vm.moveIngredientUp(keys[0])
        advanceUntilIdle()
        assertEquals(listOf("A", "B"), vm.uiState.value.ingredients.map { it.name })
        job.cancel()
    }

    @Test
    fun reorderStepsSwapsCorrectly() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addStep()
        vm.addStep()
        advanceUntilIdle()
        val keys = vm.uiState.value.steps.map { it.key }
        vm.updateStepInstruction(keys[0], "First")
        vm.updateStepInstruction(keys[1], "Second")
        advanceUntilIdle()
        assertEquals(listOf("First", "Second"), vm.uiState.value.steps.map { it.instruction })
        vm.moveStepDown(keys[0])
        advanceUntilIdle()
        assertEquals(listOf("Second", "First"), vm.uiState.value.steps.map { it.instruction })
        vm.moveStepUp(keys[0])
        advanceUntilIdle()
        assertEquals(listOf("First", "Second"), vm.uiState.value.steps.map { it.instruction })
        job.cancel()
    }

    @Test
    fun cannotMoveFirstIngredientUp() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addIngredient()
        advanceUntilIdle()
        val key = vm.uiState.value.ingredients.first().key
        vm.updateIngredientName(key, "Only")
        vm.moveIngredientUp(key)
        advanceUntilIdle()
        assertEquals(listOf("Only"), vm.uiState.value.ingredients.map { it.name })
        job.cancel()
    }

    @Test
    fun cannotMoveLastIngredientDown() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addIngredient()
        advanceUntilIdle()
        val key = vm.uiState.value.ingredients.first().key
        vm.updateIngredientName(key, "Only")
        vm.moveIngredientDown(key)
        advanceUntilIdle()
        assertEquals(listOf("Only"), vm.uiState.value.ingredients.map { it.name })
        job.cancel()
    }

    // ── ID preservation ────────────────────────────────────────

    @Test
    fun editPreservesExistingIngredientAndStepIds() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals("i1", vm.uiState.value.ingredients[0].id)
        assertEquals("s1", vm.uiState.value.steps[0].id)
        job.cancel()
    }

    @Test
    fun newIngredientsHaveNullId() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addIngredient()
        advanceUntilIdle()
        assertNull(vm.uiState.value.ingredients[0].id)
        job.cancel()
    }

    @Test
    fun newStepsHaveNullId() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addStep()
        advanceUntilIdle()
        assertNull(vm.uiState.value.steps[0].id)
        job.cancel()
    }

    // ── Timestamp and favorite preservation ────────────────────

    @Test
    fun editPreservesCreatedAtAndFavorite() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(isFavorite = true, createdAt = 1000L)
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("New Name")
        vm.save()
        advanceUntilIdle()
        val updated = repo.updatedRecipes["r1"]
        assertNotNull(updated)
        assertEquals(1000L, updated!!.createdAt)
        assertTrue(updated.isFavorite)
        job.cancel()
    }

    // ── Unsaved changes ────────────────────────────────────────

    @Test
    fun fieldChangeSetsUnsavedChanges() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        vm.updateName("X")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        job.cancel()
    }

    @Test
    fun undoingChangeClearsUnsavedChanges() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("X")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        vm.updateName("")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        job.cancel()
    }

    @Test
    fun editDiscardShowsConfirmationThenHidesIt() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.handleBack()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showDiscardConfirmation)
        vm.updateName("Changed")
        vm.handleBack()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showDiscardConfirmation)
        vm.cancelDiscard()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showDiscardConfirmation)
        assertEquals("Changed", vm.uiState.value.name)
        job.cancel()
    }

    @Test
    fun handleBackWithNoChangesDoesNotShowDialog() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.handleBack()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showDiscardConfirmation)
        job.cancel()
    }

    // ── Save error does not lose content ───────────────────────

    @Test
    fun saveErrorRetainsContent() = runTest(dispatcher) {
        val repo = FakeEditorRepository(failCreate = true)
        val vm = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("FailRecipe")
        vm.save()
        advanceUntilIdle()
        assertEquals("FailRecipe", vm.uiState.value.name)
        assertNotNull(vm.uiState.value.saveError)
        assertFalse(vm.uiState.value.isSaving)
        job.cancel()
    }

    @Test
    fun deleteErrorRetainsContent() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe), failDelete = true)
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.deleteError)
        assertFalse(vm.uiState.value.isDeleting)
        assertEquals("Tortilla", vm.uiState.value.name)
        job.cancel()
    }

    // ── Double save prevention ─────────────────────────────────

    @Test
    fun doubleSavePrevented() = runTest(dispatcher) {
        val repo = FakeEditorRepository()
        val vm = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Recipe")
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.createdDrafts.size)
        vm.save()
        advanceUntilIdle()
        assertEquals(1, repo.createdDrafts.size)
        job.cancel()
    }

    @Test
    fun doubleDeletePrevented() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, repo.deletedIds.size)
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, repo.deletedIds.size)
        job.cancel()
    }

    // ── Delete cascade ─────────────────────────────────────────

    @Test
    fun deleteCallsRepositoryWithCorrectId() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.requestDelete()
        assertTrue(vm.uiState.value.showDeleteConfirmation)
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf("r1"), repo.deletedIds)
        assertFalse(vm.uiState.value.showDeleteConfirmation)
        job.cancel()
    }

    @Test
    fun cancelDeleteHidesDialog() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.requestDelete()
        assertTrue(vm.uiState.value.showDeleteConfirmation)
        vm.cancelDelete()
        assertFalse(vm.uiState.value.showDeleteConfirmation)
        job.cancel()
    }

    // ── Navigation events ──────────────────────────────────────

    @Test
    fun createEmitsRecipeCreated() = runTest(dispatcher) {
        val repo = FakeEditorRepository()
        val vm = createViewModel(repo)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        var event: EditorNavigationEvent? = null
        val navJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.navigation.collect { event = it }
        }
        vm.updateName("New")
        vm.save()
        advanceUntilIdle()
        assertTrue(event is EditorNavigationEvent.RecipeCreated)
        assertEquals("gen-created", (event as EditorNavigationEvent.RecipeCreated).recipeId)
        navJob.cancel()
        job.cancel()
    }

    @Test
    fun editEmitsRecipeUpdated() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        var event: EditorNavigationEvent? = null
        val navJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.navigation.collect { event = it }
        }
        vm.updateName("Updated")
        vm.save()
        advanceUntilIdle()
        assertTrue(event is EditorNavigationEvent.RecipeUpdated)
        assertEquals("r1", (event as EditorNavigationEvent.RecipeUpdated).recipeId)
        navJob.cancel()
        job.cancel()
    }

    @Test
    fun deleteEmitsRecipeDeleted() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        var event: EditorNavigationEvent? = null
        val navJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.navigation.collect { event = it }
        }
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(event is EditorNavigationEvent.RecipeDeleted)
        navJob.cancel()
        job.cancel()
    }

    // ── Remove ingredients and steps ───────────────────────────

    @Test
    fun removeIngredientDecreasesCount() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("R")
        vm.addIngredient()
        vm.addIngredient()
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.ingredients.size)
        val key = vm.uiState.value.ingredients[0].key
        vm.removeIngredient(key)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.ingredients.size)
        job.cancel()
    }

    @Test
    fun removeStepDecreasesCount() = runTest(dispatcher) {
        val vm = createViewModel(FakeEditorRepository())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("R")
        vm.addStep()
        vm.addStep()
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.steps.size)
        val key = vm.uiState.value.steps[0].key
        vm.removeStep(key)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.steps.size)
        job.cancel()
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun existingRecipe() = Recipe(
        id = "r1",
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
            Ingredient("i1", "r1", "1", "kg", "Patatas", null, 0),
        ),
        steps = listOf(
            RecipeStep("s1", "r1", "Cortar", null, null, 0),
        ),
        createdAt = 100L,
        updatedAt = 200L,
    )
}

private class FakeEditorRepository(
    private val recipes: MutableMap<String, Recipe> = mutableMapOf(),
    private val failCreate: Boolean = false,
    private val failUpdate: Boolean = false,
    private val failDelete: Boolean = false,
) : RecipeRepository {
    val createdDrafts = mutableListOf<RecipeDraft>()
    val updatedRecipes = mutableMapOf<String, Recipe>()
    val deletedIds = mutableListOf<String>()

    override fun observeRecipes(): Flow<List<Recipe>> = flow { emit(recipes.values.toList()) }
    override fun observeCatalog(filter: com.rmm.recetasraquel.domain.model.RecipeCatalogFilter): Flow<List<com.rmm.recetasraquel.domain.model.RecipeSummary>> = flow { emit(emptyList()) }
    override fun observeCategories(): Flow<List<String>> = flow { emit(emptyList()) }
    override fun observeRecipe(recipeId: String): Flow<Recipe?> = flow { emit(recipes[recipeId]) }
    override suspend fun getRecipe(recipeId: String): Recipe? = recipes[recipeId]
    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun createRecipe(input: RecipeDraft): Result<String> {
        if (failCreate) return Result.failure(IllegalStateException("test"))
        createdDrafts.add(input)
        return Result.success("gen-created")
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> {
        if (failUpdate) return Result.failure(IllegalStateException("test"))
        return Result.success(Unit)
    }

    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> {
        if (failUpdate) return Result.failure(IllegalStateException("test"))
        val existing = recipes[recipeId] ?: return Result.failure(IllegalStateException("not found"))
        updatedRecipes[recipeId] = existing.copy(name = draft.name)
        return Result.success(Unit)
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        if (failDelete) return Result.failure(IllegalStateException("test"))
        deletedIds.add(recipeId)
        return Result.success(Unit)
    }
}
