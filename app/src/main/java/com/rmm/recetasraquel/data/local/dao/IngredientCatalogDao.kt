package com.rmm.recetasraquel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientComponentEntity
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

data class CatalogIngredientFrequencyRow(
    val id: String,
    val canonicalName: String,
    val categoryId: String,
    val categoryName: String,
    val defaultUnit: String?,
    val verificationStatus: String,
    val safetyRelationCount: Int,
    val regulatoryExemptionCount: Int,
    val recipeCount: Int,
)

data class CatalogIngredientSafetyRow(
    val safetyGroupId: String,
    val safetyGroupName: String,
    val safetyGroupJurisdiction: String,
    val relationType: String,
    val evidenceLevel: String,
    val sourceId: String,
    val sourceOrganization: String,
    val sourceTitle: String,
    val sourceReference: String,
    val notes: String?,
    val reviewedAt: String,
)

data class CatalogIngredientRelatedRow(
    val ingredientId: String,
    val canonicalName: String,
    val relationType: String,
    val direction: String,
)

data class CatalogIngredientComponentRow(
    val id: String,
    val parentIngredientId: String,
    val componentIngredientId: String,
    val componentName: String,
    val presenceType: String,
    val reviewedAt: String,
    val sourceReference: String?,
    val notes: String?,
)

