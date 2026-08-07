package com.rmm.recetasraquel.ui.cooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.ui.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface CookingUiState {
    data object Loading : CookingUiState
    data object NotFound : CookingUiState
    data class Error(val message: String) : CookingUiState

    data class Content(
        val recipe: Recipe,
        val steps: List<RecipeStep>,
        val currentStepIndex: Int,
        val showIngredients: Boolean,
    ) : CookingUiState {
        val currentStep: RecipeStep?
            get() = steps.getOrNull(currentStepIndex)

        val totalSteps: Int
            get() = steps.size

        val canGoPrevious: Boolean
            get() = currentStepIndex > 0 && steps.isNotEmpty()

        val canGoNext: Boolean
            get() = currentStepIndex < steps.lastIndex

        val isLastStep: Boolean
            get() = currentStep != null && !canGoNext

        val progress: Float
            get() = if (steps.isEmpty()) 0f else (currentStepIndex + 1).toFloat() / steps.size.toFloat()
    }
}

class CookingModeViewModel(
    private val repository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val recipeId: String = checkNotNull(savedStateHandle[AppRoute.RECIPE_ID])
    private val currentStepIndex = savedStateHandle.getStateFlow(CURRENT_STEP_INDEX, 0)
    private val ingredientsVisible = MutableStateFlow(false)

    val uiState: StateFlow<CookingUiState> = combine(
        repository.observeRecipe(recipeId),
        currentStepIndex,
        ingredientsVisible,
    ) { recipe, requestedIndex, showIngredients ->
        if (recipe == null) {
            CookingUiState.NotFound
        } else {
            val orderedSteps = recipe.steps.sortedBy { it.sortOrder }
            val safeIndex = if (orderedSteps.isEmpty()) {
                0
            } else {
                requestedIndex.coerceIn(0, orderedSteps.lastIndex)
            }
            CookingUiState.Content(
                recipe = recipe,
                steps = orderedSteps,
                currentStepIndex = safeIndex,
                showIngredients = showIngredients,
            )
        }
    }.catch {
        emit(CookingUiState.Error("No se pudo cargar el modo cocina."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CookingUiState.Loading,
    )

    fun previousStep() {
        val state = uiState.value as? CookingUiState.Content ?: return
        if (state.canGoPrevious) {
            savedStateHandle[CURRENT_STEP_INDEX] = state.currentStepIndex - 1
        }
    }

    fun nextStep() {
        val state = uiState.value as? CookingUiState.Content ?: return
        if (state.canGoNext) {
            savedStateHandle[CURRENT_STEP_INDEX] = state.currentStepIndex + 1
        }
    }

    fun showIngredients() {
        if (uiState.value is CookingUiState.Content) {
            ingredientsVisible.value = true
        }
    }

    fun hideIngredients() {
        ingredientsVisible.value = false
    }

    companion object {
        private const val CURRENT_STEP_INDEX = "cookingCurrentStepIndex"

        fun factory(repository: RecipeRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CookingModeViewModel(repository, createSavedStateHandle()) }
        }
    }
}
