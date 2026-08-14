package com.rmm.recetasraquel.domain.usecase

import com.rmm.recetasraquel.domain.ingredient.CatalogIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.FoodSafetyGroupOption
import com.rmm.recetasraquel.domain.ingredient.IngredientComponentPresence
import com.rmm.recetasraquel.domain.ingredient.IngredientCompositionCoverage
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
import com.rmm.recetasraquel.util.SystemTimeProvider
import com.rmm.recetasraquel.util.TimeProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface RecipeSafetySummaryResolver {
    suspend fun resolve(recipe: Recipe): Result<RecipeSafetySummary>
}

class BuildRecipeSafetySummaryUseCase(
    private val catalogRepository: IngredientCatalogRepository,
    private val customIngredientRepository: CustomIngredientRepository,
    private val aggregator: RecipeSafetyAggregator = RecipeSafetyAggregator(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val regulatoryJurisdiction: String = DEFAULT_REGULATORY_JURISDICTION,
    private val regulatoryTimeZoneId: String = DEFAULT_REGULATORY_TIME_ZONE_ID,
) : RecipeSafetySummaryResolver {
    override suspend fun resolve(recipe: Recipe): Result<RecipeSafetySummary> = runCatching {
        catalogRepository.ensureCatalogImported().getOrThrow()
        val safetyGroups = customIngredientRepository.getSafetyGroups().getOrThrow().associateBy { it.id }
        val regulatoryAsOfDate = currentRegulatoryDateIso()

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
                    regulatoryAsOfDate = regulatoryAsOfDate,
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
            reviewNotices = reviewNotices.distinct(),
            regulatoryExemptions = regulatoryExemptions.distinctBy { it.id },
        )
    }

    private suspend fun appendCatalogEvidence(
        recipeIngredient: Ingredient,
        catalogIngredientId: String,
        regulatoryAsOfDate: String,
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

        appendDirectCatalogSafety(
            recipeIngredient = recipeIngredient,
            catalogIngredientId = catalogIngredientId,
            observations = observations,
            reviewNotices = reviewNotices,
        )

        appendStructuredCompositionEvidence(
            recipeIngredient = recipeIngredient,
            parentIngredientId = catalogIngredientId,
            path = listOf(catalogIngredient.canonicalName),
            inheritedPossibility = false,
            visited = setOf(catalogIngredientId),
            depth = 0,
            observations = observations,
            reviewNotices = reviewNotices,
        )

        regulatoryExemptions += catalogRepository
            .getApplicableRegulatoryExemptions(
                ingredientId = catalogIngredientId,
                jurisdiction = regulatoryJurisdiction,
                asOfDate = regulatoryAsOfDate,
            )
            .getOrThrow()
    }

    private suspend fun appendDirectCatalogSafety(
        recipeIngredient: Ingredient,
        catalogIngredientId: String,
        observations: MutableList<RecipeSafetyObservation>,
        reviewNotices: MutableList<RecipeReviewNotice>,
    ) {
        catalogRepository.getSafetyRelations(catalogIngredientId).getOrThrow().forEach { relation ->
            val relationType = relation.relationType.toRecipeRelationTypeOrNull()
            if (relationType == null) {
                reviewNotices += recipeIngredient.reviewNotice(
                    code = "UNSUPPORTED_SAFETY_RELATION",
                    message = "Existe información de seguridad cuyo tipo no puede interpretarse. Requiere revisión.",
                )
                return@forEach
            }
            observations += relation.toObservation(
                recipeIngredient = recipeIngredient,
                relationType = relationType,
            )
        }
    }

    private suspend fun appendStructuredCompositionEvidence(
        recipeIngredient: Ingredient,
        parentIngredientId: String,
        path: List<String>,
        inheritedPossibility: Boolean,
        visited: Set<String>,
        depth: Int,
        observations: MutableList<RecipeSafetyObservation>,
        reviewNotices: MutableList<RecipeReviewNotice>,
    ) {
        if (depth >= MAX_COMPOSITION_DEPTH) {
            reviewNotices += recipeIngredient.reviewNotice(
                code = "COMPOSITION_DEPTH_LIMIT",
                message = "La composición estructurada supera el límite de profundidad verificable. Requiere revisión.",
            )
            return
        }

        val composition = catalogRepository.getComposition(parentIngredientId).getOrThrow()
        if (composition.coverage == IngredientCompositionCoverage.NONE) return

        if (composition.coverage == IngredientCompositionCoverage.PARTIAL) {
            reviewNotices += recipeIngredient.reviewNotice(
                code = "PARTIAL_STRUCTURED_COMPOSITION",
                message = "La composición estructurada de ${path.last()} es parcial y puede no incluir todos sus componentes. Requiere revisar el producto o formulación concreta.",
            )
        }

        composition.components.forEach { component ->
            if (component.componentIngredientId in visited) {
                reviewNotices += recipeIngredient.reviewNotice(
                    code = "COMPOSITION_CYCLE_DETECTED",
                    message = "Se ha detectado una referencia circular en la composición estructurada. Requiere revisión.",
                )
                return@forEach
            }

            val componentIngredient = catalogRepository.getIngredient(component.componentIngredientId).getOrThrow()
            if (componentIngredient == null) {
                reviewNotices += recipeIngredient.reviewNotice(
                    code = "COMPONENT_INGREDIENT_NOT_AVAILABLE",
                    message = "Un componente estructurado ya no está disponible en el catálogo activo. Requiere revisión.",
                )
                return@forEach
            }

            val isPossible = inheritedPossibility || component.presence == IngredientComponentPresence.POSSIBLE
            val componentPath = path + componentIngredient.canonicalName

            if (component.presence == IngredientComponentPresence.POSSIBLE) {
                reviewNotices += recipeIngredient.reviewNotice(
                    code = "POSSIBLE_CATALOG_COMPONENT",
                    message = "${componentIngredient.canonicalName} figura como componente posible de ${path.last()}, no como presencia confirmada. Requiere revisar la composición concreta.",
                )
            }

            catalogRepository.getSafetyRelations(component.componentIngredientId).getOrThrow().forEach { relation ->
                val originalType = relation.relationType.toRecipeRelationTypeOrNull()
                if (originalType == null) {
                    reviewNotices += recipeIngredient.reviewNotice(
                        code = "UNSUPPORTED_COMPONENT_SAFETY_RELATION",
                        message = "Existe información de seguridad de un componente cuyo tipo no puede interpretarse. Requiere revisión.",
                    )
                    return@forEach
                }
                val aggregatedType = if (isPossible) {
                    RecipeSafetyRelationType.UNKNOWN
                } else {
                    originalType.asContainedComponentRelation()
                }
                observations += relation.toObservation(
                    recipeIngredient = recipeIngredient,
                    relationType = aggregatedType,
                    compositionPath = componentPath,
                )
            }

            appendStructuredCompositionEvidence(
                recipeIngredient = recipeIngredient,
                parentIngredientId = component.componentIngredientId,
                path = componentPath,
                inheritedPossibility = isPossible,
                visited = visited + component.componentIngredientId,
                depth = depth + 1,
                observations = observations,
                reviewNotices = reviewNotices,
            )
        }
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
                sourceDetails = relation.sourceDetails ?: relation.sourceId,
                notes = relation.notes,
                reviewedAt = relation.reviewedAt,
            )
        }
    }

    private fun CatalogIngredientSafetyRecord.toObservation(
        recipeIngredient: Ingredient,
        relationType: RecipeSafetyRelationType,
        compositionPath: List<String>? = null,
    ) = RecipeSafetyObservation(
        ingredientId = recipeIngredient.id,
        ingredientName = recipeIngredient.name,
        safetyGroupId = safetyGroupId,
        safetyGroupName = safetyGroupName,
        relationType = relationType,
        evidenceLevel = evidenceLevel,
        sourceId = sourceId,
        sourceDetails = listOfNotNull(
            sourceDetails,
            compositionPath?.joinToString(" → ")?.let { "Composición estructurada: $it" },
        ).filter(String::isNotBlank).joinToString(" · ").takeIf(String::isNotBlank),
        notes = notes,
        reviewedAt = reviewedAt,
    )

    private fun RecipeSafetyRelationType.asContainedComponentRelation(): RecipeSafetyRelationType = when (this) {
        RecipeSafetyRelationType.INHERENT_SOURCE,
        RecipeSafetyRelationType.CONTAINS,
        RecipeSafetyRelationType.DERIVED_FROM,
        RecipeSafetyRelationType.REGULATED_COMPONENT,
        -> RecipeSafetyRelationType.CONTAINS

        RecipeSafetyRelationType.DECLARED_MAY_CONTAIN -> RecipeSafetyRelationType.DECLARED_MAY_CONTAIN
        RecipeSafetyRelationType.POSSIBLE_CROSS_REACTIVITY -> RecipeSafetyRelationType.POSSIBLE_CROSS_REACTIVITY
        RecipeSafetyRelationType.UNKNOWN -> RecipeSafetyRelationType.UNKNOWN
    }

    private fun currentRegulatoryDateIso(): String = SimpleDateFormat(DATE_PATTERN, Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone(regulatoryTimeZoneId)
    }.format(Date(timeProvider.nowEpochMillis()))

    private fun Ingredient.reviewNotice(code: String, message: String) = RecipeReviewNotice(
        code = code,
        ingredientId = id,
        ingredientName = name,
        message = message,
    )

    private fun String.toRecipeRelationTypeOrNull(): RecipeSafetyRelationType? =
        runCatching { RecipeSafetyRelationType.valueOf(this) }.getOrNull()

    companion object {
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private const val MAX_COMPOSITION_DEPTH = 12
        const val DEFAULT_REGULATORY_JURISDICTION = "EU-ES"
        const val DEFAULT_REGULATORY_TIME_ZONE_ID = "Europe/Madrid"
    }
}
