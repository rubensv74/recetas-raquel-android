package com.rmm.recetasraquel.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rmm.recetasraquel.data.local.dao.RecipeDao
import com.rmm.recetasraquel.data.local.entity.IngredientEntity
import com.rmm.recetasraquel.data.local.entity.RecipeEntity
import com.rmm.recetasraquel.data.local.entity.RecipeStepEntity

@Database(
    entities = [RecipeEntity::class, IngredientEntity::class, RecipeStepEntity::class],
    version = 1,
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
        ).build()
    }
}
