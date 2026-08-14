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
class IngredientCompositionMigration67Test {
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
    fun migrate6To7DefaultsCoverageAndCreatesIndependentComponentTable() {
        createVersion6DatabaseWithCatalogIngredient()

        val database = Room.databaseBuilder(context, RecipeDatabase::class.java, TEST_DB)
            .addMigrations(IngredientCompositionMigrations.MIGRATION_6_7)
            .build()

        try {
            val db = database.openHelper.writableDatabase
            db.query(
                "SELECT id, catalogRole, compositionCoverage FROM catalog_ingredients WHERE id = 'ingredient-v6'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ingredient-v6", cursor.getString(0))
                assertEquals("CULINARY", cursor.getString(1))
                assertEquals("NONE", cursor.getString(2))
            }

            db.query("PRAGMA table_info(`catalog_ingredient_components`)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns += cursor.getString(1)
                assertEquals(
                    setOf(
                        "id",
                        "parentIngredientId",
                        "componentIngredientId",
                        "presenceType",
                        "reviewedAt",
                        "sourceReference",
                        "notes",
                        "isActive",
                    ),
                    columns,
                )
            }

            db.execSQL(
                """
                INSERT INTO catalog_ingredient_components (
                    id, parentIngredientId, componentIngredientId, presenceType,
                    reviewedAt, sourceReference, notes, isActive
                ) VALUES (
                    'component-v7', 'ingredient-v6', 'ingredient-v6-child', 'REQUIRED',
                    '2026-08-12', 'TEST', NULL, 1
                )
                """.trimIndent(),
            )
            db.query("SELECT presenceType FROM catalog_ingredient_components WHERE id = 'component-v7'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("REQUIRED", cursor.getString(0))
            }

            db.query("PRAGMA foreign_key_check").use { cursor -> assertFalse(cursor.moveToFirst()) }
            db.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(7, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion6DatabaseWithCatalogIngredient() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion1Schema(db)
                        RecipeDatabaseMigrations.MIGRATION_1_2.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_2_3.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_3_4.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_4_5.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_5_6.migrate(db)
                        seedCatalogIngredients(db)
                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                        db.execSQL(
                            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '7371cdda6f45ff9d90227b3b3b2d581d')",
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper -> helper.writableDatabase }
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

    private fun seedCatalogIngredients(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO ingredient_categories (id, code, name, sortOrder, iconKey, isActive)
            VALUES ('category-v6', 'V6', 'Categoría v6', 0, NULL, 1)
            """.trimIndent(),
        )
        listOf("ingredient-v6", "ingredient-v6-child").forEach { id ->
            db.execSQL(
                """
                INSERT INTO catalog_ingredients (
                    id, canonicalName, normalizedName, categoryId, defaultUnit, description,
                    catalogVersion, verificationStatus, compositionVariability, sourceUpdatedAt,
                    isActive, catalogRole
                ) VALUES (
                    '$id', '$id', '$id', 'category-v6', 'g', NULL,
                    13, 'VERIFIED', 'STABLE', NULL, 1, 'CULINARY'
                )
                """.trimIndent(),
            )
        }
    }

    private companion object {
        const val TEST_DB = "ingredient-composition-migration-6-7-test.db"
    }
}
