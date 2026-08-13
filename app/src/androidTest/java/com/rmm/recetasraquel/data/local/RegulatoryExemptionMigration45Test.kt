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
class RegulatoryExemptionMigration45Test {
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
    fun migrate4To6PreservesExistingExemptionAsCatalogSnapshot() {
        createVersion4DatabaseWithRegulatoryExemption()

        val database = Room.databaseBuilder(context, RecipeDatabase::class.java, TEST_DB)
            .addMigrations(
                IngredientLibraryMigrations.MIGRATION_4_5,
                IngredientLibraryMigrations.MIGRATION_5_6,\n                IngredientCompositionMigrations.MIGRATION_6_7,
            )
            .build()

        try {
            val db = database.openHelper.writableDatabase

            db.query(
                """
                SELECT id, catalogVersion, ingredientId, safetyGroupId, jurisdiction,
                       regulatoryEffect, conditions, sourceId, effectiveFrom, effectiveTo,
                       reviewedAt, notes, isActive
                FROM regulatory_exemptions
                WHERE id = 'exemption-v4'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("exemption-v4", cursor.getString(0))
                assertEquals(10, cursor.getInt(1))
                assertEquals("ingredient-v4", cursor.getString(2))
                assertEquals("group-v4", cursor.getString(3))
                assertEquals("EU-ES", cursor.getString(4))
                assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", cursor.getString(5))
                assertEquals("Condición histórica v4", cursor.getString(6))
                assertEquals("source-v4", cursor.getString(7))
                assertEquals("2025-04-01", cursor.getString(8))
                assertTrue(cursor.isNull(9))
                assertEquals("2026-08-08", cursor.getString(10))
                assertEquals("No equivale a seguridad clínica", cursor.getString(11))
                assertEquals(1, cursor.getInt(12))
            }

            db.query("PRAGMA table_info(`regulatory_exemptions`)").use { cursor ->
                val primaryKeyPositions = mutableMapOf<String, Int>()
                while (cursor.moveToNext()) {
                    primaryKeyPositions[cursor.getString(1)] = cursor.getInt(5)
                }
                assertEquals(1, primaryKeyPositions["id"])
                assertEquals(2, primaryKeyPositions["catalogVersion"])
            }

            db.query("SELECT catalogRole FROM catalog_ingredients WHERE id = 'ingredient-v4'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("CULINARY", cursor.getString(0))
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

    private fun createVersion4DatabaseWithRegulatoryExemption() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion1Schema(db)
                        RecipeDatabaseMigrations.MIGRATION_1_2.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_2_3.migrate(db)
                        IngredientLibraryMigrations.MIGRATION_3_4.migrate(db)
                        seedRegulatoryData(db)

                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                        db.execSQL(
                            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '753409f86da2c3d13c52bb4625b1fb6d')",
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

    private fun seedRegulatoryData(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO ingredient_categories (id, code, name, sortOrder, iconKey, isActive)
            VALUES ('category-v4', 'V4', 'Categoría v4', 0, NULL, 1)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO catalog_ingredients (
                id, canonicalName, normalizedName, categoryId, defaultUnit, description,
                catalogVersion, verificationStatus, compositionVariability, sourceUpdatedAt, isActive
            ) VALUES (
                'ingredient-v4', 'Ingrediente v4', 'ingrediente v4', 'category-v4', NULL, NULL,
                10, 'REVIEWED', 'LOW', NULL, 1
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO food_safety_groups (
                id, code, displayName, conditionType, regulatoryStatus, jurisdiction, description, isActive
            ) VALUES (
                'group-v4', 'GROUP_V4', 'Grupo v4', 'ALLERGY', 'EU14', 'EU-ES', NULL, 1
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO safety_sources (
                id, organization, title, officialReference, jurisdiction,
                publicationDate, reviewDate, documentStatus, officialUrl
            ) VALUES (
                'source-v4', 'UE', 'Fuente v4', 'REF-V4', 'EU-ES',
                NULL, '2026-08-08', 'ACTIVE', NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO catalog_metadata (`key`, catalogVersion, locale, jurisdiction, reviewedAt, importedAt)
            VALUES ('master', 10, 'es-ES', 'EU-ES', '2026-08-08', 1)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO regulatory_exemptions (
                id, ingredientId, safetyGroupId, jurisdiction, regulatoryEffect, conditions,
                sourceId, effectiveFrom, effectiveTo, reviewedAt, notes, isActive
            ) VALUES (
                'exemption-v4', 'ingredient-v4', 'group-v4', 'EU-ES',
                'EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION', 'Condición histórica v4',
                'source-v4', '2025-04-01', NULL, '2026-08-08',
                'No equivale a seguridad clínica', 1
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DB = "regulatory-exemption-migration-4-6-test.db"
    }
}
