package com.rmm.recetasraquel.domain.model

data class RecipeDraft(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val servings: Int? = null,
    val preparationMinutes: Int? = null,
    val cookingMinutes: Int? = null,
    val notes: String? = null,
    val coverPhotoPath: String? = null,
    val ingredients: List<IngredientDraft> = emptyList(),
    val steps: List<RecipeStepDraft> = emptyList(),
)

data class IngredientDraft(
    val quantity: String? = null,
    val unit: String? = null,
    val name: String,
    val notes: String? = null,
)

data class RecipeStepDraft(
    val instruction: String,
    val timerMinutes: Int? = null,
    val photoPath: String? = null,
)
