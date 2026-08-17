package com.rmm.recetasraquel.domain.usecase

import com.rmm.recetasraquel.domain.ingredient.CatalogIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientDraft
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientRecord
import com.rmm.recetasraquel.domain.ingredient.FoodSafetyGroupOption
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogInformationStatus
import com.rmm.recetasraquel.domain.ingredient.IngredientComponent
import com.rmm.recetasraquel.domain.ingredient.IngredientComponentPresence
import com.rmm.recetasraquel.domain.ingredient.IngredientComposition
import com.rmm.recetasraquel.domain.ingredient.IngredientCompositionCoverage
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageRelation
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyPresentationState
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository
import com.rmm.recetasraquel.domain.repository.IngredientCatalogImportSummary
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRecipeSafetySummaryResilienceTest {

    @Test
    fun `catalog only recipe does not depend on custom safety group metadata`() = runBlocking {
        val catalogId = "catalog-1"
        val catalog = ResilientFakeCatalogRepository(
            entries = mapOf(catalogId to catalogEntry(catalogId, "Ingrediente")),
        )
        val custom = ResilientFakeCustomRepository(
            safetyGroupsResult = Result.failure(IllegalStateException("custom metadata unavailable")),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(catalog, custom)

        val result = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Ingrediente", catalogIngredientId = catalogId))),
        )

        assertTrue(result.isSuccess)
        assertEquals(0, custom.safetyGroupCalls)
    }

    @Test
    fun `composition failure preserves direct safety evidence and requires review`() = runBlocking {
        val catalogId = "catalog-1"
        val catalog = ResilientFakeCatalogRepository(
            entries = mapOf(catalogId to catalogEntry(catalogId, "Ingrediente")),
            safety = mapOf(catalogId to listOf(safetyRecord())),
            compositionFailures = setOf(catalogId),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(catalog, ResilientFakeCustomRepository())

        val summary = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Ingrediente", catalogIngredientId = catalogId))),
        ).getOrThrow()

        assertEquals(1, summary.groups.size)
        assertEquals(RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA, summary.groups.single().presentationState)
        assertTrue(summary.reviewNotices.any { it.code == "STRUCTURED_COMPOSITION_UNAVAILABLE" })
    }

    @Test
    fun `regulatory lookup failure preserves safety evidence`() = runBlocking {
        val catalogId = "catalog-1"
        val catalog = ResilientFakeCatalogRepository(
            entries = mapOf(catalogId to catalogEntry(catalogId, "Ingrediente")),
            safety = mapOf(catalogId to listOf(safetyRecord())),
            regulatoryFailures = setOf(catalogId),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(catalog, ResilientFakeCustomRepository())

        val summary = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Ingrediente", catalogIngredientId = catalogId))),
        ).getOrThrow()

        assertEquals(1, summary.groups.size)
        assertTrue(summary.reviewNotices.any { it.code == "REGULATORY_EXEMPTIONS_UNAVAILABLE" })
    }

    @Test
    fun `failure in one ingredient does not discard evidence from another ingredient`() = runBlocking {
        val badId = "catalog-bad"
        val goodId = "catalog-good"
        val catalog = ResilientFakeCatalogRepository(
            entries = mapOf(goodId to catalogEntry(goodId, "Ingrediente verificado")),
            ingredientLookupFailures = setOf(badId),
            safety = mapOf(goodId to listOf(safetyRecord())),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(catalog, ResilientFakeCustomRepository())

        val summary = useCase.resolve(
            recipe(
                listOf(
                    ingredient("recipe-ing-1", "Ingrediente con fallo", catalogIngredientId = badId),
                    ingredient("recipe-ing-2", "Ingrediente verificado", catalogIngredientId = goodId, sortOrder = 1),
                ),
            ),
        ).getOrThrow()

        assertEquals(1, summary.groups.size)
        assertEquals(
            setOf("recipe-ing-2"),
            summary.groups.single().observations.map { it.ingredientId }.toSet(),
        )
        assertTrue(summary.reviewNotices.any {
            it.code == "CATALOG_INGREDIENT_LOOKUP_FAILED" && it.ingredientId == "recipe-ing-1"
        })
    }

    @Test
    fun `direct safety lookup failure still allows structured component evidence`() = runBlocking {
        val parentId = "compound"
        val childId = "child"
        val catalog = ResilientFakeCatalogRepository(
            entries = mapOf(
                parentId to catalogEntry(parentId, "Preparado"),
                childId to catalogEntry(childId, "Componente"),
            ),
            safety = mapOf(childId to listOf(safetyRecord())),
            safetyFailures = setOf(parentId),
            compositions = mapOf(
                parentId to IngredientComposition(
                    coverage = IngredientCompositionCoverage.COMPLETE,
                    components = listOf(component(parentId, childId)),
                ),
            ),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(catalog, ResilientFakeCustomRepository())

        val summary = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Preparado", catalogIngredientId = parentId))),
        ).getOrThrow()

        assertEquals(1, summary.groups.size)
        assertEquals(RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA, summary.groups.single().presentationState)
        assertTrue(summary.reviewNotices.any { it.code == "CATALOG_SAFETY_RELATIONS_UNAVAILABLE" })
    }

    @Test
    fun `catalog import failure remains a global failure`() = runBlocking {
        val catalog = ResilientFakeCatalogRepository(
            importResult = Result.failure(IllegalStateException("catalog unavailable")),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(catalog, ResilientFakeCustomRepository())

        val result = useCase.resolve(recipe(emptyList()))

        assertTrue(result.isFailure)
    }

    private fun recipe(ingredients: List<Ingredient>) = Recipe(
        id = "recipe-1",
        name = "Receta",
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
    )

    private fun ingredient(
        id: String,
        name: String,
        catalogIngredientId: String? = null,
        customIngredientId: String? = null,
        sortOrder: Int = 0,
    ) = Ingredient(
        id = id,
        recipeId = "recipe-1",
        quantity = null,
        unit = null,
        name = name,
        notes = null,
        sortOrder = sortOrder,
        catalogIngredientId = catalogIngredientId,
        customIngredientId = customIngredientId,
    )

    private fun catalogEntry(id: String, name: String) = IngredientCatalogEntry(
        id = id,
        canonicalName = name,
        categoryId = "cat",
        categoryName = "Categoría",
        defaultUnit = null,
        verificationStatus = "REVIEWED",
        informationStatus = IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED,
    )

    private fun safetyRecord() = CatalogIngredientSafetyRecord(
        safetyGroupId = "safety-eu-nuts",
        safetyGroupName = "Frutos de cáscara",
        jurisdiction = "EU-ES",
        relationType = "INHERENT_SOURCE",
        evidenceLevel = "EU_LEGAL",
        sourceId = "TEST_SOURCE",
        sourceDetails = "Fuente de prueba",
        notes = null,
        reviewedAt = "2026-08-16",
    )

    private fun component(parentId: String, childId: String) = IngredientComponent(
        id = "component-$parentId-$childId",
        parentIngredientId = parentId,
        componentIngredientId = childId,
        componentName = "Componente",
        presence = IngredientComponentPresence.REQUIRED,
        reviewedAt = "2026-08-16",
        sourceReference = "TEST",
        notes = null,
    )
}

private class ResilientFakeCatalogRepository(
    private val importResult: Result<IngredientCatalogImportSummary> = Result.success(
        IngredientCatalogImportSummary(
            catalogVersion = 13,
            status = IngredientCatalogImportSummary.Status.ALREADY_CURRENT,
        ),
    ),
    private val entries: Map<String, IngredientCatalogEntry> = emptyMap(),
    private val safety: Map<String, List<CatalogIngredientSafetyRecord>> = emptyMap(),
    private val compositions: Map<String, IngredientComposition> = emptyMap(),
    private val ingredientLookupFailures: Set<String> = emptySet(),
    private val safetyFailures: Set<String> = emptySet(),
    private val compositionFailures: Set<String> = emptySet(),
    private val regulatoryFailures: Set<String> = emptySet(),
) : IngredientCatalogRepository {

    override suspend fun ensureCatalogImported(): Result<IngredientCatalogImportSummary> = importResult

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> =
        Result.success(emptyList())

    override suspend fun searchIngredients(
        query: String,
        categoryId: String?,
        limit: Int,
    ): Result<List<IngredientCatalogEntry>> = Result.success(emptyList())

    override suspend fun getIngredient(ingredientId: String): Result<IngredientCatalogEntry?> =
        if (ingredientId in ingredientLookupFailures) {
            Result.failure(IllegalStateException("ingredient lookup failed"))
        } else {
            Result.success(entries[ingredientId])
        }

    override suspend fun getComposition(ingredientId: String): Result<IngredientComposition> =
        if (ingredientId in compositionFailures) {
            Result.failure(IllegalStateException("composition lookup failed"))
        } else {
            Result.success(compositions[ingredientId] ?: IngredientComposition())
        }

    override suspend fun getParentRelations(
        ingredientId: String,
    ): Result<List<IngredientLineageRelation>> = Result.success(emptyList())

    override suspend fun getChildRelations(
        ingredientId: String,
    ): Result<List<IngredientLineageRelation>> = Result.success(emptyList())

    override suspend fun getSafetyRelations(
        ingredientId: String,
    ): Result<List<CatalogIngredientSafetyRecord>> =
        if (ingredientId in safetyFailures) {
            Result.failure(IllegalStateException("safety lookup failed"))
        } else {
            Result.success(safety[ingredientId].orEmpty())
        }

    override suspend fun getRegulatoryExemptions(
        ingredientId: String,
    ): Result<List<RegulatoryExemption>> = Result.success(emptyList())

    override suspend fun getApplicableRegulatoryExemptions(
        ingredientId: String,
        jurisdiction: String,
        asOfDate: String,
    ): Result<List<RegulatoryExemption>> =
        if (ingredientId in regulatoryFailures) {
            Result.failure(IllegalStateException("regulatory lookup failed"))
        } else {
            Result.success(emptyList())
        }
}

private class ResilientFakeCustomRepository(
    private val safetyGroupsResult: Result<List<FoodSafetyGroupOption>> = Result.success(emptyList()),
) : CustomIngredientRepository {

    var safetyGroupCalls: Int = 0
        private set

    override suspend fun createIngredient(input: CustomIngredientDraft): Result<String> =
        Result.failure(UnsupportedOperationException("Not used in this test"))

    override suspend fun updateIngredient(
        ingredientId: String,
        input: CustomIngredientDraft,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Not used in this test"))

    override suspend fun getIngredient(ingredientId: String): Result<CustomIngredientRecord?> =
        Result.success(null)

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> =
        Result.success(emptyList())

    override suspend fun getSafetyGroups(): Result<List<FoodSafetyGroupOption>> {
        safetyGroupCalls += 1
        return safetyGroupsResult
    }
}
