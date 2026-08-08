package com.rmm.recetasraquel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientEntity
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientRelationEntity
import com.rmm.recetasraquel.data.local.entity.CatalogMetadataEntity
import com.rmm.recetasraquel.data.local.entity.FoodSafetyGroupEntity
import com.rmm.recetasraquel.data.local.entity.IngredientAliasEntity
import com.rmm.recetasraquel.data.local.entity.IngredientCategoryEntity
import com.rmm.recetasraquel.data.local.entity.IngredientSafetyRelationEntity
import com.rmm.recetasraquel.data.local.entity.RegulatoryExemptionEntity
import com.rmm.recetasraquel.data.local.entity.SafetySourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientCatalogDao {
    @Query("SELECT * FROM catalog_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): CatalogMetadataEntity?

    @Query("SELECT * FROM ingredient_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeActiveCategories(): Flow<List<IngredientCategoryEntity>>

    @Query("SELECT COUNT(*) FROM ingredient_categories WHERE isActive = 1")
    suspend fun countActiveCategories(): Int

    @Query("SELECT COUNT(*) FROM catalog_ingredients WHERE isActive = 1")
    suspend fun countActiveIngredients(): Int

    @Query("SELECT COUNT(*) FROM ingredient_aliases")
    suspend fun countAliases(): Int

    @Query("SELECT COUNT(*) FROM catalog_ingredient_relations WHERE isActive = 1")
    suspend fun countActiveIngredientRelations(): Int

    @Query(
        "SELECT * FROM catalog_ingredient_relations " +
            "WHERE childIngredientId = :ingredientId AND isActive = 1 " +
            "ORDER BY relationType ASC, parentIngredientId ASC, id ASC",
    )
    suspend fun getParentRelations(ingredientId: String): List<CatalogIngredientRelationEntity>

    @Query(
        "SELECT * FROM catalog_ingredient_relations " +
            "WHERE parentIngredientId = :ingredientId AND isActive = 1 " +
            "ORDER BY relationType ASC, childIngredientId ASC, id ASC",
    )
    suspend fun getChildRelations(ingredientId: String): List<CatalogIngredientRelationEntity>

    @Query("SELECT COUNT(*) FROM food_safety_groups WHERE isActive = 1")
    suspend fun countActiveSafetyGroups(): Int

    @Query("SELECT COUNT(*) FROM ingredient_safety_relations")
    suspend fun countSafetyRelations(): Int

    @Query(
        "SELECT * FROM ingredient_safety_relations " +
            "WHERE ingredientId = :ingredientId " +
            "ORDER BY safetyGroupId ASC, relationType ASC, id ASC",
    )
    suspend fun getSafetyRelationsForIngredient(ingredientId: String): List<IngredientSafetyRelationEntity>

    @Query("SELECT COUNT(*) FROM regulatory_exemptions WHERE isActive = 1")
    suspend fun countActiveRegulatoryExemptions(): Int

    @Query(
        "SELECT * FROM regulatory_exemptions " +
            "WHERE ingredientId = :ingredientId AND isActive = 1 " +
            "ORDER BY safetyGroupId ASC, jurisdiction ASC, regulatoryEffect ASC, id ASC",
    )
    suspend fun getRegulatoryExemptionsForIngredient(ingredientId: String): List<RegulatoryExemptionEntity>

    @Upsert
    suspend fun upsertCategories(items: List<IngredientCategoryEntity>)

    @Upsert
    suspend fun upsertIngredients(items: List<CatalogIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAliases(items: List<IngredientAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIngredientRelations(items: List<CatalogIngredientRelationEntity>)

    @Upsert
    suspend fun upsertSafetyGroups(items: List<FoodSafetyGroupEntity>)

    @Upsert
    suspend fun upsertSafetySources(items: List<SafetySourceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSafetyRelations(items: List<IngredientSafetyRelationEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegulatoryExemptions(items: List<RegulatoryExemptionEntity>)

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

    @Query("DELETE FROM catalog_ingredient_relations")
    suspend fun deleteAllIngredientRelations()

    @Query("DELETE FROM ingredient_safety_relations")
    suspend fun deleteAllSafetyRelations()

    @Query("DELETE FROM regulatory_exemptions")
    suspend fun deleteAllRegulatoryExemptions()

    @Transaction
    suspend fun replaceShippedCatalog(
        categories: List<IngredientCategoryEntity>,
        ingredients: List<CatalogIngredientEntity>,
        aliases: List<IngredientAliasEntity>,
        ingredientRelations: List<CatalogIngredientRelationEntity>,
        safetyGroups: List<FoodSafetyGroupEntity>,
        safetySources: List<SafetySourceEntity>,
        safetyRelations: List<IngredientSafetyRelationEntity>,
        regulatoryExemptions: List<RegulatoryExemptionEntity>,
        metadata: CatalogMetadataEntity,
    ) {
        deleteAllRegulatoryExemptions()
        deleteAllSafetyRelations()
        deleteAllIngredientRelations()
        deleteAllAliases()
        deactivateAllIngredients()
        deactivateAllCategories()
        deactivateAllSafetyGroups()

        if (categories.isNotEmpty()) upsertCategories(categories)
        if (safetyGroups.isNotEmpty()) upsertSafetyGroups(safetyGroups)
        if (safetySources.isNotEmpty()) upsertSafetySources(safetySources)
        if (ingredients.isNotEmpty()) upsertIngredients(ingredients)
        if (aliases.isNotEmpty()) insertAliases(aliases)
        if (ingredientRelations.isNotEmpty()) insertIngredientRelations(ingredientRelations)
        if (safetyRelations.isNotEmpty()) insertSafetyRelations(safetyRelations)
        if (regulatoryExemptions.isNotEmpty()) insertRegulatoryExemptions(regulatoryExemptions)
        upsertMetadata(metadata)
    }
}
