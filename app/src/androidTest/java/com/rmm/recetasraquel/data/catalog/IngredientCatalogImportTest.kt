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
    fun importsVersionedInfrastructureBundleAndIsIdempotent() = runBlocking {
        val importer = CatalogImporter(
            reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets)),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 1234L },
        )

        val first = importer.ensureImported()
        assertTrue(first is CatalogImportResult.Imported)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(0, database.ingredientCatalogDao().countActiveIngredients())
        assertEquals(0, database.ingredientCatalogDao().countAliases())
        assertEquals(0, database.ingredientCatalogDao().countActiveSafetyGroups())
        assertEquals(0, database.ingredientCatalogDao().countSafetyRelations())

        val metadata = requireNotNull(database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY))
        assertEquals(1, metadata.catalogVersion)
        assertEquals("es-ES", metadata.locale)
        assertEquals("EU-ES", metadata.jurisdiction)
        assertEquals(1234L, metadata.importedAt)

        val second = importer.ensureImported()
        assertTrue(second is CatalogImportResult.AlreadyCurrent)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
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
            reader = IngredientCatalogAssetReader(BrokenVersionTwoSource()),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 200L },
        )

        val failure = runCatching { brokenImporter.ensureImported() }.exceptionOrNull()
        assertTrue(failure is CatalogValidationException)
        assertEquals(20, database.ingredientCatalogDao().countActiveCategories())
        assertEquals(1, database.ingredientCatalogDao().getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
    }

    private class BrokenVersionTwoSource : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v1/manifest.json" to """
                {
                  "catalogId":"broken",
                  "schemaVersion":1,
                  "catalogVersion":2,
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
            "ingredient-catalog/v1/categories.json" to "[]",
            "ingredient-catalog/v1/ingredients.json" to "[]",
            "ingredient-catalog/v1/aliases.json" to "[]",
            "ingredient-catalog/v1/safety-groups.json" to "[]",
            "ingredient-catalog/v1/safety-sources.json" to "[]",
            "ingredient-catalog/v1/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing broken test asset: $path")
    }
}
