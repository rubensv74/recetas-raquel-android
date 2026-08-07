package com.rmm.recetasraquel.data.catalog

import com.rmm.recetasraquel.domain.ingredient.IngredientTextNormalizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogValidatorTest {
    @Test
    fun infrastructureBundleCanBeStructurallyValidWithoutMassCatalogPopulation() {
        val result = CatalogValidator.validate(validBundle())
        assertTrue(result.errors.joinToString(), result.isValid)
    }

    @Test
    fun schemaTwoCanUseShardedIngredientAndAliasFiles() {
        val base = validBundle()
        val bundle = base.copy(
            manifest = base.manifest.copy(
                schemaVersion = 2,
                files = base.manifest.files.copy(
                    ingredients = null,
                    aliases = null,
                    ingredientShards = listOf("ingredients-a.json", "ingredients-b.json"),
                    aliasShards = listOf("aliases-a.json"),
                ),
            ),
        )

        val result = CatalogValidator.validate(bundle)
        assertTrue(result.errors.joinToString(), result.isValid)
    }

    @Test
    fun rejectsAmbiguousSingleAndShardedFileModes() {
        val base = validBundle()
        val bundle = base.copy(
            manifest = base.manifest.copy(
                schemaVersion = 2,
                files = base.manifest.files.copy(
                    ingredientShards = listOf("ingredients-a.json"),
                    aliasShards = listOf("aliases-a.json"),
                ),
            ),
        )

        val result = CatalogValidator.validate(bundle)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("exactly one ingredient file mode") })
        assertTrue(result.errors.any { it.contains("exactly one alias file mode") })
    }

    @Test
    fun detectsManifestCountMismatchAndBrokenAliasReference() {
        val base = validBundle()
        val bundle = base.copy(
            manifest = base.manifest.copy(counts = base.manifest.counts.copy(aliases = 0)),
            aliases = listOf(
                CatalogAliasRecord(
                    id = "alias-1",
                    ingredientId = "missing",
                    alias = "Tomáte",
                    normalizedAlias = "tomate",
                    languageCode = "es",
                    aliasType = "COMMON",
                ),
            ),
        )

        val result = CatalogValidator.validate(bundle)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Manifest count mismatch for aliases") })
        assertTrue(result.errors.any { it.contains("references missing ingredient") })
    }

    @Test
    fun safetyRelationRequiresKnownIngredientGroupSourceAndSupportedEvidence() {
        val base = validBundle()
        val bundle = base.copy(
            manifest = base.manifest.copy(counts = base.manifest.counts.copy(safetyRelations = 1)),
            safetyRelations = listOf(
                CatalogSafetyRelationRecord(
                    id = "rel-1",
                    ingredientId = "missing-ingredient",
                    safetyGroupId = "missing-group",
                    relationType = "CONTAINS",
                    evidenceLevel = "MODEL_GUESS",
                    sourceId = "missing-source",
                    reviewedAt = "2026-08-08",
                ),
            ),
        )

        val result = CatalogValidator.validate(bundle)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("missing ingredient") })
        assertTrue(result.errors.any { it.contains("missing safety group") })
        assertTrue(result.errors.any { it.contains("missing source") })
        assertTrue(result.errors.any { it.contains("unsupported evidenceLevel") })
    }

    @Test
    fun productionCandidateCannotPassWithInfrastructureOnlyCoverage() {
        val base = validBundle()
        val result = CatalogValidator.validate(
            base.copy(manifest = base.manifest.copy(releaseStatus = "PRODUCTION_CANDIDATE")),
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("600 canonical ingredients") })
        assertTrue(result.errors.any { it.contains("1200 aliases") })
        assertTrue(result.errors.any { it.contains("EU Annex II groups") })
    }

    @Test
    fun ingredientNormalizerIsAccentAndCaseInsensitiveWithoutSemanticGuessing() {
        assertTrue(IngredientTextNormalizer.normalize("  TOMÁTE   Rojo ") == "tomate rojo")
        assertTrue(IngredientTextNormalizer.normalize("nata cocina") == "nata cocina")
        assertTrue(IngredientTextNormalizer.normalize("nata cocina") != IngredientTextNormalizer.normalize("nata"))
    }

    private fun validBundle(): IngredientCatalogBundle {
        val categories = listOf(
            CatalogCategoryRecord(
                id = "cat-vegetables",
                code = "VEGETABLES",
                name = "Verduras y hortalizas",
                sortOrder = 10,
            ),
        )
        return IngredientCatalogBundle(
            manifest = CatalogManifest(
                catalogId = "test",
                schemaVersion = 1,
                catalogVersion = 1,
                releaseStatus = "INFRASTRUCTURE",
                locale = "es-ES",
                jurisdiction = "EU-ES",
                reviewedAt = "2026-08-08",
                files = CatalogFiles(
                    categories = "categories.json",
                    ingredients = "ingredients.json",
                    aliases = "aliases.json",
                    safetyGroups = "safety-groups.json",
                    safetySources = "safety-sources.json",
                    safetyRelations = "safety-relations.json",
                ),
                counts = CatalogCounts(
                    categories = categories.size,
                    ingredients = 0,
                    aliases = 0,
                    safetyGroups = 0,
                    safetySources = 0,
                    safetyRelations = 0,
                ),
            ),
            categories = categories,
            ingredients = emptyList(),
            aliases = emptyList(),
            safetyGroups = emptyList(),
            safetySources = emptyList(),
            safetyRelations = emptyList(),
        )
    }
}
