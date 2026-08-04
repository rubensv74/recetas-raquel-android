package com.rmm.recetasraquel.domain.repository

import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeSummary
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>
    fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>>
    fun observeCategories(): Flow<List<String>>
    fun observeRecipe(recipeId: String): Flow<Recipe?>
    suspend fun getRecipe(recipeId: String): Recipe?
    suspend fun createRecipe(input: RecipeDraft): Result<String>
    suspend fun updateRecipe(recipe: Recipe): Result<Unit>
    suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit>
    suspend fun deleteRecipe(recipeId: String): Result<Unit>
    suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit>
}
