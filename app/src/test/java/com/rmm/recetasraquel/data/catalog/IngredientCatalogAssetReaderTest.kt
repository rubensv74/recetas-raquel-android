package com.rmm.recetasraquel.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientCatalogAssetReaderTest {
    @Test
    fun readsLegacySingleFilesFromExplicitVersionDirectory() {
        val source = MapCatalogTextSource(
            mapOf(
                "ingredient-catalog/v2/manifest.json" to """
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
                "ingredient-catalog/v2/categories.json" to """[{"id":"cat-1","code":"C1","name":"Categoría","sortOrder":1}]""",
                "ingredient-catalog/v2/ingredients.json" to "[]",
                "ingredient-catalog/v2/aliases.json" to "[]",
                "ingredient-catalog/v2/safety-groups.json" to "[]",
                "ingredient-catalog/v2/safety-sources.json" to "[]",
                "ingredient-catalog/v2/safety-relations.json" to "[]",
            ),
        )

        val bundle = IngredientCatalogAssetReader(source).read("ingredient-catalog/v2")

        assertEquals(7, bundle.manifest.catalogVersion)
        assertEquals("Categoría", bundle.categories.single().name)
        assertEquals(0, bundle.ingredients.size)
    }

    @Test
    fun concatenatesSchemaTwoIngredientAndAliasShardsInOrder() {
        val source = MapCatalogTextSource(
            mapOf(
                "ingredient-catalog/v3/manifest.json" to """
                    {
                      "catalogId":"test-sharded",
                      "schemaVersion":2,
                      "catalogVersion":3,
                      "releaseStatus":"DRAFT",
                      "locale":"es-ES",
                      "jurisdiction":"EU-ES",
                      "reviewedAt":"2026-08-08",
                      "files":{
                        "categories":"categories.json",
                        "ingredientShards":["ingredients-a.json","ingredients-b.json"],
                        "aliasShards":["aliases-a.json","aliases-b.json"],
                        "safetyGroups":"safety-groups.json",
                        "safetySources":"safety-sources.json",
                        "safetyRelations":"safety-relations.json"
                      },
                      "counts":{
                        "categories":1,
                        "ingredients":2,
                        "aliases":2,
                        "safetyGroups":0,
                        "safetySources":0,
                        "safetyRelations":0
                      }
                    }
                """.trimIndent(),
                "ingredient-catalog/v3/categories.json" to """[{"id":"cat-1","code":"C1","name":"Categoría","sortOrder":1}]""",
                "ingredient-catalog/v3/ingredients-a.json" to """[{"id":"ing-a","canonicalName":"Ajo","normalizedName":"ajo","categoryId":"cat-1","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE"}]""",
                "ingredient-catalog/v3/ingredients-b.json" to """[{"id":"ing-b","canonicalName":"Pera","normalizedName":"pera","categoryId":"cat-1","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE"}]""",
                "ingredient-catalog/v3/aliases-a.json" to """[{"id":"alias-a","ingredientId":"ing-a","alias":"Ajos","normalizedAlias":"ajos","languageCode":"es","aliasType":"COMMON"}]""",
                "ingredient-catalog/v3/aliases-b.json" to """[{"id":"alias-b","ingredientId":"ing-b","alias":"Peras","normalizedAlias":"peras","languageCode":"es","aliasType":"COMMON"}]""",
                "ingredient-catalog/v3/safety-groups.json" to "[]",
                "ingredient-catalog/v3/safety-sources.json" to "[]",
                "ingredient-catalog/v3/safety-relations.json" to "[]",
            ),
        )

        val bundle = IngredientCatalogAssetReader(source).read("ingredient-catalog/v3")

        assertEquals(listOf("ing-a", "ing-b"), bundle.ingredients.map { it.id })
        assertEquals(listOf("alias-a", "alias-b"), bundle.aliases.map { it.id })
        assertEquals(3, bundle.manifest.catalogVersion)
        assertEquals(2, bundle.manifest.schemaVersion)
    }

    private class MapCatalogTextSource(
        private val values: Map<String, String>,
    ) : CatalogTextSource {
        override fun read(path: String): String = values[path] ?: error("Missing test asset: $path")
    }
}
