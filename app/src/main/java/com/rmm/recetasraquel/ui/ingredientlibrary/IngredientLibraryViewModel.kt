package com.rmm.recetasraquel.ui.ingredientlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rmm.recetasraquel.domain.ingredient.CatalogIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IngredientLibraryInfoUiState(
    val ingredient: IngredientCatalogEntry,
    val safetyRelations: List<CatalogIngredientSafetyRecord> = emptyList(),
    val regulatoryExemptions: List<RegulatoryExemption> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class IngredientLibraryUiState(
    val query: String = "",
    val categories: List<IngredientCatalogCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val results: List<IngredientCatalogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val ingredientInfo: IngredientLibraryInfoUiState? = null,
) {
    val hasActiveSearch: Boolean
        get() = query.isNotBlank() || selectedCategoryId != null
}

class IngredientLibraryViewModel(
    private val repository: IngredientCatalogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(IngredientLibraryUiState())
    val uiState: StateFlow<IngredientLibraryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var infoJob: Job? = null

    init {
        loadLibrary()
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value, errorMessage = null) }
        scheduleSearch()
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update { state ->
            state.copy(
                selectedCategoryId = if (state.selectedCategoryId == categoryId) null else categoryId,
                errorMessage = null,
            )
        }
        scheduleSearch(immediate = true)
    }

    fun clearFilters() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                selectedCategoryId = null,
                results = emptyList(),
                isSearching = false,
                errorMessage = null,
            )
        }
    }

    fun showIngredientInfo(ingredient: IngredientCatalogEntry) {
        loadIngredientInfo(ingredient)
    }

    fun dismissIngredientInfo() {
        infoJob?.cancel()
        _uiState.update { it.copy(ingredientInfo = null) }
    }

    fun retryIngredientInfo() {
        _uiState.value.ingredientInfo?.ingredient?.let(::loadIngredientInfo)
    }

    fun retry() {
        loadLibrary()
    }

    private fun loadLibrary() {
        searchJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSearching = false, errorMessage = null) }
            repository.getCategories().fold(
                onSuccess = { categories ->
                    _uiState.update {
                        it.copy(
                            categories = categories,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                    if (_uiState.value.hasActiveSearch) scheduleSearch(immediate = true)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSearching = false,
                            errorMessage = "No se pudo cargar la biblioteca de ingredientes.",
                        )
                    }
                },
            )
        }
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        val state = _uiState.value
        if (!state.hasActiveSearch) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)

            val current = _uiState.value
            repository.searchIngredients(
                query = current.query,
                categoryId = current.selectedCategoryId,
                limit = RESULT_LIMIT,
            ).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(results = results, isSearching = false, errorMessage = null)
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            results = emptyList(),
                            isSearching = false,
                            errorMessage = "No se pudieron buscar ingredientes.",
                        )
                    }
                },
            )
        }
    }

    private fun loadIngredientInfo(ingredient: IngredientCatalogEntry) {
        infoJob?.cancel()
        _uiState.update {
            it.copy(
                ingredientInfo = IngredientLibraryInfoUiState(
                    ingredient = ingredient,
                    isLoading = true,
                ),
            )
        }

        infoJob = viewModelScope.launch {
            runCatching {
                val safetyRelations = repository.getSafetyRelations(ingredient.id).getOrThrow()
                val regulatoryExemptions = repository.getRegulatoryExemptions(ingredient.id).getOrThrow()
                safetyRelations to regulatoryExemptions
            }.fold(
                onSuccess = { (safetyRelations, regulatoryExemptions) ->
                    if (_uiState.value.ingredientInfo?.ingredient?.id == ingredient.id) {
                        _uiState.update {
                            it.copy(
                                ingredientInfo = IngredientLibraryInfoUiState(
                                    ingredient = ingredient,
                                    safetyRelations = safetyRelations,
                                    regulatoryExemptions = regulatoryExemptions,
                                    isLoading = false,
                                ),
                            )
                        }
                    }
                },
                onFailure = {
                    if (_uiState.value.ingredientInfo?.ingredient?.id == ingredient.id) {
                        _uiState.update {
                            it.copy(
                                ingredientInfo = IngredientLibraryInfoUiState(
                                    ingredient = ingredient,
                                    isLoading = false,
                                    errorMessage = "No se pudo cargar la información de este ingrediente.",
                                ),
                            )
                        }
                    }
                },
            )
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 150L
        private const val RESULT_LIMIT = 120

        fun factory(repository: IngredientCatalogRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { IngredientLibraryViewModel(repository) }
        }
    }
}
