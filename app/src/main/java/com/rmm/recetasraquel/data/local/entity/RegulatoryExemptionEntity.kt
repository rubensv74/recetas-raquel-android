package com.rmm.recetasraquel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "regulatory_exemptions",
    foreignKeys = [
        ForeignKey(
            entity = CatalogIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.NO_ACTION,
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
        Index(
            value = ["ingredientId", "safetyGroupId", "jurisdiction", "regulatoryEffect"],
            unique = true,
        ),
    ],
)
data class RegulatoryExemptionEntity(
    @PrimaryKey val id: String,
    val ingredientId: String,
    val safetyGroupId: String,
    val jurisdiction: String,
    val regulatoryEffect: String,
    val conditions: String,
    val sourceId: String,
    val effectiveFrom: String?,
    val effectiveTo: String?,
    val reviewedAt: String,
    val notes: String?,
    val isActive: Boolean = true,
)
