package com.rmm.recetasraquel.app

import coil.map.Mapper
import coil.request.Options
import java.io.File

internal class RecipePhotoPathMapper(
    private val filesDir: File,
) : Mapper<String, File> {
    override fun map(data: String, options: Options): File? = resolve(filesDir, data)

    companion object {
        private const val RECIPE_PHOTOS_PREFIX = "recipe_photos/"

        internal fun resolve(filesDir: File, relativePath: String): File? {
            if (!relativePath.startsWith(RECIPE_PHOTOS_PREFIX)) return null
            return File(filesDir, relativePath)
        }
    }
}
