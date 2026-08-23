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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode

class CustomIngredientEditorUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun formRequiresExplicitCompositionAndKeepsSafetyWarningVisible() {
        setScreen(CustomIngredientEditorUiState(isLoading = false))

        composeRule.onNodeWithText("Nuevo ingrediente personalizado")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("custom_ingredient_form")
            .performScrollToNode(hasTestTag("composition_known"))

        composeRule.onNodeWithTag("composition_known")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("composition_unknown")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("custom_ingredient_form")
            .performScrollToNode(hasTestTag("custom_safety_warning"))

        composeRule.onNodeWithTag("custom_safety_warning")
            .assertIsDisplayed()

        composeRule.onNodeWithText(
            "Registra solo lo que conozcas.",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun commercialProductShowsOnlyItsAdditionalMetadataFields() {
        setScreen(
            CustomIngredientEditorUiState(
                type = CustomIngredientType.COMMERCIAL_PRODUCT,
                compositionKnown = false,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag("custom_ingredient_form")
            .performScrollToNode(hasTestTag("custom_brand"))
        composeRule.onNodeWithTag("custom_brand").assertIsDisplayed()

        composeRule.onNodeWithTag("custom_ingredient_form")
            .performScrollToNode(hasTestTag("custom_trade_name"))
        composeRule.onNodeWithTag("custom_trade_name").assertIsDisplayed()

        composeRule.onNodeWithTag("custom_ingredient_form")
            .performScrollToNode(hasTestTag("custom_label_read_at"))
        composeRule.onNodeWithTag("custom_label_read_at").assertIsDisplayed()
    }

    @Test
    fun simpleIngredientHidesCommercialOnlyMetadataFields() {
        setScreen(
            CustomIngredientEditorUiState(
                type = CustomIngredientType.SIMPLE,
                brand = "Marca residual",
                tradeName = "Nombre residual",
                labelReadAt = "2026-08-09",
                compositionKnown = true,
                isLoading = false,
            ),
        )

        assertTagDoesNotExist("custom_brand")
        assertTagDoesNotExist("custom_trade_name")
        assertTagDoesNotExist("custom_label_read_at")
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

        // Fuerza a la LazyColumn a componer USER_DECLARED.
        composeRule.onNodeWithTag("custom_ingredient_form")
            .performScrollToNode(
                hasTestTag("custom_safety_evidence_row-1_USER_DECLARED"),
            )

        composeRule.onNodeWithTag(
            "custom_safety_evidence_row-1_USER_DECLARED",
        ).fetchSemanticsNode()

        // Fuerza a la LazyColumn a componer UNVERIFIED.
        composeRule.onNodeWithTag("custom_ingredient_form")
            .performScrollToNode(
                hasTestTag("custom_safety_evidence_row-1_UNVERIFIED"),
            )

        composeRule.onNodeWithTag(
            "custom_safety_evidence_row-1_UNVERIFIED",
        ).fetchSemanticsNode()
    }

    private fun assertTagDoesNotExist(tag: String) {
        val lookup = runCatching {
            composeRule.onNodeWithTag(tag).fetchSemanticsNode()
        }
        assertTrue("Expected no semantics node with tag '$tag'", lookup.isFailure)
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
