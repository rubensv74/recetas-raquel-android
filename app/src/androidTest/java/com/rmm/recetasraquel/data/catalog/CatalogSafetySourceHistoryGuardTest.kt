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
class CatalogSafetySourceHistoryGuardTest {
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
    fun publishedSourceIdCannotBeSilentlyRewrittenByLaterCatalog() = runBlocking {
        val dao = database.ingredientCatalogDao()
        val realImporter = CatalogImporter(
            reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets)),
            dao = dao,
            timeProvider = TimeProvider { 100L },
        )
        realImporter.ensureImported("ingredient-catalog/v10")

        val before = dao.getSafetySourcesByIds(listOf(SOURCE_ID)).single()
        val changedImporter = CatalogImporter(
            reader = IngredientCatalogAssetReader(ChangedSourceBundle()),
            dao = dao,
            timeProvider = TimeProvider { 200L },
        )

        val failure = runCatching {
            changedImporter.ensureImported("ingredient-catalog/v11-source-history-test")
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message.orEmpty().contains("new source id"))
        assertEquals(10, dao.getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
        assertEquals(before, dao.getSafetySourcesByIds(listOf(SOURCE_ID)).single())
    }

    private class ChangedSourceBundle : CatalogTextSource {
        private val values = mapOf(
            "ingredient-catalog/v11-source-history-test/manifest.json" to """
                {
                  "catalogId":"source-history-test",
                  "schemaVersion":3,
                  "catalogVersion":11,
                  "releaseStatus":"DRAFT",
                  "locale":"es-ES",
                  "jurisdiction":"EU-ES",
                  "reviewedAt":"2026-08-10",
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
                    "ingredients":1,
                    "aliases":0,
                    "ingredientRelations":0,
                    "safetyGroups":0,
                    "safetySources":1,
                    "safetyRelations":0
                  }
                }
            """.trimIndent(),
            "ingredient-catalog/v11-source-history-test/categories.json" to
                """[{"id":"cat-source-history","code":"SOURCE_HISTORY","name":"Prueba","sortOrder":1,"isActive":true}]""",
            "ingredient-catalog/v11-source-history-test/ingredients.json" to
                """[{"id":"ing-source-history","canonicalName":"Ingrediente prueba","normalizedName":"ingrediente prueba","categoryId":"cat-source-history","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE","isActive":true}]""",
            "ingredient-catalog/v11-source-history-test/aliases.json" to "[]",
            "ingredient-catalog/v11-source-history-test/ingredient-relations.json" to "[]",
            "ingredient-catalog/v11-source-history-test/safety-groups.json" to "[]",
            "ingredient-catalog/v11-source-history-test/safety-sources.json" to """
                [{
                  "id":"EU_FIC_1169_2011",
                  "organization":"Unión Europea",
                  "title":"Título alterado que no debe reescribir el histórico",
                  "officialReference":"Reglamento (UE) n.º 1169/2011",
                  "jurisdiction":"EU",
                  "publicationDate":"2011-11-22",
                  "reviewDate":"2026-08-10",
                  "documentStatus":"CONSOLIDATED",
                  "officialUrl":"https://eur-lex.europa.eu/"
                }]
            """.trimIndent(),
            "ingredient-catalog/v11-source-history-test/safety-relations.json" to "[]",
        )

        override fun read(path: String): String = values[path] ?: error("Missing source history test asset: $path")
    }

    private companion object {
        const val SOURCE_ID = "EU_FIC_1169_2011"
    }
}
