package com.rmm.recetasraquel.data.repository

import com.rmm.recetasraquel.data.catalog.CatalogImportResult
import com.rmm.recetasraquel.data.catalog.CatalogImporter
import com.rmm.recetasraquel.data.local.dao.CatalogIngredientSearchRow
import com.rmm.recetasraquel.data.local.dao.IngredientCatalogDao
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientRelationEntity
import com.rmm.recetasraquel.data.local.entity.RegulatoryExemptionEntity
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogInformationStatus
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageRelation
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageType
import com.rmm.recetasraquel.domain.ingredient.IngredientTextNormalizer
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
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

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> = runCatching {
        requireCatalogReady()
        dao.getActiveCategories().map { category ->
            IngredientCatalogCategory(
                id = category.id,
                code = category.code,
                name = category.name,
                sortOrder = category.sortOrder,
                iconKey = category.iconKey,
            )
        }
    }

    override suspend fun searchIngredients(
        query: String,
        categoryId: String?,
        limit: Int,
    ): Result<List<IngredientCatalogEntry>> = runCatching {
        requireCatalogReady()
        val normalizedQuery = IngredientTextNormalizer.normalize(query)
        dao.searchActiveIngredients(
            normalizedQuery = normalizedQuery,
            categoryId = categoryId,
            limit = limit.coerceIn(MIN_SEARCH_LIMIT, MAX_SEARCH_LIMIT),
        ).map { it.toCatalogEntry() }
    }

    override suspend fun getIngredient(ingredientId: String): Result<IngredientCatalogEntry?> = runCatching {
        requireCatalogReady()
        dao.getActiveIngredientSummary(ingredientId)?.toCatalogEntry()
    }

    override suspend fun getParentRelations(ingredientId: String): Result<List<IngredientLineageRelation>> = runCatching {
        dao.getParentRelations(ingredientId).map { it.toDomain() }
    }

    override suspend fun getChildRelations(ingredientId: String): Result<List<IngredientLineageRelation>> = runCatching {
        dao.getChildRelations(ingredientId).map { it.toDomain() }
    }

    override suspend fun getRegulatoryExemptions(ingredientId: String): Result<List<RegulatoryExemption>> = runCatching {
        dao.getRegulatoryExemptionsForIngredient(ingredientId).map { it.toDomain() }
    }

    private suspend fun requireCatalogReady() {
        ensureCatalogImported().getOrThrow()
    }

    private fun CatalogIngredientSearchRow.toCatalogEntry() = IngredientCatalogEntry(
        id = id,
        canonicalName = canonicalName,
        categoryId = categoryId,
        categoryName = categoryName,
        defaultUnit = defaultUnit,
        verificationStatus = verificationStatus,
        informationStatus = when {
            safetyRelationCount > 0 -> IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED
            regulatoryExemptionCount > 0 -> IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED
            else -> IngredientCatalogInformationStatus.NO_DIRECT_SAFETY_RELATION_RECORDED
        },
    )

    private fun CatalogIngredientRelationEntity.toDomain() = IngredientLineageRelation(
        id = id,
        childIngredientId = childIngredientId,
        parentIngredientId = parentIngredientId,
        type = IngredientLineageType.valueOf(relationType),
        reviewedAt = reviewedAt,
        sourceReference = sourceReference,
        notes = notes,
    )

    private fun RegulatoryExemptionEntity.toDomain() = RegulatoryExemption(
        id = id,
        ingredientId = ingredientId,
        safetyGroupId = safetyGroupId,
        jurisdiction = jurisdiction,
        effect = RegulatoryEffect.valueOf(regulatoryEffect),
        conditions = conditions,
        sourceId = sourceId,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        reviewedAt = reviewedAt,
        notes = notes,
    )

    companion object {
        private const val MIN_SEARCH_LIMIT = 1
        private const val MAX_SEARCH_LIMIT = 200
    }
}
