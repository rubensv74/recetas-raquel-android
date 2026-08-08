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
    fun importsVersionedCulinaryBatch02AndIsIdempotent() = runBlocking {
        val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
        val bundle = reader.read()

        assertEquals(2, bundle.manifest.schemaVersion)
        assertEquals(4, bundle.manifest.catalogVersion)
        assertEquals("DRAFT", bundle.manifest.releaseStatus)
        assertEquals(20, bundle.categories.size)
        assertEquals(227, bundle.ingredients.size)
        assertEquals(220, bundle.aliases.size)
        assertEquals(0, bundle.ingredientRelations.size)
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

        val importer = CatalogImporter(
            reader = reader,
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 1234L },
        )

        val first = importer.ensureImported()
        assertTrue(first is CatalogImportResult.Imported)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(227, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(220, database.ingredientCatalogDao().countAliases())
        assertEquals(0, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(14, database.ingredientCatalogDao().countActiveSafetyGroups())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())

        val metadata = requireNotNull(database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY))
        assertEquals(4, metadata.catalogVersion)
        assertEquals("es-ES", metadata.locale)
        assertEquals("EU-ES", metadata.jurisdiction)
        assertEquals(1234L, metadata.importedAt)

        val second = importer.ensureImported()
        assertTrue(second is CatalogImportResult.AlreadyCurrent)
        assertEquals(227, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(220, database.ingredientCatalogDao().countAliases())
        assertEquals(0, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())
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
            reader = IngredientCatalogAssetReader(BrokenVersionFiveSource()),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 200L },
        )

        val failure = runCatching { brokenImporter.ensureImported() }.exceptionOrNull()
        assertTrue(failure is CatalogValidationException)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(227, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(220, database.ingredientCatalogDao().countAliases())
        assertEquals(0, database.ingredientCatalogDao().countActiveIngredientRelations())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(4, database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
    }

    private class BrokenVersionFiveSource : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v4/manifest.json" to """
                {
                  "catalogId":"broken",
                  "schemaVersion":1,
                  "catalogVersion":5,
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
            "ingredient-catalog/v4/categories.json" to "[]",
            "ingredient-catalog/v4/ingredients.json" to "[]",
            "ingredient-catalog/v4/aliases.json" to "[]",
            "ingredient-catalog/v4/safety-groups.json" to "[]",
            "ingredient-catalog/v4/safety-sources.json" to "[]",
            "ingredient-catalog/v4/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing broken test asset: $path")
    }
}
