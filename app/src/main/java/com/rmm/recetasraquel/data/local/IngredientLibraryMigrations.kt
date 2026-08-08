package com.rmm.recetasraquel.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object IngredientLibraryMigrations {
    const val MIGRATED_CUSTOM_SAFETY_SOURCE_ID = "LOCAL_USER_DECLARED_MIGRATED_V2"

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCatalogIngredientRelations(db)
            migrateCustomSafetyRelationsToSourceIds(db)
        }
    }

    private fun createCatalogIngredientRelations(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_ingredient_relations` (
                `id` TEXT NOT NULL,
                `childIngredientId` TEXT NOT NULL,
                `parentIngredientId` TEXT NOT NULL,
                `relationType` TEXT NOT NULL,
                `reviewedAt` TEXT NOT NULL,
                `sourceReference` TEXT,
                `notes` TEXT,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`childIngredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`parentIngredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_ingredient_relations_childIngredientId` ON `catalog_ingredient_relations` (`childIngredientId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalog_ingredient_relations_parentIngredientId` ON `catalog_ingredient_relations` (`parentIngredientId`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_catalog_ingredient_relations_childIngredientId_parentIngredientId_relationType` ON `catalog_ingredient_relations` (`childIngredientId`, `parentIngredientId`, `relationType`)",
        )
    }

    private fun migrateCustomSafetyRelationsToSourceIds(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT OR IGNORE INTO `safety_sources` (
                `id`, `organization`, `title`, `officialReference`, `jurisdiction`,
                `publicationDate`, `reviewDate`, `documentStatus`, `officialUrl`
            )
            SELECT
                '$MIGRATED_CUSTOM_SAFETY_SOURCE_ID',
                'Usuario',
                'Relación personalizada migrada desde Room v2',
                'LOCAL_USER_DECLARED_MIGRATED_V2',
                'LOCAL',
                NULL,
                '2026-08-08',
                'LOCAL_MIGRATION',
                NULL
            WHERE EXISTS (SELECT 1 FROM `custom_ingredient_safety_relations`)
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE `custom_ingredient_safety_relations_new` (
                `id` TEXT NOT NULL,
                `customIngredientId` TEXT NOT NULL,
                `safetyGroupId` TEXT NOT NULL,
                `relationType` TEXT NOT NULL,
                `evidenceLevel` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `sourceDetails` TEXT,
                `notes` TEXT,
                `reviewedAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`customIngredientId`) REFERENCES `custom_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`safetyGroupId`) REFERENCES `food_safety_groups`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`sourceId`) REFERENCES `safety_sources`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `custom_ingredient_safety_relations_new` (
                `id`, `customIngredientId`, `safetyGroupId`, `relationType`, `evidenceLevel`,
                `sourceId`, `sourceDetails`, `notes`, `reviewedAt`
            )
            SELECT
                `id`, `customIngredientId`, `safetyGroupId`, `relationType`, `evidenceLevel`,
                '$MIGRATED_CUSTOM_SAFETY_SOURCE_ID', `sourceDescription`, `notes`, `reviewedAt`
            FROM `custom_ingredient_safety_relations`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `custom_ingredient_safety_relations`")
        db.execSQL("ALTER TABLE `custom_ingredient_safety_relations_new` RENAME TO `custom_ingredient_safety_relations`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_ingredient_safety_relations_customIngredientId` ON `custom_ingredient_safety_relations` (`customIngredientId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_ingredient_safety_relations_safetyGroupId` ON `custom_ingredient_safety_relations` (`safetyGroupId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_ingredient_safety_relations_sourceId` ON `custom_ingredient_safety_relations` (`sourceId`)",
        )
    }
}
