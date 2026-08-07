package com.rmm.recetasraquel.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientCatalogAssetReaderTest {
    @Test
    fun readsManifestAndReferencedFilesFromVersionDirectory() {
        val source = MapCatalogTextSource(
            mapOf(
                "ingredient-catalog/v1/manifest.json" to """
                    {
                      "catalogId":"test",
                      "schemaVersion":1,
                      "catalogVersion":7,
                      "releaseStatus":"INFRASTRUCTURE",
                      "locale":"es-ES",
                      "jurisdiction":"EU-ES",
                      "reviewedAt":"2026-08-08",
                      "files":{
                        "categories":"categories.json",
                        "ingredients":"ingredients.json",
                        "aliases":"aliases.json",
                        "safetyGroups":"safety-groups.json",
                        "safetySources":"safety-sources.json",
                        "safetyRelations":"safety-relations.json"
                      },
                      "counts":{
                        "categories":1,
                        "ingredients":0,
                        "aliases":0,
                        "safetyGroups":0,
                        "safetySources":0,
                        "safetyRelations":0
                      }
                    }
                """.trimIndent(),
                "ingredient-catalog/v1/categories.json" to """[{"id":"cat-1","code":"C1","name":"Categoría","sortOrder":1}]""",
                "ingredient-catalog/v1/ingredients.json" to "[]",
                "ingredient-catalog/v1/aliases.json" to "[]",
                "ingredient-catalog/v1/safety-groups.json" to "[]",
                "ingredient-catalog/v1/safety-sources.json" to "[]",
                "ingredient-catalog/v1/safety-relations.json" to "[]",
            ),
        )

        val bundle = IngredientCatalogAssetReader(source).read()

        assertEquals(7, bundle.manifest.catalogVersion)
        assertEquals("Categoría", bundle.categories.single().name)
        assertEquals(0, bundle.ingredients.size)
    }

    private class MapCatalogTextSource(
        private val values: Map<String, String>,
    ) : CatalogTextSource {
        override fun read(path: String): String = values[path] ?: error("Missing test asset: $path")
    }
}
