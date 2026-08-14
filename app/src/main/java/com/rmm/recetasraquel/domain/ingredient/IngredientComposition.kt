package com.rmm.recetasraquel.domain.ingredient

enum class IngredientCompositionCoverage {
    NONE,
    COMPLETE,
    PARTIAL,
}

enum class IngredientComponentPresence {
    REQUIRED,
    POSSIBLE,
}

data class IngredientComponent(
    val id: String,
    val parentIngredientId: String,
    val componentIngredientId: String,
    val componentName: String,
    val presence: IngredientComponentPresence,
    val reviewedAt: String,
    val sourceReference: String?,
    val notes: String?,
)

data class IngredientComposition(
    val coverage: IngredientCompositionCoverage = IngredientCompositionCoverage.NONE,
    val components: List<IngredientComponent> = emptyList(),
)
