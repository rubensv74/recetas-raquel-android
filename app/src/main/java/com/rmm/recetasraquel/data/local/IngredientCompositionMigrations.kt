package com.rmm.recetasraquel.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object IngredientCompositionMigrations {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `catalog_ingredients` ADD COLUMN `compositionCoverage` TEXT NOT NULL DEFAULT 'NONE'",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `catalog_ingredient_components` (
                    `id` TEXT NOT NULL,
                    `parentIngredientId` TEXT NOT NULL,
                    `componentIngredientId` TEXT NOT NULL,
                    `presenceType` TEXT NOT NULL,
                    `reviewedAt` TEXT NOT NULL,
                    `sourceReference` TEXT,
                    `notes` TEXT,
                    `isActive` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`parentIngredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                    FOREIGN KEY(`componentIngredientId`) REFERENCES `catalog_ingredients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_catalog_ingredient_components_parentIngredientId` ON `catalog_ingredient_components` (`parentIngredientId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_catalog_ingredient_components_componentIngredientId` ON `catalog_ingredient_components` (`componentIngredientId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_catalog_ingredient_components_parentIngredientId_componentIngredientId` ON `catalog_ingredient_components` (`parentIngredientId`, `componentIngredientId`)",
            )
        }
    }
}
