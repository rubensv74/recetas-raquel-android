package com.rmm.recetasraquel.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rmm.recetasraquel.domain.model.IngredientDraft
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStepDraft
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.SaveRecipeInput
import com.rmm.recetasraquel.domain.usecase.SaveRecipeOperation
import com.rmm.recetasraquel.ui.navigation.AppRoute
import com.rmm.recetasraquel.util.IdGenerator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface EditorNavigationEvent {
    data class RecipeCreated(val recipeId: String) : EditorNavigationEvent
    data class RecipeUpdated(val recipeId: String) : EditorNavigationEvent
    data object RecipeDeleted : EditorNavigationEvent
    data class ShowMessage(val message: String) : EditorNavigationEvent
}

sealed interface EditorMode {
    data object Create : EditorMode
    data class Edit(val recipeId: String) : EditorMode
}

data class EditorIngredientItem(
    val key: String = UUID.randomUUID().toString(),
    val id: String? = null,
    val quantity: String = "",
    val unit: String = "",
    val name: String = "",
    val notes: String = "",
    val catalogIngredientId: String? = null,
    val customIngredientId: String? = null,
)

data class EditorStepItem(
    val key: String = UUID.randomUUID().toString(),
    val id: String? = null,
    val instruction: String = "",
    val timerMinutes: String = "",
)

data class RecipeEditorUiState(
    val mode: EditorMode = EditorMode.Create,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val recipeId: String? = null,
    val pendingRecipeId: String? = null,
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val servings: String = "",
    val preparationMinutes: String = "",
    val cookingMinutes: String = "",
    val notes: String = "",
    val ingredients: List<EditorIngredientItem> = emptyList(),
    val steps: List<EditorStepItem> = emptyList(),
    val coverPhotoState: EditorPhotoState = EditorPhotoState.None,
    val stepPhotoStates: Map<String, EditorPhotoState> = emptyMap(),
    val replacedCoverPath: String? = null,
    val replacedStepPaths: Map<String, String> = emptyMap(),
    val nameError: String? = null,
    val ingredientErrors: Map<String, String> = emptyMap(),
    val stepErrors: Map<String, String> = emptyMap(),
    val hasUnsavedChanges: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val deleteError: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val showDiscardConfirmation: Boolean = false,
) {
    companion object {
        fun forCreate() = RecipeEditorUiState(mode = EditorMode.Create)
        fun forEditLoading() = RecipeEditorUiState(mode = EditorMode.Edit(""), isLoading = true)
        fun forEditNotFound() = RecipeEditorUiState(mode = EditorMode.Edit(""), loadError = "La receta ya no está disponible.")
        fun forEditError() = RecipeEditorUiState(mode = EditorMode.Edit(""), loadError = "No se pudo cargar la receta.")
    }
}

