package com.rmm.recetasraquel.ui.cooking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CookingModeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstStepShowsLargeGuidanceAndNavigation() {
        var nextClicks = 0
        composeRule.setContent {
            RecetasRaquelTheme {
                CookingModeScreen(
                    state = sampleContent(),
                    onPreviousStep = {},
                    onNextStep = { nextClicks++ },
                    onShowIngredients = {},
                    onHideIngredients = {},
                    onFinish = {},
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
        composeRule.onNodeWithText("Cortar las patatas.").assertIsDisplayed()
        composeRule.onNodeWithTag("cooking_previous").assertIsNotEnabled()
        composeRule.onNodeWithTag("cooking_next").performClick()
        assertEquals(1, nextClicks)
    }

    @Test
    fun ingredientsAreAvailableWithoutLeavingCurrentStep() {
        composeRule.setContent {
            var showIngredients by remember { mutableStateOf(false) }
            RecetasRaquelTheme {
                CookingModeScreen(
                    state = sampleContent(showIngredients = showIngredients),
                    onPreviousStep = {},
                    onNextStep = {},
                    onShowIngredients = { showIngredients = true },
                    onHideIngredients = { showIngredients = false },
                    onFinish = {},
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("cooking_ingredients_button").performClick()
        composeRule.onNodeWithTag("cooking_ingredients_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("1/2 kg · Patatas").assertIsDisplayed()
        composeRule.onNodeWithTag("cooking_ingredients_close").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cooking_ingredients_sheet").assertDoesNotExist()
        composeRule.onNodeWithText("Cortar las patatas.").assertIsDisplayed()
    }

    @Test
    fun lastStepInvokesFinish() {
        var finishClicks = 0
        composeRule.setContent {
            RecetasRaquelTheme {
                CookingModeScreen(
                    state = sampleContent(currentStepIndex = 1),
                    onPreviousStep = {},
                    onNextStep = {},
                    onShowIngredients = {},
                    onHideIngredients = {},
                    onFinish = { finishClicks++ },
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("cooking_finish").performClick()
        assertEquals(1, finishClicks)
    }

    @Test
    fun recipeWithoutStepsHasSafeEmptyState() {
        composeRule.setContent {
            RecetasRaquelTheme {
                CookingModeScreen(
                    state = sampleContent(steps = emptyList()),
                    onPreviousStep = {},
                    onNextStep = {},
                    onShowIngredients = {},
                    onHideIngredients = {},
                    onFinish = {},
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("cooking_no_steps").assertIsDisplayed()
        composeRule.onNodeWithTag("cooking_next").assertDoesNotExist()
    }
}

private fun sampleContent(
    steps: List<RecipeStep> = sampleCookingRecipe().steps.sortedBy { it.sortOrder },
    currentStepIndex: Int = 0,
    showIngredients: Boolean = false,
): CookingUiState.Content {
    val recipe = sampleCookingRecipe().copy(steps = steps)
    return CookingUiState.Content(
        recipe = recipe,
        steps = steps,
        currentStepIndex = currentStepIndex,
        showIngredients = showIngredients,
    )
}

private fun sampleCookingRecipe() = Recipe(
    id = "recipe",
    name = "Tortilla de patatas",
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
        Ingredient("i2", "recipe", "1", null, "Cebolla", "Opcional", 1),
    ),
    steps = listOf(
        RecipeStep("s1", "recipe", "Cortar las patatas.", null, null, 0),
        RecipeStep("s2", "recipe", "Cuajar la tortilla.", 5, null, 1),
    ),
    createdAt = 1,
    updatedAt = 2,
)
