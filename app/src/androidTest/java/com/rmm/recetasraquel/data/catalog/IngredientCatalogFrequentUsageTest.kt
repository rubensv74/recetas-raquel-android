package com.rmm.recetasraquel.data.catalog

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.local.entity.IngredientEntity
import com.rmm.recetasraquel.data.local.entity.RecipeEntity
import com.rmm.recetasraquel.data.repository.LocalIngredientCatalogRepository
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngredientCatalogFrequentUsageTest {
    @Test
    fun frequentIngredientsCountDistinctRecipesAndExcludeTechnicalIdentities() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()

        try {
            val reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets))
            val importer = CatalogImporter(
                reader = reader,
                dao = database.ingredientCatalogDao(),
                timeProvider = TimeProvider { 1700L },
            )
            val repository = LocalIngredientCatalogRepository(importer, database.ingredientCatalogDao())
            assertEquals(13, repository.ensureCatalogImported().getOrThrow().catalogVersion)

            val recipeDao = database.recipeDao()
            listOf("recipe-a", "recipe-b", "recipe-c").forEachIndexed { index, recipeId ->
                recipeDao.upsertRecipe(
                    RecipeEntity(
                        id = recipeId,
                        name = "Receta ${index + 1}",
                        description = null,
                        category = null,
                        servings = null,
                        preparationMinutes = null,
                        cookingMinutes = null,
                        notes = null,
                        isFavorite = false,
                        coverPhotoPath = null,
                        createdAt = index.toLong() + 1,
                        updatedAt = index.toLong() + 1,
                    ),
                )
            }

            recipeDao.insertIngredients(
                listOf(
                    ingredient("wheat-a", "recipe-a", "ing-wheat", "Trigo", 0),
                    ingredient("milk-a", "recipe-a", "ing-milk", "Leche", 1),
                    ingredient(
                        "technical-a",
                        "recipe-a",
                        "ing-whey-alcoholic-distillates",
                        "Lactosuero utilizado para hacer destilados alcohólicos",
                        2,
                    ),
                    ingredient("wheat-b", "recipe-b", "ing-wheat", "Trigo", 0),
                    ingredient("milk-b", "recipe-b", "ing-milk", "Leche", 1),
                    ingredient(
                        "technical-b",
                        "recipe-b",
                        "ing-whey-alcoholic-distillates",
                        "Lactosuero utilizado para hacer destilados alcohólicos",
                        2,
                    ),
                    ingredient("wheat-c-1", "recipe-c", "ing-wheat", "Trigo", 0),
                    ingredient("wheat-c-2", "recipe-c", "ing-wheat", "Trigo", 1),
                    ingredient(
                        "technical-c",
                        "recipe-c",
                        "ing-whey-alcoholic-distillates",
                        "Lactosuero utilizado para hacer destilados alcohólicos",
                        2,
                    ),
                ),
            )

            val frequent = repository.getFrequentIngredients(minimumRecipeCount = 2, limit = 6).getOrThrow()

            assertEquals(listOf("ing-wheat", "ing-milk"), frequent.map { it.ingredient.id })
            assertEquals(3, frequent[0].recipeCount)
            assertEquals(2, frequent[1].recipeCount)
            assertFalse(frequent.any { it.ingredient.id == "ing-whey-alcoholic-distillates" })
        } finally {
            database.close()
        }
    }

    private fun ingredient(
        id: String,
        recipeId: String,
        catalogIngredientId: String,
        name: String,
        sortOrder: Int,
    ) = IngredientEntity(
        id = id,
        recipeId = recipeId,
        quantity = null,
        unit = null,
        name = name,
        notes = null,
        sortOrder = sortOrder,
        catalogIngredientId = catalogIngredientId,
        customIngredientId = null,
    )
}
