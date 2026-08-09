package com.rmm.recetasraquel.ui.ingredientlibrary

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogInformationStatus
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IngredientLibraryUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun idleStateInvitesSearchAndKeepsManualPathVisible() {
        setScreen(IngredientLibraryUiState(categories = sampleCategories(), isLoading = false))

        composeRule.onNodeWithText("Busca un ingrediente o elige una categoría.").assertIsDisplayed()
        composeRule.onNodeWithTag("ingredient_library_manual").assertIsDisplayed()
    }

    @Test
    fun safetyInformationIsDescriptiveAndSelectionIsExplicit() {
        var selectedId: String? = null
        val ingredient = entry(
            id = "ing-wheat",
            name = "Trigo",
            status = IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED,
        )
        setScreen(
            state = activeState(ingredient),
            onSelectIngredient = { selectedId = it.id },
        )

        composeRule.onNodeWithText("Información de seguridad registrada").assertIsDisplayed()
        composeRule.onNodeWithTag("ingredient_result_ing-wheat").performClick()
        composeRule.runOnIdle { assertEquals("ing-wheat", selectedId) }
    }

    @Test
    fun regulatoryExemptionUsesRegulatoryWordingOnly() {
        setScreen(
            activeState(
                entry(
                    id = "ing-wheat-syrup",
                    name = "Jarabe de glucosa a base de trigo",
                    status = IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED,
                ),
            ),
        )

        composeRule.onNodeWithText("Información regulatoria específica").assertIsDisplayed()
    }

    @Test
    fun missingDirectSafetyRelationWarnsThatInformationMayBeIncomplete() {
        setScreen(
            activeState(
                entry(
                    id = "ing-potato",
                    name = "Patata",
                    status = IngredientCatalogInformationStatus.NO_DIRECT_SAFETY_RELATION_RECORDED,
                ),
            ),
        )

        composeRule.onNodeWithText("La información disponible puede ser incompleta").assertIsDisplayed()
    }

    @Test
    fun categoryAndManualActionsRemainSeparate() {
        var selectedCategory: String? = null
        var manualRequested = false
        setScreen(
            state = IngredientLibraryUiState(categories = sampleCategories(), isLoading = false),
            onSelectCategory = { selectedCategory = it },
            onAddManualIngredient = { manualRequested = true },
        )

        composeRule.onNodeWithTag("ingredient_category_cat-cereals-flours").performClick()
        composeRule.runOnIdle { assertEquals("cat-cereals-flours", selectedCategory) }

        composeRule.onNodeWithTag("ingredient_library_manual").performClick()
        composeRule.runOnIdle { assertTrue(manualRequested) }
    }

    private fun setScreen(
        state: IngredientLibraryUiState,
        onSelectCategory: (String?) -> Unit = {},
        onSelectIngredient: (IngredientCatalogEntry) -> Unit = {},
        onAddManualIngredient: () -> Unit = {},
    ) {
        composeRule.setContent {
            RecetasRaquelTheme {
                IngredientLibraryScreen(
                    state = state,
                    onQueryChange = {},
                    onSelectCategory = onSelectCategory,
                    onClearFilters = {},
                    onSelectIngredient = onSelectIngredient,
                    onAddManualIngredient = onAddManualIngredient,
                    onNavigateBack = {},
                    onRetry = {},
                )
            }
        }
    }

    private fun activeState(ingredient: IngredientCatalogEntry) = IngredientLibraryUiState(
        query = ingredient.canonicalName,
        categories = sampleCategories(),
        results = listOf(ingredient),
        isLoading = false,
    )

    private fun sampleCategories() = listOf(
        IngredientCatalogCategory(
            id = "cat-cereals-flours",
            code = "CEREALS_FLOURS",
            name = "Cereales y harinas",
            sortOrder = 1,
            iconKey = null,
        ),
    )

    private fun entry(
        id: String,
        name: String,
        status: IngredientCatalogInformationStatus,
    ) = IngredientCatalogEntry(
        id = id,
        canonicalName = name,
        categoryId = "cat-cereals-flours",
        categoryName = "Cereales y harinas",
        defaultUnit = "g",
        verificationStatus = "VERIFIED",
        informationStatus = status,
    )
}
