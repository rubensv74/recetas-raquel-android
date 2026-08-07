package com.rmm.recetasraquel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientEntity
import com.rmm.recetasraquel.data.local.entity.CatalogMetadataEntity
import com.rmm.recetasraquel.data.local.entity.FoodSafetyGroupEntity
import com.rmm.recetasraquel.data.local.entity.IngredientAliasEntity
import com.rmm.recetasraquel.data.local.entity.IngredientCategoryEntity
import com.rmm.recetasraquel.data.local.entity.IngredientSafetyRelationEntity
import com.rmm.recetasraquel.data.local.entity.SafetySourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientCatalogDao {
    @Query("SELECT * FROM catalog_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): CatalogMetadataEntity?

    @Query("SELECT * FROM ingredient_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeActiveCategories(): Flow<List<IngredientCategoryEntity>>

    @Query("SELECT COUNT(*) FROM catalog_ingredients WHERE isActive = 1")
    suspend fun countActiveIngredients(): Int

    @Query("SELECT COUNT(*) FROM ingredient_aliases")
    suspend fun countAliases(): Int

    @Upsert
    suspend fun upsertCategories(items: List<IngredientCategoryEntity>)

    @Upsert
    suspend fun upsertIngredients(items: List<CatalogIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAliases(items: List<IngredientAliasEntity>)

    @Upsert
    suspend fun upsertSafetyGroups(items: List<FoodSafetyGroupEntity>)

    @Upsert
    suspend fun upsertSafetySources(items: List<SafetySourceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSafetyRelations(items: List<IngredientSafetyRelationEntity>)

    @Upsert
    suspend fun upsertMetadata(item: CatalogMetadataEntity)

    @Query("UPDATE ingredient_categories SET isActive = 0")
    suspend fun deactivateAllCategories()

    @Query("UPDATE catalog_ingredients SET isActive = 0")
    suspend fun deactivateAllIngredients()

    @Query("UPDATE food_safety_groups SET isActive = 0")
    suspend fun deactivateAllSafetyGroups()

    @Query("DELETE FROM ingredient_aliases")
    suspend fun deleteAllAliases()

    @Query("DELETE FROM ingredient_safety_relations")
    suspend fun deleteAllSafetyRelations()

    @Transaction
    suspend fun replaceShippedCatalog(
        categories: List<IngredientCategoryEntity>,
        ingredients: List<CatalogIngredientEntity>,
        aliases: List<IngredientAliasEntity>,
        safetyGroups: List<FoodSafetyGroupEntity>,
        safetySources: List<SafetySourceEntity>,
        safetyRelations: List<IngredientSafetyRelationEntity>,
        metadata: CatalogMetadataEntity,
    ) {
        deleteAllSafetyRelations()
        deleteAllAliases()
        deactivateAllIngredients()
        deactivateAllCategories()
        deactivateAllSafetyGroups()

        if (categories.isNotEmpty()) upsertCategories(categories)
        if (safetyGroups.isNotEmpty()) upsertSafetyGroups(safetyGroups)
        if (safetySources.isNotEmpty()) upsertSafetySources(safetySources)
        if (ingredients.isNotEmpty()) upsertIngredients(ingredients)
        if (aliases.isNotEmpty()) insertAliases(aliases)
        if (safetyRelations.isNotEmpty()) insertSafetyRelations(safetyRelations)
        upsertMetadata(metadata)
    }
}
