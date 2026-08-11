package com.rmm.recetasraquel.domain.ingredient

enum class RecipeSafetyPresentationState {
    PRESENCIA_IDENTIFICADA,
    DERIVADO_IDENTIFICADO,
    PUEDE_CONTENER_DECLARADO,
    POSIBLE_REACTIVIDAD_CRUZADA,
    REQUIERE_REVISION,
}

enum class RecipeSafetyRelationType {
    INHERENT_SOURCE,
    CONTAINS,
    DERIVED_FROM,
    REGULATED_COMPONENT,
    DECLARED_MAY_CONTAIN,
    POSSIBLE_CROSS_REACTIVITY,
    UNKNOWN,
}

data class RecipeSafetyObservation(
    val ingredientId: String,
    val ingredientName: String,
    val safetyGroupId: String,
    val safetyGroupName: String,
    val relationType: RecipeSafetyRelationType,
    val evidenceLevel: String,
    val sourceId: String?,
    val sourceDetails: String? = null,
    val notes: String? = null,
    val reviewedAt: String? = null,
)

data class RecipeReviewNotice(
    val code: String,
    val ingredientId: String? = null,
    val ingredientName: String? = null,
    val message: String,
)

data class RecipeSafetyGroupSummary(
    val safetyGroupId: String,
    val safetyGroupName: String,
    val presentationState: RecipeSafetyPresentationState,
    val observations: List<RecipeSafetyObservation>,
)

data class RecipeSafetySummary(
    val groups: List<RecipeSafetyGroupSummary>,
    val reviewNotices: List<RecipeReviewNotice>,
    val regulatoryExemptions: List<RegulatoryExemption>,
)

class RecipeSafetyAggregator {
    fun aggregate(
        observations: List<RecipeSafetyObservation>,
        reviewNotices: List<RecipeReviewNotice> = emptyList(),
        regulatoryExemptions: List<RegulatoryExemption> = emptyList(),
    ): RecipeSafetySummary {
        val groups = observations
            .groupBy { it.safetyGroupId }
            .map { (groupId, groupObservations) ->
                val sortedObservations = groupObservations.sortedWith(
                    compareBy<RecipeSafetyObservation>(
                        { presentationPriority(it.relationType) },
                        { it.ingredientName.lowercase() },
                        { it.ingredientId },
                        { it.relationType.name },
                    ),
                )
                RecipeSafetyGroupSummary(
                    safetyGroupId = groupId,
                    safetyGroupName = sortedObservations.first().safetyGroupName,
                    presentationState = presentationState(sortedObservations.first().relationType),
                    observations = sortedObservations,
                )
            }
            .sortedWith(compareBy({ it.safetyGroupName.lowercase() }, { it.safetyGroupId }))

        return RecipeSafetySummary(
            groups = groups,
            reviewNotices = reviewNotices.toList(),
            regulatoryExemptions = regulatoryExemptions.toList(),
        )
    }

    private fun presentationState(
        relationType: RecipeSafetyRelationType,
    ): RecipeSafetyPresentationState = when (relationType) {
        RecipeSafetyRelationType.INHERENT_SOURCE,
        RecipeSafetyRelationType.CONTAINS,
        RecipeSafetyRelationType.REGULATED_COMPONENT,
        -> RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA

        RecipeSafetyRelationType.DERIVED_FROM ->
            RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO

        RecipeSafetyRelationType.DECLARED_MAY_CONTAIN ->
            RecipeSafetyPresentationState.PUEDE_CONTENER_DECLARADO

        RecipeSafetyRelationType.POSSIBLE_CROSS_REACTIVITY ->
            RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA

        RecipeSafetyRelationType.UNKNOWN ->
            RecipeSafetyPresentationState.REQUIERE_REVISION
    }

    private fun presentationPriority(relationType: RecipeSafetyRelationType): Int =
        when (presentationState(relationType)) {
            RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA -> 0
            RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO -> 1
            RecipeSafetyPresentationState.PUEDE_CONTENER_DECLARADO -> 2
            RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA -> 3
            RecipeSafetyPresentationState.REQUIERE_REVISION -> 4
        }
}
