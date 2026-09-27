package com.rmm.recetasraquel.ui.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecipePrintUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun printButtonForwardsCurrentRecipeAndSafetyState() {
        val recipe = Recipe(
            id = "recipe-print",
            name = "Receta imprimible",
            description = null,
            category = null,
            servings = null,
            preparationMinutes = null,
            cookingMinutes = null,
            notes = null,
            isFavorite = false,
            coverPhotoPath = null,
            ingredients = emptyList(),
            steps = emptyList(),
            createdAt = 1,
            updatedAt = 2,
        )
        val summary = RecipeSafetySummary(
            groups = emptyList(),
            reviewNotices = emptyList(),
            regulatoryExemptions = emptyList(),
        )
        var callbackInvoked = false

        composeRule.setContent {
            RecetasRaquelTheme {
                RecipeDetailScreen(
                    state = DetailUiState.Content(
                        recipe = recipe,
                        safetySummary = summary,
                    ),
                    onNavigateBack = {},
                    onToggleFavorite = {},
                    onPrintRecipe = { printedRecipe, printedSummary, safetyMessage ->
                        callbackInvoked =
                            printedRecipe == recipe &&
                                printedSummary == summary &&
                                safetyMessage == null
                    },
                )
            }
        }

        composeRule
            .onNodeWithTag("detail_print_pdf")
            .performScrollTo()
            .performClick()

        assertTrue(callbackInvoked)
    }
}
