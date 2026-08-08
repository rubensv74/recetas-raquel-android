package com.rmm.recetasraquel.data.catalog

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngredientCatalogImportTest {
    private lateinit var context: Context
    private lateinit var database: RecipeDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importsVersionedDerivativeCatalogV6AndIsIdempotent() = runBlocking {
        val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
        val bundle = reader.read()

        assertEquals(3, bundle.manifest.schemaVersion)
        assertEquals(6, bundle.manifest.catalogVersion)
        assertEquals("DRAFT", bundle.manifest.releaseStatus)
        assertEquals(20, bundle.categories.size)
        assertEquals(252, bundle.ingredients.size)
        assertEquals(245, bundle.aliases.size)
        assertEquals(25, bundle.ingredientRelations.size)
        assertEquals(14, bundle.safetyGroups.size)
        assertEquals(3, bundle.safetySources.size)
        assertEquals(33, bundle.safetyRelations.size)
        assertEquals(0, bundle.regulatoryExemptions.size)

        val expectedEuCodes = setOf(
            "CEREALS_CONTAINING_GLUTEN",
            "CRUSTACEANS",
            "EGGS",
            "FISH",
            "PEANUTS",
            "SOYBEANS",
            "MILK",
            "NUTS",
            "CELERY",
            "MUSTARD",
            "SESAME",
            "SULPHUR_DIOXIDE_AND_SULPHITES",
            "LUPIN",
            "MOLLUSCS",
        )
        assertEquals(expectedEuCodes, bundle.safetyGroups.map { it.code }.toSet())

        val sulphiteRelation = bundle.safetyRelations.single { it.ingredientId == "ing-sulphites" }
        assertEquals("REGULATED_COMPONENT", sulphiteRelation.relationType)
        assertEquals("EU_LEGAL", sulphiteRelation.evidenceLevel)
        assertTrue(sulphiteRelation.notes.orEmpty().contains("10 mg/kg"))

        listOf("ing-tomato", "ing-buckwheat", "ing-chicken", "ing-pine-nut", "ing-button-mushroom").forEach { ingredientId ->
            val ingredient = bundle.ingredients.single { it.id == ingredientId }
            assertEquals("REVIEW_REQUIRED", ingredient.verificationStatus)
            assertTrue(bundle.safetyRelations.none { it.ingredientId == ingredientId })
        }

        val chickenBreast = bundle.ingredients.single { it.id == "ing-chicken-breast" }
        assertEquals("REVIEW_REQUIRED", chickenBreast.verificationStatus)
        val chickenBreastLineage = bundle.ingredientRelations.single { it.childIngredientId == "ing-chicken-breast" }
        assertEquals("ing-chicken", chickenBreastLineage.parentIngredientId)
        assertEquals("CUT_OF", chickenBreastLineage.relationType)
        assertTrue(bundle.safetyRelations.none { it.ingredientId == "ing-chicken-breast" })

        val wheatFlour = bundle.ingredients.single { it.id == "ing-wheat-flour" }
        assertEquals("REVIEW_REQUIRED", wheatFlour.verificationStatus)
        val wheatFlourLineage = bundle.ingredientRelations.single { it.childIngredientId == "ing-wheat-flour" }
        assertEquals("ing-wheat", wheatFlourLineage.parentIngredientId)
        assertEquals("DERIVED_FROM", wheatFlourLineage.relationType)
        val wheatFlourSafety = bundle.safetyRelations.single { it.ingredientId == "ing-wheat-flour" }
        assertEquals("sg-eu-cereals-gluten", wheatFlourSafety.safetyGroupId)
        assertEquals("DERIVED_FROM", wheatFlourSafety.relationType)
        assertEquals("EU_LEGAL", wheatFlourSafety.evidenceLevel)
        assertEquals("EU_FIC_1169_2011", wheatFlourSafety.sourceId)

        val chickpeaFlourLineage = bundle.ingredientRelations.single { it.childIngredientId == "ing-chickpea-flour" }
        assertEquals("ing-chickpea", chickpeaFlourLineage.parentIngredientId)
        assertEquals("DERIVED_FROM", chickpeaFlourLineage.relationType)
        assertTrue(bundle.safetyRelations.none { it.ingredientId == "ing-chickpea-flour" })

        val buckwheatFlourLineage = bundle.ingredientRelations.single { it.childIngredientId == "ing-buckwheat-flour" }
        assertEquals("ing-buckwheat", buckwheatFlourLineage.parentIngredientId)
        assertTrue(bundle.safetyRelations.none { it.ingredientId == "ing-buckwheat-flour" })

        val importer = CatalogImporter(
            reader = reader,
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 1234L },
        )

        val first = importer.ensureImported()
        assertTrue(first is CatalogImportResult.Imported)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(252, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(245, database.ingredientCatalogDao().countAliases())
        assertEquals(25, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(14, database.ingredientCatalogDao().countActiveSafetyGroups())
        assertEquals(33, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(0, database.ingredientCatalogDao().countActiveRegulatoryExemptions())

        val chickenParents = database.ingredientCatalogDao().getParentRelations("ing-chicken-breast")
        assertEquals(1, chickenParents.size)
        assertEquals("ing-chicken", chickenParents.single().parentIngredientId)
        assertEquals("CUT_OF", chickenParents.single().relationType)
        assertEquals(0, database.ingredientCatalogDao().getSafetyRelationsForIngredient("ing-chicken-breast").size)

        val wheatFlourParents = database.ingredientCatalogDao().getParentRelations("ing-wheat-flour")
        assertEquals(1, wheatFlourParents.size)
        assertEquals("ing-wheat", wheatFlourParents.single().parentIngredientId)
        assertEquals("DERIVED_FROM", wheatFlourParents.single().relationType)
        assertEquals(1, database.ingredientCatalogDao().getSafetyRelationsForIngredient("ing-wheat-flour").size)
        assertEquals(0, database.ingredientCatalogDao().getSafetyRelationsForIngredient("ing-chickpea-flour").size)
        assertEquals(0, database.ingredientCatalogDao().getSafetyRelationsForIngredient("ing-buckwheat-flour").size)

        val metadata = requireNotNull(database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY))
        assertEquals(6, metadata.catalogVersion)
        assertEquals("es-ES", metadata.locale)
        assertEquals("EU-ES", metadata.jurisdiction)
        assertEquals(1234L, metadata.importedAt)

        val second = importer.ensureImported()
        assertTrue(second is CatalogImportResult.AlreadyCurrent)
        assertEquals(252, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(245, database.ingredientCatalogDao().countAliases())
        assertEquals(25, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(33, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(0, database.ingredientCatalogDao().countActiveRegulatoryExemptions())
    }

    @Test
    fun schemaThreeLineageBundlePersistsAndCanBeTraversedWithoutSafetyPropagation() = runBlocking {
        val importer = CatalogImporter(
            reader = IngredientCatalogAssetReader(LineageBundleSource()),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 500L },
        )

        val result = importer.ensureImported("ingredient-catalog/v5-test")

        assertTrue(result is CatalogImportResult.Imported)
        assertEquals(2, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(1, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(0, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(0, database.ingredientCatalogDao().countActiveRegulatoryExemptions())

        val parents = database.ingredientCatalogDao().getParentRelations("ing-chicken-breast")
        assertEquals(1, parents.size)
        assertEquals("ing-chicken", parents.single().parentIngredientId)
        assertEquals("CUT_OF", parents.single().relationType)

        val children = database.ingredientCatalogDao().getChildRelations("ing-chicken")
        assertEquals(listOf("ing-chicken-breast"), children.map { it.childIngredientId })
    }

    @Test
    fun schemaFourRegulatoryExemptionPersistsWithoutCreatingSafetyEvidence() = runBlocking {
        val importer = CatalogImporter(
            reader = IngredientCatalogAssetReader(RegulatoryExemptionBundleSource()),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 700L },
        )

        val result = importer.ensureImported("ingredient-catalog/v7-test")

        assertTrue(result is CatalogImportResult.Imported)
        assertEquals(1, database.ingredientCatalogDao().countActiveRegulatoryExemptions())
        assertEquals(0, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(0, database.ingredientCatalogDao().countActiveIngredientRelations())

        val exemptions = database.ingredientCatalogDao().getRegulatoryExemptionsForIngredient("ing-refined-soy-oil-test")
        assertEquals(1, exemptions.size)
        val exemption = exemptions.single()
        assertEquals("sg-soy-test", exemption.safetyGroupId)
        assertEquals("EU", exemption.jurisdiction)
        assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", exemption.regulatoryEffect)
        assertEquals("source-eu-test", exemption.sourceId)
        assertTrue(exemption.conditions.contains("Synthetic fixture"))
        assertEquals(0, database.ingredientCatalogDao().getSafetyRelationsForIngredient("ing-refined-soy-oil-test").size)
    }

    @Test
    fun invalidNewBundleDoesNotPartiallyReplaceExistingCatalog() = runBlocking {
        val realImporter = CatalogImporter(
            reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets)),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 100L },
        )
        realImporter.ensureImported()

        val brokenImporter = CatalogImporter(
            reader = IngredientCatalogAssetReader(BrokenVersionSevenSource()),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 200L },
        )

        val failure = runCatching { brokenImporter.ensureImported() }.exceptionOrNull()
        assertTrue(failure is CatalogValidationException)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(252, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(245, database.ingredientCatalogDao().countAliases())
        assertEquals(25, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(33, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(0, database.ingredientCatalogDao().countActiveRegulatoryExemptions())
        assertEquals(6, database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
    }

    private class LineageBundleSource : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v5-test/manifest.json" to """
                {
                  "catalogId":"lineage-test",
                  "schemaVersion":3,
                  "catalogVersion":5,
                  "releaseStatus":"DRAFT",
                  "locale":"es-ES",
                  "jurisdiction":"EU-ES",
                  "reviewedAt":"2026-08-08",
                  "files":{
                    "categories":"categories.json",
                    "ingredients":"ingredients.json",
                    "aliases":"aliases.json",
                    "ingredientRelations":"ingredient-relations.json",
                    "safetyGroups":"safety-groups.json",
                    "safetySources":"safety-sources.json",
                    "safetyRelations":"safety-relations.json"
                  },
                  "counts":{
                    "categories":1,
                    "ingredients":2,
                    "aliases":0,
                    "ingredientRelations":1,
                    "safetyGroups":0,
                    "safetySources":0,
                    "safetyRelations":0
                  }
                }
            """.trimIndent(),
            "ingredient-catalog/v5-test/categories.json" to """[{"id":"cat-poultry","code":"POULTRY","name":"Aves","sortOrder":1,"isActive":true}]""",
            "ingredient-catalog/v5-test/ingredients.json" to """[
                {"id":"ing-chicken","canonicalName":"Pollo","normalizedName":"pollo","categoryId":"cat-poultry","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE","isActive":true},
                {"id":"ing-chicken-breast","canonicalName":"Pechuga de pollo","normalizedName":"pechuga de pollo","categoryId":"cat-poultry","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE","isActive":true}
            ]""",
            "ingredient-catalog/v5-test/aliases.json" to "[]",
            "ingredient-catalog/v5-test/ingredient-relations.json" to """[
                {"id":"lineage-chicken-breast","childIngredientId":"ing-chicken-breast","parentIngredientId":"ing-chicken","relationType":"CUT_OF","reviewedAt":"2026-08-08","sourceReference":"CULINARY_IDENTITY_REVIEW","isActive":true}
            ]""",
            "ingredient-catalog/v5-test/safety-groups.json" to "[]",
            "ingredient-catalog/v5-test/safety-sources.json" to "[]",
            "ingredient-catalog/v5-test/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing lineage test asset: $path")
    }

    private class RegulatoryExemptionBundleSource : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v7-test/manifest.json" to """
                {
                  "catalogId":"regulatory-exemption-test",
                  "schemaVersion":4,
                  "catalogVersion":7,
                  "releaseStatus":"DRAFT",
                  "locale":"es-ES",
                  "jurisdiction":"EU-ES",
                  "reviewedAt":"2026-08-08",
                  "files":{
                    "categories":"categories.json",
                    "ingredients":"ingredients.json",
                    "aliases":"aliases.json",
                    "regulatoryExemptions":"regulatory-exemptions.json",
                    "safetyGroups":"safety-groups.json",
                    "safetySources":"safety-sources.json",
                    "safetyRelations":"safety-relations.json"
                  },
                  "counts":{
                    "categories":1,
                    "ingredients":1,
                    "aliases":0,
                    "ingredientRelations":0,
                    "regulatoryExemptions":1,
                    "safetyGroups":1,
                    "safetySources":1,
                    "safetyRelations":0
                  }
                }
            """.trimIndent(),
            "ingredient-catalog/v7-test/categories.json" to """[{"id":"cat-oils","code":"OILS","name":"Aceites","sortOrder":1,"isActive":true}]""",
            "ingredient-catalog/v7-test/ingredients.json" to """[
                {"id":"ing-refined-soy-oil-test","canonicalName":"Aceite de soja totalmente refinado de prueba","normalizedName":"aceite de soja totalmente refinado de prueba","categoryId":"cat-oils","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE","isActive":true}
            ]""",
            "ingredient-catalog/v7-test/aliases.json" to "[]",
            "ingredient-catalog/v7-test/regulatory-exemptions.json" to """[
                {"id":"exemption-soy-test","ingredientId":"ing-refined-soy-oil-test","safetyGroupId":"sg-soy-test","jurisdiction":"EU","regulatoryEffect":"EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION","conditions":"Synthetic fixture only; legal conditions must be independently reviewed before production data.","sourceId":"source-eu-test","effectiveFrom":"2025-04-01","reviewedAt":"2026-08-08","isActive":true}
            ]""",
            "ingredient-catalog/v7-test/safety-groups.json" to """[
                {"id":"sg-soy-test","code":"SOYBEANS","displayName":"Soja","conditionType":"FOOD_ALLERGY","regulatoryStatus":"EU_ANNEX_II","jurisdiction":"EU","isActive":true}
            ]""",
            "ingredient-catalog/v7-test/safety-sources.json" to """[
                {"id":"source-eu-test","organization":"European Union","title":"Synthetic legal source fixture","officialReference":"TEST","jurisdiction":"EU","reviewDate":"2026-08-08"}
            ]""",
            "ingredient-catalog/v7-test/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing regulatory test asset: $path")
    }

    private class BrokenVersionSevenSource : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v6/manifest.json" to """
                {
                  "catalogId":"broken",
                  "schemaVersion":1,
                  "catalogVersion":7,
                  "releaseStatus":"INFRASTRUCTURE",
                  "locale":"es-ES",
                  "jurisdiction":"EU-ES",
                  "reviewedAt":"2026-08-08",
                  "files":{
                    "categories":"categories.json",
                    "ingredients":"ingredients.json",
                    "aliases":"aliases.json",
                    "safetyGroups":"safety-groups.json",
                    "safetySources":"safety-sources.json",
                    "safetyRelations":"safety-relations.json"
                  },
                  "counts":{
                    "categories":1,
                    "ingredients":0,
                    "aliases":0,
                    "safetyGroups":0,
                    "safetySources":0,
                    "safetyRelations":0,
                    "ingredientRelations":0
                  }
                }
            """.trimIndent(),
            "ingredient-catalog/v6/categories.json" to "[]",
            "ingredient-catalog/v6/ingredients.json" to "[]",
            "ingredient-catalog/v6/aliases.json" to "[]",
            "ingredient-catalog/v6/safety-groups.json" to "[]",
            "ingredient-catalog/v6/safety-sources.json" to "[]",
            "ingredient-catalog/v6/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing broken test asset: $path")
    }
}
