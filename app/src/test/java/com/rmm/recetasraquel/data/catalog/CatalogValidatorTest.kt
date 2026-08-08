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
    fun schemaThreeCanDeclareIngredientLineageShards() {
        val base = bundleWithTwoIngredients()
        val relation = CatalogIngredientRelationRecord(
            id = "lineage-child-parent",
            childIngredientId = "ing-child",
            parentIngredientId = "ing-parent",
            relationType = "FORM_OF",
            reviewedAt = "2026-08-08",
        )
        val bundle = base.copy(
            manifest = base.manifest.copy(
                schemaVersion = 3,
                files = base.manifest.files.copy(
                    ingredientRelationShards = listOf("relations-a.json"),
                ),
                counts = base.manifest.counts.copy(ingredientRelations = 1),
            ),
            ingredientRelations = listOf(relation),
        )

        val result = CatalogValidator.validate(bundle)
        assertTrue(result.errors.joinToString(), result.isValid)
    }

    @Test
    fun lineageCannotBeDeclaredBeforeSchemaThree() {
        val base = bundleWithTwoIngredients()
        val bundle = base.copy(
            manifest = base.manifest.copy(
                schemaVersion = 2,
                files = base.manifest.files.copy(ingredientRelations = "relations.json"),
                counts = base.manifest.counts.copy(ingredientRelations = 1),
            ),
            ingredientRelations = listOf(
                CatalogIngredientRelationRecord(
                    id = "lineage-child-parent",
                    childIngredientId = "ing-child",
                    parentIngredientId = "ing-parent",
                    relationType = "FORM_OF",
                    reviewedAt = "2026-08-08",
                ),
            ),
        )

        val result = CatalogValidator.validate(bundle)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("does not support ingredient lineage files") })
    }

    @Test
    fun lineageRejectsUnknownReferencesSelfRelationAndUnsupportedType() {
        val base = bundleWithTwoIngredients()
        val bundle = base.copy(
            manifest = base.manifest.copy(
                schemaVersion = 3,
                files = base.manifest.files.copy(ingredientRelations = "relations.json"),
                counts = base.manifest.counts.copy(ingredientRelations = 3),
            ),
            ingredientRelations = listOf(
                CatalogIngredientRelationRecord(
                    id = "missing-child",
                    childIngredientId = "missing",
                    parentIngredientId = "ing-parent",
                    relationType = "FORM_OF",
                    reviewedAt = "2026-08-08",
                ),
                CatalogIngredientRelationRecord(
                    id = "self",
                    childIngredientId = "ing-parent",
                    parentIngredientId = "ing-parent",
                    relationType = "FORM_OF",
                    reviewedAt = "2026-08-08",
                ),
                CatalogIngredientRelationRecord(
                    id = "bad-type",
                    childIngredientId = "ing-child",
                    parentIngredientId = "ing-parent",
                    relationType = "CONTAINS_ALLERGEN",
                    reviewedAt = "2026-08-08",
                ),
            ),
        )

        val result = CatalogValidator.validate(bundle)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("missing child ingredient") })
        assertTrue(result.errors.any { it.contains("same ingredient as child and parent") })
        assertTrue(result.errors.any { it.contains("unsupported relationType") })
    }

    @Test
    fun lineageRejectsCycles() {
        val base = bundleWithTwoIngredients()
        val bundle = base.copy(
            manifest = base.manifest.copy(
                schemaVersion = 3,
                files = base.manifest.files.copy(ingredientRelations = "relations.json"),
                counts = base.manifest.counts.copy(ingredientRelations = 2),
            ),
            ingredientRelations = listOf(
                CatalogIngredientRelationRecord(
                    id = "a-to-b",
                    childIngredientId = "ing-child",
                    parentIngredientId = "ing-parent",
                    relationType = "FORM_OF",
                    reviewedAt = "2026-08-08",
                ),
                CatalogIngredientRelationRecord(
                    id = "b-to-a",
                    childIngredientId = "ing-parent",
                    parentIngredientId = "ing-child",
                    relationType = "FORM_OF",
                    reviewedAt = "2026-08-08",
                ),
            ),
        )

        val result = CatalogValidator.validate(bundle)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("lineage contains a cycle") })
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

    private fun bundleWithTwoIngredients(): IngredientCatalogBundle {
        val base = validBundle()
        val ingredients = listOf(
            CatalogIngredientRecord(
                id = "ing-parent",
                canonicalName = "Ingrediente padre",
                normalizedName = "ingrediente padre",
                categoryId = "cat-vegetables",
                verificationStatus = "REVIEW_REQUIRED",
                compositionVariability = "STABLE",
            ),
            CatalogIngredientRecord(
                id = "ing-child",
                canonicalName = "Ingrediente hijo",
                normalizedName = "ingrediente hijo",
                categoryId = "cat-vegetables",
                verificationStatus = "REVIEW_REQUIRED",
                compositionVariability = "STABLE",
            ),
        )
        return base.copy(
            manifest = base.manifest.copy(
                counts = base.manifest.counts.copy(ingredients = ingredients.size),
            ),
            ingredients = ingredients,
        )
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
