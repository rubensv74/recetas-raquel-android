package com.rmm.recetasraquel.domain.repository

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
}
