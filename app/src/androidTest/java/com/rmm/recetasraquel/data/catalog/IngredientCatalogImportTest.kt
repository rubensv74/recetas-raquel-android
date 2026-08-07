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
    fun importsVersionedDraftSeedAndIsIdempotent() = runBlocking {
        val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
        val bundle = reader.read()

        assertEquals(2, bundle.manifest.catalogVersion)
        assertEquals("DRAFT", bundle.manifest.releaseStatus)
        assertEquals(20, bundle.categories.size)
        assertEquals(27, bundle.ingredients.size)
        assertEquals(22, bundle.aliases.size)
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

        val importer = CatalogImporter(
            reader = reader,
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 1234L },
        )

        val first = importer.ensureImported()
        assertTrue(first is CatalogImportResult.Imported)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(27, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(22, database.ingredientCatalogDao().countAliases())
        assertEquals(14, database.ingredientCatalogDao().countActiveSafetyGroups())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())

        val metadata = requireNotNull(database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY))
        assertEquals(2, metadata.catalogVersion)
        assertEquals("es-ES", metadata.locale)
        assertEquals("EU-ES", metadata.jurisdiction)
        assertEquals(1234L, metadata.importedAt)

        val second = importer.ensureImported()
        assertTrue(second is CatalogImportResult.AlreadyCurrent)
        assertEquals(27, database.ingredientCatalogDao().countActiveIngredients())
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
            reader = IngredientCatalogAssetReader(BrokenVersionThreeSource()),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 200L },
        )

        val failure = runCatching { brokenImporter.ensureImported() }.exceptionOrNull()
        assertTrue(failure is CatalogValidationException)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(27, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(27, database.ingredientCatalogDao().countSafetyRelations())
        assertEquals(2, database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
    }

    private class BrokenVersionThreeSource : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v2/manifest.json" to """
                {
                  "catalogId":"broken",
                  "schemaVersion":1,
                  "catalogVersion":3,
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
                    "safetyRelations":0
                  }
                }
            """.trimIndent(),
            "ingredient-catalog/v2/categories.json" to "[]",
            "ingredient-catalog/v2/ingredients.json" to "[]",
            "ingredient-catalog/v2/aliases.json" to "[]",
            "ingredient-catalog/v2/safety-groups.json" to "[]",
            "ingredient-catalog/v2/safety-sources.json" to "[]",
            "ingredient-catalog/v2/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing broken test asset: $path")
    }
}
