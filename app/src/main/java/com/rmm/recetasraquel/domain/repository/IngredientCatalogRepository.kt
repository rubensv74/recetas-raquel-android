package com.rmm.recetasraquel.domain.repository

import com.rmm.recetasraquel.domain.ingredient.CatalogIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.FrequentIngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogDetail
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
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
    suspend fun getCategories(): Result<List<IngredientCatalogCategory>>
    suspend fun searchIngredients(
        query: String,
        categoryId: String? = null,
        limit: Int = 100,
    ): Result<List<IngredientCatalogEntry>>
    suspend fun getFrequentIngredients(
        minimumRecipeCount: Int = 2,
        limit: Int = 6,
    ): Result<List<FrequentIngredientCatalogEntry>> = Result.success(emptyList())
    suspend fun getIngredient(ingredientId: String): Result<IngredientCatalogEntry?>
    suspend fun getIngredientDetail(ingredientId: String): Result<IngredientCatalogDetail> =
        Result.success(IngredientCatalogDetail())
    suspend fun getParentRelations(ingredientId: String): Result<List<IngredientLineageRelation>>
    suspend fun getChildRelations(ingredientId: String): Result<List<IngredientLineageRelation>>
    suspend fun getSafetyRelations(ingredientId: String): Result<List<CatalogIngredientSafetyRecord>> =
        Result.success(emptyList())
    suspend fun getRegulatoryExemptions(ingredientId: String): Result<List<RegulatoryExemption>>

    suspend fun getApplicableRegulatoryExemptions(
        ingredientId: String,
        jurisdiction: String,
        asOfDate: String,
    ): Result<List<RegulatoryExemption>> = getRegulatoryExemptions(ingredientId).map { exemptions ->
        exemptions.filter { exemption ->
            exemption.jurisdiction == jurisdiction &&
                (exemption.effectiveFrom == null || exemption.effectiveFrom <= asOfDate) &&
                (exemption.effectiveTo == null || exemption.effectiveTo >= asOfDate)
        }
    }
}
