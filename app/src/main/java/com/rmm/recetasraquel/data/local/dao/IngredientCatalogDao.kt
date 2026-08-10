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

data class CatalogIngredientSearchRow(
    val id: String,
    val canonicalName: String,
    val categoryId: String,
    val categoryName: String,
    val defaultUnit: String?,
    val verificationStatus: String,
    val safetyRelationCount: Int,
    val regulatoryExemptionCount: Int,
    val searchRank: Int,
)

data class CatalogIngredientSafetyRow(
    val safetyGroupId: String,
    val safetyGroupName: String,
    val relationType: String,
    val evidenceLevel: String,
    val sourceId: String,
    val sourceOrganization: String,
    val sourceTitle: String,
    val sourceReference: String,
    val notes: String?,
    val reviewedAt: String,
)

@Dao
interface IngredientCatalogDao {
    @Query("SELECT * FROM catalog_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): CatalogMetadataEntity?

    @Query("SELECT * FROM ingredient_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeActiveCategories(): Flow<List<IngredientCategoryEntity>>

    @Query("SELECT * FROM ingredient_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getActiveCategories(): List<IngredientCategoryEntity>

    @Query("SELECT COUNT(*) FROM ingredient_categories WHERE isActive = 1")
    suspend fun countActiveCategories(): Int

    @Query("SELECT COUNT(*) FROM catalog_ingredients WHERE isActive = 1")
    suspend fun countActiveIngredients(): Int

    @Query("SELECT COUNT(*) FROM ingredient_aliases")
    suspend fun countAliases(): Int

    @Query(
        """
        SELECT
            ci.id AS id,
            ci.canonicalName AS canonicalName,
            ci.categoryId AS categoryId,
            category.name AS categoryName,
            ci.defaultUnit AS defaultUnit,
            ci.verificationStatus AS verificationStatus,
            (SELECT COUNT(*) FROM ingredient_safety_relations safety WHERE safety.ingredientId = ci.id) AS safetyRelationCount,
            (SELECT COUNT(*) FROM regulatory_exemptions exemption WHERE exemption.ingredientId = ci.id AND exemption.isActive = 1) AS regulatoryExemptionCount,
            CASE
                WHEN :normalizedQuery = '' THEN 0
                WHEN ci.normalizedName = :normalizedQuery OR EXISTS (
                    SELECT 1 FROM ingredient_aliases exactAlias
                    WHERE exactAlias.ingredientId = ci.id AND exactAlias.normalizedAlias = :normalizedQuery
                ) THEN 0
                WHEN substr(ci.normalizedName, 1, length(:normalizedQuery)) = :normalizedQuery OR EXISTS (
                    SELECT 1 FROM ingredient_aliases prefixAlias
                    WHERE prefixAlias.ingredientId = ci.id
                      AND substr(prefixAlias.normalizedAlias, 1, length(:normalizedQuery)) = :normalizedQuery
                ) THEN 1
                ELSE 2
            END AS searchRank
        FROM catalog_ingredients ci
        INNER JOIN ingredient_categories category ON category.id = ci.categoryId
        WHERE ci.isActive = 1
          AND category.isActive = 1
          AND (:categoryId IS NULL OR ci.categoryId = :categoryId)
          AND (
              :normalizedQuery = ''
              OR instr(ci.normalizedName, :normalizedQuery) > 0
              OR EXISTS (
                  SELECT 1 FROM ingredient_aliases matchingAlias
                  WHERE matchingAlias.ingredientId = ci.id
                    AND instr(matchingAlias.normalizedAlias, :normalizedQuery) > 0
              )
          )
        ORDER BY searchRank ASC, ci.canonicalName COLLATE NOCASE ASC, ci.id ASC
        LIMIT :limit
        """,
    )
    suspend fun searchActiveIngredients(
        normalizedQuery: String,
        categoryId: String?,
        limit: Int,
    ): List<CatalogIngredientSearchRow>

    @Query(
        """
        SELECT
            ci.id AS id,
            ci.canonicalName AS canonicalName,
            ci.categoryId AS categoryId,
            category.name AS categoryName,
            ci.defaultUnit AS defaultUnit,
            ci.verificationStatus AS verificationStatus,
            (SELECT COUNT(*) FROM ingredient_safety_relations safety WHERE safety.ingredientId = ci.id) AS safetyRelationCount,
            (SELECT COUNT(*) FROM regulatory_exemptions exemption WHERE exemption.ingredientId = ci.id AND exemption.isActive = 1) AS regulatoryExemptionCount,
            0 AS searchRank
        FROM catalog_ingredients ci
        INNER JOIN ingredient_categories category ON category.id = ci.categoryId
        WHERE ci.id = :ingredientId
          AND ci.isActive = 1
          AND category.isActive = 1
        LIMIT 1
        """,
    )
    suspend fun getActiveIngredientSummary(ingredientId: String): CatalogIngredientSearchRow?

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

    @Query(
        """
        SELECT
            relation.safetyGroupId AS safetyGroupId,
            safetyGroup.displayName AS safetyGroupName,
            relation.relationType AS relationType,
            relation.evidenceLevel AS evidenceLevel,
            relation.sourceId AS sourceId,
            source.organization AS sourceOrganization,
            source.title AS sourceTitle,
            source.officialReference AS sourceReference,
            relation.notes AS notes,
            relation.reviewedAt AS reviewedAt
        FROM ingredient_safety_relations relation
        INNER JOIN food_safety_groups safetyGroup ON safetyGroup.id = relation.safetyGroupId
        INNER JOIN safety_sources source ON source.id = relation.sourceId
        WHERE relation.ingredientId = :ingredientId
        ORDER BY safetyGroup.displayName COLLATE NOCASE ASC, relation.relationType ASC, relation.id ASC
        """,
    )
    suspend fun getSafetyRelationDetailsForIngredient(ingredientId: String): List<CatalogIngredientSafetyRow>

    @Query("SELECT COUNT(*) FROM regulatory_exemptions WHERE isActive = 1")
    suspend fun countActiveRegulatoryExemptions(): Int

    @Query(
        "SELECT * FROM regulatory_exemptions " +
            "WHERE ingredientId = :ingredientId AND isActive = 1 " +
            "ORDER BY safetyGroupId ASC, jurisdiction ASC, regulatoryEffect ASC, id ASC",
    )
    suspend fun getRegulatoryExemptionsForIngredient(ingredientId: String): List<RegulatoryExemptionEntity>

    @Query(
        "SELECT * FROM regulatory_exemptions " +
            "WHERE ingredientId = :ingredientId " +
            "AND jurisdiction = :jurisdiction " +
            "AND isActive = 1 " +
            "AND (effectiveFrom IS NULL OR effectiveFrom <= :asOfDate) " +
            "AND (effectiveTo IS NULL OR effectiveTo >= :asOfDate) " +
            "ORDER BY safetyGroupId ASC, regulatoryEffect ASC, id ASC",
    )
    suspend fun getApplicableRegulatoryExemptionsForIngredient(
        ingredientId: String,
        jurisdiction: String,
        asOfDate: String,
    ): List<RegulatoryExemptionEntity>

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
