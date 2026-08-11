package com.rmm.recetasraquel.domain.repository

import com.rmm.recetasraquel.domain.ingredient.CustomIngredientDraft
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientRecord
import com.rmm.recetasraquel.domain.ingredient.FoodSafetyGroupOption
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory

interface CustomIngredientRepository {
    suspend fun createIngredient(input: CustomIngredientDraft): Result<String>
    suspend fun updateIngredient(ingredientId: String, input: CustomIngredientDraft): Result<Unit>
    suspend fun getIngredient(ingredientId: String): Result<CustomIngredientRecord?>
    suspend fun getCategories(): Result<List<IngredientCatalogCategory>>
    suspend fun getSafetyGroups(): Result<List<FoodSafetyGroupOption>>
}
