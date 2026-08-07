package com.rmm.recetasraquel.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class RecipePhotoPathMapperTest {
    @Test
    fun recipePhotoRelativePathResolvesInsideFilesDir() {
        val filesDir = File("C:/fake/files")
        val relativePath = "recipe_photos/recipe-1/cover_photo.jpg"

        val resolved = RecipePhotoPathMapper.resolve(filesDir, relativePath)

        assertEquals(File(filesDir, relativePath), resolved)
    }

    @Test
    fun unrelatedStringIsNotMapped() {
        val filesDir = File("C:/fake/files")

        assertNull(RecipePhotoPathMapper.resolve(filesDir, "https://example.com/photo.jpg"))
        assertNull(RecipePhotoPathMapper.resolve(filesDir, "other/photo.jpg"))
    }
}
