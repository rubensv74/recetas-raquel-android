package com.rmm.recetasraquel.domain

import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.IngredientDraft
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.model.RecipeStepDraft
import com.rmm.recetasraquel.domain.validation.RecipeValidationException
import com.rmm.recetasraquel.domain.validation.RecipeValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RecipeValidatorTest {
    @Test
    fun rejectsEmptyOrWhitespaceRecipeName() {
        assertThrows(RecipeValidationException::class.java) { RecipeValidator.normalize(RecipeDraft(name = "")) }
        assertThrows(RecipeValidationException::class.java) { RecipeValidator.normalize(RecipeDraft(name = "   ")) }
    }

    @Test
    fun rejectsInvalidServingsAndMinutes() {
        assertThrows(RecipeValidationException::class.java) { RecipeValidator.normalize(RecipeDraft(name = "R", servings = 0)) }
        assertThrows(RecipeValidationException::class.java) { RecipeValidator.normalize(RecipeDraft(name = "R", preparationMinutes = -1)) }
        assertThrows(RecipeValidationException::class.java) { RecipeValidator.normalize(RecipeDraft(name = "R", cookingMinutes = -1)) }
    }

    @Test
    fun rejectsIngredientWithoutNameAndStepWithoutInstruction() {
        assertThrows(RecipeValidationException::class.java) {
            RecipeValidator.normalize(RecipeDraft(name = "R", ingredients = listOf(IngredientDraft(name = " "))))
        }
        assertThrows(RecipeValidationException::class.java) {
            RecipeValidator.normalize(RecipeDraft(name = "R", steps = listOf(RecipeStepDraft(instruction = " "))))
        }
    }

    @Test
    fun rejectsNegativeChildValues() {
        val ingredient = ingredient(sortOrder = -1)
        assertThrows(RecipeValidationException::class.java) { RecipeValidator.normalize(recipe(ingredients = listOf(ingredient))) }
        val step = step(timerMinutes = -1)
        assertThrows(RecipeValidationException::class.java) { RecipeValidator.normalize(recipe(steps = listOf(step))) }
    }

    @Test
    fun trimsTextConvertsBlanksToNullAndNormalizesOrder() {
        val normalized = RecipeValidator.normalize(
            recipe(
                name = "  Sopa  ",
                description = "   ",
                ingredients = listOf(
                    ingredient(id = "i2", name = " Sal ", quantity = " al gusto ", sortOrder = 9),
                    ingredient(id = "i1", name = " Agua ", notes = " ", sortOrder = 4),
                ),
                steps = listOf(
                    step(id = "s2", instruction = " Hervir ", sortOrder = 7),
                    step(id = "s1", instruction = " Servir ", photoPath = " ", sortOrder = 2),
                ),
            ),
        )

        assertEquals("Sopa", normalized.name)
        assertNull(normalized.description)
        assertEquals(listOf(0, 1), normalized.ingredients.map { it.sortOrder })
        assertEquals("al gusto", normalized.ingredients.first().quantity)
        assertNull(normalized.ingredients.last().notes)
        assertEquals(listOf(0, 1), normalized.steps.map { it.sortOrder })
        assertNull(normalized.steps.last().photoPath)
    }

    private fun recipe(
        name: String = "Receta",
        description: String? = null,
        ingredients: List<Ingredient> = emptyList(),
        steps: List<RecipeStep> = emptyList(),
    ) = Recipe(
        "r1", name, description, null, null, null, null, null, false, null,
        ingredients, steps, 100, 100,
    )

    private fun ingredient(
        id: String = "i1",
        name: String = "Sal",
        quantity: String? = null,
        notes: String? = null,
        sortOrder: Int = 0,
    ) = Ingredient(id, "r1", quantity, null, name, notes, sortOrder)

    private fun step(
        id: String = "s1",
        instruction: String = "Mezclar",
        timerMinutes: Int? = null,
        photoPath: String? = null,
        sortOrder: Int = 0,
    ) = RecipeStep(id, "r1", instruction, timerMinutes, photoPath, sortOrder)
}
