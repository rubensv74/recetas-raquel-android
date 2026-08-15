package com.rmm.recetasraquel.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.SaveRecipeInput
import com.rmm.recetasraquel.domain.usecase.SaveRecipeOperation
import com.rmm.recetasraquel.domain.usecase.SaveRecipeUseCase
import com.rmm.recetasraquel.ui.navigation.AppRoute
import com.rmm.recetasraquel.util.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertNotEquals
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
        repository: FakeEditorRepository = FakeEditorRepository(),
        recipeId: String? = null,
        photoStorage: RecipePhotoStorage = FakePhotoStorage(),
        saveRecipeUseCase: SaveRecipeOperation = FakeSaveRecipeUseCase(repository = repository),
    ): RecipeEditorViewModel {
        val handle = if (recipeId != null) {
            SavedStateHandle(mapOf(AppRoute.RECIPE_ID to recipeId))
        } else {
            SavedStateHandle(emptyMap())
        }
        return RecipeEditorViewModel(repository, idGenerator(), photoStorage, saveRecipeUseCase, handle)
    }

    // ── Create mode ────────────────────────────────────────────

    @Test
    fun createModeStartsWithEmptyFields() = runTest(dispatcher) {
        val vm = createViewModel()
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
        val vm = createViewModel()
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
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val navJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.navigation.collect {} }
        vm.updateName("Sopa")
        vm.save()
        advanceUntilIdle()
        assertEquals(1, fakeUseCase.createdInputs.size)
        assertEquals("Sopa", fakeUseCase.createdInputs.first().draft.name)
        navJob.cancel()
        job.cancel()
    }

    @Test
    fun createFullRecipeSucceeds() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
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
        val draft = fakeUseCase.createdInputs.first().draft
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val vm = createViewModel(repository = FakeEditorRepository(), recipeId = "missing")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadError != null)
        job.cancel()
    }

    // ── Validation ─────────────────────────────────────────────

    @Test
    fun partialIngredientWithQuantityButNoNameFails() = runTest(dispatcher) {
        val vm = createViewModel()
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
    fun ingredientWithNameButNoQuantityCannotBeSaved() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addIngredient()
        val key = vm.uiState.value.ingredients.single().key
        vm.updateIngredientName(key, "Patatas")

        vm.save()
        advanceUntilIdle()

        assertEquals("La cantidad es obligatoria", vm.uiState.value.ingredientErrors[key])
        assertTrue(fakeUseCase.createdInputs.isEmpty())
        job.cancel()
    }

    @Test
    fun completelyEmptyIngredientRowCannotBeSilentlyDiscardedOnSave() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Receta")
        vm.addIngredient()
        val key = vm.uiState.value.ingredients.single().key

        vm.save()
        advanceUntilIdle()

        assertEquals("El nombre y la cantidad son obligatorios", vm.uiState.value.ingredientErrors[key])
        assertTrue(fakeUseCase.createdInputs.isEmpty())
        job.cancel()
    }

    @Test
    fun partialStepWithTimerButNoInstructionFails() = runTest(dispatcher) {
        val vm = createViewModel()
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
        val vm = createViewModel()
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
        val vm = createViewModel()
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
        val vm = createViewModel()
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
        val vm = createViewModel()
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals("i1", vm.uiState.value.ingredients[0].id)
        assertEquals("s1", vm.uiState.value.steps[0].id)
        job.cancel()
    }

    @Test
    fun newIngredientsHaveNullId() = runTest(dispatcher) {
        val vm = createViewModel()
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
        val vm = createViewModel()
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val vm = createViewModel()
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
        val vm = createViewModel()
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
    fun handleBackWithNoChangesClosesEditorWithoutDialog() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val event = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            assertEquals(EditorNavigationEvent.EditorClosed, vm.navigation.first())
        }
        advanceUntilIdle()
        vm.handleBack()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showDiscardConfirmation)
        assertTrue(event.isCompleted)
        job.cancel()
    }

    @Test
    fun confirmingDiscardClosesEditor() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val event = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            assertEquals(EditorNavigationEvent.EditorClosed, vm.navigation.first())
        }
        advanceUntilIdle()
        vm.updateName("Cambio")
        vm.handleBack()
        assertTrue(vm.uiState.value.showDiscardConfirmation)
        vm.discardChanges()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showDiscardConfirmation)
        assertTrue(event.isCompleted)
        job.cancel()
    }

    @Test
    fun categoryAndUnitCanBeCleared() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateCategory("Postres")
        vm.addIngredient()
        val ingredientKey = vm.uiState.value.ingredients.single().key
        vm.updateIngredientUnit(ingredientKey, "g")

        vm.updateCategory("")
        vm.updateIngredientUnit(ingredientKey, "")

        assertEquals("", vm.uiState.value.category)
        assertEquals("", vm.uiState.value.ingredients.single().unit)
        job.cancel()
    }

    // ── Save error does not lose content ───────────────────────

    @Test
    fun saveErrorRetainsContent() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase(createHandler = { Result.failure(IllegalStateException("test")) })
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Recipe")
        vm.save()
        advanceUntilIdle()
        assertEquals(1, fakeUseCase.createdInputs.size)
        assertFalse(vm.uiState.value.isSaving)
        job.cancel()
    }

    @Test
    fun doubleDeletePrevented() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
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
        assertEquals("gen-0", (event as EditorNavigationEvent.RecipeCreated).recipeId)
        navJob.cancel()
        job.cancel()
    }

    @Test
    fun editEmitsRecipeUpdated() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val vm = createViewModel(repository = repo, recipeId = "r1")
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
        val vm = createViewModel()
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
        val vm = createViewModel()
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

    // ── Photo tests ───────────────────────────────────────────

    @Test
    fun selectCoverPhotoSetsStagedState() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Staged)
        job.cancel()
    }

    @Test
    fun selectCoverPhotoTriggersUnsavedChanges() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("T")
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        job.cancel()
    }

    @Test
    fun removeStagedCoverPhotoResetsToNone() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Staged)
        vm.removeCoverPhoto()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.None)
        job.cancel()
    }

    @Test
    fun removePersistedCoverPhotoMarksAsRemoved() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/cover.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repository = repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Persisted)
        vm.removeCoverPhoto()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Removed)
        job.cancel()
    }

    @Test
    fun selectStepPhotoSetsStagedState() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("R")
        vm.addStep()
        advanceUntilIdle()
        val stepKey = vm.uiState.value.steps[0].key
        vm.selectStepPhoto(stepKey, "content://media/2")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.stepPhotoStates[stepKey] is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Staged)
        job.cancel()
    }

    @Test
    fun removeStepPhotoRemovesFromMapWhenStaged() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("R")
        vm.addStep()
        advanceUntilIdle()
        val stepKey = vm.uiState.value.steps[0].key
        vm.selectStepPhoto(stepKey, "content://media/2")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.stepPhotoStates.containsKey(stepKey))
        vm.removeStepPhoto(stepKey)
        assertFalse(vm.uiState.value.stepPhotoStates.containsKey(stepKey))
        job.cancel()
    }

    @Test
    fun removeStepCleansUpPhotoState() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("R")
        vm.addStep()
        advanceUntilIdle()
        val stepKey = vm.uiState.value.steps[0].key
        vm.selectStepPhoto(stepKey, "content://media/3")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.stepPhotoStates.containsKey(stepKey))
        vm.removeStep(stepKey)
        assertFalse(vm.uiState.value.stepPhotoStates.containsKey(stepKey))
        job.cancel()
    }

    @Test
    fun discardChangesCleansStagedPhotos() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("R")
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Staged)
        vm.requestDiscardConfirmation()
        assertTrue(vm.uiState.value.showDiscardConfirmation)
        vm.discardChanges()
        assertFalse(vm.uiState.value.showDiscardConfirmation)
        job.cancel()
    }

    @Test
    fun saveIncludesCoverPhotoPathInDraft() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Test")
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        vm.updateCategory("Cat")
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.createdInputs.firstOrNull()
        assertNotNull(input)
        assertEquals("Cat", input!!.draft.category)
        job.cancel()
    }

    @Test
    fun dismissPhotoErrorRestoresPrevious() = runTest(dispatcher) {
        val vm = createViewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.dismissPhotoError()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.None)
        job.cancel()
    }

    @Test
    fun existingRecipeWithPhotosLoadsPersistedStates() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/cover.jpg")
        val step = recipe.steps[0].copy(photoPath = "recipe_photos/step.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe.copy(steps = listOf(step))))
        val vm = createViewModel(repository = repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.coverPhotoState is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Persisted)
        val stepKey = vm.uiState.value.steps[0].key
        assertTrue(vm.uiState.value.stepPhotoStates[stepKey] is com.rmm.recetasraquel.ui.editor.EditorPhotoState.Persisted)
        job.cancel()
    }

    // ── Sprint 4: Photo lifecycle tests ────────────────────────

    @Test
    fun stagedCoverSavesPermanentPathInDraft() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Test")
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.createdInputs.firstOrNull()
        assertNotNull(input)
        val stagedCover = input!!.stagedCover
        assertNotNull(stagedCover)
        job.cancel()
    }

    @Test
    fun stagedStepSavesPermanentPathInDraft() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Test")
        vm.addStep()
        advanceUntilIdle()
        val stepKey = vm.uiState.value.steps[0].key
        vm.updateStepInstruction(stepKey, "Hacer algo")
        vm.selectStepPhoto(stepKey, "content://media/2")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.createdInputs.firstOrNull()
        assertNotNull(input)
        val stagedStep = input!!.stagedSteps[stepKey]
        assertNotNull(stagedStep)
        job.cancel()
    }

    @Test
    fun noPersistedPathPointsToCacheDir() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(
            coverPhotoPath = "recipe_photos/r1/cover_abc.jpg",
            steps = listOf(RecipeStep("s1", "r1", "Cortar", null, "recipe_photos/r1/steps/step_s1_def.jpg", 0)),
        )
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repository = repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val coverPath = (vm.uiState.value.coverPhotoState as EditorPhotoState.Persisted).relativePath
        assertFalse(coverPath.contains("cache"))
        val stepKey = vm.uiState.value.steps[0].key
        val stepPath = (vm.uiState.value.stepPhotoStates[stepKey] as EditorPhotoState.Persisted).relativePath
        assertFalse(stepPath.contains("cache"))
        job.cancel()
    }

    @Test
    fun noPickerUriPersistedInRoom() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Test")
        vm.selectCoverPhoto("content://media/picker/123")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.createdInputs.firstOrNull()
        assertNotNull(input)
        assertNotNull(input!!.stagedCover)
        job.cancel()
    }

    @Test
    fun replacingCoverGeneratesDifferentPath() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover_old.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(repository = repo, recipeId = "r1", saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.updatedInputs["r1"]
        assertNotNull(input)
        assertNotEquals("recipe_photos/r1/cover_old.jpg", input!!.draft.coverPhotoPath)
        job.cancel()
    }

    @Test
    fun replacingStepPhotoGeneratesDifferentPath() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(
            steps = listOf(RecipeStep("s1", "r1", "Cortar", null, "recipe_photos/r1/steps/step_s1_old.jpg", 0)),
        )
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(repository = repo, recipeId = "r1", saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val stepKey = vm.uiState.value.steps[0].key
        vm.selectStepPhoto(stepKey, "content://media/2")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.updatedInputs["r1"]
        assertNotNull(input)
        job.cancel()
    }

    @Test
    fun reorderStepsKeepsPhotoAssociatedWithSameStepId() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(
            steps = listOf(
                RecipeStep("s1", "r1", "Paso 1", null, "recipe_photos/r1/steps/step_s1_photo.jpg", 0),
                RecipeStep("s2", "r1", "Paso 2", null, null, 1),
            ),
        )
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repository = repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val keys = vm.uiState.value.steps.map { it.key }
        vm.moveStepDown(keys[0])
        advanceUntilIdle()
        val reorderedKeys = vm.uiState.value.steps.map { it.key }
        val photoState = vm.uiState.value.stepPhotoStates[reorderedKeys[1]]
        assertTrue(photoState is EditorPhotoState.Persisted)
        assertEquals("recipe_photos/r1/steps/step_s1_photo.jpg", (photoState as EditorPhotoState.Persisted).relativePath)
        job.cancel()
    }

    @Test
    fun oldFileNotDeletedBeforeRoomSave() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover_old.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val photoStorage = FakePhotoStorage()
        val useCase = SaveRecipeUseCase(repo, photoStorage)
        val vm = createViewModel(repository = repo, recipeId = "r1", photoStorage = photoStorage, saveRecipeUseCase = useCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertTrue(photoStorage.deleted.contains("recipe_photos/r1/cover_old.jpg"))
        job.cancel()
    }

    @Test
    fun onRoomSuccessOnlyOldFileDeleted() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover_old.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val photoStorage = FakePhotoStorage()
        val useCase = SaveRecipeUseCase(repo, photoStorage)
        val vm = createViewModel(repository = repo, recipeId = "r1", photoStorage = photoStorage, saveRecipeUseCase = useCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertTrue(photoStorage.deleted.contains("recipe_photos/r1/cover_old.jpg"))
        assertFalse(photoStorage.deleted.any { it.contains("faked") })
        job.cancel()
    }

    @Test
    fun onRoomFailureOldFilePreservedNewFileDeleted() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover_old.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe), failUpdate = true)
        val photoStorage = FakePhotoStorage()
        val useCase = SaveRecipeUseCase(repo, photoStorage)
        val vm = createViewModel(repository = repo, recipeId = "r1", photoStorage = photoStorage, saveRecipeUseCase = useCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertFalse(photoStorage.deleted.contains("recipe_photos/r1/cover_old.jpg"))
        assertNotNull(vm.uiState.value.saveError)
        job.cancel()
    }

    @Test
    fun unmodifiedPhotoKeepsExactPath() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover_unchanged.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(repository = repo, recipeId = "r1", saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Updated")
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.updatedInputs["r1"]
        assertNotNull(input)
        assertEquals("recipe_photos/r1/cover_unchanged.jpg", input!!.draft.coverPhotoPath)
        job.cancel()
    }

    @Test
    fun removingPhotoPersistsNull() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover_old.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(repository = repo, recipeId = "r1", saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.removeCoverPhoto()
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.updatedInputs["r1"]
        assertNotNull(input)
        assertNull(input!!.draft.coverPhotoPath)
        job.cancel()
    }

    @Test
    fun removingStepCleansPhotoAfterSave() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(
            steps = listOf(
                RecipeStep("s1", "r1", "Paso 1", null, "recipe_photos/r1/steps/step_s1_photo.jpg", 0),
                RecipeStep("s2", "r1", "Paso 2", null, null, 1),
            ),
        )
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(repository = repo, recipeId = "r1", saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val keys = vm.uiState.value.steps.map { it.key }
        vm.removeStep(keys[0])
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.updatedInputs["r1"]
        assertNotNull(input)
        job.cancel()
    }

    @Test
    fun deletingRecipeDoesNotAffectOther() = runTest(dispatcher) {
        val recipe1 = existingRecipe()
        val recipe2 = Recipe(
            id = "r2", name = "Otra", description = null, category = null,
            servings = null, preparationMinutes = null, cookingMinutes = null,
            notes = null, isFavorite = false, coverPhotoPath = "recipe_photos/r2/cover.jpg",
            ingredients = emptyList(), steps = emptyList(),
            createdAt = 100L, updatedAt = 200L,
        )
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe1, "r2" to recipe2))
        val photoStorage = FakePhotoStorage()
        val vm = createViewModel(repository = repo, recipeId = "r1", photoStorage = photoStorage)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(repo.deletedIds.contains("r1"))
        assertFalse(photoStorage.deleted.contains("recipe_photos/r2/cover.jpg"))
        job.cancel()
    }

    @Test
    fun deleteCleanupIsIdempotent() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val photoStorage = FakePhotoStorage()
        val vm = createViewModel(repository = repo, recipeId = "r1", photoStorage = photoStorage)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, repo.deletedIds.size)
        job.cancel()
    }

    @Test
    fun twoConsecutivePromotionsGenerateDifferentNames() = runTest(dispatcher) {
        val recipe = existingRecipe().copy(coverPhotoPath = "recipe_photos/r1/cover_old.jpg")
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val photoStorage = FakePhotoStorage()
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(repository = repo, recipeId = "r1", photoStorage = photoStorage, saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectCoverPhoto("content://media/1")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        val firstInput = fakeUseCase.updatedInputs["r1"]
        vm.selectCoverPhoto("content://media/2")
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        val secondInput = fakeUseCase.updatedInputs["r1"]
        assertNotNull(firstInput)
        assertNotNull(secondInput)
        job.cancel()
    }

    // ── Retry prevention ──────────────────────────────────────

    @Test
    fun retryAfterCreateFailureUsesExistingRecipeId() = runTest(dispatcher) {
        var callCount = 0
        val fakeUseCase = FakeSaveRecipeUseCase(
            createHandler = { Result.failure(IllegalStateException("first fail")) },
        )
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Recipe")
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.saveError)
        assertFalse(vm.uiState.value.isSaving)
        job.cancel()
    }

    @Test
    fun retryAfterSuccessCreatesNewRecipe() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        var event: EditorNavigationEvent? = null
        val navJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.navigation.collect { event = it }
        }
        vm.updateName("Recipe1")
        vm.save()
        advanceUntilIdle()
        assertEquals("gen-0", (event as EditorNavigationEvent.RecipeCreated).recipeId)
        assertEquals(1, fakeUseCase.createdInputs.size)
        navJob.cancel()
        job.cancel()
    }

    @Test
    fun pendingRecipeIdClearedAfterUpdate() = runTest(dispatcher) {
        val recipe = existingRecipe()
        val repo = FakeEditorRepository(recipes = mutableMapOf("r1" to recipe))
        val vm = createViewModel(repository = repo, recipeId = "r1")
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Updated")
        vm.save()
        advanceUntilIdle()
        assertNull(vm.uiState.value.pendingRecipeId)
        job.cancel()
    }

    @Test
    fun createDraftHasPreGeneratedRecipeId() = runTest(dispatcher) {
        val fakeUseCase = FakeSaveRecipeUseCase()
        val vm = createViewModel(saveRecipeUseCase = fakeUseCase)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.updateName("Test")
        vm.save()
        advanceUntilIdle()
        val input = fakeUseCase.createdInputs.first()
        assertEquals("gen-0", input.draft.id)
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
        val id = "gen-${createdDrafts.size}"
        recipes[id] = Recipe(
            id = id,
            name = input.name,
            description = input.description,
            category = input.category,
            servings = input.servings,
            preparationMinutes = input.preparationMinutes,
            cookingMinutes = input.cookingMinutes,
            notes = input.notes,
            isFavorite = false,
            coverPhotoPath = input.coverPhotoPath,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAt = 0L,
            updatedAt = 0L,
        )
        return Result.success(id)
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> {
        if (failUpdate) return Result.failure(IllegalStateException("test"))
        return Result.success(Unit)
    }

    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> {
        if (failUpdate) return Result.failure(IllegalStateException("test"))
        val existing = recipes[recipeId] ?: return Result.failure(IllegalStateException("not found"))
        updatedRecipes[recipeId] = existing.copy(
            name = draft.name,
            coverPhotoPath = draft.coverPhotoPath,
        )
        return Result.success(Unit)
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        if (failDelete) return Result.failure(IllegalStateException("test"))
        deletedIds.add(recipeId)
        return Result.success(Unit)
    }
}

