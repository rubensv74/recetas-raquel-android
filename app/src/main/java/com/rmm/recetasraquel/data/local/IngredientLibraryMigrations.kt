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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createRegulatoryExemptions(db)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            versionRegulatoryExemptions(db)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `catalog_ingredients` ADD COLUMN `catalogRole` TEXT NOT NULL DEFAULT 'CULINARY'",
            )
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

    private fun createRegulatoryExemptions(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `regulatory_exemptions` (
                `id` TEXT NOT NULL,
                `ingredientId` TEXT NOT NULL,
                `safetyGroupId` TEXT NOT NULL,
                `jurisdiction` TEXT NOT NULL,
                `regulatoryEffect` TEXT NOT NULL,
                `conditions` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `effectiveFrom` TEXT,
                `effectiveTo` TEXT,
                `reviewedAt` TEXT NOT NULL,
                `notes` TEXT,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`ingredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`safetyGroupId`) REFERENCES `food_safety_groups`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`sourceId`) REFERENCES `safety_sources`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_regulatory_exemptions_ingredientId` ON `regulatory_exemptions` (`ingredientId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_regulatory_exemptions_safetyGroupId` ON `regulatory_exemptions` (`safetyGroupId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_regulatory_exemptions_sourceId` ON `regulatory_exemptions` (`sourceId`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_regulatory_exemptions_ingredientId_safetyGroupId_jurisdiction_regulatoryEffect` ON `regulatory_exemptions` (`ingredientId`, `safetyGroupId`, `jurisdiction`, `regulatoryEffect`)",
        )
    }

    private fun versionRegulatoryExemptions(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `regulatory_exemptions_new` (
                `id` TEXT NOT NULL,
                `catalogVersion` INTEGER NOT NULL,
                `ingredientId` TEXT NOT NULL,
                `safetyGroupId` TEXT NOT NULL,
                `jurisdiction` TEXT NOT NULL,
                `regulatoryEffect` TEXT NOT NULL,
                `conditions` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `effectiveFrom` TEXT,
                `effectiveTo` TEXT,
                `reviewedAt` TEXT NOT NULL,
                `notes` TEXT,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`, `catalogVersion`),
                FOREIGN KEY(`ingredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`safetyGroupId`) REFERENCES `food_safety_groups`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`sourceId`) REFERENCES `safety_sources`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `regulatory_exemptions_new` (
                `id`, `catalogVersion`, `ingredientId`, `safetyGroupId`, `jurisdiction`,
                `regulatoryEffect`, `conditions`, `sourceId`, `effectiveFrom`, `effectiveTo`,
                `reviewedAt`, `notes`, `isActive`
            )
            SELECT
                `id`,
                COALESCE((SELECT `catalogVersion` FROM `catalog_metadata` WHERE `key` = 'master' LIMIT 1), 0),
                `ingredientId`, `safetyGroupId`, `jurisdiction`, `regulatoryEffect`, `conditions`,
                `sourceId`, `effectiveFrom`, `effectiveTo`, `reviewedAt`, `notes`, `isActive`
            FROM `regulatory_exemptions`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `regulatory_exemptions`")
        db.execSQL("ALTER TABLE `regulatory_exemptions_new` RENAME TO `regulatory_exemptions`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_regulatory_exemptions_ingredientId` ON `regulatory_exemptions` (`ingredientId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_regulatory_exemptions_safetyGroupId` ON `regulatory_exemptions` (`safetyGroupId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_regulatory_exemptions_sourceId` ON `regulatory_exemptions` (`sourceId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_regulatory_exemptions_catalogVersion` ON `regulatory_exemptions` (`catalogVersion`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_regulatory_exemptions_ingredientId_safetyGroupId_jurisdiction_regulatoryEffect_catalogVersion` ON `regulatory_exemptions` (`ingredientId`, `safetyGroupId`, `jurisdiction`, `regulatoryEffect`, `catalogVersion`)",
        )
    }
}
