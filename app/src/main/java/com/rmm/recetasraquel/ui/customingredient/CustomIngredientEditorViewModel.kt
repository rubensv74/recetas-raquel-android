package com.rmm.recetasraquel.ui.customingredient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientDraft
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyDeclaration
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyEvidence
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientType
import com.rmm.recetasraquel.domain.ingredient.FoodSafetyGroupOption
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomIngredientSafetyRow(
    val key: String = UUID.randomUUID().toString(),
    val safetyGroupId: String = "",
    val relationType: CustomIngredientSafetyRelationType = CustomIngredientSafetyRelationType.UNKNOWN,
    val evidenceLevel: CustomIngredientSafetyEvidence = CustomIngredientSafetyEvidence.UNVERIFIED,
    val sourceDetails: String = "",
    val notes: String = "",
)

data class CustomIngredientEditorUiState(
    val name: String = "",
    val type: CustomIngredientType = CustomIngredientType.SIMPLE,
    val categories: List<IngredientCatalogCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val defaultUnit: String = "",
    val aliasesText: String = "",
    val brand: String = "",
    val tradeName: String = "",
    val compositionKnown: Boolean? = null,
    val labelReadAt: String = "",
    val notes: String = "",
    val safetyGroups: List<FoodSafetyGroupOption> = emptyList(),
    val safetyRows: List<CustomIngredientSafetyRow> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val validationMessage: String? = null,
)

sealed interface CustomIngredientEditorEvent {
    data class IngredientCreated(
        val ingredientId: String,
        val name: String,
        val defaultUnit: String?,
    ) : CustomIngredientEditorEvent
}

class CustomIngredientEditorViewModel(
    private val repository: CustomIngredientRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomIngredientEditorUiState())
    val uiState: StateFlow<CustomIngredientEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CustomIngredientEditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CustomIngredientEditorEvent> = _events.asSharedFlow()

    init {
        loadReferenceData()
    }

    fun setName(value: String) = update { copy(name = value, validationMessage = null) }
    fun setType(value: CustomIngredientType) = update { copy(type = value, validationMessage = null) }
    fun setCategory(value: String?) = update { copy(selectedCategoryId = value, validationMessage = null) }
    fun setDefaultUnit(value: String) = update { copy(defaultUnit = value) }
    fun setAliasesText(value: String) = update { copy(aliasesText = value) }
    fun setBrand(value: String) = update { copy(brand = value) }
    fun setTradeName(value: String) = update { copy(tradeName = value) }
    fun setCompositionKnown(value: Boolean) = update { copy(compositionKnown = value, validationMessage = null) }
    fun setLabelReadAt(value: String) = update { copy(labelReadAt = value) }
    fun setNotes(value: String) = update { copy(notes = value) }

    fun addSafetyRow() {
        _uiState.update { state ->
            state.copy(
                safetyRows = state.safetyRows + CustomIngredientSafetyRow(
                    safetyGroupId = state.safetyGroups.firstOrNull()?.id.orEmpty(),
                ),
                validationMessage = null,
            )
        }
    }

    fun removeSafetyRow(key: String) = update {
        copy(safetyRows = safetyRows.filterNot { it.key == key }, validationMessage = null)
    }

    fun setSafetyGroup(key: String, groupId: String) = updateSafetyRow(key) { copy(safetyGroupId = groupId) }
    fun setSafetyRelationType(key: String, value: CustomIngredientSafetyRelationType) =
        updateSafetyRow(key) { copy(relationType = value) }
    fun setSafetyEvidence(key: String, value: CustomIngredientSafetyEvidence) =
        updateSafetyRow(key) { copy(evidenceLevel = value) }
    fun setSafetySourceDetails(key: String, value: String) = updateSafetyRow(key) { copy(sourceDetails = value) }
    fun setSafetyNotes(key: String, value: String) = updateSafetyRow(key) { copy(notes = value) }

    fun retry() = loadReferenceData()

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(validationMessage = "Indica un nombre para el ingrediente.") }
            return
        }
        val compositionKnown = state.compositionKnown
        if (compositionKnown == null) {
            _uiState.update { it.copy(validationMessage = "Indica si conoces la composición del ingrediente.") }
            return
        }
        if (state.safetyRows.any { it.safetyGroupId.isBlank() }) {
            _uiState.update { it.copy(validationMessage = "Selecciona un grupo para cada declaración de seguridad.") }
            return
        }

        val isCommercialProduct = state.type == CustomIngredientType.COMMERCIAL_PRODUCT
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, validationMessage = null) }
            repository.createIngredient(
                CustomIngredientDraft(
                    name = name,
                    categoryId = state.selectedCategoryId,
                    defaultUnit = state.defaultUnit.trim().takeIf(String::isNotEmpty),
                    type = state.type,
                    aliases = state.aliasesText.split(',').map(String::trim).filter(String::isNotEmpty),
                    brand = state.brand.trim().takeIf { isCommercialProduct && it.isNotEmpty() },
                    tradeName = state.tradeName.trim().takeIf { isCommercialProduct && it.isNotEmpty() },
                    compositionKnown = compositionKnown,
                    labelReadAt = state.labelReadAt.trim().takeIf { isCommercialProduct && it.isNotEmpty() },
                    notes = state.notes.trim().takeIf(String::isNotEmpty),
                    safetyDeclarations = state.safetyRows.map { row ->
                        CustomIngredientSafetyDeclaration(
                            safetyGroupId = row.safetyGroupId,
                            relationType = row.relationType,
                            evidenceLevel = row.evidenceLevel,
                            sourceDetails = row.sourceDetails.trim().takeIf(String::isNotEmpty),
                            notes = row.notes.trim().takeIf(String::isNotEmpty),
                        )
                    },
                ),
            ).fold(
                onSuccess = { ingredientId ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(
                        CustomIngredientEditorEvent.IngredientCreated(
                            ingredientId = ingredientId,
                            name = name,
                            defaultUnit = state.defaultUnit.trim().takeIf(String::isNotEmpty),
                        ),
                    )
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "No se pudo guardar el ingrediente personalizado. Revisa los datos e inténtalo de nuevo.",
                        )
                    }
                },
            )
        }
    }

    private fun loadReferenceData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val categories = repository.getCategories()
            val groups = repository.getSafetyGroups()
            if (categories.isFailure || groups.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudieron cargar los datos necesarios para crear el ingrediente.",
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    categories = categories.getOrThrow(),
                    safetyGroups = groups.getOrThrow(),
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    private inline fun update(transform: CustomIngredientEditorUiState.() -> CustomIngredientEditorUiState) {
        _uiState.update(transform)
    }

    private inline fun updateSafetyRow(
        key: String,
        crossinline transform: CustomIngredientSafetyRow.() -> CustomIngredientSafetyRow,
    ) {
        _uiState.update { state ->
            state.copy(
                safetyRows = state.safetyRows.map { row -> if (row.key == key) row.transform() else row },
                validationMessage = null,
            )
        }
    }

    companion object {
        fun factory(repository: CustomIngredientRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CustomIngredientEditorViewModel(repository) }
        }
    }
}
