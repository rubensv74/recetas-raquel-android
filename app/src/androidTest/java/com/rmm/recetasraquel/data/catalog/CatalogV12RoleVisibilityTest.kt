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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Contract gate: technical regulatory identities stay addressable internally but never enter normal culinary search.
@RunWith(AndroidJUnit4::class)
class CatalogV12RoleVisibilityTest {
    @Test
    fun technicalRegulatoryIdentitiesStayInternalWhileCulinaryExemptionsRemainSearchable() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val bundle = reader.read()
            val technicalRecords = bundle.ingredients.filter { it.catalogRole == "REGULATORY_TECHNICAL" }

            assertEquals(12, bundle.manifest.catalogVersion)
            assertEquals(TECHNICAL_IDENTITIES.keys, technicalRecords.map { it.id }.toSet())

            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1300L },
            )
            val repository = LocalIngredientCatalogRepository(importer, database.ingredientCatalogDao())
            val importResult = repository.ensureCatalogImported().getOrThrow()
            assertEquals(12, importResult.catalogVersion)

            TECHNICAL_IDENTITIES.forEach { (id, canonicalName) ->
                assertNotNull(repository.getIngredient(id).getOrThrow())
                val searchResults = repository.searchIngredients(query = canonicalName).getOrThrow()
                assertFalse(searchResults.any { it.id == id })
            }

            val lactitolResults = repository.searchIngredients(query = "Lactitol").getOrThrow()
            assertTrue(lactitolResults.any { it.id == "ing-lactitol" })

            val wheatGlucoseResults = repository.searchIngredients(
                query = "Jarabe de glucosa a base de trigo",
            ).getOrThrow()
            assertTrue(wheatGlucoseResults.any { it.id == "ing-wheat-glucose-syrup" })
        } finally {
            database.close()
        }
    }

    private companion object {
        val TECHNICAL_IDENTITIES = mapOf(
            "ing-gluten-cereals-alcoholic-distillates" to
                "Cereales con gluten utilizados para hacer destilados alcohólicos",
            "ing-fish-gelatine-vitamin-carotenoid-carrier" to
                "Gelatina de pescado utilizada como soporte de vitaminas o carotenoides",
            "ing-fish-gelatine-isinglass-beer-wine-fining" to
                "Gelatina de pescado o ictiocola utilizada como clarificante en cerveza y vino",
            "ing-whey-alcoholic-distillates" to
                "Lactosuero utilizado para hacer destilados alcohólicos",
            "ing-nuts-alcoholic-distillates" to
                "Frutos de cáscara utilizados para hacer destilados alcohólicos",
        )
    }
}
