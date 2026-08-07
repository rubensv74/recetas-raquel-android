package com.rmm.recetasraquel.domain.usecase

interface SaveRecipeOperation {
    suspend fun create(input: SaveRecipeInput): Result<String>
    suspend fun update(recipeId: String, input: SaveRecipeInput): Result<Unit>
}
