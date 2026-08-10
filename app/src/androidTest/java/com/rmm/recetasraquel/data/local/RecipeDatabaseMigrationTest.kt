package com.rmm.recetasraquel.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate1To5PreservesLegacyRecipeDataAndAddsVersionedRegulatoryInfrastructure() = runBlocking {
        createVersion1Database()

        val database = Room.databaseBuilder(context, RecipeDatabase::class.java, TEST_DB)
            .addMigrations(
                RecipeDatabaseMigrations.MIGRATION_1_2,
                IngredientLibraryMigrations.MIGRATION_2_3,
                IngredientLibraryMigrations.MIGRATION_3_4,
                IngredientLibraryMigrations.MIGRATION_4_5,
            )
            .build()

        try {
            val migrated = database.openHelper.writableDatabase

            migrated.query(
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

            migrated.query(
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

            migrated.query(
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

            migrated.query("SELECT COUNT(*) FROM catalog_ingredient_relations").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            migrated.query("SELECT COUNT(*) FROM regulatory_exemptions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            migrated.query("PRAGMA table_info(`regulatory_exemptions`)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns += cursor.getString(1)
                assertTrue("catalogVersion" in columns)
                assertTrue("ingredientId" in columns)
                assertTrue("safetyGroupId" in columns)
                assertTrue("jurisdiction" in columns)
                assertTrue("regulatoryEffect" in columns)
                assertTrue("conditions" in columns)
                assertTrue("sourceId" in columns)
                assertTrue("effectiveFrom" in columns)
                assertTrue("effectiveTo" in columns)
                assertTrue("reviewedAt" in columns)
                assertTrue("isActive" in columns)
            }

            migrated.query("PRAGMA table_info(`custom_ingredient_safety_relations`)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns += cursor.getString(1)
                assertTrue("sourceId" in columns)
                assertTrue("sourceDetails" in columns)
                assertFalse("sourceDescription" in columns)
            }

            migrated.query(
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

            migrated.query("PRAGMA foreign_key_check").use { cursor ->
                assertFalse(cursor.moveToFirst())
            }

            migrated.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(5, cursor.getInt(0))
            }

            val recipe = database.recipeDao().getRecipeWithDetails("recipe-1")
            assertNotNull(recipe)
            requireNotNull(recipe)
            assertEquals(2, recipe.ingredients.size)
            assertEquals("legacy:ingredient-1", recipe.ingredients[0].customIngredientId)
            assertEquals("legacy:ingredient-2", recipe.ingredients[1].customIngredientId)
        } finally {
            database.close()
        }
    }

    private fun createVersion1Database() {
        val dbFile = context.getDatabasePath(TEST_DB)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recipes` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT,
                    `category` TEXT,
                    `servings` INTEGER,
                    `preparationMinutes` INTEGER,
                    `cookingMinutes` INTEGER,
                    `notes` TEXT,
                    `isFavorite` INTEGER NOT NULL,
                    `coverPhotoPath` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ingredients` (
                    `id` TEXT NOT NULL,
                    `recipeId` TEXT NOT NULL,
                    `quantity` TEXT,
                    `unit` TEXT,
                    `name` TEXT NOT NULL,
                    `notes` TEXT,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId_sortOrder` ON `ingredients` (`recipeId`, `sortOrder`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recipe_steps` (
                    `id` TEXT NOT NULL,
                    `recipeId` TEXT NOT NULL,
                    `instruction` TEXT NOT NULL,
                    `timerMinutes` INTEGER,
                    `photoPath` TEXT,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recipe_steps_recipeId_sortOrder` ON `recipe_steps` (`recipeId`, `sortOrder`)",
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, 'ecb081d644ae9043925c1afc4b24a70c')",
            )

            db.execSQL(
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
            db.execSQL(
                """
                INSERT INTO ingredients (
                    id, recipeId, quantity, unit, name, notes, sortOrder
                ) VALUES (
                    'ingredient-1', 'recipe-1', '1/2', 'kg', 'Queso crema', 'A temperatura ambiente', 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO ingredients (
                    id, recipeId, quantity, unit, name, notes, sortOrder
                ) VALUES (
                    'ingredient-2', 'recipe-1', '125', 'g', 'Azúcar', NULL, 1
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO recipe_steps (
                    id, recipeId, instruction, timerMinutes, photoPath, sortOrder
                ) VALUES (
                    'step-1', 'recipe-1', 'Mezclar', 5, 'recipe_photos/recipe-1/step.jpg', 0
                )
                """.trimIndent(),
            )

            db.version = 1
        }
    }

    private companion object {
        const val TEST_DB = "recipes-migration-1-5-test.db"
    }
}
