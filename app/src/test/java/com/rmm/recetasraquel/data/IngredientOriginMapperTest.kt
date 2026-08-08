package com.rmm.recetasraquel.data

import com.rmm.recetasraquel.data.mapper.RecipeMapper.RECIPE_FREE_TEXT_CUSTOM_PREFIX
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toPersisted
import com.rmm.recetasraquel.data.mapper.RecipeMapper.toUpdatedRecipe
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.IngredientDraft
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.validation.RecipeValidationException
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun originlessFreeTextIngredientGetsDedicatedCompatibilityCustomOriginBeforePersistence() {
        val recipe = recipe(
            Ingredient(
                id = "i1",
                recipeId = "r1",
                quantity = "2",
                unit = "ud",
                name = "Ingrediente casero",
                notes = null,
                sortOrder = 0,
            ),
        )

        val persisted = recipe.toPersisted(updatedAt = 300L)
        val ingredient = persisted.ingredients.single()
        val custom = persisted.compatibilityCustomIngredients.single()

        assertNull(ingredient.catalogIngredientId)
        assertEquals("${RECIPE_FREE_TEXT_CUSTOM_PREFIX}i1", ingredient.customIngredientId)
        assertEquals(ingredient.customIngredientId, custom.id)
        assertEquals("Ingrediente casero", custom.name)
        assertEquals("ingrediente casero", custom.normalizedName)
        assertEquals("ud", custom.defaultUnit)
        assertEquals("RECIPE_FREE_TEXT_COMPAT", custom.ingredientType)
        assertTrue(!custom.compositionKnown)
    }

    @Test
    fun existingCatalogOriginDoesNotCreateCompatibilityCustomMaster() {
        val recipe = recipe(
            Ingredient(
                id = "i1",
                recipeId = "r1",
                quantity = null,
                unit = null,
                name = "Tomate",
                notes = null,
                sortOrder = 0,
                catalogIngredientId = "ing-tomato",
            ),
        )

        val persisted = recipe.toPersisted()

        assertEquals("ing-tomato", persisted.ingredients.single().catalogIngredientId)
        assertNull(persisted.ingredients.single().customIngredientId)
        assertTrue(persisted.compatibilityCustomIngredients.isEmpty())
    }

    @Test
    fun dualOriginIsRejectedBeforePersistence() {
        val recipe = recipe(
            Ingredient(
                id = "i1",
                recipeId = "r1",
                quantity = null,
                unit = null,
                name = "Leche",
                notes = null,
                sortOrder = 0,
                catalogIngredientId = "ing-milk",
                customIngredientId = "custom:milk",
            ),
        )

        assertThrows(RecipeValidationException::class.java) {
            recipe.toPersisted()
        }
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
