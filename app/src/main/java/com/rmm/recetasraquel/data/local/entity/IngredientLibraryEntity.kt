package com.rmm.recetasraquel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredient_categories",
    indices = [Index(value = ["code"], unique = true)],
)
data class IngredientCategoryEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val sortOrder: Int,
    val iconKey: String?,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "catalog_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = IngredientCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["normalizedName"]),
    ],
)
data class CatalogIngredientEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val normalizedName: String,
    val categoryId: String,
    val defaultUnit: String?,
    val description: String?,
    val catalogVersion: Int,
    val verificationStatus: String,
    val compositionVariability: String,
    val catalogRole: String = "CULINARY",
    val sourceUpdatedAt: Long?,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "ingredient_aliases",
    foreignKeys = [
        ForeignKey(
            entity = CatalogIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["ingredientId"]),
        Index(value = ["normalizedAlias"]),
    ],
)
data class IngredientAliasEntity(
    @PrimaryKey val id: String,
    val ingredientId: String,
    val alias: String,
    val normalizedAlias: String,
    val languageCode: String,
    val aliasType: String,
)

@Entity(
    tableName = "catalog_ingredient_relations",
    foreignKeys = [
        ForeignKey(
            entity = CatalogIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["childIngredientId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = CatalogIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentIngredientId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["childIngredientId"]),
        Index(value = ["parentIngredientId"]),
        Index(value = ["childIngredientId", "parentIngredientId", "relationType"], unique = true),
    ],
)
data class CatalogIngredientRelationEntity(
    @PrimaryKey val id: String,
    val childIngredientId: String,
    val parentIngredientId: String,
    val relationType: String,
    val reviewedAt: String,
    val sourceReference: String?,
    val notes: String?,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "food_safety_groups",
    indices = [Index(value = ["code"], unique = true)],
)
data class FoodSafetyGroupEntity(
    @PrimaryKey val id: String,
    val code: String,
    val displayName: String,
    val conditionType: String,
    val regulatoryStatus: String,
    val jurisdiction: String,
    val description: String?,
    val isActive: Boolean = true,
)

@Entity(tableName = "safety_sources")
data class SafetySourceEntity(
    @PrimaryKey val id: String,
    val organization: String,
    val title: String,
    val officialReference: String,
    val jurisdiction: String,
    val publicationDate: String?,
    val reviewDate: String,
    val documentStatus: String?,
    val officialUrl: String?,
)

@Entity(
    tableName = "ingredient_safety_relations",
    foreignKeys = [
        ForeignKey(
            entity = CatalogIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodSafetyGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["safetyGroupId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SafetySourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["ingredientId"]),
        Index(value = ["safetyGroupId"]),
        Index(value = ["sourceId"]),
    ],
)
data class IngredientSafetyRelationEntity(
    @PrimaryKey val id: String,
    val ingredientId: String,
    val safetyGroupId: String,
    val relationType: String,
    val evidenceLevel: String,
    val sourceId: String,
    val notes: String?,
    val reviewedAt: String,
)

@Entity(
    tableName = "custom_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = IngredientCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["normalizedName"]),
        Index(value = ["categoryId"]),
    ],
)
data class CustomIngredientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val categoryId: String?,
    val defaultUnit: String?,
    val ingredientType: String,
    val brand: String?,
    val tradeName: String?,
    val compositionKnown: Boolean,
    val labelReadAt: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "custom_ingredient_aliases",
    foreignKeys = [
        ForeignKey(
            entity = CustomIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["customIngredientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["customIngredientId"]),
        Index(value = ["normalizedAlias"]),
    ],
)
data class CustomIngredientAliasEntity(
    @PrimaryKey val id: String,
    val customIngredientId: String,
    val alias: String,
    val normalizedAlias: String,
    val languageCode: String,
    val aliasType: String,
)

@Entity(
    tableName = "custom_ingredient_safety_relations",
    foreignKeys = [
        ForeignKey(
            entity = CustomIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["customIngredientId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodSafetyGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["safetyGroupId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SafetySourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["customIngredientId"]),
        Index(value = ["safetyGroupId"]),
        Index(value = ["sourceId"]),
    ],
)
data class CustomIngredientSafetyRelationEntity(
    @PrimaryKey val id: String,
    val customIngredientId: String,
    val safetyGroupId: String,
    val relationType: String,
    val evidenceLevel: String,
    val sourceId: String,
    val sourceDetails: String?,
    val notes: String?,
    val reviewedAt: String,
)

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
    @PrimaryKey val key: String,
    val catalogVersion: Int,
    val locale: String,
    val jurisdiction: String,
    val reviewedAt: String,
    val importedAt: Long,
)
