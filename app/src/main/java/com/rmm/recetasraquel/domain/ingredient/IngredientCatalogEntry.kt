package com.rmm.recetasraquel.domain.ingredient

data class IngredientCatalogCategory(
    val id: String,
    val code: String,
    val name: String,
    val sortOrder: Int,
    val iconKey: String?,
)

enum class IngredientCatalogInformationStatus {
    SAFETY_RELATIONS_RECORDED,
    REGULATORY_EXEMPTION_RECORDED,
    NO_DIRECT_SAFETY_RELATION_RECORDED,
}

data class IngredientCatalogEntry(
    val id: String,
    val canonicalName: String,
    val categoryId: String,
    val categoryName: String,
    val defaultUnit: String?,
    val verificationStatus: String,
    val informationStatus: IngredientCatalogInformationStatus,
)

data class CatalogIngredientSafetyRecord(
    val safetyGroupId: String,
    val safetyGroupName: String,
    val jurisdiction: String,
    val relationType: String,
    val evidenceLevel: String,
    val sourceId: String,
    val sourceDetails: String?,
    val notes: String?,
    val reviewedAt: String,
)
