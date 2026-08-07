package com.rmm.recetasraquel.data.mapper

import com.rmm.recetasraquel.data.local.entity.IngredientEntity
import com.rmm.recetasraquel.data.local.entity.RecipeEntity
import com.rmm.recetasraquel.data.local.entity.RecipeStepEntity
import com.rmm.recetasraquel.data.local.relation.RecipeWithDetails
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.validation.RecipeValidator
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider

data class PersistedRecipe(
    val recipe: RecipeEntity,
    val ingredients: List<IngredientEntity>,
    val steps: List<RecipeStepEntity>,
)

object RecipeMapper {
    fun normalizeDraft(draft: RecipeDraft): RecipeDraft {
        val filtered = draft.copy(
            ingredients = draft.ingredients.filter { it.name.isNotBlank() },
            steps = draft.steps.filter { it.instruction.isNotBlank() || it.timerMinutes != null },
        )
        return RecipeValidator.normalize(filtered)
    }

    fun RecipeEntity.toSummary(): RecipeSummary = RecipeSummary(
        id = id,
        name = name,
        category = category,
        servings = servings,
        preparationMinutes = preparationMinutes,
        cookingMinutes = cookingMinutes,
        isFavorite = isFavorite,
        coverPhotoPath = coverPhotoPath,
        updatedAt = updatedAt,
    )

    fun RecipeEntity.toDomain(): Recipe = Recipe(
        id = id,
        name = name,
        description = description,
        category = category,
        servings = servings,
        preparationMinutes = preparationMinutes,
        cookingMinutes = cookingMinutes,
        notes = notes,
        isFavorite = isFavorite,
        coverPhotoPath = coverPhotoPath,
        ingredients = emptyList(),
        steps = emptyList(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun IngredientEntity.toDomain(): Ingredient = Ingredient(
        id = id,
        recipeId = recipeId,
        quantity = quantity,
        unit = unit,
        name = name,
        notes = notes,
        sortOrder = sortOrder,
        catalogIngredientId = catalogIngredientId,
        customIngredientId = customIngredientId,
    )

    fun RecipeStepEntity.toDomain(): RecipeStep = RecipeStep(
        id, recipeId, instruction, timerMinutes, photoPath, sortOrder,
    )

    fun RecipeWithDetails.toDomain(): Recipe = recipe.toDomain().copy(
        ingredients = ingredients.sortedBy(IngredientEntity::sortOrder).map { it.toDomain() },
        steps = steps.sortedBy(RecipeStepEntity::sortOrder).map { it.toDomain() },
    )

    fun Recipe.toPersisted(updatedAt: Long = this.updatedAt): PersistedRecipe {
        val normalized = RecipeValidator.normalize(this)
        return PersistedRecipe(
            recipe = normalized.toEntity(updatedAt),
            ingredients = normalized.ingredients.map { it.toEntity(normalized.id) },
            steps = normalized.steps.map { it.toEntity(normalized.id) },
        )
    }

    fun RecipeDraft.toNewRecipe(idGenerator: IdGenerator, timeProvider: TimeProvider): Recipe {
        val normalized = normalizeDraft(this)
        val recipeId = normalized.id ?: idGenerator.newId()
        val now = timeProvider.nowEpochMillis()
        return Recipe(
            id = recipeId,
            name = normalized.name,
            description = normalized.description,
            category = normalized.category,
            servings = normalized.servings,
            preparationMinutes = normalized.preparationMinutes,
            cookingMinutes = normalized.cookingMinutes,
            notes = normalized.notes,
            isFavorite = false,
            coverPhotoPath = normalized.coverPhotoPath,
            ingredients = normalized.ingredients.mapIndexed { index, ingredient ->
                Ingredient(
                    id = ingredient.id ?: idGenerator.newId(),
                    recipeId = recipeId,
                    quantity = ingredient.quantity,
                    unit = ingredient.unit,
                    name = ingredient.name,
                    notes = ingredient.notes,
                    sortOrder = index,
                    catalogIngredientId = ingredient.catalogIngredientId,
                    customIngredientId = ingredient.customIngredientId,
                )
            },
            steps = normalized.steps.mapIndexed { index, step ->
                RecipeStep(
                    id = step.id ?: idGenerator.newId(),
                    recipeId = recipeId,
                    instruction = step.instruction,
                    timerMinutes = step.timerMinutes,
                    photoPath = step.photoPath,
                    sortOrder = index,
                )
            },
            createdAt = now,
            updatedAt = now,
        )
    }

    fun RecipeDraft.toUpdatedRecipe(
        existingRecipe: Recipe,
        idGenerator: IdGenerator,
        timeProvider: TimeProvider,
    ): Recipe {
        val normalized = normalizeDraft(this)
        val now = timeProvider.nowEpochMillis()
        return Recipe(
            id = existingRecipe.id,
            name = normalized.name,
            description = normalized.description,
            category = normalized.category,
            servings = normalized.servings,
            preparationMinutes = normalized.preparationMinutes,
            cookingMinutes = normalized.cookingMinutes,
            notes = normalized.notes,
            isFavorite = existingRecipe.isFavorite,
            coverPhotoPath = normalized.coverPhotoPath,
            ingredients = normalized.ingredients.mapIndexed { index, ingredient ->
                Ingredient(
                    id = ingredient.id ?: idGenerator.newId(),
                    recipeId = existingRecipe.id,
                    quantity = ingredient.quantity,
                    unit = ingredient.unit,
                    name = ingredient.name,
                    notes = ingredient.notes,
                    sortOrder = index,
                    catalogIngredientId = ingredient.catalogIngredientId,
                    customIngredientId = ingredient.customIngredientId,
                )
            },
            steps = normalized.steps.mapIndexed { index, step ->
                RecipeStep(
                    id = step.id ?: idGenerator.newId(),
                    recipeId = existingRecipe.id,
                    instruction = step.instruction,
                    timerMinutes = step.timerMinutes,
                    photoPath = step.photoPath,
                    sortOrder = index,
                )
            },
            createdAt = existingRecipe.createdAt,
            updatedAt = now,
        )
    }

    private fun Recipe.toEntity(newUpdatedAt: Long) = RecipeEntity(
        id, name, description, category, servings, preparationMinutes, cookingMinutes,
        notes, isFavorite, coverPhotoPath, createdAt, newUpdatedAt,
    )

    private fun Ingredient.toEntity(parentRecipeId: String) = IngredientEntity(
        id = id,
        recipeId = parentRecipeId,
        quantity = quantity,
        unit = unit,
        name = name,
        notes = notes,
        sortOrder = sortOrder,
        catalogIngredientId = catalogIngredientId,
        customIngredientId = customIngredientId,
    )

    private fun RecipeStep.toEntity(parentRecipeId: String) = RecipeStepEntity(
        id, parentRecipeId, instruction, timerMinutes, photoPath, sortOrder,
    )
}
