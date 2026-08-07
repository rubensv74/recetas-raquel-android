package com.rmm.recetasraquel.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rmm.recetasraquel.data.local.dao.RecipeDao
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientEntity
import com.rmm.recetasraquel.data.local.entity.CatalogMetadataEntity
import com.rmm.recetasraquel.data.local.entity.CustomIngredientAliasEntity
import com.rmm.recetasraquel.data.local.entity.CustomIngredientEntity
import com.rmm.recetasraquel.data.local.entity.CustomIngredientSafetyRelationEntity
import com.rmm.recetasraquel.data.local.entity.FoodSafetyGroupEntity
import com.rmm.recetasraquel.data.local.entity.IngredientAliasEntity
import com.rmm.recetasraquel.data.local.entity.IngredientCategoryEntity
import com.rmm.recetasraquel.data.local.entity.IngredientEntity
import com.rmm.recetasraquel.data.local.entity.IngredientSafetyRelationEntity
import com.rmm.recetasraquel.data.local.entity.RecipeEntity
import com.rmm.recetasraquel.data.local.entity.RecipeStepEntity
import com.rmm.recetasraquel.data.local.entity.SafetySourceEntity

@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        RecipeStepEntity::class,
        IngredientCategoryEntity::class,
        CatalogIngredientEntity::class,
        IngredientAliasEntity::class,
        FoodSafetyGroupEntity::class,
        SafetySourceEntity::class,
        IngredientSafetyRelationEntity::class,
        CustomIngredientEntity::class,
        CustomIngredientAliasEntity::class,
        CustomIngredientSafetyRelationEntity::class,
        CatalogMetadataEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        const val DATABASE_NAME = "recipes.db"

        fun build(context: Context): RecipeDatabase = Room.databaseBuilder(
            context.applicationContext,
            RecipeDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(RecipeDatabaseMigrations.MIGRATION_1_2)
            .build()
    }
}
