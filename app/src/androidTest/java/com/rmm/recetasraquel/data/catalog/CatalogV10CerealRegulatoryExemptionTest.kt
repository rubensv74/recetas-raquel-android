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
class CatalogV10CerealRegulatoryExemptionTest {
    @Test
    fun cerealTechnicalIdentitiesRemainRegulatoryExemptionsOnly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val bundle = reader.read()
            val validation = CatalogValidator.validate(bundle)

            assertTrue(validation.errors.joinToString(separator = "\n"), validation.isValid)
            assertEquals(4, bundle.manifest.schemaVersion)
            assertEquals(10, bundle.manifest.catalogVersion)
            assertEquals("DRAFT", bundle.manifest.releaseStatus)
            assertEquals(20, bundle.categories.size)
            assertEquals(266, bundle.ingredients.size)
            assertEquals(245, bundle.aliases.size)
            assertEquals(39, bundle.ingredientRelations.size)
            assertEquals(14, bundle.safetyGroups.size)
            assertEquals(3, bundle.safetySources.size)
            assertEquals(33, bundle.safetyRelations.size)
            assertEquals(14, bundle.regulatoryExemptions.size)

            val wheatIds = listOf(
                "ing-wheat-glucose-syrup",
                "ing-wheat-dextrose",
                "ing-wheat-maltodextrin",
            )
            val barleyId = "ing-barley-glucose-syrup"
            val allNewIds = wheatIds + barleyId

            allNewIds.forEach { ingredientId ->
                val ingredient = bundle.ingredients.single { it.id == ingredientId }
                assertEquals("VERIFIED", ingredient.verificationStatus)
                assertEquals("VARIABLE_BY_PREPARATION", ingredient.compositionVariability)
                assertTrue(ingredient.sourceUpdatedAt == null)

                val exemption = bundle.regulatoryExemptions.single { it.ingredientId == ingredientId }
                assertEquals("sg-eu-cereals-gluten", exemption.safetyGroupId)
                assertEquals("EU-ES", exemption.jurisdiction)
                assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", exemption.regulatoryEffect)
                assertEquals("EU_FIC_1169_2011", exemption.sourceId)
                assertTrue(exemption.effectiveFrom == null)
                assertTrue(exemption.effectiveTo == null)
                assertEquals("2026-08-09", exemption.reviewedAt)

                assertTrue(bundle.safetyRelations.none { it.ingredientId == ingredientId })
            }

            wheatIds.forEach { ingredientId ->
                val lineage = bundle.ingredientRelations.single { it.childIngredientId == ingredientId }
                assertEquals("ing-wheat", lineage.parentIngredientId)
                assertEquals("DERIVED_FROM", lineage.relationType)
                assertEquals("EU_FIC_1169_2011", lineage.sourceReference)
            }

            val barleyLineage = bundle.ingredientRelations.single { it.childIngredientId == barleyId }
            assertEquals("ing-barley", barleyLineage.parentIngredientId)
            assertEquals("DERIVED_FROM", barleyLineage.relationType)
            assertEquals("EU_FIC_1169_2011", barleyLineage.sourceReference)

            val wheatSafety = bundle.safetyRelations.single { it.ingredientId == "ing-wheat" }
            assertEquals("sg-eu-cereals-gluten", wheatSafety.safetyGroupId)
            val barleySafety = bundle.safetyRelations.single { it.ingredientId == "ing-barley" }
            assertEquals("sg-eu-cereals-gluten", barleySafety.safetyGroupId)

            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1100L },
            )
            val result = importer.ensureImported()

            assertTrue(result is CatalogImportResult.Imported)
            val dao = database.ingredientCatalogDao()
            assertEquals(266, dao.countActiveIngredients())
            assertEquals(245, dao.countAliases())
            assertEquals(39, dao.countActiveIngredientRelations())
            assertEquals(33, dao.countSafetyRelations())
            assertEquals(14, dao.countActiveRegulatoryExemptions())

            allNewIds.forEach { ingredientId ->
                assertEquals(0, dao.getSafetyRelationsForIngredient(ingredientId).size)
                assertEquals(1, dao.getRegulatoryExemptionsForIngredient(ingredientId).size)
            }

            assertEquals(10, dao.getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
        } finally {
            database.close()
        }
    }
}
