package com.rmm.recetasraquel.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngredientCatalogRoleMigration56Test {
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
    fun migrate5To6PreservesIngredientAndDefaultsRoleToCulinary() {
        createVersion5DatabaseWithCatalogIngredient()

        val database = Room.databaseBuilder(context, RecipeDatabase::class.java, TEST_DB)
            .addMigrations(IngredientLibraryMigrations.MIGRATION_5_6)
            .build()

        try {
            val db = database.openHelper.writableDatabase

            db.query(
                """
                SELECT id, canonicalName, normalizedName, categoryId, defaultUnit, description,
                       catalogVersion, verificationStatus, compositionVariability, sourceUpdatedAt,
                       isActive, catalogRole
                FROM catalog_ingredients
                WHERE id = 'ingredient-v5'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ingredient-v5", cursor.getString(0))
                assertEquals("Ingrediente v5", cursor.getString(1))
                assertEquals("ingrediente v5", cursor.getString(2))
                assertEquals("category-v5", cursor.getString(3))
                assertEquals("g", cursor.getString(4))
                assertEquals("Registro previo a catalogRole", cursor.getString(5))
                assertEquals(11, cursor.getInt(6))
                assertEquals("VERIFIED", cursor.getString(7))
                assertEquals("STABLE", cursor.getString(8))
                assertEquals(1234L, cursor.getLong(9))
                assertEquals(1, cursor.getInt(10))
                assertEquals("CULINARY", cursor.getString(11))
            }

            db.query("PRAGMA table_info(`catalog_ingredients`)").use { cursor ->
                var roleFound = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == "catalogRole") {
                        roleFound = true
                        assertEquals("TEXT", cursor.getString(2))
                        assertEquals(1, cursor.getInt(3))
                        assertEquals("'CULINARY'", cursor.getString(4))
                    }
                }
                assertTrue(roleFound)
            }

            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertFalse(cursor.moveToFirst())
            }

            db.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(6, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion5DatabaseWithCatalogIngredient() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion1Schema(db)
                        RecipeDatabaseMigrations.MIGRATION_1_2.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_2_3.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_3_4.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_4_5.migrate(db)
                        seedCatalogIngredient(db)

                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                        db.execSQL(
                            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, 'f032c684ca112fcab2b4eaa88acb94a2')",
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase
        }
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recipes` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `category` TEXT,
                `servings` INTEGER, `preparationMinutes` INTEGER, `cookingMinutes` INTEGER,
                `notes` TEXT, `isFavorite` INTEGER NOT NULL, `coverPhotoPath` TEXT,
                `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ingredients` (
                `id` TEXT NOT NULL, `recipeId` TEXT NOT NULL, `quantity` TEXT, `unit` TEXT,
                `name` TEXT NOT NULL, `notes` TEXT, `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId_sortOrder` ON `ingredients` (`recipeId`, `sortOrder`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recipe_steps` (
                `id` TEXT NOT NULL, `recipeId` TEXT NOT NULL, `instruction` TEXT NOT NULL,
                `timerMinutes` INTEGER, `photoPath` TEXT, `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_steps_recipeId_sortOrder` ON `recipe_steps` (`recipeId`, `sortOrder`)")
    }

    private fun seedCatalogIngredient(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO ingredient_categories (id, code, name, sortOrder, iconKey, isActive)
            VALUES ('category-v5', 'V5', 'Categoría v5', 0, NULL, 1)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO catalog_ingredients (
                id, canonicalName, normalizedName, categoryId, defaultUnit, description,
                catalogVersion, verificationStatus, compositionVariability, sourceUpdatedAt, isActive
            ) VALUES (
                'ingredient-v5', 'Ingrediente v5', 'ingrediente v5', 'category-v5', 'g',
                'Registro previo a catalogRole', 11, 'VERIFIED', 'STABLE', 1234, 1
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DB = "ingredient-catalog-role-migration-5-6-test.db"
    }
}
