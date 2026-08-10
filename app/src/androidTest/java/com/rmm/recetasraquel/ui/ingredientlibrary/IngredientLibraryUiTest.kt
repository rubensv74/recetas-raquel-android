package com.rmm.recetasraquel.ui.ingredientlibrary

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rmm.recetasraquel.domain.ingredient.CatalogIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.FrequentIngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogDetail
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogInformationStatus
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogRelatedPresentation
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogRelationDirection
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageType
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun frequentStateUsesExistingRecipeHistoryAndKeepsSelectionExplicit() {
        var selectedId: String? = null
        val wheat = entry(
            id = "ing-wheat",
            name = "Trigo",
            status = IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED,
        )
        setScreen(
            state = IngredientLibraryUiState(
                categories = sampleCategories(),
                frequentIngredients = listOf(
                    FrequentIngredientCatalogEntry(
                        ingredient = wheat,
                        recipeCount = 3,
                    ),
                ),
                isLoading = false,
            ),
            onSelectIngredient = { selectedId = it.id },
        )

        composeRule.onNodeWithTag("ingredient_library_frequent").assertIsDisplayed()
        composeRule.onNodeWithText("Frecuentes").assertIsDisplayed()
        composeRule.onNodeWithText("Usado en 3 recetas").assertIsDisplayed()
        composeRule.onNodeWithTag("ingredient_result_ing-wheat").performClick()
        composeRule.runOnIdle { assertEquals("ing-wheat", selectedId) }
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
    fun informationActionDoesNotSelectIngredient() {
        var selectedId: String? = null
        var infoId: String? = null
        val ingredient = entry(
            id = "ing-wheat",
            name = "Trigo",
            status = IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED,
        )
        setScreen(
            state = activeState(ingredient),
            onSelectIngredient = { selectedId = it.id },
            onShowIngredientInfo = { infoId = it.id },
        )

        composeRule.onNodeWithTag("ingredient_info_ing-wheat").performClick()
        composeRule.runOnIdle {
            assertEquals("ing-wheat", infoId)
            assertNull(selectedId)
        }
    }

    @Test
    fun identityDetailShowsDescriptionAliasesAndRelatedCulinaryPresentations() {
        val ingredient = entry(
            id = "ing-wheat-flour",
            name = "Harina de trigo",
            status = IngredientCatalogInformationStatus.NO_DIRECT_SAFETY_RELATION_RECORDED,
        )
        setScreen(
            activeState(ingredient).copy(
                ingredientInfo = IngredientLibraryInfoUiState(
                    ingredient = ingredient,
                    catalogDetail = IngredientCatalogDetail(
                        description = "Harina obtenida a partir de trigo.",
                        aliases = listOf("Harina trigo"),
                        relatedPresentations = listOf(
                            IngredientCatalogRelatedPresentation(
                                ingredientId = "ing-wheat",
                                canonicalName = "Trigo",
                                relationType = IngredientLineageType.DERIVED_FROM,
                                direction = IngredientCatalogRelationDirection.PARENT,
                            ),
                        ),
                    ),
                    isLoading = false,
                ),
            ),
        )

        composeRule.onNodeWithTag("ingredient_library_description").fetchSemanticsNode()
        composeRule.onNodeWithText("Harina obtenida a partir de trigo.").fetchSemanticsNode()
        composeRule.onNodeWithTag("ingredient_library_aliases").fetchSemanticsNode()
        composeRule.onNodeWithText("Harina trigo").fetchSemanticsNode()
        composeRule.onNodeWithText("Presentaciones relacionadas").fetchSemanticsNode()
        composeRule.onNodeWithText("Derivado de: Trigo").fetchSemanticsNode()
        composeRule.onNodeWithTag("ingredient_library_lineage_disclaimer").fetchSemanticsNode()
    }

    @Test
    fun safetyDetailShowsGroupRelationEvidenceJurisdictionSourceAndReview() {
        val ingredient = entry(
            id = "ing-wheat",
            name = "Trigo",
            status = IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED,
        )
        setScreen(
            activeState(ingredient).copy(
                ingredientInfo = IngredientLibraryInfoUiState(
                    ingredient = ingredient,
                    safetyRelations = listOf(
                        CatalogIngredientSafetyRecord(
                            safetyGroupId = "sg-eu-cereals-gluten",
                            safetyGroupName = "Cereales que contienen gluten",
                            jurisdiction = "EU-ES",
                            relationType = "INHERENT_SOURCE",
                            evidenceLevel = "EU_LEGAL",
                            sourceId = "EU_FIC_1169_2011",
                            sourceDetails = "Unión Europea · Reglamento (UE) n.º 1169/2011 · Anexo II",
                            notes = "Relación directa documentada.",
                            reviewedAt = "2026-08-08",
                        ),
                    ),
                    isLoading = false,
                ),
            ),
        )

        composeRule.onNodeWithTag("ingredient_library_info_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Información de seguridad alimentaria").fetchSemanticsNode()
        composeRule.onNodeWithText("Cereales que contienen gluten").fetchSemanticsNode()
        composeRule.onNodeWithText("Relación: Fuente inherente del grupo").fetchSemanticsNode()
        composeRule.onNodeWithText("Evidencia: Fuente jurídica de la Unión Europea").fetchSemanticsNode()
        composeRule.onNodeWithText("Ámbito: Unión Europea / España").fetchSemanticsNode()
        composeRule.onNodeWithText("Fuente: Unión Europea · Reglamento (UE) n.º 1169/2011 · Anexo II").fetchSemanticsNode()
        composeRule.onNodeWithText("Revisado: 2026-08-08").fetchSemanticsNode()
    }

    @Test
    fun regulatoryDetailKeepsNoSafetyEvidenceAndLegalExemptionSeparate() {
        val ingredient = entry(
            id = "ing-wheat-glucose-syrup",
            name = "Jarabe de glucosa a base de trigo",
            status = IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED,
        )
        setScreen(
            activeState(ingredient).copy(
                ingredientInfo = IngredientLibraryInfoUiState(
                    ingredient = ingredient,
                    regulatoryExemptions = listOf(
                        RegulatoryExemption(
                            id = "reg-wheat-glucose",
                            ingredientId = ingredient.id,
                            safetyGroupId = "sg-eu-cereals-gluten",
                            jurisdiction = "EU-ES",
                            effect = RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION,
                            conditions = "Solo bajo las condiciones concretas definidas por el Anexo II.",
                            sourceId = "EU_FIC_1169_2011",
                            effectiveFrom = null,
                            effectiveTo = null,
                            reviewedAt = "2026-08-10",
                            notes = "No implica ausencia de alérgeno.",
                        ),
                    ),
                    isLoading = false,
                ),
            ),
        )

        composeRule.onNodeWithTag("ingredient_library_no_direct_safety").fetchSemanticsNode()
        composeRule.onNodeWithText("Información regulatoria de etiquetado").fetchSemanticsNode()
        composeRule.onNodeWithText("Condiciones: Solo bajo las condiciones concretas definidas por el Anexo II.").fetchSemanticsNode()
        composeRule.onNodeWithTag("ingredient_library_regulatory_disclaimer").fetchSemanticsNode()
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
        onShowIngredientInfo: (IngredientCatalogEntry) -> Unit = {},
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
                    onShowIngredientInfo = onShowIngredientInfo,
                    onDismissIngredientInfo = {},
                    onRetryIngredientInfo = {},
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
