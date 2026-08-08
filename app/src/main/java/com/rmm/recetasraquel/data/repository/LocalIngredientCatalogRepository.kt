package com.rmm.recetasraquel.data.repository

import com.rmm.recetasraquel.data.catalog.CatalogImportResult
import com.rmm.recetasraquel.data.catalog.CatalogImporter
import com.rmm.recetasraquel.data.local.dao.IngredientCatalogDao
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientRelationEntity
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageRelation
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageType
import com.rmm.recetasraquel.domain.repository.IngredientCatalogImportSummary
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository

class LocalIngredientCatalogRepository(
    private val importer: CatalogImporter,
    private val dao: IngredientCatalogDao,
) : IngredientCatalogRepository {
    override suspend fun ensureCatalogImported(): Result<IngredientCatalogImportSummary> = runCatching {
        when (val result = importer.ensureImported()) {
            is CatalogImportResult.Imported -> IngredientCatalogImportSummary(
                catalogVersion = result.catalogVersion,
                status = IngredientCatalogImportSummary.Status.IMPORTED,
            )
            is CatalogImportResult.AlreadyCurrent -> IngredientCatalogImportSummary(
                catalogVersion = result.catalogVersion,
                status = IngredientCatalogImportSummary.Status.ALREADY_CURRENT,
            )
            is CatalogImportResult.DatabaseNewer -> IngredientCatalogImportSummary(
                catalogVersion = result.databaseVersion,
                status = IngredientCatalogImportSummary.Status.DATABASE_NEWER,
            )
        }
    }

    override suspend fun getParentRelations(ingredientId: String): Result<List<IngredientLineageRelation>> = runCatching {
        dao.getParentRelations(ingredientId).map { it.toDomain() }
    }

    override suspend fun getChildRelations(ingredientId: String): Result<List<IngredientLineageRelation>> = runCatching {
        dao.getChildRelations(ingredientId).map { it.toDomain() }
    }

    private fun CatalogIngredientRelationEntity.toDomain() = IngredientLineageRelation(
        id = id,
        childIngredientId = childIngredientId,
        parentIngredientId = parentIngredientId,
        type = IngredientLineageType.valueOf(relationType),
        reviewedAt = reviewedAt,
        sourceReference = sourceReference,
        notes = notes,
    )
}
