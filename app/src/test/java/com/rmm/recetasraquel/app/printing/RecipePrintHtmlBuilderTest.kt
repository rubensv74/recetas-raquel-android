package com.rmm.recetasraquel.app.printing

import com.rmm.recetasraquel.domain.ingredient.RecipeReviewNotice
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyGroupSummary
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyObservation
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyPresentationState
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipePrintHtmlBuilderTest {

    @Test
    fun allergenBlockIsProminentAtomicAndMarksResponsibleIngredient() {
        val recipe = sampleRecipe()
        val summary = RecipeSafetySummary(
            groups = listOf(
                RecipeSafetyGroupSummary(
                    safetyGroupId = "sg-eu-cereals-gluten",
                    safetyGroupName = "Cereales que contienen gluten",
                    presentationState = RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA,
                    observations = listOf(
                        RecipeSafetyObservation(
                            ingredientId = "ingredient-1",
                            ingredientName = "Harina de trigo",
                            safetyGroupId = "sg-eu-cereals-gluten",
                            safetyGroupName = "Cereales que contienen gluten",
                            relationType = RecipeSafetyRelationType.CONTAINS,
                            evidenceLevel = "EU_LEGAL",
                            sourceId = "EU_FIC_1169_2011",
                        ),
                    ),
                ),
            ),
            reviewNotices = emptyList(),
            regulatoryExemptions = emptyList(),
        )

        val html = RecipePrintHtmlBuilder.build(
            recipe = recipe,
            safetySummary = summary,
            safetyMessage = null,
        )

        assertTrue(html.contains("Alérgenos y seguridad alimentaria"))
        assertTrue(html.contains("CONTIENE / PRESENCIA IDENTIFICADA"))
        assertTrue(html.contains("Cereales que contienen gluten"))
        assertTrue(html.contains("Alérgenos/seguridad: Contiene: Cereales que contienen gluten"))
        assertTrue(html.contains("break-inside: avoid"))
        assertTrue(html.contains("border: 3px solid #A95010"))
    }

    @Test
    fun stepPhotosAreEmbeddedInPreparation() {
        val recipe = sampleRecipe().copy(
            steps = listOf(
                RecipeStep(
                    id = "step-1",
                    recipeId = "recipe-1",
                    instruction = "Amasar.",
                    timerMinutes = 10,
                    photoPath = "recipe_photos/recipe-1/steps/step-1.jpg",
                    sortOrder = 0,
                ),
            ),
        )

        val html = RecipePrintHtmlBuilder.build(
            recipe = recipe,
            safetySummary = RecipeSafetySummary(
                groups = emptyList(),
                reviewNotices = emptyList(),
                regulatoryExemptions = emptyList(),
            ),
            safetyMessage = null,
            stepPhotoDataUris = mapOf(
                "step-1" to "data:image/jpeg;base64,STEP_IMAGE",
            ),
        )

        assertTrue(html.contains("""class="step-photo""""))
        assertTrue(html.contains("data:image/jpeg;base64,STEP_IMAGE"))
        assertTrue(html.contains("""alt="Paso 1""""))
        assertTrue(html.indexOf("Amasar.") < html.indexOf("STEP_IMAGE"))
    }

    @Test
    fun unresolvedSafetyNeverPrintsAFalseSafeClaim() {
        val html = RecipePrintHtmlBuilder.build(
            recipe = sampleRecipe(),
            safetySummary = null,
            safetyMessage = "No se pudo cargar la información de seguridad alimentaria. Requiere revisión.",
        )

        assertTrue(html.contains("NO VERIFICADO"))
        assertTrue(html.contains("Requiere revisión"))
        assertFalse(html.contains("sin alérgenos", ignoreCase = true))
        assertFalse(html.contains("apta para alérgicos", ignoreCase = true))
    }

    @Test
    fun regulatoryExemptionRemainsSeparateAndKeepsMandatoryDisclaimer() {
        val recipe = sampleRecipe()
        val summary = RecipeSafetySummary(
            groups = emptyList(),
            reviewNotices = listOf(
                RecipeReviewNotice(
                    code = "CHECK_LABEL",
                    ingredientId = "ingredient-1",
                    ingredientName = "Harina de trigo",
                    message = "Comprueba la etiqueta actual.",
                ),
            ),
            regulatoryExemptions = listOf(
                RegulatoryExemption(
                    id = "rex-1",
                    ingredientId = "catalog-1",
                    safetyGroupId = "sg-eu-cereals-gluten",
                    jurisdiction = "EU-ES",
                    effect = RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION,
                    conditions = "Solo bajo las condiciones legales indicadas.",
                    sourceId = "EU_FIC_1169_2011",
                    effectiveFrom = null,
                    effectiveTo = null,
                    reviewedAt = "2026-09-26",
                    notes = null,
                ),
            ),
        )

        val html = RecipePrintHtmlBuilder.build(
            recipe = recipe,
            safetySummary = summary,
            safetyMessage = null,
        )

        assertTrue(html.contains("Información regulatoria de etiquetado"))
        assertTrue(html.contains("Esta información se refiere a obligaciones de etiquetado."))
        assertTrue(html.contains("No significa que el alérgeno esté ausente"))
    }

    @Test
    fun userTextIsEscapedBeforeEnteringPrintableHtml() {
        val recipe = sampleRecipe().copy(
            name = "<Tarta & crema>",
            notes = "<script>alert('x')</script>",
        )

        val html = RecipePrintHtmlBuilder.build(
            recipe = recipe,
            safetySummary = RecipeSafetySummary(
                groups = emptyList(),
                reviewNotices = emptyList(),
                regulatoryExemptions = emptyList(),
            ),
            safetyMessage = null,
        )

        assertTrue(html.contains("&lt;Tarta &amp; crema&gt;"))
        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"))
        assertFalse(html.contains("<script>alert('x')</script>"))
    }

    private fun sampleRecipe(): Recipe = Recipe(
        id = "recipe-1",
        name = "Pan casero",
        description = "Receta de prueba",
        category = "Panadería",
        servings = 4,
        preparationMinutes = 20,
        cookingMinutes = 35,
        notes = "Servir templado.",
        isFavorite = false,
        coverPhotoPath = null,
        ingredients = listOf(
            Ingredient(
                id = "ingredient-1",
                recipeId = "recipe-1",
                quantity = "500",
                unit = "g",
                name = "Harina de trigo",
                notes = null,
                sortOrder = 0,
                catalogIngredientId = "catalog-1",
            ),
        ),
        steps = listOf(
            RecipeStep(
                id = "step-1",
                recipeId = "recipe-1",
                instruction = "Amasar.",
                timerMinutes = 10,
                photoPath = null,
                sortOrder = 0,
            ),
        ),
        createdAt = 1,
        updatedAt = 2,
    )
}
