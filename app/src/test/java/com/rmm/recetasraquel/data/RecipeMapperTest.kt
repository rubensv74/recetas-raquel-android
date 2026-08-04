package com.rmm.recetasraquel.data

import com.rmm.recetasraquel.data.local.entity.IngredientEntity
import com.rmm.recetasraquel.data.local.entity.RecipeEntity
import com.rmm.recetasraquel.data.local.entity.RecipeStepEntity
import com.rmm.recetasraquel.data.local.relation.RecipeWithDetails
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toDomain
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toNewRecipe
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toPersisted
import com.rmm.recetasraquel.domain.model.IngredientDraft
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStepDraft
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeMapperTest {
    @Test
    fun draftGeneratesDeterministicIdsTimestampsAndNonNumericQuantity() {
        val ids = ArrayDeque(listOf("recipe-id", "ingredient-id", "step-id"))
        val recipe = RecipeDraft(
            name = " Tortilla ",
            ingredients = listOf(IngredientDraft(quantity = "1/2", name = " Cebolla ")),
            steps = listOf(RecipeStepDraft(instruction = " Cocinar ")),
        ).toNewRecipe(IdGenerator { ids.removeFirst() }, TimeProvider { 1234L })

        assertEquals("recipe-id", recipe.id)
        assertEquals("ingredient-id", recipe.ingredients.single().id)
        assertEquals("step-id", recipe.steps.single().id)
        assertEquals("1/2", recipe.ingredients.single().quantity)
        assertEquals(1234L, recipe.createdAt)
        assertEquals(1234L, recipe.updatedAt)
        assertFalse(recipe.isFavorite)
    }

    @Test
    fun relationMapsNullableFieldsAndSortsChildren() {
        val details = RecipeWithDetails(
            recipe = recipeEntity(),
            ingredients = listOf(
                IngredientEntity("i2", "r", "al gusto", null, "Sal", null, 1),
                IngredientEntity("i1", "r", null, "ml", "Agua", null, 0),
            ),
            steps = listOf(
                RecipeStepEntity("s2", "r", "Servir", null, null, 1),
                RecipeStepEntity("s1", "r", "Mezclar", 0, null, 0),
            ),
        ).toDomain()

        assertEquals(listOf("i1", "i2"), details.ingredients.map { it.id })
        assertEquals(listOf("s1", "s2"), details.steps.map { it.id })
        assertNull(details.description)
        assertNull(details.ingredients.first().quantity)
    }

    @Test
    fun domainToEntityPreservesIdsAndCreatedAtButChangesUpdatedAt() {
        val domain = RecipeWithDetails(
            recipeEntity(),
            listOf(IngredientEntity("i", "r", "una pizca", null, "Sal", null, 0)),
            listOf(RecipeStepEntity("s", "r", "Mezclar", null, null, 0)),
        ).toDomain()

        val persisted = domain.toPersisted(updatedAt = 999L)

        assertEquals("r", persisted.recipe.id)
        assertEquals("i", persisted.ingredients.single().id)
        assertEquals("s", persisted.steps.single().id)
        assertEquals(100L, persisted.recipe.createdAt)
        assertEquals(999L, persisted.recipe.updatedAt)
    }

    private fun recipeEntity() = RecipeEntity(
        id = "r",
        name = "Receta",
        description = null,
        category = null,
        servings = null,
        preparationMinutes = null,
        cookingMinutes = null,
        notes = null,
        isFavorite = false,
        coverPhotoPath = null,
        createdAt = 100L,
        updatedAt = 200L,
    )
}
