package com.rmm.recetasraquel.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class LocalRecipePhotoStorage(
    private val context: Context,
) : RecipePhotoStorage {

    private val photosDir: File
        get() = File(context.filesDir, RECIPE_PHOTOS_DIR).also { it.mkdirs() }

    private val stagingDir: File
        get() = File(context.cacheDir, RECIPE_PHOTO_STAGING_DIR).also { it.mkdirs() }

    override suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto> =
        withContext(Dispatchers.IO) {
            try {
                val sourceUri = android.net.Uri.parse(sourceUriString)
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext Result.failure(IllegalArgumentException("No se pudo leer la fotografía"))

                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap == null) {
                    return@withContext Result.failure(IllegalArgumentException("El contenido no es una imagen válida"))
                }

                val oriented = fixOrientation(bitmap, sourceUri)
                val resized = resize(oriented)
                if (resized !== oriented) oriented.recycle()

                val photoId = UUID.randomUUID().toString()
                val stagedFile = File(stagingDir, "${STAGE_PREFIX}${photoId}.jpg")
                FileOutputStream(stagedFile).use { out ->
                    resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
                resized.recycle()

                val relativePath = "$RECIPE_PHOTOS_DIR/${STAGE_PREFIX}${photoId}.jpg"
                Result.success(StagedPhoto(stagedFile = stagedFile, relativePath = relativePath))
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException("No se pudo procesar la fotografía: ${e.message}"))
            }
        }

    override suspend fun promotePhoto(
        stagedPhoto: StagedPhoto,
        destination: PhotoDestination,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val relativePath = buildRelativePath(destination)
            val targetFile = File(context.filesDir, relativePath)
            targetFile.parentFile?.mkdirs()

            val moved = stagedPhoto.stagedFile.renameTo(targetFile)
            if (!moved) {
                stagedPhoto.stagedFile.copyTo(targetFile, overwrite = true)
                stagedPhoto.stagedFile.delete()
            }

            Result.success(relativePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(relativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) file.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (stagedPhoto.stagedFile.exists()) stagedPhoto.stagedFile.delete()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.success(Unit)
            }
        }

    override suspend fun resolve(relativePath: String): File? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, relativePath)
        if (file.exists()) file else null
    }

    override suspend fun cleanStaging(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stagingDir.listFiles()?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun getRecipePhotoPaths(recipeId: String): List<String> =
        withContext(Dispatchers.IO) {
            val prefix = "$RECIPE_PHOTOS_DIR/$recipeId"
            photosDir.listFiles()
                ?.filter { it.name.startsWith(recipeId) }
                ?.map { "${RECIPE_PHOTOS_DIR}/${it.name}" }
                .orEmpty()
        }

    private fun buildRelativePath(destination: PhotoDestination): String = when (destination) {
        is PhotoDestination.Cover -> "$RECIPE_PHOTOS_DIR/${destination.recipeId}/cover_${UUID.randomUUID()}.jpg"
        is PhotoDestination.Step -> "$RECIPE_PHOTOS_DIR/${destination.recipeId}/steps/step_${destination.stepId}_${UUID.randomUUID()}.jpg"
    }

    private fun fixOrientation(bitmap: Bitmap, uri: android.net.Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        val maxSide = MAX_DIMENSION.coerceAtLeast(1)
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSide && height <= maxSide) return bitmap

        val ratio = maxSide.toFloat() / maxOf(width, height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (resized !== bitmap) bitmap.recycle()
        return resized
    }

    companion object {
        private const val RECIPE_PHOTOS_DIR = "recipe_photos"
        private const val RECIPE_PHOTO_STAGING_DIR = "recipe_photo_staging"
        private const val STAGE_PREFIX = "staging_"
        private const val JPEG_QUALITY = 85
        private const val MAX_DIMENSION = 2048
    }
}
