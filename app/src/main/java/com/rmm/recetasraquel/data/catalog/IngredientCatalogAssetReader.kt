package com.rmm.recetasraquel.data.catalog

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

interface CatalogTextSource {
    fun read(path: String): String
}

class AndroidAssetCatalogTextSource(
    private val assets: AssetManager,
) : CatalogTextSource {
    override fun read(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
}

class IngredientCatalogAssetReader(
    private val source: CatalogTextSource,
    private val gson: Gson = Gson(),
) {
    fun read(versionDirectory: String = DEFAULT_VERSION_DIRECTORY): IngredientCatalogBundle {
        val manifestPath = "$versionDirectory/manifest.json"
        val manifest = gson.fromJson(source.read(manifestPath), CatalogManifest::class.java)
        return IngredientCatalogBundle(
            manifest = manifest,
            categories = readList(versionDirectory, manifest.files.categories),
            ingredients = readFileSet(
                versionDirectory = versionDirectory,
                singleFile = manifest.files.ingredients,
                shards = manifest.files.ingredientShards,
            ),
            aliases = readFileSet(
                versionDirectory = versionDirectory,
                singleFile = manifest.files.aliases,
                shards = manifest.files.aliasShards,
            ),
            ingredientRelations = readFileSet(
                versionDirectory = versionDirectory,
                singleFile = manifest.files.ingredientRelations,
                shards = manifest.files.ingredientRelationShards,
            ),
            safetyGroups = readList(versionDirectory, manifest.files.safetyGroups),
            safetySources = readList(versionDirectory, manifest.files.safetySources),
            safetyRelations = readList(versionDirectory, manifest.files.safetyRelations),
            regulatoryExemptions = readFileSet(
                versionDirectory = versionDirectory,
                singleFile = manifest.files.regulatoryExemptions,
                shards = manifest.files.regulatoryExemptionShards,
            ),
        )
    }

    private inline fun <reified T> readFileSet(
        versionDirectory: String,
        singleFile: String?,
        shards: List<String>?,
    ): List<T> {
        val files = when {
            !shards.isNullOrEmpty() -> shards
            !singleFile.isNullOrBlank() -> listOf(singleFile)
            else -> emptyList()
        }
        return files.flatMap { readList<T>(versionDirectory, it) }
    }

    private inline fun <reified T> readList(versionDirectory: String, fileName: String): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(source.read("$versionDirectory/$fileName"), type)
    }

    companion object {
        const val DEFAULT_VERSION_DIRECTORY = "ingredient-catalog/v12"
    }
}
