package com.rmm.recetasraquel.domain.validation

import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.IngredientDraft
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.model.RecipeStepDraft

class RecipeValidationException(message: String) : IllegalArgumentException(message)

object RecipeValidator {
    fun normalize(draft: RecipeDraft): RecipeDraft {
        validateRecipeFields(
            name = draft.name,
            servings = draft.servings,
            preparationMinutes = draft.preparationMinutes,
            cookingMinutes = draft.cookingMinutes,
        )
        draft.ingredients.forEach {
            validateIngredient(it.name, 0)
            validateIngredientOrigin(it.catalogIngredientId, it.customIngredientId)
        }
        draft.steps.forEach { validateStep(it.instruction, it.timerMinutes, 0) }
        return draft.copy(
            name = draft.name.trim(),
            description = draft.description.normalizedOrNull(),
            category = draft.category.normalizedOrNull(),
            notes = draft.notes.normalizedOrNull(),
            coverPhotoPath = draft.coverPhotoPath.normalizedOrNull(),
            ingredients = draft.ingredients.map { ingredient ->
                ingredient.copy(
                    quantity = ingredient.quantity.normalizedOrNull(),
                    unit = ingredient.unit.normalizedOrNull(),
                    name = ingredient.name.trim(),
                    notes = ingredient.notes.normalizedOrNull(),
                )
            },
            steps = draft.steps.map { step ->
                step.copy(
                    instruction = step.instruction.trim(),
                    photoPath = step.photoPath.normalizedOrNull(),
                )
            },
        )
    }

    fun normalize(recipe: Recipe): Recipe {
        validateRecipeFields(recipe.name, recipe.servings, recipe.preparationMinutes, recipe.cookingMinutes)
        return recipe.copy(
            name = recipe.name.trim(),
            description = recipe.description.normalizedOrNull(),
            category = recipe.category.normalizedOrNull(),
            notes = recipe.notes.normalizedOrNull(),
            coverPhotoPath = recipe.coverPhotoPath.normalizedOrNull(),
            ingredients = recipe.ingredients.mapIndexed { index, ingredient ->
                validateIngredient(ingredient.name, ingredient.sortOrder)
                validateIngredientOrigin(ingredient.catalogIngredientId, ingredient.customIngredientId)
                ingredient.normalized(index)
            },
            steps = recipe.steps.mapIndexed { index, step ->
                validateStep(step.instruction, step.timerMinutes, step.sortOrder)
                step.normalized(index)
            },
        )
    }

    private fun validateRecipeFields(
        name: String,
        servings: Int?,
        preparationMinutes: Int?,
        cookingMinutes: Int?,
    ) {
        if (name.isBlank()) throw RecipeValidationException("El nombre de la receta es obligatorio")
        if (servings != null && servings <= 0) throw RecipeValidationException("Las raciones deben ser mayores que cero")
        if (preparationMinutes != null && preparationMinutes < 0) {
            throw RecipeValidationException("El tiempo de preparación no puede ser negativo")
        }
        if (cookingMinutes != null && cookingMinutes < 0) {
            throw RecipeValidationException("El tiempo de cocción no puede ser negativo")
        }
    }

    private fun validateIngredient(name: String, sortOrder: Int) {
        if (name.isBlank()) throw RecipeValidationException("El nombre del ingrediente es obligatorio")
        if (sortOrder < 0) throw RecipeValidationException("El orden del ingrediente no puede ser negativo")
    }

    private fun validateIngredientOrigin(catalogIngredientId: String?, customIngredientId: String?) {
        if (catalogIngredientId != null && customIngredientId != null) {
            throw RecipeValidationException("Un ingrediente no puede proceder a la vez del catálogo y de un ingrediente personalizado")
        }
    }

    private fun validateStep(instruction: String, timerMinutes: Int?, sortOrder: Int) {
        if (instruction.isBlank()) throw RecipeValidationException("La instrucción del paso es obligatoria")
        if (timerMinutes != null && timerMinutes < 0) {
            throw RecipeValidationException("El temporizador no puede ser negativo")
        }
        if (sortOrder < 0) throw RecipeValidationException("El orden del paso no puede ser negativo")
    }

    private fun Ingredient.normalized(order: Int) = copy(
        quantity = quantity.normalizedOrNull(),
        unit = unit.normalizedOrNull(),
        name = name.trim(),
        notes = notes.normalizedOrNull(),
        sortOrder = order,
    )

    private fun RecipeStep.normalized(order: Int) = copy(
        instruction = instruction.trim(),
        photoPath = photoPath.normalizedOrNull(),
        sortOrder = order,
    )

    private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
