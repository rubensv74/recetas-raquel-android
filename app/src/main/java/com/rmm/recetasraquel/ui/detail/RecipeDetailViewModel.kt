package com.rmm.recetasraquel.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.RecipeSafetySummaryResolver
import com.rmm.recetasraquel.ui.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Content(
        val recipe: Recipe,
        val safetySummary: RecipeSafetySummary? = null,
        val safetyMessage: String? = null,
        val actionMessage: String? = null,
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle,
    private val safetySummaryResolver: RecipeSafetySummaryResolver? = null,
) : ViewModel() {
    private val recipeId: String = checkNotNull(savedStateHandle[AppRoute.RECIPE_ID])
    private val actionMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeRecipe(recipeId),
        actionMessage,
    ) { recipe, message ->
        if (recipe == null) {
            DetailUiState.NotFound
        } else {
            val safetyResult = safetySummaryResolver?.resolve(recipe)
            DetailUiState.Content(
                recipe = recipe,
                safetySummary = safetyResult?.getOrNull(),
                safetyMessage = if (safetyResult?.isFailure == true) {
                    "No se pudo cargar la información de seguridad alimentaria. Requiere revisión."
                } else {
                    null
                },
                actionMessage = message,
            )
        }
    }.catch {
        emit(DetailUiState.Error("No se pudo cargar la receta."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading,
    )

    fun toggleFavorite() {
        val recipe = (uiState.value as? DetailUiState.Content)?.recipe ?: return
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
        fun factory(
            repository: RecipeRepository,
            safetySummaryResolver: RecipeSafetySummaryResolver? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RecipeDetailViewModel(
                    repository = repository,
                    savedStateHandle = createSavedStateHandle(),
                    safetySummaryResolver = safetySummaryResolver,
                )
            }
        }
    }
}
