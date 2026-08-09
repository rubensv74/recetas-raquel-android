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
class CatalogV9SoyDerivativeExemptionTest {
    @Test
    fun reviewedSoyDerivativeExemptionsRemainRegulatoryOnly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val bundle = reader.read()
            val validation = CatalogValidator.validate(bundle)

            assertTrue(validation.errors.joinToString(separator = "\n"), validation.isValid)
            assertEquals(4, bundle.manifest.schemaVersion)
            assertEquals(9, bundle.manifest.catalogVersion)
            assertEquals("DRAFT", bundle.manifest.releaseStatus)
            assertEquals(20, bundle.categories.size)
            assertEquals(262, bundle.ingredients.size)
            assertEquals(245, bundle.aliases.size)
            assertEquals(35, bundle.ingredientRelations.size)
            assertEquals(14, bundle.safetyGroups.size)
            assertEquals(3, bundle.safetySources.size)
            assertEquals(33, bundle.safetyRelations.size)
            assertEquals(10, bundle.regulatoryExemptions.size)

            val directSoyDerivativeIds = listOf(
                "ing-soy-natural-mixed-tocopherols-e306",
                "ing-soy-natural-d-alpha-tocopherol",
                "ing-soy-natural-d-alpha-tocopherol-acetate",
                "ing-soy-natural-d-alpha-tocopherol-succinate",
                "ing-soy-phytosterols",
                "ing-soy-phytosterol-esters",
            )
            val allNewIds = directSoyDerivativeIds + "ing-soy-plant-stanol-ester"

            allNewIds.forEach { ingredientId ->
                val ingredient = bundle.ingredients.single { it.id == ingredientId }
                assertEquals("VERIFIED", ingredient.verificationStatus)
                assertEquals("VARIABLE_BY_PREPARATION", ingredient.compositionVariability)
                assertTrue(ingredient.sourceUpdatedAt == null)

                val exemption = bundle.regulatoryExemptions.single { it.ingredientId == ingredientId }
                assertEquals("sg-eu-soybeans", exemption.safetyGroupId)
                assertEquals("EU-ES", exemption.jurisdiction)
                assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", exemption.regulatoryEffect)
                assertEquals("EU_FIC_1169_2011", exemption.sourceId)
                assertTrue(exemption.effectiveFrom == null)
                assertTrue(exemption.effectiveTo == null)
                assertEquals("2026-08-09", exemption.reviewedAt)

                assertTrue(bundle.safetyRelations.none { it.ingredientId == ingredientId })
            }

            directSoyDerivativeIds.forEach { ingredientId ->
                val lineage = bundle.ingredientRelations.single { it.childIngredientId == ingredientId }
                assertEquals("ing-soybean", lineage.parentIngredientId)
                assertEquals("DERIVED_FROM", lineage.relationType)
                assertEquals("EU_FIC_1169_2011", lineage.sourceReference)
            }

            val stanolLineage = bundle.ingredientRelations.single {
                it.childIngredientId == "ing-soy-plant-stanol-ester"
            }
            assertEquals("ing-soy-phytosterols", stanolLineage.parentIngredientId)
            assertEquals("DERIVED_FROM", stanolLineage.relationType)
            assertEquals("EU_FIC_1169_2011", stanolLineage.sourceReference)

            val soybeanSafety = bundle.safetyRelations.single { it.ingredientId == "ing-soybean" }
            assertEquals("sg-eu-soybeans", soybeanSafety.safetyGroupId)

            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1000L },
            )
            val result = importer.ensureImported()

            assertTrue(result is CatalogImportResult.Imported)
            val dao = database.ingredientCatalogDao()
            assertEquals(262, dao.countActiveIngredients())
            assertEquals(245, dao.countAliases())
            assertEquals(35, dao.countActiveIngredientRelations())
            assertEquals(33, dao.countSafetyRelations())
            assertEquals(10, dao.countActiveRegulatoryExemptions())

            allNewIds.forEach { ingredientId ->
                assertEquals(0, dao.getSafetyRelationsForIngredient(ingredientId).size)
                assertEquals(1, dao.getRegulatoryExemptionsForIngredient(ingredientId).size)
            }

            assertEquals(9, dao.getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)
        } finally {
            database.close()
        }
    }
}
