package com.rmm.recetasraquel.data.repository

import com.rmm.recetasraquel.data.local.dao.CustomIngredientAggregate
import com.rmm.recetasraquel.data.local.dao.CustomIngredientDao
import com.rmm.recetasraquel.data.local.entity.CustomIngredientAliasEntity
import com.rmm.recetasraquel.data.local.entity.CustomIngredientEntity
import com.rmm.recetasraquel.data.local.entity.CustomIngredientSafetyRelationEntity
import com.rmm.recetasraquel.data.local.entity.SafetySourceEntity
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientDraft
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientRecord
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyEvidence
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientType
import com.rmm.recetasraquel.domain.ingredient.FoodSafetyGroupOption
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogCategory
import com.rmm.recetasraquel.domain.ingredient.IngredientTextNormalizer
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class LocalCustomIngredientRepository(
    private val dao: CustomIngredientDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
) : CustomIngredientRepository {
    override suspend fun createIngredient(input: CustomIngredientDraft): Result<String> = runCatching {
        val now = timeProvider.nowEpochMillis()
        val id = idGenerator.newId()
        val prepared = prepareAggregate(
            ingredientId = id,
            createdAt = now,
            updatedAt = now,
            input = input,
        )
        dao.replaceAggregate(
            ingredient = prepared.ingredient,
            aliases = prepared.aliases,
            safetyRelations = prepared.safetyRelations,
            localSafetySource = prepared.localSafetySource,
        )
        id
    }

    override suspend fun updateIngredient(
        ingredientId: String,
        input: CustomIngredientDraft,
    ): Result<Unit> = runCatching {
        val existing = dao.getIngredient(ingredientId)
            ?: error("Custom ingredient not found: $ingredientId")
        val prepared = prepareAggregate(
            ingredientId = ingredientId,
            createdAt = existing.createdAt,
            updatedAt = timeProvider.nowEpochMillis(),
            input = input,
        )
        dao.replaceAggregate(
            ingredient = prepared.ingredient,
            aliases = prepared.aliases,
            safetyRelations = prepared.safetyRelations,
            localSafetySource = prepared.localSafetySource,
        )
    }

    override suspend fun getIngredient(ingredientId: String): Result<CustomIngredientRecord?> = runCatching {
        dao.getAggregate(ingredientId)?.toDomain()
    }

    override suspend fun getCategories(): Result<List<IngredientCatalogCategory>> = runCatching {
        dao.getActiveCategories().map { category ->
            IngredientCatalogCategory(
                id = category.id,
                code = category.code,
                name = category.name,
                sortOrder = category.sortOrder,
                iconKey = category.iconKey,
            )
        }
    }

    override suspend fun getSafetyGroups(): Result<List<FoodSafetyGroupOption>> = runCatching {
        dao.getActiveSafetyGroups().map { group ->
            FoodSafetyGroupOption(
                id = group.id,
                code = group.code,
                displayName = group.displayName,
                conditionType = group.conditionType,
                regulatoryStatus = group.regulatoryStatus,
                jurisdiction = group.jurisdiction,
            )
        }
    }

    private suspend fun prepareAggregate(
        ingredientId: String,
        createdAt: Long,
        updatedAt: Long,
        input: CustomIngredientDraft,
    ): PreparedAggregate {
        val name = input.name.trim()
        require(name.isNotEmpty()) { "Custom ingredient name is required" }
        val normalizedName = IngredientTextNormalizer.normalize(name)
        require(normalizedName.isNotEmpty()) { "Custom ingredient normalized name is required" }

        val categoryId = input.categoryId?.trim()?.takeIf(String::isNotEmpty)
        if (categoryId != null) {
            requireNotNull(dao.getActiveCategory(categoryId)) { "Unknown or inactive category: $categoryId" }
        }

        val isCommercialProduct = input.type == CustomIngredientType.COMMERCIAL_PRODUCT
        val brand = if (isCommercialProduct) input.brand.cleaned() else null
        val tradeName = if (isCommercialProduct) input.tradeName.cleaned() else null
        val labelReadAt = if (isCommercialProduct) input.labelReadAt.cleaned() else null
        if (labelReadAt != null) LocalDate.parse(labelReadAt)

        val aliases = input.aliases
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { alias -> alias to IngredientTextNormalizer.normalize(alias) }
            .filter { (_, normalized) -> normalized.isNotEmpty() && normalized != normalizedName }
            .distinctBy { (_, normalized) -> normalized }
            .toList()

        val semanticRelations = mutableSetOf<Pair<String, CustomIngredientSafetyRelationType>>()
        input.safetyDeclarations.forEach { declaration ->
            require(declaration.safetyGroupId.isNotBlank()) { "Safety group is required" }
            requireNotNull(dao.getActiveSafetyGroup(declaration.safetyGroupId)) {
                "Unknown or inactive safety group: ${declaration.safetyGroupId}"
            }
            require(semanticRelations.add(declaration.safetyGroupId to declaration.relationType)) {
                "Duplicate custom safety relation for ${declaration.safetyGroupId}/${declaration.relationType}"
            }
        }

        val reviewedAt = Instant.ofEpochMilli(updatedAt).atZone(ZoneOffset.UTC).toLocalDate().toString()
        val localSafetySource = if (input.safetyDeclarations.isNotEmpty()) {
            localSafetySource(reviewedAt)
        } else {
            null
        }

        return PreparedAggregate(
            ingredient = CustomIngredientEntity(
                id = ingredientId,
                name = name,
                normalizedName = normalizedName,
                categoryId = categoryId,
                defaultUnit = input.defaultUnit.cleaned(),
                ingredientType = input.type.name,
                brand = brand,
                tradeName = tradeName,
                compositionKnown = input.compositionKnown,
                labelReadAt = labelReadAt,
                notes = input.notes.cleaned(),
                createdAt = createdAt,
                updatedAt = updatedAt,
                isActive = true,
            ),
            aliases = aliases.map { (alias, normalizedAlias) ->
                CustomIngredientAliasEntity(
                    id = idGenerator.newId(),
                    customIngredientId = ingredientId,
                    alias = alias,
                    normalizedAlias = normalizedAlias,
                    languageCode = "es-ES",
                    aliasType = "COMMON",
                )
            },
            safetyRelations = input.safetyDeclarations.map { declaration ->
                CustomIngredientSafetyRelationEntity(
                    id = idGenerator.newId(),
                    customIngredientId = ingredientId,
                    safetyGroupId = declaration.safetyGroupId,
                    relationType = declaration.relationType.name,
                    evidenceLevel = declaration.evidenceLevel.name,
                    sourceId = LOCAL_USER_DECLARED_SOURCE_ID,
                    sourceDetails = declaration.sourceDetails.cleaned(),
                    notes = declaration.notes.cleaned(),
                    reviewedAt = reviewedAt,
                )
            },
            localSafetySource = localSafetySource,
        )
    }

    private fun localSafetySource(reviewedAt: String) = SafetySourceEntity(
        id = LOCAL_USER_DECLARED_SOURCE_ID,
        organization = "Usuario",
        title = "Declaración local del usuario",
        officialReference = LOCAL_USER_DECLARED_SOURCE_ID,
        jurisdiction = "LOCAL",
        publicationDate = null,
        reviewDate = reviewedAt,
        documentStatus = "USER_DECLARED",
        officialUrl = null,
    )

    private fun CustomIngredientAggregate.toDomain(): CustomIngredientRecord = CustomIngredientRecord(
        id = ingredient.id,
        name = ingredient.name,
        categoryId = ingredient.categoryId,
        defaultUnit = ingredient.defaultUnit,
        type = CustomIngredientType.valueOf(ingredient.ingredientType),
        aliases = aliases.map { it.alias },
        brand = ingredient.brand,
        tradeName = ingredient.tradeName,
        compositionKnown = ingredient.compositionKnown,
        labelReadAt = ingredient.labelReadAt,
        notes = ingredient.notes,
        createdAt = ingredient.createdAt,
        updatedAt = ingredient.updatedAt,
        safetyRelations = safetyRelations.map { relation ->
            CustomIngredientSafetyRecord(
                id = relation.id,
                safetyGroupId = relation.safetyGroupId,
                relationType = CustomIngredientSafetyRelationType.valueOf(relation.relationType),
                evidenceLevel = CustomIngredientSafetyEvidence.valueOf(relation.evidenceLevel),
                sourceId = relation.sourceId,
                sourceDetails = relation.sourceDetails,
                notes = relation.notes,
                reviewedAt = relation.reviewedAt,
            )
        },
    )

    private fun String?.cleaned(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private data class PreparedAggregate(
        val ingredient: CustomIngredientEntity,
        val aliases: List<CustomIngredientAliasEntity>,
        val safetyRelations: List<CustomIngredientSafetyRelationEntity>,
        val localSafetySource: SafetySourceEntity?,
    )

    companion object {
        const val LOCAL_USER_DECLARED_SOURCE_ID = "LOCAL_USER_DECLARED"
    }
}
