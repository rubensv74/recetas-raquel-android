package com.rmm.recetasraquel.ui.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import org.junit.Rule
import org.junit.Test

class RecipeEditorIngredientIdentityUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun catalogIngredientShowsLibraryOriginAndUsageFields() {
        setScreen(
            EditorIngredientItem(
                key = "catalog-row",
                name = "Trigo",
                unit = "g",
                catalogIngredientId = "ing-wheat",
            ),
        )

        composeRule.onNodeWithText("Ingrediente de biblioteca").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("ingredient_quantity_catalog-row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Unidad").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Observaciones").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun categoryAndUnitAreSelectedFromDropdowns() {
        setScreen(
            EditorIngredientItem(
                key = "catalog-row",
                name = "Trigo",
                unit = "g",
                catalogIngredientId = "ing-wheat",
            ),
            category = "Postres",
        )

        composeRule.onNodeWithTag("editor_category").performClick()
        composeRule.onNodeWithText("Sin categoría").assertIsDisplayed()
        composeRule.onNodeWithText("Arroces").assertIsDisplayed().performClick()

        composeRule.onNodeWithTag("ingredient_unit_catalog-row").performScrollTo().performClick()
        composeRule.onNodeWithText("Sin unidad").assertIsDisplayed()
        composeRule.onNodeWithText("kg").assertIsDisplayed()
    }

    @Test
    fun categorySelectorExplainsThatMoreOptionsCanBeReachedByScrolling() {
        setScreen(
            EditorIngredientItem(
                key = "catalog-row",
                name = "Trigo",
                unit = "g",
                catalogIngredientId = "ing-wheat",
            ),
        )

        composeRule.onNodeWithText("Desliza la lista para ver todas las categorías")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun customIngredientShowsPersonalizedOrigin() {
        setScreen(
            EditorIngredientItem(
                key = "custom-row",
                name = "Salsa de la casa",
                unit = "g",
                customIngredientId = "custom-1",
            ),
        )

        composeRule.onNodeWithText("Ingrediente personalizado").performScrollTo().assertIsDisplayed()
    }

    private fun setScreen(
        ingredient: EditorIngredientItem,
        category: String = "",
    ) {
        composeRule.setContent {
            RecetasRaquelTheme {
                RecipeEditorScreen(
                    state = RecipeEditorUiState.forCreate().copy(
                        category = category,
                        ingredients = listOf(ingredient),
                    ),
                    onNameChange = {},
                    onCategoryChange = {},
                    onDescriptionChange = {},
                    onServingsChange = {},
                    onPreparationMinutesChange = {},
                    onCookingMinutesChange = {},
                    onNotesChange = {},
                    onAddIngredient = {},
                    onIngredientQuantityChange = { _, _ -> },
                    onIngredientUnitChange = { _, _ -> },
                    onIngredientNameChange = { _, _ -> },
                    onIngredientNotesChange = { _, _ -> },
                    onRemoveIngredient = {},
                    onMoveIngredientUp = {},
                    onMoveIngredientDown = {},
                    onAddStep = {},
                    onStepInstructionChange = { _, _ -> },
                    onStepTimerChange = { _, _ -> },
                    onRemoveStep = {},
                    onMoveStepUp = {},
                    onMoveStepDown = {},
                    onCoverPhotoSelected = {},
                    onRemoveCoverPhoto = {},
                    onStepPhotoSelected = { _, _ -> },
                    onRemoveStepPhoto = {},
                    onSave = {},
                    onNavigateBack = {},
                    onDelete = {},
                    onConfirmDelete = {},
                    onCancelDelete = {},
                    onConfirmDiscard = {},
                    onCancelDiscard = {},
                    onDismissSaveError = {},
                    onDismissPhotoError = {},
                )
            }
        }
    }
}
