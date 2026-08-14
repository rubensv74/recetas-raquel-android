package com.rmm.recetasraquel.domain.usecase

import com.rmm.recetasraquel.domain.ingredient.CatalogIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientDraft
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientRecord
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyEvidence
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientType
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
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository
import com.rmm.recetasraquel.domain.repository.IngredientCatalogImportSummary
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository
import com.rmm.recetasraquel.util.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRecipeSafetySummaryUseCaseTest {
    private val glutenGroup = FoodSafetyGroupOption(
        id = "safety-eu-cereals-gluten",
        code = "EU_GLUTEN_CEREALS",
        displayName = "Cereales que contienen gluten",
        conditionType = "CELIAC_DISEASE",
        regulatoryStatus = "EU14",
        jurisdiction = "EU-ES",
    )

    @Test
    fun `catalog and custom evidence aggregate without losing provenance or exemptions`() = runTestBlocking {
        val catalogId = "ing-wheat-flour"
        val customId = "custom-sauce"
        val exemption = RegulatoryExemption(
            id = "exemption-1",
            ingredientId = catalogId,
            safetyGroupId = glutenGroup.id,
            jurisdiction = "EU-ES",
            effect = RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION,
            conditions = "Condición regulatoria de prueba",
            sourceId = "EU_FIC_1169_2011",
            effectiveFrom = null,
            effectiveTo = null,
            reviewedAt = "2026-08-09",
            notes = null,
        )
        val catalogRepository = FakeCatalogRepository(
            entries = mapOf(catalogId to catalogEntry(catalogId, "Harina de trigo")),
            safety = mapOf(catalogId to listOf(glutenSafetyRecord())),
            exemptions = mapOf(catalogId to listOf(exemption)),
        )
        val customRepository = FakeCustomRepository(
            safetyGroups = listOf(glutenGroup),
            ingredients = mapOf(
                customId to CustomIngredientRecord(
                    id = customId,
                    name = "Salsa preparada",
                    categoryId = null,
                    defaultUnit = null,
                    type = CustomIngredientType.COMMERCIAL_PRODUCT,
                    aliases = emptyList(),
                    brand = null,
                    tradeName = null,
                    compositionKnown = false,
                    labelReadAt = null,
                    notes = null,
                    createdAt = 1,
                    updatedAt = 2,
                    safetyRelations = listOf(
                        CustomIngredientSafetyRecord(
                            id = "custom-rel-1",
                            safetyGroupId = glutenGroup.id,
                            relationType = CustomIngredientSafetyRelationType.DECLARED_MAY_CONTAIN,
                            evidenceLevel = CustomIngredientSafetyEvidence.USER_DECLARED,
                            sourceId = "LOCAL_USER_DECLARED",
                            sourceDetails = "Etiqueta leída por el usuario",
                            notes = null,
                            reviewedAt = "2026-08-09",
                        ),
                    ),
                ),
            ),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(catalogRepository, customRepository)

        val result = useCase.resolve(
            recipe(
                listOf(
                    ingredient("recipe-ing-1", "Harina de trigo", catalogIngredientId = catalogId),
                    ingredient("recipe-ing-2", "Salsa preparada", customIngredientId = customId, sortOrder = 1),
                ),
            ),
        ).getOrThrow()

        assertEquals(1, result.groups.size)
        assertEquals(RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA, result.groups.single().presentationState)
        assertEquals(2, result.groups.single().observations.size)
        assertEquals(setOf("recipe-ing-1", "recipe-ing-2"), result.groups.single().observations.map { it.ingredientId }.toSet())
        assertEquals(listOf(exemption), result.regulatoryExemptions)
        assertTrue(result.reviewNotices.any { it.code == "UNKNOWN_COMPOSITION" && it.ingredientId == "recipe-ing-2" })
    }

    @Test
    fun `required compound component propagates safety as contained without using lineage`() = runTestBlocking {
        val breadId = "ing-wheat-bread"
        val flourId = "ing-wheat-flour"
        val catalogRepository = FakeCatalogRepository(
            entries = mapOf(
                breadId to catalogEntry(breadId, "Pan de trigo"),
                flourId to catalogEntry(flourId, "Harina de trigo"),
            ),
            safety = mapOf(flourId to listOf(glutenSafetyRecord(relationType = "INHERENT_SOURCE"))),
            compositions = mapOf(
                breadId to IngredientComposition(
                    coverage = IngredientCompositionCoverage.PARTIAL,
                    components = listOf(component(breadId, flourId, "Harina de trigo", IngredientComponentPresence.REQUIRED)),
                ),
            ),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(
            catalogRepository,
            FakeCustomRepository(safetyGroups = listOf(glutenGroup)),
        )

        val result = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Pan de trigo", catalogIngredientId = breadId))),
        ).getOrThrow()

        val group = result.groups.single()
        assertEquals(RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA, group.presentationState)
        assertEquals(RecipeSafetyRelationType.CONTAINS, group.observations.single().relationType)
        assertEquals("recipe-ing-1", group.observations.single().ingredientId)
        assertTrue(group.observations.single().sourceDetails.orEmpty().contains("Pan de trigo → Harina de trigo"))
        assertTrue(result.reviewNotices.any { it.code == "PARTIAL_STRUCTURED_COMPOSITION" })
    }

    @Test
    fun `possible compound component produces review evidence but never confirmed presence`() = runTestBlocking {
        val sauceId = "ing-variable-sauce"
        val flourId = "ing-wheat-flour"
        val catalogRepository = FakeCatalogRepository(
            entries = mapOf(
                sauceId to catalogEntry(sauceId, "Salsa variable"),
                flourId to catalogEntry(flourId, "Harina de trigo"),
            ),
            safety = mapOf(flourId to listOf(glutenSafetyRecord(relationType = "CONTAINS"))),
            compositions = mapOf(
                sauceId to IngredientComposition(
                    coverage = IngredientCompositionCoverage.PARTIAL,
                    components = listOf(component(sauceId, flourId, "Harina de trigo", IngredientComponentPresence.POSSIBLE)),
                ),
            ),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(
            catalogRepository,
            FakeCustomRepository(safetyGroups = listOf(glutenGroup)),
        )

        val result = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Salsa variable", catalogIngredientId = sauceId))),
        ).getOrThrow()

        val group = result.groups.single()
        assertEquals(RecipeSafetyPresentationState.REQUIERE_REVISION, group.presentationState)
        assertEquals(RecipeSafetyRelationType.UNKNOWN, group.observations.single().relationType)
        assertTrue(result.reviewNotices.any { it.code == "POSSIBLE_CATALOG_COMPONENT" })
        assertTrue(result.groups.none { summary ->
            summary.observations.any { it.relationType == RecipeSafetyRelationType.CONTAINS }
        })
    }

    @Test
    fun `component regulatory exemption is not propagated to compound recipe ingredient`() = runTestBlocking {
        val compoundId = "compound"
        val childId = "exempt-child"
        val childExemption = regulatoryExemption(
            id = "child-exemption",
            ingredientId = childId,
            jurisdiction = "EU-ES",
            effectiveFrom = null,
            effectiveTo = null,
        )
        val repository = FakeCatalogRepository(
            entries = mapOf(
                compoundId to catalogEntry(compoundId, "Preparado"),
                childId to catalogEntry(childId, "Componente exento"),
            ),
            compositions = mapOf(
                compoundId to IngredientComposition(
                    coverage = IngredientCompositionCoverage.COMPLETE,
                    components = listOf(component(compoundId, childId, "Componente exento", IngredientComponentPresence.REQUIRED)),
                ),
            ),
            exemptions = mapOf(childId to listOf(childExemption)),
        )
        val useCase = BuildRecipeSafetySummaryUseCase(
            repository,
            FakeCustomRepository(safetyGroups = listOf(glutenGroup)),
        )

        val result = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Preparado", catalogIngredientId = compoundId))),
        ).getOrThrow()

        assertTrue(result.regulatoryExemptions.isEmpty())
    }

    @Test
    fun `only exemptions applicable to jurisdiction and date reach recipe summary`() = runTestBlocking {
        val catalogId = "ing-soy-oil-refined"
        val applicable = regulatoryExemption(
            id = "applicable",
            ingredientId = catalogId,
            jurisdiction = "EU-ES",
            effectiveFrom = "2025-04-01",
            effectiveTo = null,
        )
        val foreign = regulatoryExemption(
            id = "foreign",
            ingredientId = catalogId,
            jurisdiction = "US",
            effectiveFrom = null,
            effectiveTo = null,
        )
        val future = regulatoryExemption(
            id = "future",
            ingredientId = catalogId,
            jurisdiction = "EU-ES",
            effectiveFrom = "2027-01-01",
            effectiveTo = null,
        )
        val expired = regulatoryExemption(
            id = "expired",
            ingredientId = catalogId,
            jurisdiction = "EU-ES",
            effectiveFrom = null,
            effectiveTo = "2025-12-31",
        )
        val useCase = BuildRecipeSafetySummaryUseCase(
            catalogRepository = FakeCatalogRepository(
                entries = mapOf(catalogId to catalogEntry(catalogId, "Aceite de soja totalmente refinado")),
                exemptions = mapOf(catalogId to listOf(applicable, foreign, future, expired)),
            ),
            customIngredientRepository = FakeCustomRepository(safetyGroups = listOf(glutenGroup)),
            timeProvider = TimeProvider { FIXED_2026_08_10_NOON_UTC },
            regulatoryJurisdiction = "EU-ES",
            regulatoryTimeZoneId = "Europe/Madrid",
        )

        val result = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Aceite de soja totalmente refinado", catalogIngredientId = catalogId))),
        ).getOrThrow()

        assertEquals(listOf(applicable), result.regulatoryExemptions)
        assertTrue(result.groups.isEmpty())
    }

    @Test
    fun `originless recipe ingredient becomes global review notice instead of inferred safety`() = runTestBlocking {
        val useCase = BuildRecipeSafetySummaryUseCase(
            FakeCatalogRepository(),
            FakeCustomRepository(safetyGroups = listOf(glutenGroup)),
        )

        val result = useCase.resolve(recipe(listOf(ingredient("legacy-1", "Ingrediente libre")))).getOrThrow()

        assertTrue(result.groups.isEmpty())
        assertTrue(result.regulatoryExemptions.isEmpty())
        assertEquals("UNRESOLVED_INGREDIENT_IDENTITY", result.reviewNotices.single().code)
    }

    @Test
    fun `missing referenced catalog identity requires review`() = runTestBlocking {
        val useCase = BuildRecipeSafetySummaryUseCase(
            FakeCatalogRepository(),
            FakeCustomRepository(safetyGroups = listOf(glutenGroup)),
        )

        val result = useCase.resolve(
            recipe(listOf(ingredient("recipe-ing-1", "Ingrediente retirado", catalogIngredientId = "missing"))),
        ).getOrThrow()

        assertTrue(result.groups.isEmpty())
        assertEquals("CATALOG_INGREDIENT_NOT_AVAILABLE", result.reviewNotices.single().code)
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

    private fun glutenSafetyRecord(relationType: String = "CONTAINS") = CatalogIngredientSafetyRecord(
        safetyGroupId = glutenGroup.id,
        safetyGroupName = glutenGroup.displayName,
        jurisdiction = glutenGroup.jurisdiction,
        relationType = relationType,
        evidenceLevel = "EU_LEGAL",
        sourceId = "EU_FIC_1169_2011",
        sourceDetails = "Unión Europea · Reglamento (UE) 1169/2011",
        notes = null,
        reviewedAt = "2026-08-09",
    )

    private fun component(
        parentId: String,
        childId: String,
        childName: String,
        presence: IngredientComponentPresence,
    ) = IngredientComponent(
        id = "component-$parentId-$childId",
        parentIngredientId = parentId,
        componentIngredientId = childId,
        componentName = childName,
        presence = presence,
        reviewedAt = "2026-08-12",
        sourceReference = "TEST",
        notes = null,
    )

    private fun regulatoryExemption(
        id: String,
        ingredientId: String,
        jurisdiction: String,
        effectiveFrom: String?,
        effectiveTo: String?,
    ) = RegulatoryExemption(
        id = id,
        ingredientId = ingredientId,
        safetyGroupId = glutenGroup.id,
        jurisdiction = jurisdiction,
        effect = RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION,
        conditions = "Condición regulatoria de prueba",
        sourceId = "TEST_SOURCE",
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        reviewedAt = "2026-08-09",
        notes = null,
    )

    companion object {
        private const val FIXED_2026_08_10_NOON_UTC = 1_786_363_200_000L
    }
}

