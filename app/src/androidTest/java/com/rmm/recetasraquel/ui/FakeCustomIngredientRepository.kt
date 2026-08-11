package com.rmm.recetasraquel.ui

import com.rmm.recetasraquel.domain.ingredient.CustomIngredientDraft
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientRecord
import com.rmm.recetasraquel.domain.ingredient.FoodSafetyGroupOption
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository

internal class FakeCustomIngredientRepository : CustomIngredientRepository {
    var lastCreated: CustomIngredientDraft? = null
        private set

    override suspend fun createIngredient(input: CustomIngredientDraft): Result<String> {
        lastCreated = input
        return Result.success("custom-test-ingredient")
    }

    override suspend fun updateIngredient(
        ingredientId: String,
        input: CustomIngredientDraft,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun getIngredient(ingredientId: String): Result<CustomIngredientRecord?> = Result.success(null)

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> = Result.success(emptyList())

    override suspend fun getSafetyGroups(): Result<List<FoodSafetyGroupOption>> = Result.success(emptyList())
}
