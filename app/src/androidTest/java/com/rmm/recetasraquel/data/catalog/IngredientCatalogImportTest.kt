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
    fun importsVersionedLineageCatalogV5AndIsIdempotent() = runBlocking {
        val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
        val bundle = reader.read()

        assertEquals(3, bundle.manifest.schemaVersion)
        assertEquals(5, bundle.manifest.catalogVersion)
        assertEquals("DRAFT", bundle.manifest.releaseStatus)
        assertEquals(20, bundle.categories.size)
        assertEquals(242, bundle.ingredients.size)
        assertEquals(235, bundle.aliases.size)
        assertEquals(15, bundle.ingredientRelations.size)
        assertEquals(14, bundle.safetyGroups.size)
        assertEquals(3, bundle.safetySources.size)
        assertEquals(27, bundle.safetyRelations.size)

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

        val importer = CatalogImporter(
            reader = reader,
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 1234L },
        )

        val first = importer.ensureImported()
        assertTrue(first is CatalogImportResult.Imported)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(242, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(235, database.ingredientCatalogDao().countAliases())
        assertEquals(15, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(14, database.ingredientCatalogDao().countActiveSafetyGroups())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())

        val parents = database.ingredientCatalogDao().getParentRelations("ing-chicken-breast")
        assertEquals(1, parents.size)
        assertEquals("ing-chicken", parents.single().parentIngredientId)
        assertEquals("CUT_OF", parents.single().relationType)
        assertEquals(0, database.ingredientCatalogDao().getSafetyRelationsForIngredient("ing-chicken-breast").size)

        val metadata = requireNotNull(database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY))
        assertEquals(5, metadata.catalogVersion)
        assertEquals("es-ES", metadata.locale)
        assertEquals("EU-ES", metadata.jurisdiction)
        assertEquals(1234L, metadata.importedAt)

        val second = importer.ensureImported()
        assertTrue(second is CatalogImportResult.AlreadyCurrent)
        assertEquals(242, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(235, database.ingredientCatalogDao().countAliases())
        assertEquals(15, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())
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

        val parents = database.ingredientCatalogDao().getParentRelations("ing-chicken-breast")
        assertEquals(1, parents.size)
        assertEquals("ing-chicken", parents.single().parentIngredientId)
        assertEquals("CUT_OF", parents.single().relationType)

        val children = database.ingredientCatalogDao().getChildRelations("ing-chicken")
        assertEquals(listOf("ing-chicken-breast"), children.map { it.childIngredientId })
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
            reader = IngredientCatalogAssetReader(BrokenVersionSixSource()),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 200L },
        )

        val failure = runCatching { brokenImporter.ensureImported() }.exceptionOrNull()
        assertTrue(failure is CatalogValidationException)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(242, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(235, database.ingredientCatalogDao().countAliases())
        assertEquals(15, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(5, database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
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

    private class BrokenVersionSixSource : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v5/manifest.json" to """
                {
                  "catalogId":"broken",
                  "schemaVersion":1,
                  "catalogVersion":6,
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
            "ingredient-catalog/v5/categories.json" to "[]",
            "ingredient-catalog/v5/ingredients.json" to "[]",
            "ingredient-catalog/v5/aliases.json" to "[]",
            "ingredient-catalog/v5/safety-groups.json" to "[]",
            "ingredient-catalog/v5/safety-sources.json" to "[]",
            "ingredient-catalog/v5/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing broken test asset: $path")
    }
}
