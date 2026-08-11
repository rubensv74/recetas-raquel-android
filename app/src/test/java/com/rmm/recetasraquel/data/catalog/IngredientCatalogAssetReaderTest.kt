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
        assertEquals(0, bundle.ingredientRelations.size)
        assertEquals(0, bundle.regulatoryExemptions.size)
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
        assertEquals(0, bundle.ingredientRelations.size)
        assertEquals(0, bundle.regulatoryExemptions.size)
        assertEquals(3, bundle.manifest.catalogVersion)
        assertEquals(2, bundle.manifest.schemaVersion)
    }

    @Test
    fun readsSchemaThreeLineageShards() {
        val source = MapCatalogTextSource(
            mapOf(
                "ingredient-catalog/v5/manifest.json" to """
                    {
                      "catalogId":"test-lineage",
                      "schemaVersion":3,
                      "catalogVersion":5,
                      "releaseStatus":"DRAFT",
                      "locale":"es-ES",
                      "jurisdiction":"EU-ES",
                      "reviewedAt":"2026-08-08",
                      "files":{
                        "categories":"categories.json",
                        "ingredientShards":["ingredients.json"],
                        "aliasShards":["aliases.json"],
                        "ingredientRelationShards":["lineage-a.json","lineage-b.json"],
                        "safetyGroups":"safety-groups.json",
                        "safetySources":"safety-sources.json",
                        "safetyRelations":"safety-relations.json"
                      },
                      "counts":{
                        "categories":1,
                        "ingredients":2,
                        "aliases":0,
                        "ingredientRelations":2,
                        "safetyGroups":0,
                        "safetySources":0,
                        "safetyRelations":0
                      }
                    }
                """.trimIndent(),
                "ingredient-catalog/v5/categories.json" to """[{"id":"cat-1","code":"C1","name":"Categoría","sortOrder":1}]""",
                "ingredient-catalog/v5/ingredients.json" to """[{"id":"ing-parent","canonicalName":"Pollo","normalizedName":"pollo","categoryId":"cat-1","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE"},{"id":"ing-child","canonicalName":"Pechuga de pollo","normalizedName":"pechuga de pollo","categoryId":"cat-1","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE"}]""",
                "ingredient-catalog/v5/aliases.json" to "[]",
                "ingredient-catalog/v5/lineage-a.json" to """[{"id":"lineage-a","childIngredientId":"ing-child","parentIngredientId":"ing-parent","relationType":"CUT_OF","reviewedAt":"2026-08-08"}]""",
                "ingredient-catalog/v5/lineage-b.json" to """[{"id":"lineage-b","childIngredientId":"ing-parent","parentIngredientId":"ing-parent","relationType":"FORM_OF","reviewedAt":"2026-08-08","isActive":false}]""",
                "ingredient-catalog/v5/safety-groups.json" to "[]",
                "ingredient-catalog/v5/safety-sources.json" to "[]",
                "ingredient-catalog/v5/safety-relations.json" to "[]",
            ),
        )

        val bundle = IngredientCatalogAssetReader(source).read("ingredient-catalog/v5")

        assertEquals(listOf("lineage-a", "lineage-b"), bundle.ingredientRelations.map { it.id })
        assertEquals("CUT_OF", bundle.ingredientRelations.first().relationType)
        assertEquals(0, bundle.regulatoryExemptions.size)
        assertEquals(3, bundle.manifest.schemaVersion)
    }

    @Test
    fun readsSchemaFourRegulatoryExemptionShards() {
        val source = MapCatalogTextSource(
            mapOf(
                "ingredient-catalog/v7/manifest.json" to """
                    {
                      "catalogId":"test-regulatory",
                      "schemaVersion":4,
                      "catalogVersion":7,
                      "releaseStatus":"DRAFT",
                      "locale":"es-ES",
                      "jurisdiction":"EU-ES",
                      "reviewedAt":"2026-08-08",
                      "files":{
                        "categories":"categories.json",
                        "ingredientShards":["ingredients.json"],
                        "aliasShards":["aliases.json"],
                        "regulatoryExemptionShards":["exemptions-a.json","exemptions-b.json"],
                        "safetyGroups":"safety-groups.json",
                        "safetySources":"safety-sources.json",
                        "safetyRelations":"safety-relations.json"
                      },
                      "counts":{
                        "categories":1,
                        "ingredients":1,
                        "aliases":0,
                        "ingredientRelations":0,
                        "regulatoryExemptions":2,
                        "safetyGroups":1,
                        "safetySources":1,
                        "safetyRelations":0
                      }
                    }
                """.trimIndent(),
                "ingredient-catalog/v7/categories.json" to """[{"id":"cat-1","code":"C1","name":"Categoría","sortOrder":1}]""",
                "ingredient-catalog/v7/ingredients.json" to """[{"id":"ing-test","canonicalName":"Derivado de prueba","normalizedName":"derivado de prueba","categoryId":"cat-1","verificationStatus":"REVIEW_REQUIRED","compositionVariability":"STABLE"}]""",
                "ingredient-catalog/v7/aliases.json" to "[]",
                "ingredient-catalog/v7/exemptions-a.json" to """[{"id":"ex-a","ingredientId":"ing-test","safetyGroupId":"sg-test","jurisdiction":"EU","regulatoryEffect":"EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION","conditions":"Condición A","sourceId":"source-test","reviewedAt":"2026-08-08"}]""",
                "ingredient-catalog/v7/exemptions-b.json" to """[{"id":"ex-b","ingredientId":"ing-test","safetyGroupId":"sg-test","jurisdiction":"ES","regulatoryEffect":"EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION","conditions":"Condición B","sourceId":"source-test","reviewedAt":"2026-08-08"}]""",
                "ingredient-catalog/v7/safety-groups.json" to """[{"id":"sg-test","code":"SOYBEANS","displayName":"Soja","conditionType":"FOOD_ALLERGY","regulatoryStatus":"EU_ANNEX_II","jurisdiction":"EU","isActive":true}]""",
                "ingredient-catalog/v7/safety-sources.json" to """[{"id":"source-test","organization":"European Union","title":"Fuente de prueba","officialReference":"TEST","jurisdiction":"EU","reviewDate":"2026-08-08"}]""",
                "ingredient-catalog/v7/safety-relations.json" to "[]",
            ),
        )

        val bundle = IngredientCatalogAssetReader(source).read("ingredient-catalog/v7")

        assertEquals(4, bundle.manifest.schemaVersion)
        assertEquals(listOf("ex-a", "ex-b"), bundle.regulatoryExemptions.map { it.id })
        assertEquals("EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION", bundle.regulatoryExemptions.first().regulatoryEffect)
        assertEquals(0, bundle.safetyRelations.size)
    }

    private class MapCatalogTextSource(
        private val values: Map<String, String>,
    ) : CatalogTextSource {
        override fun read(path: String): String = values[path] ?: error("Missing test asset: $path")
    }
}
