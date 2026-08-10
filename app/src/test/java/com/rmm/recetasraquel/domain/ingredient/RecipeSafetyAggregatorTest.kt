package com.rmm.recetasraquel.domain.ingredient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RecipeSafetyAggregatorTest {
    private val aggregator = RecipeSafetyAggregator()

    @Test
    fun `presence wins presentation while all observations are preserved`() {
        val contains = observation(
            ingredientId = "ingredient-a",
            ingredientName = "Ingrediente A",
            relationType = RecipeSafetyRelationType.CONTAINS,
        )
        val mayContain = observation(
            ingredientId = "ingredient-b",
            ingredientName = "Ingrediente B",
            relationType = RecipeSafetyRelationType.DECLARED_MAY_CONTAIN,
        )
        val crossReactive = observation(
            ingredientId = "ingredient-c",
            ingredientName = "Ingrediente C",
            relationType = RecipeSafetyRelationType.POSSIBLE_CROSS_REACTIVITY,
        )

        val result = aggregator.aggregate(listOf(crossReactive, mayContain, contains))

        assertEquals(1, result.groups.size)
        val group = result.groups.single()
        assertEquals(RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA, group.presentationState)
        assertEquals(listOf(contains, mayContain, crossReactive), group.observations)
    }

    @Test
    fun `derived from is not collapsed into identified presence`() {
        val derived = observation(relationType = RecipeSafetyRelationType.DERIVED_FROM)

        val result = aggregator.aggregate(listOf(derived))

        assertEquals(
            RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO,
            result.groups.single().presentationState,
        )
    }

    @Test
    fun `possible cross reactivity remains its own presentation state`() {
        val crossReactive = observation(
            relationType = RecipeSafetyRelationType.POSSIBLE_CROSS_REACTIVITY,
        )

        val result = aggregator.aggregate(listOf(crossReactive))

        assertEquals(
            RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA,
            result.groups.single().presentationState,
        )
    }

    @Test
    fun `unknown relation requires review`() {
        val unknown = observation(relationType = RecipeSafetyRelationType.UNKNOWN)

        val result = aggregator.aggregate(listOf(unknown))

        assertEquals(
            RecipeSafetyPresentationState.REQUIERE_REVISION,
            result.groups.single().presentationState,
        )
    }

    @Test
    fun `review notices are global and are not assigned to fabricated groups`() {
        val notice = RecipeReviewNotice(
            code = "UNKNOWN_COMPOSITION",
            ingredientId = "custom-1",
            ingredientName = "Producto casero",
            message = "La información disponible puede ser incompleta. Requiere revisión.",
        )

        val result = aggregator.aggregate(
            observations = emptyList(),
            reviewNotices = listOf(notice),
        )

        assertEquals(emptyList<RecipeSafetyGroupSummary>(), result.groups)
        assertEquals(listOf(notice), result.reviewNotices)
    }

    @Test
    fun `regulatory exemptions stay separate and never suppress safety observations`() {
        val observation = observation(relationType = RecipeSafetyRelationType.CONTAINS)
        val exemption = RegulatoryExemption(
            id = "exemption-1",
            ingredientId = observation.ingredientId,
            safetyGroupId = observation.safetyGroupId,
            jurisdiction = "EU-ES",
            effect = RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION,
            conditions = "Condición regulatoria de prueba",
            sourceId = "EU_FIC_1169_2011",
            effectiveFrom = null,
            effectiveTo = null,
            reviewedAt = "2026-08-09",
            notes = null,
        )

        val result = aggregator.aggregate(
            observations = listOf(observation),
            regulatoryExemptions = listOf(exemption),
        )

        assertEquals(RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA, result.groups.single().presentationState)
        assertEquals(listOf(observation), result.groups.single().observations)
        assertEquals(listOf(exemption), result.regulatoryExemptions)
        assertSame(exemption, result.regulatoryExemptions.single())
    }

    @Test
    fun `group summaries use deterministic alphabetical order`() {
        val zeta = observation(
            safetyGroupId = "group-z",
            safetyGroupName = "Zeta",
        )
        val alpha = observation(
            safetyGroupId = "group-a",
            safetyGroupName = "Alpha",
        )

        val result = aggregator.aggregate(listOf(zeta, alpha))

        assertEquals(listOf("group-a", "group-z"), result.groups.map { it.safetyGroupId })
    }

    private fun observation(
        ingredientId: String = "ingredient-1",
        ingredientName: String = "Ingrediente",
        safetyGroupId: String = "group-1",
        safetyGroupName: String = "Grupo",
        relationType: RecipeSafetyRelationType = RecipeSafetyRelationType.CONTAINS,
    ) = RecipeSafetyObservation(
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        safetyGroupId = safetyGroupId,
        safetyGroupName = safetyGroupName,
        relationType = relationType,
        evidenceLevel = "EU_LEGAL",
        sourceId = "source-1",
        reviewedAt = "2026-08-09",
    )
}
