package com.rmm.recetasraquel.ui

import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageRelation
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.repository.IngredientCatalogImportSummary
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository

internal object FakeIngredientCatalogRepository : IngredientCatalogRepository {
    override suspend fun ensureCatalogImported(): Result<IngredientCatalogImportSummary> =
        Result.success(
            IngredientCatalogImportSummary(
                catalogVersion = 0,
                status = IngredientCatalogImportSummary.Status.ALREADY_CURRENT,
            ),
        )

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> = Result.success(emptyList())

    override suspend fun searchIngredients(
        query: String,
        categoryId: String?,
        limit: Int,
    ): Result<List<IngredientCatalogEntry>> = Result.success(emptyList())

    override suspend fun getIngredient(ingredientId: String): Result<IngredientCatalogEntry?> = Result.success(null)

    override suspend fun getParentRelations(ingredientId: String): Result<List<IngredientLineageRelation>> =
        Result.success(emptyList())

    override suspend fun getChildRelations(ingredientId: String): Result<List<IngredientLineageRelation>> =
        Result.success(emptyList())

    override suspend fun getRegulatoryExemptions(ingredientId: String): Result<List<RegulatoryExemption>> =
        Result.success(emptyList())
}
