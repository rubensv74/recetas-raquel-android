package com.rmm.recetasraquel.data.catalog

import com.rmm.recetasraquel.data.local.dao.IngredientCatalogDao
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientEntity
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientRelationEntity
import com.rmm.recetasraquel.data.local.entity.CatalogMetadataEntity
import com.rmm.recetasraquel.data.local.entity.FoodSafetyGroupEntity
import com.rmm.recetasraquel.data.local.entity.IngredientAliasEntity
import com.rmm.recetasraquel.data.local.entity.IngredientCategoryEntity
import com.rmm.recetasraquel.data.local.entity.IngredientSafetyRelationEntity
import com.rmm.recetasraquel.data.local.entity.SafetySourceEntity
import com.rmm.recetasraquel.util.TimeProvider

sealed interface CatalogImportResult {
    data class Imported(val catalogVersion: Int) : CatalogImportResult
    data class AlreadyCurrent(val catalogVersion: Int) : CatalogImportResult
    data class DatabaseNewer(val databaseVersion: Int, val assetVersion: Int) : CatalogImportResult
}

class CatalogImporter(
    private val reader: IngredientCatalogAssetReader,
    private val dao: IngredientCatalogDao,
    private val timeProvider: TimeProvider,
) {
    suspend fun ensureImported(
        versionDirectory: String = IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY,
    ): CatalogImportResult {
        val bundle = reader.read(versionDirectory)
        CatalogValidator.validate(bundle).requireValid()

        val manifest = bundle.manifest
        val existing = dao.getMetadata(METADATA_KEY)
        if (existing != null) {
            if (existing.catalogVersion > manifest.catalogVersion) {
                return CatalogImportResult.DatabaseNewer(existing.catalogVersion, manifest.catalogVersion)
            }
            if (
                existing.catalogVersion == manifest.catalogVersion &&
                existing.locale == manifest.locale &&
                existing.jurisdiction == manifest.jurisdiction
            ) {
                return CatalogImportResult.AlreadyCurrent(existing.catalogVersion)
            }
        }

        dao.replaceShippedCatalog(
            categories = bundle.categories.map { it.toEntity() },
            ingredients = bundle.ingredients.map { it.toEntity(manifest.catalogVersion) },
            aliases = bundle.aliases.map { it.toEntity() },
            ingredientRelations = bundle.ingredientRelations.map { it.toEntity() },
            safetyGroups = bundle.safetyGroups.map { it.toEntity() },
            safetySources = bundle.safetySources.map { it.toEntity() },
            safetyRelations = bundle.safetyRelations.map { it.toEntity() },
            metadata = CatalogMetadataEntity(
                key = METADATA_KEY,
                catalogVersion = manifest.catalogVersion,
                locale = manifest.locale,
                jurisdiction = manifest.jurisdiction,
                reviewedAt = manifest.reviewedAt,
                importedAt = timeProvider.nowEpochMillis(),
            ),
        )

        return CatalogImportResult.Imported(manifest.catalogVersion)
    }

    private fun CatalogCategoryRecord.toEntity() = IngredientCategoryEntity(
        id = id,
        code = code,
        name = name,
        sortOrder = sortOrder,
        iconKey = iconKey,
        isActive = isActive,
    )

    private fun CatalogIngredientRecord.toEntity(catalogVersion: Int) = CatalogIngredientEntity(
        id = id,
        canonicalName = canonicalName,
        normalizedName = normalizedName,
        categoryId = categoryId,
        defaultUnit = defaultUnit,
        description = description,
        catalogVersion = catalogVersion,
        verificationStatus = verificationStatus,
        compositionVariability = compositionVariability,
        sourceUpdatedAt = sourceUpdatedAt,
        isActive = isActive,
    )

    private fun CatalogAliasRecord.toEntity() = IngredientAliasEntity(
        id = id,
        ingredientId = ingredientId,
        alias = alias,
        normalizedAlias = normalizedAlias,
        languageCode = languageCode,
        aliasType = aliasType,
    )

    private fun CatalogIngredientRelationRecord.toEntity() = CatalogIngredientRelationEntity(
        id = id,
        childIngredientId = childIngredientId,
        parentIngredientId = parentIngredientId,
        relationType = relationType,
        reviewedAt = reviewedAt,
        sourceReference = sourceReference,
        notes = notes,
        isActive = isActive,
    )

    private fun CatalogSafetyGroupRecord.toEntity() = FoodSafetyGroupEntity(
        id = id,
        code = code,
        displayName = displayName,
        conditionType = conditionType,
        regulatoryStatus = regulatoryStatus,
        jurisdiction = jurisdiction,
        description = description,
        isActive = isActive,
    )

    private fun CatalogSafetySourceRecord.toEntity() = SafetySourceEntity(
        id = id,
        organization = organization,
        title = title,
        officialReference = officialReference,
        jurisdiction = jurisdiction,
        publicationDate = publicationDate,
        reviewDate = reviewDate,
        documentStatus = documentStatus,
        officialUrl = officialUrl,
    )

    private fun CatalogSafetyRelationRecord.toEntity() = IngredientSafetyRelationEntity(
        id = id,
        ingredientId = ingredientId,
        safetyGroupId = safetyGroupId,
        relationType = relationType,
        evidenceLevel = evidenceLevel,
        sourceId = sourceId,
        notes = notes,
        reviewedAt = reviewedAt,
    )

    companion object {
        const val METADATA_KEY = "master"
    }
}
