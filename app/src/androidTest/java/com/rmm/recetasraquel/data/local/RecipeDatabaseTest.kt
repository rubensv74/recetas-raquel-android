package com.rmm.recetasraquel.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.entity.IngredientEntity
import com.rmm.recetasraquel.data.local.entity.RecipeEntity
import com.rmm.recetasraquel.data.local.entity.RecipeStepEntity
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toDomain
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: RecipeDatabase

    @Before
    fun createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun createsAndRetrievesRecipeWithAllFields() = runBlocking {
        val recipe = recipeEntity(
            description = "Descripción",
            category = "Cena",
            servings = 2,
            preparationMinutes = 10,
            cookingMinutes = 20,
            notes = "Notas",
            coverPhotoPath = "cover.jpg",
        )
        val ingredients = listOf(ingredientEntity("i1", "1/2", "kg", "Patatas", "Nuevas", 0))
        val steps = listOf(stepEntity("s1", "Cocinar", 20, "step.jpg", 0))

        database.recipeDao().saveRecipeWithDetails(recipe, ingredients, steps)
        val saved = database.recipeDao().getRecipeWithDetails(RECIPE_ID)

        requireNotNull(saved)
        assertEquals(recipe, saved.recipe)
        assertEquals(ingredients, saved.ingredients)
        assertEquals(steps, saved.steps)
    }

    @Test
    fun mapperReturnsIngredientsAndStepsInSortOrder() = runBlocking {
        database.recipeDao().saveRecipeWithDetails(
            recipeEntity(),
            listOf(
                ingredientEntity("i2", name = "Segundo", sortOrder = 1),
                ingredientEntity("i1", name = "Primero", sortOrder = 0),
            ),
            listOf(
                stepEntity("s2", instruction = "Segundo", sortOrder = 1),
                stepEntity("s1", instruction = "Primero", sortOrder = 0),
            ),
        )

        val domain = requireNotNull(database.recipeDao().getRecipeWithDetails(RECIPE_ID)).toDomain()
        assertEquals(listOf("i1", "i2"), domain.ingredients.map { it.id })
        assertEquals(listOf("s1", "s2"), domain.steps.map { it.id })
    }

    @Test
    fun transactionalUpdateReplacesChildrenWithoutDuplicates() = runBlocking {
        val dao = database.recipeDao()
        dao.saveRecipeWithDetails(
            recipeEntity(),
            listOf(ingredientEntity("old-i")),
            listOf(stepEntity("old-s")),
        )

        dao.saveRecipeWithDetails(
            recipeEntity(name = "Actualizada", updatedAt = 200L),
            listOf(ingredientEntity("new-i")),
            listOf(stepEntity("new-s")),
        )

        val saved = requireNotNull(dao.getRecipeWithDetails(RECIPE_ID))
        assertEquals("Actualizada", saved.recipe.name)
        assertEquals(listOf("new-i"), saved.ingredients.map { it.id })
        assertEquals(listOf("new-s"), saved.steps.map { it.id })
    }

    @Test
    fun deletingRecipeCascadesToIngredientsAndSteps() = runBlocking {
        val dao = database.recipeDao()
        dao.saveRecipeWithDetails(
            recipeEntity(),
            listOf(ingredientEntity("i1")),
            listOf(stepEntity("s1")),
        )

        assertEquals(1, dao.deleteRecipe(RECIPE_ID))

        assertNull(dao.getRecipeWithDetails(RECIPE_ID))
        assertEquals(emptyList<IngredientEntity>(), dao.getIngredients(RECIPE_ID))
        assertEquals(emptyList<RecipeStepEntity>(), dao.getSteps(RECIPE_ID))
    }

    @Test
    fun favoriteUpdatePreservesChildrenAndCreatedAt() = runBlocking {
        val dao = database.recipeDao()
        val ingredients = listOf(ingredientEntity("i1"))
        val steps = listOf(stepEntity("s1"))
        dao.saveRecipeWithDetails(recipeEntity(), ingredients, steps)

        assertEquals(1, dao.updateFavorite(RECIPE_ID, true, 300L))

        val saved = requireNotNull(dao.getRecipeWithDetails(RECIPE_ID))
        assertEquals(true, saved.recipe.isFavorite)
        assertEquals(100L, saved.recipe.createdAt)
        assertEquals(300L, saved.recipe.updatedAt)
        assertEquals(ingredients, saved.ingredients)
        assertEquals(steps, saved.steps)
    }

    @Test
    fun fileDatabasePersistsAfterCloseAndReopen() = runBlocking {
        database.close()
        val databaseName = "recipes-persistence-test.db"
        context.deleteDatabase(databaseName)
        var fileDatabase = Room.databaseBuilder(context, RecipeDatabase::class.java, databaseName).build()
        try {
            fileDatabase.recipeDao().saveRecipeWithDetails(
                recipeEntity(),
                listOf(ingredientEntity("i1")),
                listOf(stepEntity("s1")),
            )
            fileDatabase.close()

            fileDatabase = Room.databaseBuilder(context, RecipeDatabase::class.java, databaseName).build()
            val restored = fileDatabase.recipeDao().getRecipeWithDetails(RECIPE_ID)
            requireNotNull(restored)
            assertEquals("Receta", restored.recipe.name)
            assertEquals("i1", restored.ingredients.single().id)
            assertEquals("s1", restored.steps.single().id)
            assertFalse(restored.recipe.isFavorite)
        } finally {
            fileDatabase.close()
            context.deleteDatabase(databaseName)
            database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        }
    }

    @Test
    fun catalogSearchesNameCategoryAndIngredientWithoutDuplicates() = runBlocking {
        val dao = database.recipeDao()
        dao.saveRecipeWithDetails(
            recipeEntity(id = "tortilla", name = "Tortilla de patatas", category = "Principal", updatedAt = 300),
            listOf(
                ingredientEntity("potato-1", name = "Patata roja", recipeId = "tortilla"),
                ingredientEntity("potato-2", name = "Patata blanca", recipeId = "tortilla", sortOrder = 1),
            ),
            emptyList(),
        )
        dao.saveRecipeWithDetails(
            recipeEntity(id = "cream", name = "Crema", category = "Sopas", updatedAt = 200),
            listOf(ingredientEntity("pumpkin", name = "Calabaza", recipeId = "cream")),
            emptyList(),
        )

        assertEquals(listOf("tortilla"), dao.observeCatalog("tilla", false, null).first().map { it.id })
        assertEquals(listOf("cream"), dao.observeCatalog("calabaza", false, null).first().map { it.id })
        assertEquals(listOf("cream"), dao.observeCatalog("sop", false, null).first().map { it.id })
        assertEquals(listOf("tortilla"), dao.observeCatalog("patata", false, null).first().map { it.id })
    }

    @Test
    fun catalogCombinesFavoritesCategoryAndQueryAndOrdersByUpdatedAt() = runBlocking {
        val dao = database.recipeDao()
        dao.saveRecipeWithDetails(
            recipeEntity(id = "older", name = "Arroz antiguo", category = "Principal", isFavorite = true, updatedAt = 100),
            emptyList(), emptyList(),
        )
        dao.saveRecipeWithDetails(
            recipeEntity(id = "newer", name = "Arroz nuevo", category = "Principal", isFavorite = true, updatedAt = 300),
            emptyList(), emptyList(),
        )
        dao.saveRecipeWithDetails(
            recipeEntity(id = "other", name = "Arroz dulce", category = "Postres", isFavorite = true, updatedAt = 400),
            emptyList(), emptyList(),
        )
        dao.saveRecipeWithDetails(
            recipeEntity(id = "not-favorite", name = "Arroz blanco", category = "Principal", updatedAt = 500),
            emptyList(), emptyList(),
        )

        val result = dao.observeCatalog("arroz", true, "Principal").first()
        assertEquals(listOf("newer", "older"), result.map { it.id })
    }

    @Test
    fun categoriesExcludeNullBlankAndDuplicatesAndAreSorted() = runBlocking {
        val dao = database.recipeDao()
        listOf(null, " ", "Sopas", "Postres", "Sopas").forEachIndexed { index, category ->
            dao.saveRecipeWithDetails(
                recipeEntity(id = "r$index", category = category), emptyList(), emptyList(),
            )
        }

        assertEquals(listOf("Postres", "Sopas"), dao.observeCategories().first())
    }

    private fun recipeEntity(
        id: String = RECIPE_ID,
        name: String = "Receta",
        description: String? = null,
        category: String? = null,
        servings: Int? = null,
        preparationMinutes: Int? = null,
        cookingMinutes: Int? = null,
        notes: String? = null,
        coverPhotoPath: String? = null,
        isFavorite: Boolean = false,
        updatedAt: Long = 100L,
    ) = RecipeEntity(
        id, name, description, category, servings, preparationMinutes, cookingMinutes,
        notes, isFavorite, coverPhotoPath, 100L, updatedAt,
    )

    private fun ingredientEntity(
        id: String,
        quantity: String? = null,
        unit: String? = null,
        name: String = "Ingrediente",
        notes: String? = null,
        sortOrder: Int = 0,
        recipeId: String = RECIPE_ID,
    ) = IngredientEntity(id, recipeId, quantity, unit, name, notes, sortOrder)

    private fun stepEntity(
        id: String,
        instruction: String = "Paso",
        timerMinutes: Int? = null,
        photoPath: String? = null,
        sortOrder: Int = 0,
    ) = RecipeStepEntity(id, RECIPE_ID, instruction, timerMinutes, photoPath, sortOrder)

    private companion object {
        const val RECIPE_ID = "recipe-id"
    }
}
