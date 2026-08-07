package com.rmm.recetasraquel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val category: String?,
    val servings: Int?,
    val preparationMinutes: Int?,
    val cookingMinutes: Int?,
    val notes: String?,
    val isFavorite: Boolean = false,
    val coverPhotoPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CatalogIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["catalogIngredientId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = CustomIngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["customIngredientId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["recipeId", "sortOrder"]),
        Index(value = ["catalogIngredientId"]),
        Index(value = ["customIngredientId"]),
    ],
)
data class IngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val quantity: String?,
    val unit: String?,
    val name: String,
    val notes: String?,
    val sortOrder: Int,
    val catalogIngredientId: String? = null,
    val customIngredientId: String? = null,
)

@Entity(
    tableName = "recipe_steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recipeId", "sortOrder"])],
)
data class RecipeStepEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val instruction: String,
    val timerMinutes: Int?,
    val photoPath: String?,
    val sortOrder: Int,
)
