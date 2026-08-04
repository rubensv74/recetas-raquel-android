package com.rmm.recetasraquel.data.repository

import android.database.sqlite.SQLiteException
import androidx.sqlite.SQLiteException as AndroidXSQLiteException
import com.rmm.recetasraquel.data.local.dao.RecipeDao
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toDomain
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toNewRecipe
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toPersisted
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toSummary
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toUpdatedRecipe
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.validation.RecipeValidationException
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecipeNotFoundException(recipeId: String) :
    IllegalArgumentException("No existe la receta $recipeId")

class LocalRecipeRepository(
    private val dao: RecipeDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
) : RecipeRepository {
    override fun observeRecipes(): Flow<List<Recipe>> =
        dao.observeRecipes().map { recipes -> recipes.map { it.toDomain() } }

    override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> {
        val normalized = filter.normalized()
        return dao.observeCatalog(
            query = normalized.query,
            favoritesOnly = normalized.favoritesOnly,
            category = normalized.category,
        ).map { recipes -> recipes.map { it.toSummary() } }
    }

    override fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    override fun observeRecipe(recipeId: String): Flow<Recipe?> =
        dao.observeRecipeWithDetails(recipeId).map { it?.toDomain() }

    override suspend fun getRecipe(recipeId: String): Recipe? =
        dao.getRecipeWithDetails(recipeId)?.toDomain()

    override suspend fun createRecipe(input: RecipeDraft): Result<String> {
        return try {
            val recipe = input.toNewRecipe(idGenerator, timeProvider)
            val persisted = recipe.toPersisted()
            dao.saveRecipeWithDetails(persisted.recipe, persisted.ingredients, persisted.steps)
            Result.success(recipe.id)
        } catch (error: RecipeValidationException) {
            Result.failure(error)
        } catch (error: SQLiteException) {
            Result.failure(error)
        } catch (error: AndroidXSQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> {
        return try {
            val persisted = recipe.toPersisted(updatedAt = timeProvider.nowEpochMillis())
            dao.saveRecipeWithDetails(persisted.recipe, persisted.ingredients, persisted.steps)
            Result.success(Unit)
        } catch (error: RecipeValidationException) {
            Result.failure(error)
        } catch (error: SQLiteException) {
            Result.failure(error)
        } catch (error: AndroidXSQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> {
        return try {
            val existing = dao.getRecipeWithDetails(recipeId)?.toDomain()
                ?: throw RecipeNotFoundException(recipeId)
            val recipe = draft.toUpdatedRecipe(existing, idGenerator, timeProvider)
            val persisted = recipe.toPersisted(updatedAt = timeProvider.nowEpochMillis())
            dao.saveRecipeWithDetails(persisted.recipe, persisted.ingredients, persisted.steps)
            Result.success(Unit)
        } catch (error: RecipeNotFoundException) {
            Result.failure(error)
        } catch (error: RecipeValidationException) {
            Result.failure(error)
        } catch (error: SQLiteException) {
            Result.failure(error)
        } catch (error: AndroidXSQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> = databaseResult {
        if (dao.deleteRecipe(recipeId) == 0) throw RecipeNotFoundException(recipeId)
    }

    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = databaseResult {
        if (dao.updateFavorite(recipeId, isFavorite, timeProvider.nowEpochMillis()) == 0) {
            throw RecipeNotFoundException(recipeId)
        }
    }

    private suspend fun databaseResult(operation: suspend () -> Unit): Result<Unit> {
        return try {
            operation()
            Result.success(Unit)
        } catch (error: RecipeNotFoundException) {
            Result.failure(error)
        } catch (error: SQLiteException) {
            Result.failure(error)
        } catch (error: AndroidXSQLiteException) {
            Result.failure(error)
        }
    }
}
