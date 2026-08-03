package com.rmm.recetasraquel.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val isLoading: Boolean = true,
    val filter: RecipeCatalogFilter = RecipeCatalogFilter(),
    val categories: List<String> = emptyList(),
    val recipes: List<RecipeSummary> = emptyList(),
    val hasAnyRecipes: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeCatalogViewModel(
    private val repository: RecipeRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(RecipeCatalogFilter())
    private val retrySignal = MutableStateFlow(0)
    private val actionMessage = MutableStateFlow<String?>(null)

    private val catalog = combine(filter, retrySignal) { value, _ -> value }
        .flatMapLatest(repository::observeCatalog)
    private val allRecipes = retrySignal.flatMapLatest {
        repository.observeCatalog(RecipeCatalogFilter())
    }

    val uiState: StateFlow<CatalogUiState> = combine(
        filter,
        catalog,
        allRecipes,
        repository.observeCategories(),
        actionMessage,
    ) { currentFilter, recipes, all, categories, message ->
        CatalogUiState(
            isLoading = false,
            filter = currentFilter,
            categories = categories,
            recipes = recipes,
            hasAnyRecipes = all.isNotEmpty(),
            actionMessage = message,
        )
    }.catch {
        emit(CatalogUiState(isLoading = false, filter = filter.value, errorMessage = "No se pudieron cargar las recetas."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState(),
    )

    fun setQuery(query: String) {
        filter.value = filter.value.copy(query = query)
    }

    fun toggleFavorites() {
        filter.value = filter.value.copy(favoritesOnly = !filter.value.favoritesOnly)
    }

    fun selectCategory(category: String?) {
        filter.value = filter.value.copy(category = category?.trim()?.takeIf(String::isNotEmpty))
    }

    fun clearFilters() {
        filter.value = RecipeCatalogFilter()
    }

    fun retry() {
        retrySignal.value += 1
    }

    fun toggleFavorite(recipe: RecipeSummary) {
        viewModelScope.launch {
            if (repository.setFavorite(recipe.id, !recipe.isFavorite).isFailure) {
                actionMessage.value = "No se pudo cambiar el favorito."
            }
        }
    }

    fun consumeActionMessage() {
        actionMessage.value = null
    }

    companion object {
        fun factory(repository: RecipeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RecipeCatalogViewModel(repository) as T
            }
    }
}
