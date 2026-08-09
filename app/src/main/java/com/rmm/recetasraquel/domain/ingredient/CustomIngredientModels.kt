package com.rmm.recetasraquel.domain.ingredient

enum class CustomIngredientType {
    SIMPLE,
    COMPOUND,
    COMMERCIAL_PRODUCT,
}

enum class CustomIngredientSafetyRelationType {
    INHERENT_SOURCE,
    CONTAINS,
    DERIVED_FROM,
    REGULATED_COMPONENT,
    DECLARED_MAY_CONTAIN,
    POSSIBLE_CROSS_REACTIVITY,
    UNKNOWN,
}

enum class CustomIngredientSafetyEvidence {
    USER_DECLARED,
    UNVERIFIED,
}

data class CustomIngredientSafetyDeclaration(
    val safetyGroupId: String,
    val relationType: CustomIngredientSafetyRelationType,
    val evidenceLevel: CustomIngredientSafetyEvidence,
    val sourceDetails: String? = null,
    val notes: String? = null,
)

data class CustomIngredientDraft(
    val name: String,
    val categoryId: String? = null,
    val defaultUnit: String? = null,
    val type: CustomIngredientType = CustomIngredientType.SIMPLE,
    val aliases: List<String> = emptyList(),
    val brand: String? = null,
    val tradeName: String? = null,
    val compositionKnown: Boolean,
    val labelReadAt: String? = null,
    val notes: String? = null,
    val safetyDeclarations: List<CustomIngredientSafetyDeclaration> = emptyList(),
)

data class CustomIngredientSafetyRecord(
    val id: String,
    val safetyGroupId: String,
    val relationType: CustomIngredientSafetyRelationType,
    val evidenceLevel: CustomIngredientSafetyEvidence,
    val sourceId: String,
    val sourceDetails: String?,
    val notes: String?,
    val reviewedAt: String,
)

data class CustomIngredientRecord(
    val id: String,
    val name: String,
    val categoryId: String?,
    val defaultUnit: String?,
    val type: CustomIngredientType,
    val aliases: List<String>,
    val brand: String?,
    val tradeName: String?,
    val compositionKnown: Boolean,
    val labelReadAt: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val safetyRelations: List<CustomIngredientSafetyRecord>,
)

data class FoodSafetyGroupOption(
    val id: String,
    val code: String,
    val displayName: String,
    val conditionType: String,
    val regulatoryStatus: String,
    val jurisdiction: String,
)