private class FakeCatalogRepository(
    private val entries: Map<String, IngredientCatalogEntry> = emptyMap(),
    private val safety: Map<String, List<CatalogIngredientSafetyRecord>> = emptyMap(),
    private val exemptions: Map<String, List<RegulatoryExemption>> = emptyMap(),
    private val compositions: Map<String, IngredientComposition> = emptyMap(),
) : IngredientCatalogRepository {
    override suspend fun ensureCatalogImported(): Result<IngredientCatalogImportSummary> = Result.success(
        IngredientCatalogImportSummary(13, IngredientCatalogImportSummary.Status.ALREADY_CURRENT),
    )

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> = Result.success(emptyList())

    override suspend fun searchIngredients(
        query: String,
        categoryId: String?,
        limit: Int,
    ): Result<List<IngredientCatalogEntry>> = Result.success(emptyList())

    override suspend fun getIngredient(ingredientId: String): Result<IngredientCatalogEntry?> =
        Result.success(entries[ingredientId])

    override suspend fun getComposition(ingredientId: String): Result<IngredientComposition> =
        Result.success(compositions[ingredientId] ?: IngredientComposition())

    override suspend fun getParentRelations(ingredientId: String): Result<List<IngredientLineageRelation>> =
        Result.success(emptyList())

    override suspend fun getChildRelations(ingredientId: String): Result<List<IngredientLineageRelation>> =
        Result.success(emptyList())

    override suspend fun getSafetyRelations(ingredientId: String): Result<List<CatalogIngredientSafetyRecord>> =
        Result.success(safety[ingredientId].orEmpty())

    override suspend fun getRegulatoryExemptions(ingredientId: String): Result<List<RegulatoryExemption>> =
        Result.success(exemptions[ingredientId].orEmpty())
}

private class FakeCustomRepository(
    private val safetyGroups: List<FoodSafetyGroupOption> = emptyList(),
    private val ingredients: Map<String, CustomIngredientRecord> = emptyMap(),
) : CustomIngredientRepository {
    override suspend fun createIngredient(input: CustomIngredientDraft): Result<String> =
        Result.failure(UnsupportedOperationException())

    override suspend fun updateIngredient(ingredientId: String, input: CustomIngredientDraft): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getIngredient(ingredientId: String): Result<CustomIngredientRecord?> =
        Result.success(ingredients[ingredientId])

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> = Result.success(emptyList())

    override suspend fun getSafetyGroups(): Result<List<FoodSafetyGroupOption>> = Result.success(safetyGroups)
}

private fun <T> runTestBlocking(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
