package com.rmm.recetasraquel.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RecipeDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createIngredientLibraryTables(db)
            migrateLegacyIngredientsToCustomOrigins(db)
            rebuildRecipeIngredientsWithOrigins(db)
        }
    }

    private fun createIngredientLibraryTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ingredient_categories` (
                `id` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `iconKey` TEXT,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ingredient_categories_code` ON `ingredient_categories` (`code`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_ingredients` (
                `id` TEXT NOT NULL,
                `canonicalName` TEXT NOT NULL,
                `normalizedName` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `defaultUnit` TEXT,
                `description` TEXT,
                `catalogVersion` INTEGER NOT NULL,
                `verificationStatus` TEXT NOT NULL,
                `compositionVariability` TEXT NOT NULL,
                `sourceUpdatedAt` INTEGER,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`categoryId`) REFERENCES `ingredient_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_catalog_ingredients_categoryId` ON `catalog_ingredients` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_catalog_ingredients_normalizedName` ON `catalog_ingredients` (`normalizedName`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ingredient_aliases` (
                `id` TEXT NOT NULL,
                `ingredientId` TEXT NOT NULL,
                `alias` TEXT NOT NULL,
                `normalizedAlias` TEXT NOT NULL,
                `languageCode` TEXT NOT NULL,
                `aliasType` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`ingredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredient_aliases_ingredientId` ON `ingredient_aliases` (`ingredientId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredient_aliases_normalizedAlias` ON `ingredient_aliases` (`normalizedAlias`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `food_safety_groups` (
                `id` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `conditionType` TEXT NOT NULL,
                `regulatoryStatus` TEXT NOT NULL,
                `jurisdiction` TEXT NOT NULL,
                `description` TEXT,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_food_safety_groups_code` ON `food_safety_groups` (`code`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `safety_sources` (
                `id` TEXT NOT NULL,
                `organization` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `officialReference` TEXT NOT NULL,
                `jurisdiction` TEXT NOT NULL,
                `publicationDate` TEXT,
                `reviewDate` TEXT NOT NULL,
                `documentStatus` TEXT,
                `officialUrl` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ingredient_safety_relations` (
                `id` TEXT NOT NULL,
                `ingredientId` TEXT NOT NULL,
                `safetyGroupId` TEXT NOT NULL,
                `relationType` TEXT NOT NULL,
                `evidenceLevel` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `notes` TEXT,
                `reviewedAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`ingredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`safetyGroupId`) REFERENCES `food_safety_groups`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`sourceId`) REFERENCES `safety_sources`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredient_safety_relations_ingredientId` ON `ingredient_safety_relations` (`ingredientId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredient_safety_relations_safetyGroupId` ON `ingredient_safety_relations` (`safetyGroupId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredient_safety_relations_sourceId` ON `ingredient_safety_relations` (`sourceId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_ingredients` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `normalizedName` TEXT NOT NULL,
                `categoryId` TEXT,
                `defaultUnit` TEXT,
                `ingredientType` TEXT NOT NULL,
                `brand` TEXT,
                `tradeName` TEXT,
                `compositionKnown` INTEGER NOT NULL,
                `labelReadAt` TEXT,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`categoryId`) REFERENCES `ingredient_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_ingredients_normalizedName` ON `custom_ingredients` (`normalizedName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_ingredients_categoryId` ON `custom_ingredients` (`categoryId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_ingredient_aliases` (
                `id` TEXT NOT NULL,
                `customIngredientId` TEXT NOT NULL,
                `alias` TEXT NOT NULL,
                `normalizedAlias` TEXT NOT NULL,
                `languageCode` TEXT NOT NULL,
                `aliasType` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`customIngredientId`) REFERENCES `custom_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_ingredient_aliases_customIngredientId` ON `custom_ingredient_aliases` (`customIngredientId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_ingredient_aliases_normalizedAlias` ON `custom_ingredient_aliases` (`normalizedAlias`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_ingredient_safety_relations` (
                `id` TEXT NOT NULL,
                `customIngredientId` TEXT NOT NULL,
                `safetyGroupId` TEXT NOT NULL,
                `relationType` TEXT NOT NULL,
                `evidenceLevel` TEXT NOT NULL,
                `sourceDescription` TEXT,
                `notes` TEXT,
                `reviewedAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`customIngredientId`) REFERENCES `custom_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`safetyGroupId`) REFERENCES `food_safety_groups`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_ingredient_safety_relations_customIngredientId` ON `custom_ingredient_safety_relations` (`customIngredientId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_ingredient_safety_relations_safetyGroupId` ON `custom_ingredient_safety_relations` (`safetyGroupId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_metadata` (
                `key` TEXT NOT NULL,
                `catalogVersion` INTEGER NOT NULL,
                `locale` TEXT NOT NULL,
                `jurisdiction` TEXT NOT NULL,
                `reviewedAt` TEXT NOT NULL,
                `importedAt` INTEGER NOT NULL,
                PRIMARY KEY(`key`)
            )
            """.trimIndent(),
        )
    }

    private fun migrateLegacyIngredientsToCustomOrigins(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `custom_ingredients` (
                `id`,
                `name`,
                `normalizedName`,
                `categoryId`,
                `defaultUnit`,
                `ingredientType`,
                `brand`,
                `tradeName`,
                `compositionKnown`,
                `labelReadAt`,
                `notes`,
                `createdAt`,
                `updatedAt`,
                `isActive`
            )
            SELECT
                'legacy:' || i.`id`,
                i.`name`,
                lower(trim(i.`name`)),
                NULL,
                i.`unit`,
                'LEGACY_UNCLASSIFIED',
                NULL,
                NULL,
                0,
                NULL,
                NULL,
                r.`createdAt`,
                r.`updatedAt`,
                1
            FROM `ingredients` AS i
            INNER JOIN `recipes` AS r ON r.`id` = i.`recipeId`
            """.trimIndent(),
        )
    }

    private fun rebuildRecipeIngredientsWithOrigins(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `ingredients_new` (
                `id` TEXT NOT NULL,
                `recipeId` TEXT NOT NULL,
                `quantity` TEXT,
                `unit` TEXT,
                `name` TEXT NOT NULL,
                `notes` TEXT,
                `sortOrder` INTEGER NOT NULL,
                `catalogIngredientId` TEXT,
                `customIngredientId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`catalogIngredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`customIngredientId`) REFERENCES `custom_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `ingredients_new` (
                `id`, `recipeId`, `quantity`, `unit`, `name`, `notes`, `sortOrder`,
                `catalogIngredientId`, `customIngredientId`
            )
            SELECT
                `id`, `recipeId`, `quantity`, `unit`, `name`, `notes`, `sortOrder`,
                NULL, 'legacy:' || `id`
            FROM `ingredients`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `ingredients`")
        db.execSQL("ALTER TABLE `ingredients_new` RENAME TO `ingredients`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId_sortOrder` ON `ingredients` (`recipeId`, `sortOrder`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_catalogIngredientId` ON `ingredients` (`catalogIngredientId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_customIngredientId` ON `ingredients` (`customIngredientId`)")
    }
}
