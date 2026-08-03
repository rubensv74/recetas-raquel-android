package com.rmm.recetasraquel.ui.components

import com.rmm.recetasraquel.domain.model.Ingredient

fun formatTotalTime(preparationMinutes: Int?, cookingMinutes: Int?): String? = when {
    preparationMinutes == null && cookingMinutes == null -> null
    else -> "${(preparationMinutes ?: 0) + (cookingMinutes ?: 0)} min"
}

fun formatIngredient(ingredient: Ingredient): String {
    val amount = listOfNotNull(ingredient.quantity, ingredient.unit).joinToString(" ")
    return if (amount.isBlank()) ingredient.name else "$amount · ${ingredient.name}"
}
