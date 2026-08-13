package com.rmm.recetasraquel.data.catalog

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.repository.LocalIngredientCatalogRepository
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogRelationDirection
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageType
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngredientCatalogIdentityDetailTest {
    @Test
    fun flourDetailExposesAliasAndExplicitCulinaryParent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1500L },
            )
            val repository = LocalIngredientCatalogRepository(importer, database.ingredientCatalogDao())

            assertEquals(13, repository.ensureCatalogImported().getOrThrow().catalogVersion)

            val detail = repository.getIngredientDetail("ing-wheat-flour").getOrThrow()
            assertTrue("Harina trigo" in detail.aliases)

            val wheat = detail.relatedPresentations.single { it.ingredientId == "ing-wheat" }
            assertEquals("Trigo", wheat.canonicalName)
            assertEquals(IngredientLineageType.DERIVED_FROM, wheat.relationType)
            assertEquals(IngredientCatalogRelationDirection.PARENT, wheat.direction)
        } finally {
            database.close()
        }
    }

    @Test
    fun relatedPresentationsExcludeRegulatoryTechnicalIdentities() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1600L },
            )
            val repository = LocalIngredientCatalogRepository(importer, database.ingredientCatalogDao())

            repository.ensureCatalogImported().getOrThrow()

            val detail = repository.getIngredientDetail("ing-milk").getOrThrow()
            assertTrue(detail.relatedPresentations.any { it.ingredientId == "ing-lactitol" })
            assertFalse(detail.relatedPresentations.any { it.ingredientId == "ing-whey-alcoholic-distillates" })
        } finally {
            database.close()
        }
    }
}
