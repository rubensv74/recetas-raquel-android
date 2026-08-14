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
class IngredientLibraryMigration23Test {
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
    fun migrate2To6PreservesCustomSafetyRelationAndCreatesSourceBackedContract() {
        createVersion2DatabaseWithCustomSafetyRelation()

        val database = Room.databaseBuilder(context, RecipeDatabase::class.java, TEST_DB)
            .addMigrations(
                IngredientLibraryMigrations.MIGRATION_2_3,
                IngredientLibraryMigrations.MIGRATION_3_4,
                IngredientLibraryMigrations.MIGRATION_4_5,
                IngredientLibraryMigrations.MIGRATION_5_6,
                IngredientCompositionMigrations.MIGRATION_6_7,
            )
            .build()

        try {
            val db = database.openHelper.writableDatabase

            db.query(
                """
                SELECT sourceId, sourceDetails, notes, reviewedAt
                FROM custom_ingredient_safety_relations
                WHERE id = 'custom-rel-1'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(IngredientLibraryMigrations.MIGRATED_CUSTOM_SAFETY_SOURCE_ID, cursor.getString(0))
                assertEquals("Etiqueta revisada manualmente", cursor.getString(1))
                assertEquals("Conservar detalle histórico", cursor.getString(2))
                assertEquals("2026-08-08", cursor.getString(3))
            }

            db.query(
                "SELECT officialReference, documentStatus FROM safety_sources WHERE id = ?",
                arrayOf(IngredientLibraryMigrations.MIGRATED_CUSTOM_SAFETY_SOURCE_ID),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("LOCAL_USER_DECLARED_MIGRATED_V2", cursor.getString(0))
                assertEquals("LOCAL_MIGRATION", cursor.getString(1))
            }

            db.query("SELECT COUNT(*) FROM catalog_ingredient_relations").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            db.query("SELECT COUNT(*) FROM regulatory_exemptions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            db.query("PRAGMA table_info(`regulatory_exemptions`)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns += cursor.getString(1)
                assertTrue("catalogVersion" in columns)
            }

            db.query("PRAGMA table_info(`catalog_ingredients`)").use { cursor ->
                var catalogRoleFound = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == "catalogRole") {
                        catalogRoleFound = true
                        assertEquals("TEXT", cursor.getString(2))
                        assertEquals(1, cursor.getInt(3))
                        assertEquals("'CULINARY'", cursor.getString(4))
                    }
                }
                assertTrue(catalogRoleFound)
            }

            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertFalse(cursor.moveToFirst())
            }

            db.query("PRAGMA user_version").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(7, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion2DatabaseWithCustomSafetyRelation() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion1Schema(db)
                        seedVersion1Recipe(db)
                        RecipeDatabaseMigrations.MIGRATION_1_2.migrate(db)

                        db.execSQL(
                            """
                            INSERT INTO food_safety_groups (
                                id, code, displayName, conditionType, regulatoryStatus,
                                jurisdiction, description, isActive
                            ) VALUES (
                                'group-user', 'USER_TEST', 'Grupo prueba', 'FOOD_ALLERGY',
                                'USER_DECLARED', 'LOCAL', NULL, 1
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            INSERT INTO custom_ingredient_safety_relations (
                                id, customIngredientId, safetyGroupId, relationType, evidenceLevel,
                                sourceDescription, notes, reviewedAt
                            ) VALUES (
                                'custom-rel-1', 'legacy:ingredient-1', 'group-user', 'CONTAINS',
                                'USER_DECLARED', 'Etiqueta revisada manualmente',
                                'Conservar detalle histórico', '2026-08-08'
                            )
                            """.trimIndent(),
                        )

                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                        db.execSQL(
                            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '9f2d86b1dfe78cbad8d67f81358c4c9a')",
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

    private fun seedVersion1Recipe(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO recipes (
                id, name, description, category, servings, preparationMinutes,
                cookingMinutes, notes, isFavorite, coverPhotoPath, createdAt, updatedAt
            ) VALUES (
                'recipe-1', 'Prueba', NULL, NULL, NULL, NULL,
                NULL, NULL, 0, NULL, 100, 200
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO ingredients (
                id, recipeId, quantity, unit, name, notes, sortOrder
            ) VALUES (
                'ingredient-1', 'recipe-1', '1', 'ud', 'Ingrediente personalizado', NULL, 0
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DB = "recipes-migration-2-6-test.db"
    }
}
