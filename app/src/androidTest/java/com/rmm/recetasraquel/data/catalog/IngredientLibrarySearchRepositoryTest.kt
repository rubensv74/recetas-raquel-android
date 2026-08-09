package com.rmm.recetasraquel.data.catalog

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.repository.LocalIngredientCatalogRepository
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogInformationStatus
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngredientLibrarySearchRepositoryTest {
    @Test
    fun searchUsesCanonicalRankingCategoryFilterAndNoFuzzyIdentity() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1200L },
            )
            val repository = LocalIngredientCatalogRepository(importer, database.ingredientCatalogDao())

            val categories = repository.getCategories().getOrThrow()
            assertEquals(20, categories.size)

            val wheatResults = repository.searchIngredients(query = "Trigo").getOrThrow()
            assertTrue(wheatResults.isNotEmpty())
            assertEquals("ing-wheat", wheatResults.first().id)
            assertEquals("Trigo", wheatResults.first().canonicalName)
            assertEquals(
                IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED,
                wheatResults.first().informationStatus,
            )

            val cerealOnly = repository.searchIngredients(
                query = "trigo",
                categoryId = "cat-cereals-flours",
            ).getOrThrow()
            assertTrue(cerealOnly.isNotEmpty())
            assertTrue(cerealOnly.all { it.categoryId == "cat-cereals-flours" })

            val exemptTechnicalIngredient = repository.getIngredient("ing-wheat-glucose-syrup").getOrThrow()
            requireNotNull(exemptTechnicalIngredient)
            assertEquals(
                IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED,
                exemptTechnicalIngredient.informationStatus,
            )

            val exactTechnicalSearch = repository.searchIngredients(
                query = "Jarabe de glucosa a base de trigo",
            ).getOrThrow()
            assertEquals("ing-wheat-glucose-syrup", exactTechnicalSearch.first().id)

            val accentInsensitive = repository.searchIngredients(query = "sesamo").getOrThrow()
            assertTrue(accentInsensitive.any { it.id == "ing-sesame" })

            val fuzzyTypo = repository.searchIngredients(query = "trigp").getOrThrow()
            assertFalse(fuzzyTypo.any { it.id == "ing-wheat" })
        } finally {
            database.close()
        }
    }
}
