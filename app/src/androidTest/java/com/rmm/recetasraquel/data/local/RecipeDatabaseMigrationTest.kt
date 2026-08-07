package com.rmm.recetasraquel.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RecipeDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RecipeDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2PreservesLegacyRecipeDataAndCreatesCustomIngredientOrigins() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO recipes (
                    id, name, description, category, servings, preparationMinutes,
                    cookingMinutes, notes, isFavorite, coverPhotoPath, createdAt, updatedAt
                ) VALUES (
                    'recipe-1', 'Tarta de queso', 'Demo', 'Postres', 8, 15,
                    45, 'Nota receta', 1, 'recipe_photos/recipe-1/cover.jpg', 100, 200
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO ingredients (
                    id, recipeId, quantity, unit, name, notes, sortOrder
                ) VALUES (
                    'ingredient-1', 'recipe-1', '1/2', 'kg', 'Queso crema', 'A temperatura ambiente', 0
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO ingredients (
                    id, recipeId, quantity, unit, name, notes, sortOrder
                ) VALUES (
                    'ingredient-2', 'recipe-1', '125', 'g', 'Azúcar', NULL, 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO recipe_steps (
                    id, recipeId, instruction, timerMinutes, photoPath, sortOrder
                ) VALUES (
                    'step-1', 'recipe-1', 'Mezclar', 5, 'recipe_photos/recipe-1/step.jpg', 0
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            RecipeDatabaseMigrations.MIGRATION_1_2,
        )

        db.query(
            """
            SELECT name, description, category, servings, preparationMinutes,
                   cookingMinutes, notes, isFavorite, coverPhotoPath, createdAt, updatedAt
            FROM recipes WHERE id = 'recipe-1'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Tarta de queso", cursor.getString(0))
            assertEquals("Demo", cursor.getString(1))
            assertEquals("Postres", cursor.getString(2))
            assertEquals(8, cursor.getInt(3))
            assertEquals(15, cursor.getInt(4))
            assertEquals(45, cursor.getInt(5))
            assertEquals("Nota receta", cursor.getString(6))
            assertEquals(1, cursor.getInt(7))
            assertEquals("recipe_photos/recipe-1/cover.jpg", cursor.getString(8))
            assertEquals(100L, cursor.getLong(9))
            assertEquals(200L, cursor.getLong(10))
        }

        db.query(
            """
            SELECT id, quantity, unit, name, notes, sortOrder,
                   catalogIngredientId, customIngredientId
            FROM ingredients
            WHERE recipeId = 'recipe-1'
            ORDER BY sortOrder
            """.trimIndent(),
        ).use { cursor ->
            assertEquals(2, cursor.count)

            assertTrue(cursor.moveToFirst())
            assertEquals("ingredient-1", cursor.getString(0))
            assertEquals("1/2", cursor.getString(1))
            assertEquals("kg", cursor.getString(2))
            assertEquals("Queso crema", cursor.getString(3))
            assertEquals("A temperatura ambiente", cursor.getString(4))
            assertEquals(0, cursor.getInt(5))
            assertTrue(cursor.isNull(6))
            assertEquals("legacy:ingredient-1", cursor.getString(7))

            assertTrue(cursor.moveToNext())
            assertEquals("ingredient-2", cursor.getString(0))
            assertEquals("125", cursor.getString(1))
            assertEquals("g", cursor.getString(2))
            assertEquals("Azúcar", cursor.getString(3))
            assertTrue(cursor.isNull(4))
            assertEquals(1, cursor.getInt(5))
            assertTrue(cursor.isNull(6))
            assertEquals("legacy:ingredient-2", cursor.getString(7))
        }

        db.query(
            """
            SELECT id, name, normalizedName, defaultUnit, ingredientType,
                   compositionKnown, createdAt, updatedAt, isActive
            FROM custom_ingredients
            ORDER BY id
            """.trimIndent(),
        ).use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("legacy:ingredient-1", cursor.getString(0))
            assertEquals("Queso crema", cursor.getString(1))
            assertEquals("queso crema", cursor.getString(2))
            assertEquals("kg", cursor.getString(3))
            assertEquals("LEGACY_UNCLASSIFIED", cursor.getString(4))
            assertEquals(0, cursor.getInt(5))
            assertEquals(100L, cursor.getLong(6))
            assertEquals(200L, cursor.getLong(7))
            assertEquals(1, cursor.getInt(8))
        }

        db.query(
            """
            SELECT instruction, timerMinutes, photoPath, sortOrder
            FROM recipe_steps WHERE id = 'step-1'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Mezclar", cursor.getString(0))
            assertEquals(5, cursor.getInt(1))
            assertEquals("recipe_photos/recipe-1/step.jpg", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
        }

        db.query("PRAGMA foreign_key_check").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }

        db.close()
    }

    private companion object {
        const val TEST_DB = "recipes-migration-1-2-test.db"
    }
}
