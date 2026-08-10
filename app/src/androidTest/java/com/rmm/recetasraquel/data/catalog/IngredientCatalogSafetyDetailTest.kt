package com.rmm.recetasraquel.data.catalog

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.repository.LocalIngredientCatalogRepository
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngredientCatalogSafetyDetailTest {
    @Test
    fun wheatSafetyDetailIncludesJurisdictionAndOfficialSource() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1400L },
            )
            val repository = LocalIngredientCatalogRepository(importer, database.ingredientCatalogDao())

            val importResult = repository.ensureCatalogImported().getOrThrow()
            assertEquals(12, importResult.catalogVersion)

            val relation = repository.getSafetyRelations("ing-wheat").getOrThrow().single()
            assertEquals("sg-eu-cereals-gluten", relation.safetyGroupId)
            assertEquals("Cereales que contienen gluten", relation.safetyGroupName)
            assertEquals("EU-ES", relation.jurisdiction)
            assertEquals("INHERENT_SOURCE", relation.relationType)
            assertEquals("EU_LEGAL", relation.evidenceLevel)
            assertEquals("EU_FIC_1169_2011", relation.sourceId)
            assertNotNull(relation.sourceDetails)
            assertTrue(relation.sourceDetails.orEmpty().contains("Reglamento"))
            assertEquals("2026-08-08", relation.reviewedAt)
        } finally {
            database.close()
        }
    }
}
