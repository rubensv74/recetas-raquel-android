package com.rmm.recetasraquel.data.catalog

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogV8SoyRegulatoryExemptionTest {
    @Test
    fun fullyRefinedSoyOilAndFatRemainRegulatoryExemptionsOnly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val bundle = reader.read()
            val validation = CatalogValidator.validate(bundle)

            assertTrue(validation.errors.joinToString(separator = "\n"), validation.isValid)
            assertEquals(4, bundle.manifest.schemaVersion)
            assertEquals(8, bundle.manifest.catalogVersion)
            assertEquals(20, bundle.categories.size)
            assertEquals(255, bundle.ingredients.size)
            assertEquals(245, bundle.aliases.size)
            assertEquals(28, bundle.ingredientRelations.size)
            assertEquals(33, bundle.safetyRelations.size)
            assertEquals(3, bundle.regulatoryExemptions.size)

            listOf(
                "ing-fully-refined-soybean-oil",
                "ing-fully-refined-soybean-fat",
            ).forEach { ingredientId ->
                val ingredient = bundle.ingredients.single { it.id == ingredientId }
                assertEquals("VERIFIED", ingredient.verificationStatus)
                assertEquals("VARIABLE_BY_PREPARATION", ingredient.compositionVariability)
                assertTrue(ingredient.sourceUpdatedAt == null)

                val lineage = bundle.ingredientRelations.single { it.childIngredientId == ingredientId }
                assertEquals("ing-soybean", lineage.parentIngredientId)
                assertEquals("DERIVED_FROM", lineage.relationType)
                assertEquals("EU_FIC_1169_2011", lineage.sourceReference)

                val exemption = bundle.regulatoryExemptions.single { it.ingredientId == ingredientId }
                assertEquals("sg-eu-soybeans", exemption.safetyGroupId)
                assertEquals("EU-ES", exemption.jurisdiction)
                assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", exemption.regulatoryEffect)
                assertEquals("EU_FIC_1169_2011", exemption.sourceId)
                assertTrue(exemption.effectiveFrom == null)
                assertTrue(exemption.effectiveTo == null)

                assertTrue(bundle.safetyRelations.none { it.ingredientId == ingredientId })
            }

            val soybeanSafety = bundle.safetyRelations.single { it.ingredientId == "ing-soybean" }
            assertEquals("sg-eu-soybeans", soybeanSafety.safetyGroupId)

            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 900L },
            )
            val result = importer.ensureImported()

            assertTrue(result is CatalogImportResult.Imported)
            val dao = database.ingredientCatalogDao()
            assertEquals(255, dao.countActiveIngredients())
            assertEquals(245, dao.countAliases())
            assertEquals(28, dao.countActiveIngredientRelations())
            assertEquals(33, dao.countSafetyRelations())
            assertEquals(3, dao.countActiveRegulatoryExemptions())
            assertEquals(0, dao.getSafetyRelationsForIngredient("ing-fully-refined-soybean-oil").size)
            assertEquals(0, dao.getSafetyRelationsForIngredient("ing-fully-refined-soybean-fat").size)
            assertEquals(1, dao.getRegulatoryExemptionsForIngredient("ing-fully-refined-soybean-oil").size)
            assertEquals(1, dao.getRegulatoryExemptionsForIngredient("ing-fully-refined-soybean-fat").size)
            assertEquals(8, dao.getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
        } finally {
            database.close()
        }
    }
}
