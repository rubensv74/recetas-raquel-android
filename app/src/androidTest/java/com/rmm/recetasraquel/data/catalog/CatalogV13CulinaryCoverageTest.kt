package com.rmm.recetasraquel.data.catalog

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.repository.LocalIngredientCatalogRepository
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogV13CulinaryCoverageTest {
    @Test
    fun v13ExpandsEverydayCoverageWithoutInventingSafetyRelations() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val bundle = reader.read("ingredient-catalog/v13")
            val validation = CatalogValidator.validate(bundle)

            assertTrue(validation.errors.joinToString(separator = "\n"), validation.isValid)
            assertEquals(4, bundle.manifest.schemaVersion)
            assertEquals(13, bundle.manifest.catalogVersion)
            assertEquals("DRAFT", bundle.manifest.releaseStatus)
            assertEquals(20, bundle.categories.size)
            assertEquals(554, bundle.ingredients.size)
            assertEquals(335, bundle.aliases.size)
            assertEquals(143, bundle.ingredientRelations.size)
            assertEquals(14, bundle.safetyGroups.size)
            assertEquals(3, bundle.safetySources.size)
            assertEquals(133, bundle.safetyRelations.size)
            assertEquals(20, bundle.regulatoryExemptions.size)

            val representativeCulinaryIds = setOf(
                "ing-extra-virgin-olive-oil",
                "ing-basmati-rice",
                "ing-honey",
                "ing-water",
                "ing-canned-chickpeas",
                "ing-beef-shank",
                "ing-mozzarella",
                "ing-quail-egg",
                "ing-salmon",
                "ing-shrimp",
                "ing-octopus",
            )
            representativeCulinaryIds.forEach { ingredientId ->
                val ingredient = bundle.ingredients.single { it.id == ingredientId }
                assertEquals("CULINARY", ingredient.catalogRole)
                assertTrue(ingredient.isActive)
            }
            assertEquals("g", bundle.ingredients.single { it.id == "ing-milk-powder" }.defaultUnit)

            val expectedSafety = mapOf(
                "ing-lactose-free-milk" to Pair("sg-eu-milk", "INHERENT_SOURCE"),
                "ing-mozzarella" to Pair("sg-eu-milk", "DERIVED_FROM"),
                "ing-quail-egg" to Pair("sg-eu-eggs", "INHERENT_SOURCE"),
                "ing-salmon" to Pair("sg-eu-fish", "INHERENT_SOURCE"),
                "ing-shrimp" to Pair("sg-eu-crustaceans", "INHERENT_SOURCE"),
                "ing-octopus" to Pair("sg-eu-molluscs", "INHERENT_SOURCE"),
            )
            expectedSafety.forEach { (ingredientId, expected) ->
                val relation = bundle.safetyRelations.single { it.ingredientId == ingredientId }
                assertEquals(expected.first, relation.safetyGroupId)
                assertEquals(expected.second, relation.relationType)
                assertEquals("EU_LEGAL", relation.evidenceLevel)
                assertEquals("EU_FIC_1169_2011", relation.sourceId)
                assertEquals("2026-08-11", relation.reviewedAt)
            }

            setOf(
                "ing-extra-virgin-olive-oil",
                "ing-basmati-rice",
                "ing-honey",
                "ing-water",
                "ing-beef-shank",
            ).forEach { ingredientId ->
                assertTrue(bundle.safetyRelations.none { it.ingredientId == ingredientId })
            }

            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1400L },
            )
            val dao = database.ingredientCatalogDao()

            val v12 = importer.ensureImported("ingredient-catalog/v12")
            assertTrue(v12 is CatalogImportResult.Imported)
            assertEquals(272, dao.countActiveIngredients())
            assertEquals(33, dao.countSafetyRelations())
            assertEquals(20, dao.countActiveRegulatoryExemptions())
            assertEquals(20, dao.countRegulatoryExemptionSnapshots())

            val v13 = importer.ensureImported("ingredient-catalog/v13")
            assertTrue(v13 is CatalogImportResult.Imported)
            assertEquals(554, dao.countActiveIngredients())
            assertEquals(143, dao.countActiveIngredientRelations())
            assertEquals(133, dao.countSafetyRelations())
            assertEquals(20, dao.countActiveRegulatoryExemptions())
            assertEquals(40, dao.countRegulatoryExemptionSnapshots())
            assertEquals(13, dao.getMetadata(CatalogImporter.METADATA_KEY)?.catalogVersion)

            val repository = LocalIngredientCatalogRepository(importer, dao)
            val aove = repository.searchIngredients("AOVE").getOrThrow()
            assertTrue(aove.any { it.id == "ing-extra-virgin-olive-oil" })

            val choco = repository.searchIngredients("Choco").getOrThrow()
            assertTrue(choco.any { it.id == "ing-cuttlefish" })

            val lactoseFree = repository.searchIngredients("Leche deslactosada").getOrThrow()
            assertTrue(lactoseFree.any { it.id == "ing-lactose-free-milk" })

            val salmonRelations = repository.getSafetyRelations("ing-salmon").getOrThrow()
            assertEquals(1, salmonRelations.size)
            assertEquals("sg-eu-fish", salmonRelations.single().safetyGroupId)

            val oilRelations = repository.getSafetyRelations("ing-extra-virgin-olive-oil").getOrThrow()
            assertTrue(oilRelations.isEmpty())

            val technicalSearch = repository.searchIngredients(
                "Gelatina de pescado o ictiocola utilizada como clarificante en cerveza y vino",
            ).getOrThrow()
            assertFalse(technicalSearch.any { it.id == "ing-fish-gelatine-isinglass-beer-wine-fining" })
        } finally {
            database.close()
        }
    }
}
