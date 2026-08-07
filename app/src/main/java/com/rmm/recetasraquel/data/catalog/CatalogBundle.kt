package com.rmm.recetasraquel.data.catalog

data class CatalogManifest(
    val catalogId: String,
    val schemaVersion: Int,
    val catalogVersion: Int,
    val releaseStatus: String,
    val locale: String,
    val jurisdiction: String,
    val reviewedAt: String,
    val files: CatalogFiles,
    val counts: CatalogCounts,
)

data class CatalogFiles(
    val categories: String,
    val ingredients: String? = null,
    val aliases: String? = null,
    val ingredientShards: List<String>? = null,
    val aliasShards: List<String>? = null,
    val safetyGroups: String,
    val safetySources: String,
    val safetyRelations: String,
)

data class CatalogCounts(
    val categories: Int,
    val ingredients: Int,
    val aliases: Int,
    val safetyGroups: Int,
    val safetySources: Int,
    val safetyRelations: Int,
)

data class CatalogCategoryRecord(
    val id: String,
    val code: String,
    val name: String,
    val sortOrder: Int,
    val iconKey: String? = null,
    val isActive: Boolean = true,
)

data class CatalogIngredientRecord(
    val id: String,
    val canonicalName: String,
    val normalizedName: String,
    val categoryId: String,
    val defaultUnit: String? = null,
    val description: String? = null,
    val verificationStatus: String,
    val compositionVariability: String,
    val sourceUpdatedAt: Long? = null,
    val isActive: Boolean = true,
)

data class CatalogAliasRecord(
    val id: String,
    val ingredientId: String,
    val alias: String,
    val normalizedAlias: String,
    val languageCode: String,
    val aliasType: String,
)

data class CatalogSafetyGroupRecord(
    val id: String,
    val code: String,
    val displayName: String,
    val conditionType: String,
    val regulatoryStatus: String,
    val jurisdiction: String,
    val description: String? = null,
    val isActive: Boolean = true,
)

data class CatalogSafetySourceRecord(
    val id: String,
    val organization: String,
    val title: String,
    val officialReference: String,
    val jurisdiction: String,
    val publicationDate: String? = null,
    val reviewDate: String,
    val documentStatus: String? = null,
    val officialUrl: String? = null,
)

data class CatalogSafetyRelationRecord(
    val id: String,
    val ingredientId: String,
    val safetyGroupId: String,
    val relationType: String,
    val evidenceLevel: String,
    val sourceId: String,
    val notes: String? = null,
    val reviewedAt: String,
)

data class IngredientCatalogBundle(
    val manifest: CatalogManifest,
    val categories: List<CatalogCategoryRecord>,
    val ingredients: List<CatalogIngredientRecord>,
    val aliases: List<CatalogAliasRecord>,
    val safetyGroups: List<CatalogSafetyGroupRecord>,
    val safetySources: List<CatalogSafetySourceRecord>,
    val safetyRelations: List<CatalogSafetyRelationRecord>,
)
