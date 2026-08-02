package com.rmm.recetasraquel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rmm.recetasraquel.data.local.entity.IngredientEntity
import com.rmm.recetasraquel.data.local.entity.RecipeEntity
import com.rmm.recetasraquel.data.local.entity.RecipeStepEntity
import com.rmm.recetasraquel.data.local.relation.RecipeWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    fun observeRecipeWithDetails(recipeId: String): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeWithDetails(recipeId: String): RecipeWithDetails?

    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSteps(steps: List<RecipeStepEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    suspend fun deleteSteps(recipeId: String)

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipe(recipeId: String): Int

    @Query("UPDATE recipes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :recipeId")
    suspend fun updateFavorite(recipeId: String, isFavorite: Boolean, updatedAt: Long): Int

    @Query("SELECT * FROM ingredients WHERE recipeId = :recipeId ORDER BY sortOrder ASC")
    suspend fun getIngredients(recipeId: String): List<IngredientEntity>

    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY sortOrder ASC")
    suspend fun getSteps(recipeId: String): List<RecipeStepEntity>

    @Transaction
    suspend fun saveRecipeWithDetails(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        steps: List<RecipeStepEntity>,
    ) {
        upsertRecipe(recipe)
        deleteIngredients(recipe.id)
        deleteSteps(recipe.id)
        if (ingredients.isNotEmpty()) insertIngredients(ingredients)
        if (steps.isNotEmpty()) insertSteps(steps)
    }
}