@Dao
interface IngredientCatalogDao {
    @Query("SELECT * FROM catalog_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): CatalogMetadataEntity?

    @Query("SELECT * FROM safety_sources WHERE id IN (:sourceIds)")
    suspend fun getSafetySourcesByIds(sourceIds: List<String>): List<SafetySourceEntity>

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
            (
                SELECT COUNT(*)
                FROM regulatory_exemptions exemption
                WHERE exemption.ingredientId = ci.id
                  AND exemption.catalogVersion = (
                      SELECT catalogVersion FROM catalog_metadata WHERE `key` = 'master' LIMIT 1
                  )
                  AND exemption.isActive = 1
            ) AS regulatoryExemptionCount,
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
          AND ci.catalogRole = 'CULINARY'
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
            (
                SELECT COUNT(*)
                FROM regulatory_exemptions exemption
                WHERE exemption.ingredientId = ci.id
                  AND exemption.catalogVersion = (
                      SELECT catalogVersion FROM catalog_metadata WHERE `key` = 'master' LIMIT 1
                  )
                  AND exemption.isActive = 1
            ) AS regulatoryExemptionCount,
            COUNT(DISTINCT used.recipeId) AS recipeCount
        FROM ingredients used
        INNER JOIN catalog_ingredients ci ON ci.id = used.catalogIngredientId
        INNER JOIN ingredient_categories category ON category.id = ci.categoryId
        WHERE used.catalogIngredientId IS NOT NULL
          AND ci.isActive = 1
          AND ci.catalogRole = 'CULINARY'
          AND category.isActive = 1
        GROUP BY
            ci.id,
            ci.canonicalName,
            ci.categoryId,
            category.name,
            ci.defaultUnit,
            ci.verificationStatus
        HAVING COUNT(DISTINCT used.recipeId) >= :minimumRecipeCount
        ORDER BY recipeCount DESC, ci.canonicalName COLLATE NOCASE ASC, ci.id ASC
        LIMIT :limit
        """,
    )
    suspend fun getFrequentCulinaryIngredients(
        minimumRecipeCount: Int,
        limit: Int,
    ): List<CatalogIngredientFrequencyRow>

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
            (
                SELECT COUNT(*)
                FROM regulatory_exemptions exemption
                WHERE exemption.ingredientId = ci.id
                  AND exemption.catalogVersion = (
                      SELECT catalogVersion FROM catalog_metadata WHERE `key` = 'master' LIMIT 1
                  )
                  AND exemption.isActive = 1
            ) AS regulatoryExemptionCount,
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

    @Query("SELECT description FROM catalog_ingredients WHERE id = :ingredientId AND isActive = 1 LIMIT 1")
    suspend fun getIngredientDescription(ingredientId: String): String?

    @Query("SELECT compositionCoverage FROM catalog_ingredients WHERE id = :ingredientId AND isActive = 1 LIMIT 1")
    suspend fun getIngredientCompositionCoverage(ingredientId: String): String?

    @Query(
        "SELECT alias FROM ingredient_aliases " +
            "WHERE ingredientId = :ingredientId " +
            "ORDER BY alias COLLATE NOCASE ASC, id ASC",
    )
    suspend fun getIngredientAliases(ingredientId: String): List<String>

    @Query(
        """
        SELECT
            related.id AS ingredientId,
            related.canonicalName AS canonicalName,
            relation.relationType AS relationType,
            'PARENT' AS direction
        FROM catalog_ingredient_relations relation
        INNER JOIN catalog_ingredients related ON related.id = relation.parentIngredientId
        WHERE relation.childIngredientId = :ingredientId
          AND relation.isActive = 1
          AND related.isActive = 1
          AND related.catalogRole = 'CULINARY'
        UNION ALL
        SELECT
            related.id AS ingredientId,
            related.canonicalName AS canonicalName,
            relation.relationType AS relationType,
            'CHILD' AS direction
        FROM catalog_ingredient_relations relation
        INNER JOIN catalog_ingredients related ON related.id = relation.childIngredientId
        WHERE relation.parentIngredientId = :ingredientId
          AND relation.isActive = 1
          AND related.isActive = 1
          AND related.catalogRole = 'CULINARY'
        """,
    )
    suspend fun getCulinaryRelatedPresentations(ingredientId: String): List<CatalogIngredientRelatedRow>

    @Query(
        """
        SELECT
            component.id AS id,
            component.parentIngredientId AS parentIngredientId,
            component.componentIngredientId AS componentIngredientId,
            child.canonicalName AS componentName,
            component.presenceType AS presenceType,
            component.reviewedAt AS reviewedAt,
            component.sourceReference AS sourceReference,
            component.notes AS notes
        FROM catalog_ingredient_components component
        INNER JOIN catalog_ingredients child ON child.id = component.componentIngredientId
        WHERE component.parentIngredientId = :ingredientId
          AND component.isActive = 1
          AND child.isActive = 1
        ORDER BY
            CASE component.presenceType WHEN 'REQUIRED' THEN 0 ELSE 1 END,
            child.canonicalName COLLATE NOCASE ASC,
            component.id ASC
        """,
    )
    suspend fun getActiveIngredientComponents(ingredientId: String): List<CatalogIngredientComponentRow>

    @Query("SELECT COUNT(*) FROM catalog_ingredient_relations WHERE isActive = 1")
    suspend fun countActiveIngredientRelations(): Int

    @Query("SELECT COUNT(*) FROM catalog_ingredient_components WHERE isActive = 1")
    suspend fun countActiveIngredientComponents(): Int

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
            safetyGroup.jurisdiction AS safetyGroupJurisdiction,
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

    @Query(
        """
        SELECT COUNT(*)
        FROM regulatory_exemptions
        WHERE catalogVersion = (
            SELECT catalogVersion FROM catalog_metadata WHERE `key` = 'master' LIMIT 1
        )
          AND isActive = 1
        """,
    )
    suspend fun countActiveRegulatoryExemptions(): Int

    @Query("SELECT COUNT(*) FROM regulatory_exemptions")
    suspend fun countRegulatoryExemptionSnapshots(): Int

    @Query(
        """
        SELECT *
        FROM regulatory_exemptions
        WHERE ingredientId = :ingredientId
          AND catalogVersion = (
              SELECT catalogVersion FROM catalog_metadata WHERE `key` = 'master' LIMIT 1
          )
          AND isActive = 1
        ORDER BY safetyGroupId ASC, jurisdiction ASC, regulatoryEffect ASC, id ASC
        """,
    )
    suspend fun getRegulatoryExemptionsForIngredient(ingredientId: String): List<RegulatoryExemptionEntity>

    @Query(
        """
        SELECT *
        FROM regulatory_exemptions
        WHERE ingredientId = :ingredientId
          AND catalogVersion = :catalogVersion
        ORDER BY safetyGroupId ASC, jurisdiction ASC, regulatoryEffect ASC, id ASC
        """,
    )
    suspend fun getRegulatoryExemptionsForIngredientAtCatalogVersion(
        ingredientId: String,
        catalogVersion: Int,
    ): List<RegulatoryExemptionEntity>

    @Query(
        """
        SELECT *
        FROM regulatory_exemptions
        WHERE ingredientId = :ingredientId
          AND catalogVersion = (
              SELECT catalogVersion FROM catalog_metadata WHERE `key` = 'master' LIMIT 1
          )
          AND jurisdiction = :jurisdiction
          AND isActive = 1
          AND (effectiveFrom IS NULL OR effectiveFrom <= :asOfDate)
          AND (effectiveTo IS NULL OR effectiveTo >= :asOfDate)
        ORDER BY safetyGroupId ASC, regulatoryEffect ASC, id ASC
        """,
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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIngredientComponents(items: List<CatalogIngredientComponentEntity>)

    @Upsert
    suspend fun upsertSafetyGroups(items: List<FoodSafetyGroupEntity>)

    @Upsert
    suspend fun upsertSafetySources(items: List<SafetySourceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSafetyRelations(items: List<IngredientSafetyRelationEntity>)

    @Upsert
    suspend fun upsertRegulatoryExemptions(items: List<RegulatoryExemptionEntity>)

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

    @Query("DELETE FROM catalog_ingredient_components")
    suspend fun deleteAllIngredientComponents()

    @Query("DELETE FROM ingredient_safety_relations")
    suspend fun deleteAllSafetyRelations()

    @Transaction
    suspend fun replaceShippedCatalog(
        categories: List<IngredientCategoryEntity>,
        ingredients: List<CatalogIngredientEntity>,
        aliases: List<IngredientAliasEntity>,
        ingredientRelations: List<CatalogIngredientRelationEntity>,
        ingredientComponents: List<CatalogIngredientComponentEntity>,
        safetyGroups: List<FoodSafetyGroupEntity>,
        safetySources: List<SafetySourceEntity>,
        safetyRelations: List<IngredientSafetyRelationEntity>,
        regulatoryExemptions: List<RegulatoryExemptionEntity>,
        metadata: CatalogMetadataEntity,
    ) {
        deactivateAllCategories()
        deactivateAllIngredients()
        deactivateAllSafetyGroups()
        deleteAllAliases()
        deleteAllIngredientRelations()
        deleteAllIngredientComponents()
        deleteAllSafetyRelations()
        upsertCategories(categories)
        upsertIngredients(ingredients)
        insertAliases(aliases)
        insertIngredientRelations(ingredientRelations)
        insertIngredientComponents(ingredientComponents)
        upsertSafetyGroups(safetyGroups)
        upsertSafetySources(safetySources)
        insertSafetyRelations(safetyRelations)
        upsertRegulatoryExemptions(regulatoryExemptions)
        upsertMetadata(metadata)
    }
}
