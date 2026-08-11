package com.rmm.recetasraquel.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.repository.LocalRecipeRepository
import com.rmm.recetasraquel.domain.model.IngredientDraft
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeOriginPersistenceTest {
    private lateinit var context: Context
    private lateinit var database: RecipeDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun legacyFreeTextEditorCreatesAndUpdatesRecipeWithExplicitCustomOrigin() = runBlocking {
        val ids = ArrayDeque(listOf("recipe-1", "ingredient-1"))
        var now = 100L
        val repository = LocalRecipeRepository(
            dao = database.recipeDao(),
            idGenerator = IdGenerator { ids.removeFirst() },
            timeProvider = TimeProvider { now },
        )

        val create = repository.createRecipe(
            RecipeDraft(
                name = "Receta libre",
                ingredients = listOf(
                    IngredientDraft(
                        quantity = "250",
                        unit = "g",
                        name = "Ingrediente casero",
                    ),
                ),
            ),
        )

        assertTrue(create.isSuccess)
        assertEquals("recipe-1", create.getOrNull())

        val created = repository.getRecipe("recipe-1")
        assertNotNull(created)
        val createdIngredient = requireNotNull(created).ingredients.single()
        assertEquals("recipe-custom:ingredient-1", createdIngredient.customIngredientId)
        assertEquals(null, createdIngredient.catalogIngredientId)

        database.openHelper.writableDatabase.query(
            """
            SELECT name, normalizedName, ingredientType, compositionKnown
            FROM custom_ingredients
            WHERE id = 'recipe-custom:ingredient-1'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Ingrediente casero", cursor.getString(0))
            assertEquals("ingrediente casero", cursor.getString(1))
            assertEquals("RECIPE_FREE_TEXT_COMPAT", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
        }

        now = 200L
        val update = repository.updateRecipeFromDraft(
            recipeId = "recipe-1",
            draft = RecipeDraft(
                id = "recipe-1",
                name = "Receta libre actualizada",
                ingredients = listOf(
                    IngredientDraft(
                        id = "ingredient-1",
                        quantity = "300",
                        unit = "g",
                        name = "Ingrediente casero actualizado",
                    ),
                ),
            ),
        )

        assertTrue(update.isSuccess)
        val updated = requireNotNull(repository.getRecipe("recipe-1"))
        val updatedIngredient = updated.ingredients.single()
        assertEquals("recipe-custom:ingredient-1", updatedIngredient.customIngredientId)
        assertEquals("Ingrediente casero actualizado", updatedIngredient.name)

        database.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { cursor ->
            assertTrue(!cursor.moveToFirst())
        }
    }
}
