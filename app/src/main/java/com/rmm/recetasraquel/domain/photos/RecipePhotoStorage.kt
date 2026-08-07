package com.rmm.recetasraquel.domain.photos

import java.io.File

data class StagedPhoto(
    val stagedFile: File,
    val relativePath: String,
)

sealed interface PhotoDestination {
    data class Cover(val recipeId: String) : PhotoDestination
    data class Step(val recipeId: String, val stepId: String) : PhotoDestination
}

interface RecipePhotoStorage {
    suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto>
    suspend fun promotePhoto(stagedPhoto: StagedPhoto, destination: PhotoDestination): Result<String>
    suspend fun delete(relativePath: String): Result<Unit>
    suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit>
    suspend fun resolve(relativePath: String): File?
    suspend fun cleanStaging(): Result<Unit>
    suspend fun getRecipePhotoPaths(recipeId: String): List<String>
}
