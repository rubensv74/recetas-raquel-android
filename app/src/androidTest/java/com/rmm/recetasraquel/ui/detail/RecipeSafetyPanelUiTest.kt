package com.rmm.recetasraquel.ui.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.rmm.recetasraquel.domain.ingredient.RecipeReviewNotice
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyGroupSummary
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyObservation
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyPresentationState
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import org.junit.Rule
import org.junit.Test

class RecipeSafetyPanelUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun panelShowsGroupedEvidenceAndReviewWarning() {
        val summary = RecipeSafetySummary(
            groups = listOf(
                RecipeSafetyGroupSummary(
                    safetyGroupId = "safety-eu-cereals-gluten",
                    safetyGroupName = "Cereales que contienen gluten",
                    presentationState = RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA,
                    observations = listOf(
                        RecipeSafetyObservation(
                            ingredientId = "ingredient-1",
                            ingredientName = "Harina de trigo",
                            safetyGroupId = "safety-eu-cereals-gluten",
                            safetyGroupName = "Cereales que contienen gluten",
                            relationType = RecipeSafetyRelationType.CONTAINS,
                            evidenceLevel = "EU_LEGAL",
                            sourceId = "EU_FIC_1169_2011",
                            sourceDetails = "Unión Europea · Reglamento (UE) 1169/2011",
                            reviewedAt = "2026-08-09",
                        ),
                    ),
                ),
            ),
            reviewNotices = listOf(
                RecipeReviewNotice(
                    code = "UNKNOWN_COMPOSITION",
                    ingredientId = "ingredient-2",
                    ingredientName = "Salsa preparada",
                    message = "La composición está marcada como desconocida. Requiere revisión.",
                ),
            ),
            regulatoryExemptions = emptyList(),
        )

        setDetail(summary)

        composeRule.onNodeWithTag("recipe_safety_panel").assertIsDisplayed()
        composeRule.onNodeWithText("Cereales que contienen gluten").assertIsDisplayed()
        composeRule.onNodeWithText("Presencia identificada").assertIsDisplayed()
        composeRule.onNodeWithText("Requiere revisión").assertIsDisplayed()
    }

    @Test
    fun emptySummaryUsesNeutralLanguageInsteadOfSafeClaim() {
        setDetail(
            RecipeSafetySummary(
                groups = emptyList(),
                reviewNotices = emptyList(),
                regulatoryExemptions = emptyList(),
            ),
        )

        composeRule.onNodeWithText("No se han detectado coincidencias en los datos registrados.").assertIsDisplayed()
        composeRule.onNodeWithText(
            "La información disponible puede ser incompleta. Comprueba las etiquetas y la información del fabricante cuando corresponda.",
        ).assertIsDisplayed()
    }

    private fun setDetail(summary: RecipeSafetySummary) {
        composeRule.setContent {
            RecetasRaquelTheme {
                RecipeDetailScreen(
                    state = DetailUiState.Content(
                        recipe = Recipe(
                            id = "recipe-1",
                            name = "Receta de prueba",
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
                        ),
                        safetySummary = summary,
                    ),
                    onNavigateBack = {},
                    onToggleFavorite = {},
                )
            }
        }
    }
}
