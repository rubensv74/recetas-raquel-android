package com.rmm.recetasraquel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rmm.recetasraquel.data.local.entity.CustomIngredientAliasEntity
import com.rmm.recetasraquel.data.local.entity.CustomIngredientEntity
import com.rmm.recetasraquel.data.local.entity.CustomIngredientSafetyRelationEntity
import com.rmm.recetasraquel.data.local.entity.FoodSafetyGroupEntity
import com.rmm.recetasraquel.data.local.entity.IngredientCategoryEntity
import com.rmm.recetasraquel.data.local.entity.SafetySourceEntity

data class CustomIngredientAggregate(
    val ingredient: CustomIngredientEntity,
    val aliases: List<CustomIngredientAliasEntity>,
    val safetyRelations: List<CustomIngredientSafetyRelationEntity>,
)

@Dao
interface CustomIngredientDao {
    @Query("SELECT * FROM custom_ingredients WHERE id = :ingredientId AND isActive = 1 LIMIT 1")
    suspend fun getIngredient(ingredientId: String): CustomIngredientEntity?

    @Query("SELECT * FROM custom_ingredient_aliases WHERE customIngredientId = :ingredientId ORDER BY alias COLLATE NOCASE ASC, id ASC")
    suspend fun getAliases(ingredientId: String): List<CustomIngredientAliasEntity>

    @Query("SELECT * FROM custom_ingredient_safety_relations WHERE customIngredientId = :ingredientId ORDER BY safetyGroupId ASC, relationType ASC, id ASC")
    suspend fun getSafetyRelations(ingredientId: String): List<CustomIngredientSafetyRelationEntity>

    @Transaction
    suspend fun getAggregate(ingredientId: String): CustomIngredientAggregate? {
        val ingredient = getIngredient(ingredientId) ?: return null
        return CustomIngredientAggregate(
            ingredient = ingredient,
            aliases = getAliases(ingredientId),
            safetyRelations = getSafetyRelations(ingredientId),
        )
    }

    @Query("SELECT * FROM ingredient_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getActiveCategories(): List<IngredientCategoryEntity>

    @Query("SELECT * FROM ingredient_categories WHERE id = :categoryId AND isActive = 1 LIMIT 1")
    suspend fun getActiveCategory(categoryId: String): IngredientCategoryEntity?

    @Query("SELECT * FROM food_safety_groups WHERE isActive = 1 ORDER BY displayName COLLATE NOCASE ASC, id ASC")
    suspend fun getActiveSafetyGroups(): List<FoodSafetyGroupEntity>

    @Query("SELECT * FROM food_safety_groups WHERE id = :groupId AND isActive = 1 LIMIT 1")
    suspend fun getActiveSafetyGroup(groupId: String): FoodSafetyGroupEntity?

    @Query("SELECT COUNT(*) FROM custom_ingredients WHERE isActive = 1")
    suspend fun countActiveIngredients(): Int

    @Upsert
    suspend fun upsertIngredient(item: CustomIngredientEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAliases(items: List<CustomIngredientAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSafetyRelations(items: List<CustomIngredientSafetyRelationEntity>)

    @Upsert
    suspend fun upsertSafetySource(item: SafetySourceEntity)

    @Query("DELETE FROM custom_ingredient_aliases WHERE customIngredientId = :ingredientId")
    suspend fun deleteAliases(ingredientId: String)

    @Query("DELETE FROM custom_ingredient_safety_relations WHERE customIngredientId = :ingredientId")
    suspend fun deleteSafetyRelations(ingredientId: String)

    @Transaction
    suspend fun replaceAggregate(
        ingredient: CustomIngredientEntity,
        aliases: List<CustomIngredientAliasEntity>,
        safetyRelations: List<CustomIngredientSafetyRelationEntity>,
        localSafetySource: SafetySourceEntity?,
    ) {
        if (localSafetySource != null) upsertSafetySource(localSafetySource)
        upsertIngredient(ingredient)
        deleteSafetyRelations(ingredient.id)
        deleteAliases(ingredient.id)
        if (aliases.isNotEmpty()) insertAliases(aliases)
        if (safetyRelations.isNotEmpty()) insertSafetyRelations(safetyRelations)
    }
}
