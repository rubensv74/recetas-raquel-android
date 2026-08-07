package com.rmm.recetasraquel.data

import com.rmm.recetasraquel.data.mapper.RecipeMapper.toUpdatedRecipe
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.IngredientDraft
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IngredientOriginMapperTest {
    @Test
    fun updatePreservesExistingCustomOriginWhenLegacyEditorDoesNotSendOriginFields() {
        val existing = recipe(
            Ingredient(
                id = "i1",
                recipeId = "r1",
                quantity = "1/2",
                unit = "kg",
                name = "Queso crema",
                notes = null,
                sortOrder = 0,
                customIngredientId = "legacy:i1",
            ),
        )
        val draft = RecipeDraft(
            name = "Receta",
            ingredients = listOf(
                IngredientDraft(
                    id = "i1",
                    quantity = "500",
                    unit = "g",
                    name = "Queso crema",
                ),
            ),
        )

        val updated = draft.toUpdatedRecipe(existing, IdGenerator { "new-id" }, TimeProvider { 999L })

        assertEquals("legacy:i1", updated.ingredients.single().customIngredientId)
        assertNull(updated.ingredients.single().catalogIngredientId)
    }

    @Test
    fun explicitCatalogOriginReplacesExistingCustomOriginAsExclusivePair() {
        val existing = recipe(
            Ingredient(
                id = "i1",
                recipeId = "r1",
                quantity = null,
                unit = null,
                name = "Leche",
                notes = null,
                sortOrder = 0,
                customIngredientId = "legacy:i1",
            ),
        )
        val draft = RecipeDraft(
            name = "Receta",
            ingredients = listOf(
                IngredientDraft(
                    id = "i1",
                    name = "Leche",
                    catalogIngredientId = "catalog:milk",
                    customIngredientId = null,
                ),
            ),
        )

        val updated = draft.toUpdatedRecipe(existing, IdGenerator { "new-id" }, TimeProvider { 999L })

        assertEquals("catalog:milk", updated.ingredients.single().catalogIngredientId)
        assertNull(updated.ingredients.single().customIngredientId)
    }

    private fun recipe(ingredient: Ingredient) = Recipe(
        id = "r1",
        name = "Receta",
        description = null,
        category = null,
        servings = null,
        preparationMinutes = null,
        cookingMinutes = null,
        notes = null,
        isFavorite = false,
        coverPhotoPath = null,
        ingredients = listOf(ingredient),
        steps = emptyList(),
        createdAt = 100L,
        updatedAt = 200L,
    )
}
