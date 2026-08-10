package com.rmm.recetasraquel.domain.usecase

import com.rmm.recetasraquel.domain.ingredient.FoodSafetyGroupOption
import com.rmm.recetasraquel.domain.ingredient.RecipeReviewNotice
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyAggregator
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyObservation
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository

interface RecipeSafetySummaryResolver {
    suspend fun resolve(recipe: Recipe): Result<RecipeSafetySummary>
}

class BuildRecipeSafetySummaryUseCase(
    private val catalogRepository: IngredientCatalogRepository,
    private val customIngredientRepository: CustomIngredientRepository,
    private val aggregator: RecipeSafetyAggregator = RecipeSafetyAggregator(),
) : RecipeSafetySummaryResolver {
    override suspend fun resolve(recipe: Recipe): Result<RecipeSafetySummary> = runCatching {
        catalogRepository.ensureCatalogImported().getOrThrow()
        val safetyGroups = customIngredientRepository.getSafetyGroups().getOrThrow().associateBy { it.id }

        val observations = mutableListOf<RecipeSafetyObservation>()
        val reviewNotices = mutableListOf<RecipeReviewNotice>()
        val regulatoryExemptions = mutableListOf<RegulatoryExemption>()

        recipe.ingredients.sortedBy { it.sortOrder }.forEach { ingredient ->
            val catalogIngredientId = ingredient.catalogIngredientId
            val customIngredientId = ingredient.customIngredientId

            when {
                catalogIngredientId != null && customIngredientId != null -> {
                    reviewNotices += ingredient.reviewNotice(
                        code = "AMBIGUOUS_INGREDIENT_ORIGIN",
                        message = "El ingrediente tiene más de un origen registrado. La información disponible puede ser incompleta. Requiere revisión.",
                    )
                }

                catalogIngredientId != null -> appendCatalogEvidence(
                    recipeIngredient = ingredient,
                    catalogIngredientId = catalogIngredientId,
                    observations = observations,
                    reviewNotices = reviewNotices,
                    regulatoryExemptions = regulatoryExemptions,
                )

                customIngredientId != null -> appendCustomEvidence(
                    recipeIngredient = ingredient,
                    customIngredientId = customIngredientId,
                    safetyGroups = safetyGroups,
                    observations = observations,
                    reviewNotices = reviewNotices,
                )

                else -> {
                    reviewNotices += ingredient.reviewNotice(
                        code = "UNRESOLVED_INGREDIENT_IDENTITY",
                        message = "La identidad del ingrediente no está vinculada a la biblioteca. La información disponible puede ser incompleta. Requiere revisión.",
                    )
                }
            }
        }

        aggregator.aggregate(
            observations = observations,
            reviewNotices = reviewNotices,
            regulatoryExemptions = regulatoryExemptions.distinctBy { it.id },
        )
    }

    private suspend fun appendCatalogEvidence(
        recipeIngredient: Ingredient,
        catalogIngredientId: String,
        observations: MutableList<RecipeSafetyObservation>,
        reviewNotices: MutableList<RecipeReviewNotice>,
        regulatoryExemptions: MutableList<RegulatoryExemption>,
    ) {
        val catalogIngredient = catalogRepository.getIngredient(catalogIngredientId).getOrThrow()
        if (catalogIngredient == null) {
            reviewNotices += recipeIngredient.reviewNotice(
                code = "CATALOG_INGREDIENT_NOT_AVAILABLE",
                message = "El ingrediente de catálogo ya no está disponible en la versión activa. La información disponible puede ser incompleta. Requiere revisión.",
            )
            return
        }

        catalogRepository.getSafetyRelations(catalogIngredientId).getOrThrow().forEach { relation ->
            val relationType = relation.relationType.toRecipeRelationTypeOrNull()
            if (relationType == null) {
                reviewNotices += recipeIngredient.reviewNotice(
                    code = "UNSUPPORTED_SAFETY_RELATION",
                    message = "Existe información de seguridad cuyo tipo no puede interpretarse. Requiere revisión.",
                )
                return@forEach
            }
            observations += RecipeSafetyObservation(
                ingredientId = recipeIngredient.id,
                ingredientName = recipeIngredient.name,
                safetyGroupId = relation.safetyGroupId,
                safetyGroupName = relation.safetyGroupName,
                relationType = relationType,
                evidenceLevel = relation.evidenceLevel,
                sourceId = relation.sourceId,
                sourceDetails = relation.sourceDetails,
                notes = relation.notes,
                reviewedAt = relation.reviewedAt,
            )
        }

        regulatoryExemptions += catalogRepository
            .getRegulatoryExemptions(catalogIngredientId)
            .getOrThrow()
    }

    private suspend fun appendCustomEvidence(
        recipeIngredient: Ingredient,
        customIngredientId: String,
        safetyGroups: Map<String, FoodSafetyGroupOption>,
        observations: MutableList<RecipeSafetyObservation>,
        reviewNotices: MutableList<RecipeReviewNotice>,
    ) {
        val customIngredient = customIngredientRepository.getIngredient(customIngredientId).getOrThrow()
        if (customIngredient == null) {
            reviewNotices += recipeIngredient.reviewNotice(
                code = "CUSTOM_INGREDIENT_NOT_AVAILABLE",
                message = "El ingrediente personalizado ya no está disponible. La información disponible puede ser incompleta. Requiere revisión.",
            )
            return
        }

        if (!customIngredient.compositionKnown) {
            reviewNotices += recipeIngredient.reviewNotice(
                code = "UNKNOWN_COMPOSITION",
                message = "La composición de este ingrediente personalizado está marcada como desconocida. La información disponible puede ser incompleta. Requiere revisión.",
            )
        }

        customIngredient.safetyRelations.forEach { relation ->
            val safetyGroup = safetyGroups[relation.safetyGroupId]
            if (safetyGroup == null) {
                reviewNotices += recipeIngredient.reviewNotice(
                    code = "SAFETY_GROUP_METADATA_NOT_AVAILABLE",
                    message = "Una declaración de seguridad no dispone de metadatos activos del grupo. Requiere revisión.",
                )
            }

            observations += RecipeSafetyObservation(
                ingredientId = recipeIngredient.id,
                ingredientName = recipeIngredient.name,
                safetyGroupId = relation.safetyGroupId,
                safetyGroupName = safetyGroup?.displayName ?: relation.safetyGroupId,
                relationType = RecipeSafetyRelationType.valueOf(relation.relationType.name),
                evidenceLevel = relation.evidenceLevel.name,
                sourceId = relation.sourceId,
                sourceDetails = relation.sourceDetails,
                notes = relation.notes,
                reviewedAt = relation.reviewedAt,
            )
        }
    }

    private fun Ingredient.reviewNotice(code: String, message: String) = RecipeReviewNotice(
        code = code,
        ingredientId = id,
        ingredientName = name,
        message = message,
    )

    private fun String.toRecipeRelationTypeOrNull(): RecipeSafetyRelationType? =
        runCatching { RecipeSafetyRelationType.valueOf(this) }.getOrNull()
}
