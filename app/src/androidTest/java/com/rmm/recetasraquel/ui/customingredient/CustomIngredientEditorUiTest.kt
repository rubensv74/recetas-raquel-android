package com.rmm.recetasraquel.ui.customingredient

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyEvidence
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientType
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import org.junit.Rule
import org.junit.Test

class CustomIngredientEditorUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun formRequiresExplicitCompositionAndKeepsSafetyWarningVisible() {
        setScreen(CustomIngredientEditorUiState(isLoading = false))

        composeRule.onNodeWithText("Nuevo ingrediente personalizado").assertIsDisplayed()
        composeRule.onNodeWithTag("composition_known").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("composition_unknown").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Registra solo lo que conozcas. La ausencia de declaraciones no significa ausencia de alérgenos o riesgo. Comprueba siempre la etiqueta del producto cuando corresponda.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun commercialProductShowsBrandAndTradeNameFields() {
        setScreen(
            CustomIngredientEditorUiState(
                type = CustomIngredientType.COMMERCIAL_PRODUCT,
                compositionKnown = false,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("custom_brand").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("custom_trade_name").assertIsDisplayed()
    }

    @Test
    fun safetyDeclarationOffersOnlyUserDeclaredOrUnverifiedEvidence() {
        setScreen(
            CustomIngredientEditorUiState(
                compositionKnown = true,
                isLoading = false,
                safetyRows = listOf(
                    CustomIngredientSafetyRow(
                        key = "row-1",
                        relationType = CustomIngredientSafetyRelationType.UNKNOWN,
                        evidenceLevel = CustomIngredientSafetyEvidence.UNVERIFIED,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Declaración del usuario").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("No verificado").assertIsDisplayed()
    }

    private fun setScreen(state: CustomIngredientEditorUiState) {
        composeRule.setContent {
            RecetasRaquelTheme {
                CustomIngredientEditorScreen(
                    state = state,
                    onNameChange = {},
                    onTypeChange = {},
                    onCategoryChange = {},
                    onDefaultUnitChange = {},
                    onAliasesChange = {},
                    onBrandChange = {},
                    onTradeNameChange = {},
                    onCompositionKnownChange = {},
                    onLabelReadAtChange = {},
                    onNotesChange = {},
                    onAddSafetyRow = {},
                    onRemoveSafetyRow = {},
                    onSafetyGroupChange = { _, _ -> },
                    onSafetyRelationTypeChange = { _, _ -> },
                    onSafetyEvidenceChange = { _, _ -> },
                    onSafetySourceDetailsChange = { _, _ -> },
                    onSafetyNotesChange = { _, _ -> },
                    onSave = {},
                    onRetry = {},
                    onNavigateBack = {},
                )
            }
        }
    }
}
