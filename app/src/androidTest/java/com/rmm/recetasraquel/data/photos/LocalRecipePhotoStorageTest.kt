package com.rmm.recetasraquel.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalRecipePhotoStorageTest {

    private lateinit var context: Context
    private lateinit var storage: LocalRecipePhotoStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = LocalRecipePhotoStorage(context)
        File(context.filesDir, "recipe_photos").deleteRecursively()
        File(context.cacheDir, "recipe_photo_staging").deleteRecursively()
    }

    private fun createTestImage(): Uri {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.RED)
        val file = File(context.cacheDir, "test_input.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun promoteAndResolveWorks() = runTest {
        val uri = createTestImage()
        val staged = storage.stagePhoto(uri.toString()).getOrThrow()
        assertNotNull(staged.stagedFile)
        assertTrue(staged.stagedFile.exists())
        assertNotNull(staged.relativePath)

        val recipeId = "test_recipe_1"
        val relativePath = storage.promotePhoto(staged, PhotoDestination.Cover(recipeId)).getOrThrow()
        assertNotNull(relativePath)
        assertTrue(relativePath.contains(recipeId))

        val resolved = storage.resolve(relativePath)
        assertNotNull(resolved)
        assertTrue(resolved!!.exists())

        val notFound = storage.resolve("nonexistent/path.jpg")
        assertNull(notFound)
    }

    @Test
    fun stepPhotoPromoteWorks() = runTest {
        val uri = createTestImage()
        val staged = storage.stagePhoto(uri.toString()).getOrThrow()
        val recipeId = "test_recipe_2"
        val stepId = "step_1"
        val relativePath = storage.promotePhoto(staged, PhotoDestination.Step(recipeId, stepId)).getOrThrow()
        assertTrue(relativePath.contains(recipeId))
        assertTrue(relativePath.contains(stepId))
        assertNotNull(storage.resolve(relativePath))
    }

    @Test
    fun deleteRemovesFile() = runTest {
        val uri = createTestImage()
        val staged = storage.stagePhoto(uri.toString()).getOrThrow()
        val recipeId = "test_recipe_3"
        val relativePath = storage.promotePhoto(staged, PhotoDestination.Cover(recipeId)).getOrThrow()
        assertTrue(storage.resolve(relativePath) != null)

        storage.delete(relativePath).getOrThrow()
        assertNull(storage.resolve(relativePath))
    }

    @Test
    fun deleteIsIdempotent() = runTest {
        storage.delete("nonexistent/path.jpg").getOrThrow()
    }

    @Test
    fun deleteStagedWorks() = runTest {
        val uri = createTestImage()
        val staged = storage.stagePhoto(uri.toString()).getOrThrow()
        assertTrue(staged.stagedFile.exists())
        storage.deleteStaged(staged).getOrThrow()
        assertFalse(staged.stagedFile.exists())
    }

    @Test
    fun deleteStagedIsIdempotent() = runTest {
        val uri = createTestImage()
        val staged = storage.stagePhoto(uri.toString()).getOrThrow()
        storage.deleteStaged(staged).getOrThrow()
        storage.deleteStaged(staged).getOrThrow()
    }

    @Test
    fun cleanStagingRemovesAllFiles() = runTest {
        val uri1 = createTestImage()
        val uri2 = createTestImage()
        storage.stagePhoto(uri1.toString()).getOrThrow()
        storage.stagePhoto(uri2.toString()).getOrThrow()

        val stagingDir = File(context.cacheDir, "recipe_photo_staging")
        assertTrue(stagingDir.listFiles()?.isNotEmpty() == true)

        storage.cleanStaging().getOrThrow()
        assertTrue(stagingDir.listFiles()?.isEmpty() == true)
    }

    @Test
    fun getRecipePhotoPathsReturnsPathsForRecipe() = runTest {
        val uri = createTestImage()
        val staged = storage.stagePhoto(uri.toString()).getOrThrow()
        val recipeId = "test_recipe_4"
        storage.promotePhoto(staged, PhotoDestination.Cover(recipeId)).getOrThrow()

        val paths = storage.getRecipePhotoPaths(recipeId)
        assertEquals(1, paths.size)
        assertTrue(paths[0].contains(recipeId))
    }

    @Test
    fun getRecipePhotoPathsReturnsEmptyForUnknownRecipe() = runTest {
        val paths = storage.getRecipePhotoPaths("nonexistent_recipe")
        assertTrue(paths.isEmpty())
    }

    @Test
    fun stagedFileNamesAreUnique() = runTest {
        val uri = createTestImage()
        val staged1 = storage.stagePhoto(uri.toString()).getOrThrow()
        val staged2 = storage.stagePhoto(uri.toString()).getOrThrow()
        assertTrue(staged1.stagedFile.name != staged2.stagedFile.name)
        assertTrue(staged1.relativePath != staged2.relativePath)
    }
}
