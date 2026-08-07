package com.rmm.recetasraquel.domain.model

data class Recipe(
    val id: String,
    val name: String,
    val description: String?,
    val category: String?,
    val servings: Int?,
    val preparationMinutes: Int?,
    val cookingMinutes: Int?,
    val notes: String?,
    val isFavorite: Boolean,
    val coverPhotoPath: String?,
    val ingredients: List<Ingredient>,
    val steps: List<RecipeStep>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Ingredient(
    val id: String,
    val recipeId: String,
    val quantity: String?,
    val unit: String?,
    val name: String,
    val notes: String?,
    val sortOrder: Int,
    val catalogIngredientId: String? = null,
    val customIngredientId: String? = null,
)

data class RecipeStep(
    val id: String,
    val recipeId: String,
    val instruction: String,
    val timerMinutes: Int?,
    val photoPath: String?,
    val sortOrder: Int,
)
