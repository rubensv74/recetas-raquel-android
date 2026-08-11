package com.rmm.recetasraquel.domain.ingredient

enum class IngredientLineageType {
    VARIANT_OF,
    CUT_OF,
    DERIVED_FROM,
    FORM_OF,
}

data class IngredientLineageRelation(
    val id: String,
    val childIngredientId: String,
    val parentIngredientId: String,
    val type: IngredientLineageType,
    val reviewedAt: String,
    val sourceReference: String?,
    val notes: String?,
)
