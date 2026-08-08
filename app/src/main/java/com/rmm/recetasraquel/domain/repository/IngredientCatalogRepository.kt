package com.rmm.recetasraquel.domain.repository

import com.rmm.recetasraquel.domain.ingredient.IngredientLineageRelation
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption

data class IngredientCatalogImportSummary(
    val catalogVersion: Int,
    val status: Status,
) {
    enum class Status {
        IMPORTED,
        ALREADY_CURRENT,
        DATABASE_NEWER,
    }
}

interface IngredientCatalogRepository {
    suspend fun ensureCatalogImported(): Result<IngredientCatalogImportSummary>
    suspend fun getParentRelations(ingredientId: String): Result<List<IngredientLineageRelation>>
    suspend fun getChildRelations(ingredientId: String): Result<List<IngredientLineageRelation>>
    suspend fun getRegulatoryExemptions(ingredientId: String): Result<List<RegulatoryExemption>>
}