class RecipeEditorViewModel(
    private val repository: RecipeRepository,
    private val idGenerator: IdGenerator,
    private val photoStorage: RecipePhotoStorage,
    private val saveRecipeUseCase: SaveRecipeOperation,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val recipeId: String? = savedStateHandle[AppRoute.RECIPE_ID]
    private val mode: EditorMode = if (recipeId == null || recipeId == "new") {
        EditorMode.Create
    } else {
        EditorMode.Edit(recipeId)
    }

    private val _uiState = MutableStateFlow(
        when (mode) {
            is EditorMode.Create -> RecipeEditorUiState.forCreate()
            is EditorMode.Edit -> RecipeEditorUiState.forEditLoading()
        }
    )
    val uiState: StateFlow<RecipeEditorUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<EditorNavigationEvent>()
    val navigation: SharedFlow<EditorNavigationEvent> = _navigation.asSharedFlow()

    private var initialState: NormalizedEditorState = NormalizedEditorState()

    init {
        if (mode is EditorMode.Edit) {
            loadRecipe(mode.recipeId)
        } else {
            initialState = computeNormalizedState(_uiState.value)
        }
    }

    private fun loadRecipe(id: String) {
        viewModelScope.launch {
            val recipe = repository.getRecipe(id)
            if (recipe == null) {
                _uiState.value = RecipeEditorUiState.forEditNotFound()
                return@launch
            }
            val coverState = recipe.coverPhotoPath?.let { EditorPhotoState.Persisted(it) }
                ?: EditorPhotoState.None
            val stepStates = mutableMapOf<String, EditorPhotoState>()
            val steps = recipe.steps.sortedBy { it.sortOrder }.map { step ->
                val key = UUID.randomUUID().toString()
                step.photoPath?.let { stepStates[key] = EditorPhotoState.Persisted(it) }
                EditorStepItem(
                    key = key,
                    id = step.id,
                    instruction = step.instruction,
                    timerMinutes = step.timerMinutes?.toString() ?: "",
                )
            }
            _uiState.value = RecipeEditorUiState(
                mode = mode,
                recipeId = recipe.id,
                name = recipe.name,
                category = recipe.category ?: "",
                description = recipe.description ?: "",
                servings = recipe.servings?.toString() ?: "",
                preparationMinutes = recipe.preparationMinutes?.toString() ?: "",
                cookingMinutes = recipe.cookingMinutes?.toString() ?: "",
                notes = recipe.notes ?: "",
                ingredients = recipe.ingredients.sortedBy { it.sortOrder }.map { ing ->
                    EditorIngredientItem(
                        key = UUID.randomUUID().toString(),
                        id = ing.id,
                        quantity = ing.quantity ?: "",
                        unit = ing.unit ?: "",
                        name = ing.name,
                        notes = ing.notes ?: "",
                        catalogIngredientId = ing.catalogIngredientId,
                        customIngredientId = ing.customIngredientId,
                    )
                },
                steps = steps,
                coverPhotoState = coverState,
                stepPhotoStates = stepStates,
            )
            initialState = computeNormalizedState(_uiState.value)
        }
    }

    // --- Field updates ---

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
        checkForUnsavedChanges()
    }

    fun updateCategory(value: String) {
        _uiState.update { it.copy(category = value) }
        checkForUnsavedChanges()
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
        checkForUnsavedChanges()
    }

    fun updateServings(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(servings = value) }
            checkForUnsavedChanges()
        }
    }

    fun updatePreparationMinutes(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(preparationMinutes = value) }
            checkForUnsavedChanges()
        }
    }

    fun updateCookingMinutes(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(cookingMinutes = value) }
            checkForUnsavedChanges()
        }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
        checkForUnsavedChanges()
    }

    // --- Ingredients ---

    fun addIngredient() {
        _uiState.update {
            it.copy(ingredients = it.ingredients + EditorIngredientItem())
        }
        checkForUnsavedChanges()
    }

    fun addCatalogIngredient(
        catalogIngredientId: String,
        canonicalName: String,
        defaultUnit: String?,
    ) {
        require(catalogIngredientId.isNotBlank())
        require(canonicalName.isNotBlank())
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients + EditorIngredientItem(
                    name = canonicalName,
                    unit = defaultUnit.orEmpty(),
                    catalogIngredientId = catalogIngredientId,
                    customIngredientId = null,
                ),
            )
        }
        checkForUnsavedChanges()
    }

    fun updateIngredientQuantity(key: String, value: String) {
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients.map {
                    if (it.key == key) it.copy(quantity = value) else it
                },
            )
        }
        checkForUnsavedChanges()
    }

    fun updateIngredientUnit(key: String, value: String) {
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients.map {
                    if (it.key == key) it.copy(unit = value) else it
                },
            )
        }
        checkForUnsavedChanges()
    }

    fun updateIngredientName(key: String, value: String) {
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients.map { item ->
                    if (item.key == key && item.catalogIngredientId == null) item.copy(name = value) else item
                },
                ingredientErrors = state.ingredientErrors - key,
            )
        }
        checkForUnsavedChanges()
    }

    fun updateIngredientNotes(key: String, value: String) {
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients.map {
                    if (it.key == key) it.copy(notes = value) else it
                },
            )
        }
        checkForUnsavedChanges()
    }

    fun removeIngredient(key: String) {
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients.filter { it.key != key },
                ingredientErrors = state.ingredientErrors - key,
            )
        }
        checkForUnsavedChanges()
    }

    fun moveIngredientUp(key: String) {
        _uiState.update { state ->
            val list = state.ingredients.toMutableList()
            val index = list.indexOfFirst { it.key == key }
            if (index > 0) {
                list[index] = list[index - 1].also { list[index - 1] = list[index] }
            }
            state.copy(ingredients = list)
        }
        checkForUnsavedChanges()
    }

    fun moveIngredientDown(key: String) {
        _uiState.update { state ->
            val list = state.ingredients.toMutableList()
            val index = list.indexOfFirst { it.key == key }
            if (index in 0 until list.lastIndex) {
                list[index] = list[index + 1].also { list[index + 1] = list[index] }
            }
            state.copy(ingredients = list)
        }
        checkForUnsavedChanges()
    }

    // --- Steps ---

    fun addStep() {
        _uiState.update { it.copy(steps = it.steps + EditorStepItem()) }
        checkForUnsavedChanges()
    }

    fun updateStepInstruction(key: String, value: String) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map {
                    if (it.key == key) it.copy(instruction = value) else it
                },
                stepErrors = state.stepErrors - key,
            )
        }
        checkForUnsavedChanges()
    }

    fun updateStepTimer(key: String, value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { state ->
                state.copy(
                    steps = state.steps.map {
                        if (it.key == key) it.copy(timerMinutes = value) else it
                    },
                )
            }
            checkForUnsavedChanges()
        }
    }

    fun removeStep(key: String) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.filter { it.key != key },
                stepErrors = state.stepErrors - key,
                stepPhotoStates = state.stepPhotoStates - key,
            )
        }
        checkForUnsavedChanges()
    }

    fun moveStepUp(key: String) {
        _uiState.update { state ->
            val list = state.steps.toMutableList()
            val index = list.indexOfFirst { it.key == key }
            if (index > 0) {
                list[index] = list[index - 1].also { list[index - 1] = list[index] }
            }
            state.copy(steps = list)
        }
        checkForUnsavedChanges()
    }

    fun moveStepDown(key: String) {
        _uiState.update { state ->
            val list = state.steps.toMutableList()
            val index = list.indexOfFirst { it.key == key }
            if (index in 0 until list.lastIndex) {
                list[index] = list[index + 1].also { list[index + 1] = list[index] }
            }
            state.copy(steps = list)
        }
        checkForUnsavedChanges()
    }

    // --- Cover photo ---

    fun selectCoverPhoto(uri: Uri) {
        selectCoverPhoto(uri.toString())
    }

    fun selectCoverPhoto(uriString: String) {
        val previous = _uiState.value.coverPhotoState
        val replacePath = if (previous is EditorPhotoState.Persisted) previous.relativePath else null
        _uiState.update { it.copy(coverPhotoState = EditorPhotoState.Processing(previous), replacedCoverPath = replacePath ?: it.replacedCoverPath) }
        viewModelScope.launch {
            val result = photoStorage.stagePhoto(uriString)
            result.fold(
                onSuccess = { staged ->
                    _uiState.update { it.copy(coverPhotoState = EditorPhotoState.Staged(staged)) }
                    checkForUnsavedChanges()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(coverPhotoState = EditorPhotoState.Error(previous, error.message ?: "Error"))
                    }
                },
            )
        }
    }

    fun removeCoverPhoto() {
        val current = _uiState.value.coverPhotoState
        when (current) {
            is EditorPhotoState.Staged -> {
                viewModelScope.launch { photoStorage.deleteStaged(current.stagedPhoto) }
                _uiState.update { it.copy(coverPhotoState = EditorPhotoState.None) }
            }
            is EditorPhotoState.Persisted -> {
                _uiState.update { it.copy(coverPhotoState = EditorPhotoState.Removed(current)) }
            }
            else -> {}
        }
        checkForUnsavedChanges()
    }

    // --- Step photos ---

    fun selectStepPhoto(stepKey: String, uri: Uri) {
        selectStepPhoto(stepKey, uri.toString())
    }

    fun selectStepPhoto(stepKey: String, uriString: String) {
        val previous = _uiState.value.stepPhotoStates[stepKey] ?: EditorPhotoState.None
        val replacePath = if (previous is EditorPhotoState.Persisted) previous.relativePath else null
        _uiState.update { state ->
            state.copy(
                stepPhotoStates = state.stepPhotoStates + (stepKey to EditorPhotoState.Processing(previous)),
                replacedStepPaths = if (replacePath != null) state.replacedStepPaths + (stepKey to replacePath) else state.replacedStepPaths,
            )
        }
        viewModelScope.launch {
            val result = photoStorage.stagePhoto(uriString)
            result.fold(
                onSuccess = { staged ->
                    _uiState.update { state ->
                        state.copy(stepPhotoStates = state.stepPhotoStates + (stepKey to EditorPhotoState.Staged(staged)))
                    }
                    checkForUnsavedChanges()
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            stepPhotoStates = state.stepPhotoStates + (
                                stepKey to EditorPhotoState.Error(previous, error.message ?: "Error")
                            ),
                        )
                    }
                },
            )
        }
    }

    fun removeStepPhoto(stepKey: String) {
        val current = _uiState.value.stepPhotoStates[stepKey] ?: return
        when (current) {
            is EditorPhotoState.Staged -> {
                viewModelScope.launch { photoStorage.deleteStaged(current.stagedPhoto) }
                _uiState.update { state ->
                    state.copy(stepPhotoStates = state.stepPhotoStates - stepKey)
                }
            }
            is EditorPhotoState.Persisted -> {
                _uiState.update { state ->
                    state.copy(stepPhotoStates = state.stepPhotoStates + (stepKey to EditorPhotoState.Removed(current)))
                }
            }
            else -> {}
        }
        checkForUnsavedChanges()
    }

    fun dismissPhotoError() {
        val current = _uiState.value.coverPhotoState
        if (current is EditorPhotoState.Error) {
            _uiState.update { it.copy(coverPhotoState = current.previous ?: EditorPhotoState.None) }
        }
        _uiState.update { state ->
            val cleaned = state.stepPhotoStates.mapValues { (_, v) ->
                if (v is EditorPhotoState.Error) v.previous ?: EditorPhotoState.None else v
            }
            state.copy(stepPhotoStates = cleaned)
        }
    }

    // --- Unsaved changes detection ---

    private fun checkForUnsavedChanges() {
        val current = computeNormalizedState(_uiState.value)
        _uiState.update { it.copy(hasUnsavedChanges = current != initialState) }
    }

    private fun computeNormalizedState(state: RecipeEditorUiState): NormalizedEditorState {
        return NormalizedEditorState(
            name = state.name.trim(),
            category = state.category.trim().ifEmpty { null },
            description = state.description.trim().ifEmpty { null },
            servings = state.servings.trim().ifEmpty { null },
            preparationMinutes = state.preparationMinutes.trim().ifEmpty { null },
            cookingMinutes = state.cookingMinutes.trim().ifEmpty { null },
            notes = state.notes.trim().ifEmpty { null },
            ingredients = state.ingredients
                .filter { it.name.isNotBlank() }
                .map {
                    NormalizedIngredient(
                        quantity = it.quantity.trim(),
                        unit = it.unit.trim(),
                        name = it.name.trim(),
                        notes = it.notes.trim(),
                        catalogIngredientId = it.catalogIngredientId,
                        customIngredientId = it.customIngredientId,
                    )
                },
            steps = state.steps
                .filter { it.instruction.isNotBlank() || it.timerMinutes.isNotBlank() }
                .map { NormalizedStep(it.instruction.trim(), it.timerMinutes.trim()) },
            coverPhotoState = normalizePhotoState(state.coverPhotoState),
            stepPhotoStates = state.stepPhotoStates
                .filter { state.steps.any { step -> step.key == it.key } }
                .mapValues { normalizePhotoState(it.value) },
        )
    }

    private fun normalizePhotoState(state: EditorPhotoState): String = when (state) {
        is EditorPhotoState.None -> "none"
        is EditorPhotoState.Persisted -> "persisted:${state.relativePath}"
        is EditorPhotoState.Staged -> "staged:${state.stagedPhoto.relativePath}"
        is EditorPhotoState.Processing -> "processing"
        is EditorPhotoState.Removed -> "removed"
        is EditorPhotoState.Error -> "error"
    }

    // --- Validation ---

    private fun validate(): Boolean {
        var valid = true
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es obligatorio") }
            valid = false
        }

        val ingredientErrors = mutableMapOf<String, String>()
        state.ingredients.forEach { item ->
            if (item.name.isBlank() && (item.quantity.isNotBlank() || item.unit.isNotBlank() || item.notes.isNotBlank())) {
                ingredientErrors[item.key] = "El nombre es obligatorio"
                valid = false
            }
        }

        val stepErrors = mutableMapOf<String, String>()
        state.steps.forEach { item ->
            if (item.instruction.isBlank() && item.timerMinutes.isNotBlank()) {
                stepErrors[item.key] = "La instrucción es obligatoria"
                valid = false
            }
        }

        _uiState.update {
            it.copy(ingredientErrors = ingredientErrors, stepErrors = stepErrors)
        }
        return valid
    }

    // --- Save ---

    fun save() {
        if (!validate()) return
        if (_uiState.value.isSaving) return

        when (mode) {
            is EditorMode.Create -> createRecipe()
            is EditorMode.Edit -> updateRecipe(mode.recipeId)
        }
    }

    private fun createRecipe() {
        val state = _uiState.value
        val recipeId = state.pendingRecipeId ?: idGenerator.newId()
        _uiState.update { it.copy(isSaving = true, saveError = null, pendingRecipeId = recipeId) }
        viewModelScope.launch {
            val input = buildSaveInput(state, recipeId)
            val result = saveRecipeUseCase.create(input)
            if (result.isSuccess) {
                val newRecipeId = result.getOrThrow()
                _uiState.update { it.copy(isSaving = false, pendingRecipeId = newRecipeId) }
                _navigation.emit(EditorNavigationEvent.RecipeCreated(newRecipeId))
            } else {
                _uiState.update {
                    it.copy(isSaving = false, saveError = "No se pudo guardar la receta. Inténtalo de nuevo.")
                }
            }
        }
    }

    private fun updateRecipe(id: String) {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val input = buildSaveInput(state)
            val result = saveRecipeUseCase.update(id, input)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, pendingRecipeId = null) }
                _navigation.emit(EditorNavigationEvent.RecipeUpdated(id))
            } else {
                _uiState.update {
                    it.copy(isSaving = false, saveError = "No se pudo guardar la receta. Inténtalo de nuevo.")
                }
            }
        }
    }

    private fun buildSaveInput(state: RecipeEditorUiState, recipeId: String? = null): SaveRecipeInput {
        val stepIds = mutableMapOf<String, String>()
        state.steps.forEach { step ->
            if (step.id == null) {
                stepIds[step.key] = idGenerator.newId()
            } else {
                stepIds[step.key] = step.id
            }
        }

        val draft = buildDraft(state, stepIds, recipeId)

        val stagedCover = (state.coverPhotoState as? EditorPhotoState.Staged)?.stagedPhoto
        val stagedSteps = state.steps.associate { step ->
            step.key to (state.stepPhotoStates[step.key] as? EditorPhotoState.Staged)?.stagedPhoto
        }

        return SaveRecipeInput(
            draft = draft,
            stagedCover = stagedCover,
            stagedSteps = stagedSteps,
            replacedCoverPath = state.replacedCoverPath,
            replacedStepPaths = state.replacedStepPaths,
        )
    }

    private fun buildDraft(
        state: RecipeEditorUiState,
        stepIds: Map<String, String>,
        recipeId: String? = null,
    ): RecipeDraft {
        val coverPath = resolveCoverPath(state)
        return RecipeDraft(
            id = recipeId,
            name = state.name,
            description = state.description.ifBlank { null },
            category = state.category.ifBlank { null },
            servings = state.servings.toIntOrNull(),
            preparationMinutes = state.preparationMinutes.toIntOrNull(),
            cookingMinutes = state.cookingMinutes.toIntOrNull(),
            notes = state.notes.ifBlank { null },
            coverPhotoPath = coverPath,
            ingredients = state.ingredients
                .filter { it.name.isNotBlank() }
                .map { item ->
                    IngredientDraft(
                        id = item.id,
                        quantity = item.quantity.ifBlank { null },
                        unit = item.unit.ifBlank { null },
                        name = item.name,
                        notes = item.notes.ifBlank { null },
                        catalogIngredientId = item.catalogIngredientId,
                        customIngredientId = item.customIngredientId,
                    )
                },
            steps = state.steps
                .filter { it.instruction.isNotBlank() || it.timerMinutes.isNotBlank() }
                .map { item ->
                    val stepPhoto = state.stepPhotoStates[item.key]
                    RecipeStepDraft(
                        id = stepIds[item.key],
                        stepKey = item.key,
                        instruction = item.instruction,
                        timerMinutes = item.timerMinutes.toIntOrNull(),
                        photoPath = resolveStepPhotoPath(stepPhoto),
                    )
                },
        )
    }

    private fun resolveCoverPath(state: RecipeEditorUiState): String? = when (val cover = state.coverPhotoState) {
        is EditorPhotoState.Persisted -> cover.relativePath
        is EditorPhotoState.Staged -> null
        is EditorPhotoState.Removed -> null
        else -> null
    }

    private fun resolveStepPhotoPath(photo: EditorPhotoState?): String? = when (photo) {
        is EditorPhotoState.Persisted -> photo.relativePath
        is EditorPhotoState.Staged -> null
        is EditorPhotoState.Removed -> null
        else -> null
    }

    // --- Delete ---

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun confirmDelete() {
        val id = (mode as? EditorMode.Edit)?.recipeId ?: return
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false, deleteError = null) }
        viewModelScope.launch {
            val photoPaths = photoStorage.getRecipePhotoPaths(id)
            val result = repository.deleteRecipe(id)
            if (result.isSuccess) {
                photoPaths.forEach { photoStorage.delete(it) }
                _navigation.emit(EditorNavigationEvent.RecipeDeleted)
            } else {
                _uiState.update {
                    it.copy(isDeleting = false, deleteError = "No se pudo eliminar la receta.")
                }
            }
        }
    }

    // --- Back / discard ---

    fun requestDiscardConfirmation() {
        _uiState.update { it.copy(showDiscardConfirmation = true) }
    }

    fun cancelDiscard() {
        _uiState.update { it.copy(showDiscardConfirmation = false) }
    }

    fun discardChanges() {
        cleanStaging()
        _uiState.update { it.copy(showDiscardConfirmation = false) }
    }

    fun handleBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardConfirmation = true) }
        }
    }

    fun consumeSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    private fun cleanStaging() {
        val state = _uiState.value
        if (state.coverPhotoState is EditorPhotoState.Staged) {
            viewModelScope.launch { photoStorage.deleteStaged(state.coverPhotoState.stagedPhoto) }
        }
        state.stepPhotoStates.values.filterIsInstance<EditorPhotoState.Staged>().forEach { staged ->
            viewModelScope.launch { photoStorage.deleteStaged(staged.stagedPhoto) }
        }
    }

    companion object {
        fun factory(
            repository: RecipeRepository,
            idGenerator: IdGenerator,
            photoStorage: RecipePhotoStorage,
            saveRecipeUseCase: SaveRecipeOperation,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RecipeEditorViewModel(repository, idGenerator, photoStorage, saveRecipeUseCase, createSavedStateHandle())
            }
        }
    }
}

private data class NormalizedEditorState(
    val name: String = "",
    val category: String? = null,
    val description: String? = null,
    val servings: String? = null,
    val preparationMinutes: String? = null,
    val cookingMinutes: String? = null,
    val notes: String? = null,
    val ingredients: List<NormalizedIngredient> = emptyList(),
    val steps: List<NormalizedStep> = emptyList(),
    val coverPhotoState: String = "none",
    val stepPhotoStates: Map<String, String> = emptyMap(),
)

private data class NormalizedIngredient(
    val quantity: String,
    val unit: String,
    val name: String,
    val notes: String,
    val catalogIngredientId: String?,
    val customIngredientId: String?,
)

private data class NormalizedStep(
    val instruction: String,
    val timerMinutes: String,
)
