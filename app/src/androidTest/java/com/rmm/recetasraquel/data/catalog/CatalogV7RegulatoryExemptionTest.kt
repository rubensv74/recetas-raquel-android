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
class CatalogV7RegulatoryExemptionTest {
    @Test
    fun reviewedMustardBehenicAcidExemptionRemainsRegulatoryOnly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val bundle = reader.read()
            val validation = CatalogValidator.validate(bundle)

            assertTrue(validation.errors.joinToString(separator = "\n"), validation.isValid)
            assertEquals(4, bundle.manifest.schemaVersion)
            assertEquals(7, bundle.manifest.catalogVersion)
            assertEquals("DRAFT", bundle.manifest.releaseStatus)
            assertEquals(20, bundle.categories.size)
            assertEquals(253, bundle.ingredients.size)
            assertEquals(245, bundle.aliases.size)
            assertEquals(26, bundle.ingredientRelations.size)
            assertEquals(14, bundle.safetyGroups.size)
            assertEquals(3, bundle.safetySources.size)
            assertEquals(33, bundle.safetyRelations.size)
            assertEquals(1, bundle.regulatoryExemptions.size)

            val ingredient = bundle.ingredients.single { it.id == "ing-mustard-behenic-acid" }
            assertEquals("VERIFIED", ingredient.verificationStatus)
            assertEquals("VARIABLE_BY_PREPARATION", ingredient.compositionVariability)

            val lineage = bundle.ingredientRelations.single { it.childIngredientId == ingredient.id }
            assertEquals("ing-mustard", lineage.parentIngredientId)
            assertEquals("DERIVED_FROM", lineage.relationType)
            assertEquals("EU_MUSTARD_2024_2512", lineage.sourceReference)

            val exemption = bundle.regulatoryExemptions.single()
            assertEquals("rex-eu-mustard-behenic-acid-2024-2512", exemption.id)
            assertEquals(ingredient.id, exemption.ingredientId)
            assertEquals("sg-eu-mustard", exemption.safetyGroupId)
            assertEquals("EU-ES", exemption.jurisdiction)
            assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", exemption.regulatoryEffect)
            assertEquals("EU_MUSTARD_2024_2512", exemption.sourceId)
            assertEquals("2025-04-01", exemption.effectiveFrom)
            assertTrue(exemption.effectiveTo == null)
            assertTrue(exemption.conditions.contains("85 %"))
            assertTrue(exemption.conditions.contains("E470a"))
            assertTrue(exemption.conditions.contains("E471"))
            assertTrue(exemption.conditions.contains("E477"))

            assertTrue(bundle.safetyRelations.none { it.ingredientId == ingredient.id })
            assertEquals(33, bundle.safetyRelations.size)

            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 800L },
            )
            val result = importer.ensureImported()

            assertTrue(result is CatalogImportResult.Imported)
            val dao = database.ingredientCatalogDao()
            assertEquals(253, dao.countActiveIngredients())
            assertEquals(245, dao.countAliases())
            assertEquals(26, dao.countActiveIngredientRelations())
            assertEquals(33, dao.countSafetyRelations())
            assertEquals(1, dao.countActiveRegulatoryExemptions())

            val persistedExemption = dao.getRegulatoryExemptionsForIngredient(ingredient.id).single()
            assertEquals("sg-eu-mustard", persistedExemption.safetyGroupId)
            assertEquals("EU_MUSTARD_2024_2512", persistedExemption.sourceId)
            assertEquals("2025-04-01", persistedExemption.effectiveFrom)
            assertEquals(0, dao.getSafetyRelationsForIngredient(ingredient.id).size)

            val parents = dao.getParentRelations(ingredient.id)
            assertEquals(1, parents.size)
            assertEquals("ing-mustard", parents.single().parentIngredientId)
            assertEquals("DERIVED_FROM", parents.single().relationType)

            val metadata = requireNotNull(dao.getMetadata(CatalogImporter.METADATA_KEY))
            assertEquals(7, metadata.catalogVersion)
            assertEquals("EU-ES", metadata.jurisdiction)
        } finally {
            database.close()
        }
    }
}