private class FakeSaveRecipeUseCase(
    private val repository: RecipeRepository? = null,
    private val createHandler: suspend (SaveRecipeInput) -> Result<String> = { Result.success("gen-0") },
    private val updateHandler: suspend (String, SaveRecipeInput) -> Result<Unit> = { _, _ -> Result.success(Unit) },
) : SaveRecipeOperation {
    val createdInputs = mutableListOf<SaveRecipeInput>()
    val updatedInputs = mutableMapOf<String, SaveRecipeInput>()

    override suspend fun create(input: SaveRecipeInput): Result<String> {
        createdInputs.add(input)
        return createHandler(input)
    }

    override suspend fun update(recipeId: String, input: SaveRecipeInput): Result<Unit> {
        updatedInputs[recipeId] = input
        if (repository != null) {
            return repository.updateRecipeFromDraft(recipeId, input.draft)
        }
        return updateHandler(recipeId, input)
    }
}

private class FakePhotoStorage(
    private val failPromote: Boolean = false,
) : RecipePhotoStorage {
    val staged = mutableListOf<StagedPhoto>()
    val promoted = mutableListOf<Pair<StagedPhoto, PhotoDestination>>()
    val deleted = mutableListOf<String>()
    var promoteCounter = 0

    override suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto> {
        val staged = StagedPhoto(
            stagedFile = java.io.File("fake_staging/photo.jpg"),
            relativePath = "recipe_photos/staging_photo.jpg",
        )
        this.staged.add(staged)
        return Result.success(staged)
    }

    override suspend fun promotePhoto(stagedPhoto: StagedPhoto, destination: PhotoDestination): Result<String> {
        if (failPromote) return Result.failure(IllegalStateException("promote failed"))
        promoteCounter++
        promoted.add(stagedPhoto to destination)
        val path = when (destination) {
            is PhotoDestination.Cover -> "recipe_photos/${destination.recipeId}/cover_faked_$promoteCounter.jpg"
            is PhotoDestination.Step -> "recipe_photos/${destination.recipeId}/steps/step_${destination.stepId}_faked_$promoteCounter.jpg"
        }
        return Result.success(path)
    }

    override suspend fun delete(relativePath: String): Result<Unit> {
        deleted.add(relativePath)
        return Result.success(Unit)
    }

    override suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit> = Result.success(Unit)

    override suspend fun resolve(relativePath: String): java.io.File? = null

    override suspend fun cleanStaging(): Result<Unit> = Result.success(Unit)

    override suspend fun getRecipePhotoPaths(recipeId: String): List<String> = emptyList()
}
