package com.rmm.recetasraquel.ui.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.rmm.recetasraquel.domain.ingredient.RecipeReviewNotice
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyGroupSummary
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyObservation
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyPresentationState
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.model.Ingredient
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

    @Test
    fun regulatoryExemptionUsesSeparateLabelingPanelWithoutSafeClaim() {
        val catalogIngredientId = "ing-soy-oil-fully-refined"
        val summary = RecipeSafetySummary(
            groups = emptyList(),
            reviewNotices = emptyList(),
            regulatoryExemptions = listOf(
                RegulatoryExemption(
                    id = "rex-soy-oil",
                    ingredientId = catalogIngredientId,
                    safetyGroupId = "sg-eu-soybeans",
                    jurisdiction = "EU-ES",
                    effect = RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION,
                    conditions = "Exclusivamente aceite y grasa de semilla de soja totalmente refinados.",
                    sourceId = "EU_FIC_1169_2011",
                    effectiveFrom = null,
                    effectiveTo = null,
                    reviewedAt = "2026-08-10",
                    notes = null,
                ),
            ),
        )
        val ingredients = listOf(
            Ingredient(
                id = "recipe-ing-1",
                recipeId = "recipe-1",
                quantity = null,
                unit = null,
                name = "Aceite de soja totalmente refinado",
                notes = null,
                sortOrder = 0,
                catalogIngredientId = catalogIngredientId,
                customIngredientId = null,
            ),
        )

        setDetail(summary, ingredients)

        composeRule.onNodeWithTag("recipe_regulatory_panel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Información regulatoria de etiquetado").assertIsDisplayed()
        composeRule.onNodeWithText("Aceite de soja totalmente refinado").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Condiciones: Exclusivamente aceite y grasa de semilla de soja totalmente refinados.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Fuente: Unión Europea · Reglamento (UE) n.º 1169/2011 · Anexo II",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Esta información se refiere a obligaciones de etiquetado. No significa que el alérgeno esté ausente, que no exista riesgo ni que el alimento sea apto para una persona alérgica o intolerante.",
        ).assertIsDisplayed()
    }

    private fun setDetail(
        summary: RecipeSafetySummary,
        ingredients: List<Ingredient> = emptyList(),
    ) {
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
                            ingredients = ingredients,
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
