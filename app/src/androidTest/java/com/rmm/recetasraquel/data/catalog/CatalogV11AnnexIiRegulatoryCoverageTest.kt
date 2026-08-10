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
class CatalogV11AnnexIiRegulatoryCoverageTest {
    @Test
    fun annexIiExplicitExemptionsAreCoveredWithoutExemptingGenericSources() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val bundle = reader.read("ingredient-catalog/v11")
            val validation = CatalogValidator.validate(bundle)

            assertTrue(validation.errors.joinToString(separator = "\n"), validation.isValid)
            assertEquals(4, bundle.manifest.schemaVersion)
            assertEquals(11, bundle.manifest.catalogVersion)
            assertEquals("DRAFT", bundle.manifest.releaseStatus)
            assertEquals(20, bundle.categories.size)
            assertEquals(272, bundle.ingredients.size)
            assertEquals(245, bundle.aliases.size)
            assertEquals(43, bundle.ingredientRelations.size)
            assertEquals(14, bundle.safetyGroups.size)
            assertEquals(3, bundle.safetySources.size)
            assertEquals(33, bundle.safetyRelations.size)
            assertEquals(20, bundle.regulatoryExemptions.size)

            val expectedGroups = mapOf(
                "ing-gluten-cereals-alcoholic-distillates" to "sg-eu-cereals-gluten",
                "ing-fish-gelatine-vitamin-carotenoid-carrier" to "sg-eu-fish",
                "ing-fish-gelatine-isinglass-beer-wine-fining" to "sg-eu-fish",
                "ing-whey-alcoholic-distillates" to "sg-eu-milk",
                "ing-lactitol" to "sg-eu-milk",
                "ing-nuts-alcoholic-distillates" to "sg-eu-nuts",
            )

            expectedGroups.forEach { (ingredientId, groupId) ->
                val ingredient = bundle.ingredients.single { it.id == ingredientId }
                assertEquals("VERIFIED", ingredient.verificationStatus)

                val exemption = bundle.regulatoryExemptions.single { it.ingredientId == ingredientId }
                assertEquals(groupId, exemption.safetyGroupId)
                assertEquals("EU-ES", exemption.jurisdiction)
                assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", exemption.regulatoryEffect)
                assertEquals("EU_FIC_1169_2011", exemption.sourceId)
                assertEquals("2026-08-10", exemption.reviewedAt)
                assertTrue(exemption.conditions.isNotBlank())
                assertTrue(exemption.notes.orEmpty().contains("No implica ausencia"))
                assertTrue(bundle.safetyRelations.none { it.ingredientId == ingredientId })
            }

            val genericSourceIds = setOf(
                "ing-wheat",
                "ing-spelt",
                "ing-khorasan",
                "ing-rye",
                "ing-barley",
                "ing-oats",
                "ing-fish",
                "ing-milk",
                "ing-almond",
                "ing-hazelnut",
                "ing-walnut",
                "ing-cashew",
                "ing-pecan",
                "ing-brazil-nut",
                "ing-pistachio",
                "ing-macadamia",
            )
            genericSourceIds.forEach { ingredientId ->
                assertTrue(bundle.regulatoryExemptions.none { it.ingredientId == ingredientId })
            }

            val expectedParents = mapOf(
                "ing-fish-gelatine-vitamin-carotenoid-carrier" to "ing-fish",
                "ing-fish-gelatine-isinglass-beer-wine-fining" to "ing-fish",
                "ing-whey-alcoholic-distillates" to "ing-milk",
                "ing-lactitol" to "ing-milk",
            )
            expectedParents.forEach { (childId, parentId) ->
                val lineage = bundle.ingredientRelations.single { it.childIngredientId == childId }
                assertEquals(parentId, lineage.parentIngredientId)
                assertEquals("DERIVED_FROM", lineage.relationType)
                assertEquals("EU_FIC_1169_2011", lineage.sourceReference)
            }

            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1200L },
            )
            val dao = database.ingredientCatalogDao()

            val v10 = importer.ensureImported("ingredient-catalog/v10")
            assertTrue(v10 is CatalogImportResult.Imported)
            assertEquals(14, dao.countActiveRegulatoryExemptions())
            assertEquals(14, dao.countRegulatoryExemptionSnapshots())
            assertEquals(10, dao.getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)

            val v11 = importer.ensureImported("ingredient-catalog/v11")
            assertTrue(v11 is CatalogImportResult.Imported)
            assertEquals(272, dao.countActiveIngredients())
            assertEquals(43, dao.countActiveIngredientRelations())
            assertEquals(33, dao.countSafetyRelations())
            assertEquals(20, dao.countActiveRegulatoryExemptions())
            assertEquals(34, dao.countRegulatoryExemptionSnapshots())
            assertEquals(11, dao.getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)

            expectedGroups.keys.forEach { ingredientId ->
                assertEquals(0, dao.getSafetyRelationsForIngredient(ingredientId).size)
                assertEquals(1, dao.getRegulatoryExemptionsForIngredient(ingredientId).size)
            }

            genericSourceIds.forEach { ingredientId ->
                assertEquals(0, dao.getRegulatoryExemptionsForIngredient(ingredientId).size)
            }
        } finally {
            database.close()
        }
    }
}
