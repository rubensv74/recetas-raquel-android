package com.rmm.recetasraquel.domain.ingredient

enum class RegulatoryEffect {
    EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION,
}

data class RegulatoryExemption(
    val id: String,
    val ingredientId: String,
    val safetyGroupId: String,
    val jurisdiction: String,
    val effect: RegulatoryEffect,
    val conditions: String,
    val sourceId: String,
    val effectiveFrom: String?,
    val effectiveTo: String?,
    val reviewedAt: String,
    val notes: String?,
)
