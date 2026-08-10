package com.rmm.recetasraquel.domain.ingredient

data class IngredientSafetyRecord(
    val id: String,
    val ingredientId: String,
    val safetyGroupId: String,
    val relationType: RecipeSafetyRelationType,
    val evidenceLevel: String,
    val sourceId: String,
    val sourceDetails: String? = null,
    val notes: String? = null,
    val reviewedAt: String? = null,
)
