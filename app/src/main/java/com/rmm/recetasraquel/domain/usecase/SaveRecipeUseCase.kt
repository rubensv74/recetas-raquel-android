package com.rmm.recetasraquel.domain.usecase

import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStepDraft
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.repository.RecipeRepository

data class SaveRecipeInput(
    val draft: RecipeDraft,
    val stagedCover: StagedPhoto? = null,
    val stagedSteps: Map<String, StagedPhoto?> = emptyMap(),
    val replacedCoverPath: String? = null,
    val replacedStepPaths: Map<String, String> = emptyMap(),
)

class SaveRecipeUseCase(
    private val repository: RecipeRepository,
    private val photoStorage: RecipePhotoStorage,
) : SaveRecipeOperation {
    override suspend fun create(input: SaveRecipeInput): Result<String> {
        val recipeId = input.draft.id
            ?: return Result.failure(IllegalStateException("Recipe ID must be pre-generated for create"))

        val promotedResult = promoteAll(recipeId, input)
        if (promotedResult.isFailure) return Result.failure(promotedResult.exceptionOrNull()!!)
        val promoted = promotedResult.getOrThrow()

        val draft = resolveDraftPaths(input.draft, promoted)
        val result = repository.createRecipe(draft)

        return if (result.isSuccess) {
            photoStorage.cleanStaging()
            Result.success(recipeId)
        } else {
            deletePromotedFiles(promoted)
            result
        }
    }

    override suspend fun update(recipeId: String, input: SaveRecipeInput): Result<Unit> {
        val promotedResult = promoteAll(recipeId, input)
        if (promotedResult.isFailure) return Result.failure(promotedResult.exceptionOrNull()!!)
        val promoted = promotedResult.getOrThrow()

        val draft = resolveDraftPaths(input.draft, promoted)
        val result = repository.updateRecipeFromDraft(recipeId, draft)

        return if (result.isSuccess) {
            deleteOldPhotos(input.replacedCoverPath, input.replacedStepPaths, promoted)
            photoStorage.cleanStaging()
            Result.success(Unit)
        } else {
            deletePromotedFiles(promoted)
            result
        }
    }

    private suspend fun promoteAll(
        recipeId: String,
        input: SaveRecipeInput,
    ): Result<PromotedPaths> {
        var coverPath: String? = null
        val stepPaths = mutableMapOf<String, String>()

        input.stagedCover?.let { staged ->
            val result = photoStorage.promotePhoto(staged, PhotoDestination.Cover(recipeId))
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull()!!)
            }
            coverPath = result.getOrThrow()
        }

        for ((stepKey, staged) in input.stagedSteps) {
            if (staged != null) {
                val result = photoStorage.promotePhoto(staged, PhotoDestination.Step(recipeId, stepKey))
                if (result.isFailure) {
                    deletePromotedFiles(PromotedPaths(coverPath, stepPaths))
                    return Result.failure(result.exceptionOrNull()!!)
                }
                stepPaths[stepKey] = result.getOrThrow()
            }
        }

        return Result.success(PromotedPaths(coverPath, stepPaths))
    }

    private fun resolveDraftPaths(
        base: RecipeDraft,
        promoted: PromotedPaths,
    ): RecipeDraft {
        val coverPath = when {
            promoted.coverPath != null -> promoted.coverPath
            base.coverPhotoPath != null -> base.coverPhotoPath
            else -> null
        }

        val steps = base.steps.map { step ->
            val key = step.stepKey
            val promotedPath = if (key != null) promoted.stepPaths[key] else null
            when {
                promotedPath != null -> step.copy(photoPath = promotedPath)
                step.photoPath != null -> step
                else -> step
            }
        }

        return base.copy(coverPhotoPath = coverPath, steps = steps)
    }

    private suspend fun deleteOldPhotos(
        oldCover: String?,
        oldStepPaths: Map<String, String>,
        promoted: PromotedPaths,
    ) {
        val newCoverPath = promoted.coverPath
        if (oldCover != null && newCoverPath != oldCover) {
            photoStorage.delete(oldCover)
        }
        for ((key, oldPath) in oldStepPaths) {
            val newPath = promoted.stepPaths[key]
            if (oldPath != newPath) {
                photoStorage.delete(oldPath)
            }
        }
    }

    private suspend fun deletePromotedFiles(promoted: PromotedPaths) {
        promoted.coverPath?.let { photoStorage.delete(it) }
        promoted.stepPaths.values.forEach { photoStorage.delete(it) }
    }

    private data class PromotedPaths(
        val coverPath: String?,
        val stepPaths: Map<String, String>,
    )
}
