package com.rmm.recetasraquel.data.catalog

import com.rmm.recetasraquel.domain.ingredient.IngredientTextNormalizer
import java.time.LocalDate

class CatalogValidationException(
    val errors: List<String>,
) : IllegalArgumentException(errors.joinToString(separator = "\n"))

data class CatalogValidationResult(
    val errors: List<String>,
) {
    val isValid: Boolean get() = errors.isEmpty()

    fun requireValid() {
        if (!isValid) throw CatalogValidationException(errors)
    }
}

object CatalogValidator {
    private val supportedSchemaVersions = setOf(1, 2)
    private val releaseStatuses = setOf("INFRASTRUCTURE", "DRAFT", "PRODUCTION_CANDIDATE")
    private val verificationStatuses = setOf("VERIFIED", "REVIEW_REQUIRED", "UNVERIFIED")
    private val compositionVariability = setOf("STABLE", "VARIABLE_BY_BRAND", "VARIABLE_BY_PREPARATION", "UNKNOWN")
    private val aliasTypes = setOf("COMMON", "REGIONAL", "SPELLING", "PRESENTATION", "SCIENTIFIC", "OTHER")
    private val conditionTypes = setOf(
        "FOOD_ALLERGY",
        "FOOD_INTOLERANCE",
        "CELIAC_DISEASE",
        "NON_CELIAC_SENSITIVITY",
        "OTHER_MEDICALLY_INDICATED_RESTRICTION",
    )
    private val relationTypes = setOf(
        "INHERENT_SOURCE",
        "CONTAINS",
        "DERIVED_FROM",
        "REGULATED_COMPONENT",
        "DECLARED_MAY_CONTAIN",
        "POSSIBLE_CROSS_REACTIVITY",
        "UNKNOWN",
    )
    private val evidenceLevels = setOf(
        "EU_LEGAL",
        "OFFICIAL_SCIENTIFIC",
        "OFFICIAL_HEALTH_AUTHORITY",
        "MANUFACTURER_LABEL",
        "USER_DECLARED",
        "UNVERIFIED",
    )
    private val euAnnexIiCodes = setOf(
        "CEREALS_CONTAINING_GLUTEN",
        "CRUSTACEANS",
        "EGGS",
        "FISH",
        "PEANUTS",
        "SOYBEANS",
        "MILK",
        "NUTS",
        "CELERY",
        "MUSTARD",
        "SESAME",
        "SULPHUR_DIOXIDE_AND_SULPHITES",
        "LUPIN",
        "MOLLUSCS",
    )

    fun validate(bundle: IngredientCatalogBundle): CatalogValidationResult {
        val errors = mutableListOf<String>()
        val manifest = bundle.manifest

        requireNonBlank(errors, "manifest.catalogId", manifest.catalogId)
        if (manifest.schemaVersion !in supportedSchemaVersions) {
            errors += "Unsupported schemaVersion: ${manifest.schemaVersion}"
        }
        requirePositive(errors, "manifest.catalogVersion", manifest.catalogVersion)
        if (manifest.releaseStatus !in releaseStatuses) errors += "Unsupported releaseStatus: ${manifest.releaseStatus}"
        requireNonBlank(errors, "manifest.locale", manifest.locale)
        requireNonBlank(errors, "manifest.jurisdiction", manifest.jurisdiction)
        requireDate(errors, "manifest.reviewedAt", manifest.reviewedAt)
        validateFileLayout(manifest.files, manifest.schemaVersion, errors)
        validateCounts(bundle, errors)

        validateUnique(errors, "category id", bundle.categories.map { it.id })
        validateUnique(errors, "category code", bundle.categories.map { it.code })
        validateUnique(errors, "ingredient id", bundle.ingredients.map { it.id })
        validateUnique(errors, "ingredient normalizedName", bundle.ingredients.map { it.normalizedName })
        validateUnique(errors, "alias id", bundle.aliases.map { it.id })
        validateUnique(errors, "safety group id", bundle.safetyGroups.map { it.id })
        validateUnique(errors, "safety group code", bundle.safetyGroups.map { it.code })
        validateUnique(errors, "safety source id", bundle.safetySources.map { it.id })
        validateUnique(errors, "safety relation id", bundle.safetyRelations.map { it.id })

        val categoryIds = bundle.categories.mapTo(mutableSetOf()) { it.id }
        val ingredientIds = bundle.ingredients.mapTo(mutableSetOf()) { it.id }
        val groupIds = bundle.safetyGroups.mapTo(mutableSetOf()) { it.id }
        val sourceIds = bundle.safetySources.mapTo(mutableSetOf()) { it.id }

        bundle.categories.forEach { category ->
            requireNonBlank(errors, "category.id", category.id)
            requireNonBlank(errors, "category.code", category.code)
            requireNonBlank(errors, "category.name", category.name)
            if (category.sortOrder < 0) errors += "Category ${category.id} has negative sortOrder"
        }

        bundle.ingredients.forEach { ingredient ->
            requireNonBlank(errors, "ingredient.id", ingredient.id)
            requireNonBlank(errors, "ingredient.canonicalName", ingredient.canonicalName)
            if (ingredient.categoryId !in categoryIds) errors += "Ingredient ${ingredient.id} references missing category ${ingredient.categoryId}"
            if (ingredient.normalizedName != IngredientTextNormalizer.normalize(ingredient.canonicalName)) {
                errors += "Ingredient ${ingredient.id} has invalid normalizedName"
            }
            if (ingredient.verificationStatus !in verificationStatuses) {
                errors += "Ingredient ${ingredient.id} has unsupported verificationStatus ${ingredient.verificationStatus}"
            }
            if (ingredient.compositionVariability !in compositionVariability) {
                errors += "Ingredient ${ingredient.id} has unsupported compositionVariability ${ingredient.compositionVariability}"
            }
        }

        bundle.aliases.forEach { alias ->
            requireNonBlank(errors, "alias.id", alias.id)
            requireNonBlank(errors, "alias.alias", alias.alias)
            if (alias.ingredientId !in ingredientIds) errors += "Alias ${alias.id} references missing ingredient ${alias.ingredientId}"
            if (alias.normalizedAlias != IngredientTextNormalizer.normalize(alias.alias)) errors += "Alias ${alias.id} has invalid normalizedAlias"
            if (alias.aliasType !in aliasTypes) errors += "Alias ${alias.id} has unsupported aliasType ${alias.aliasType}"
            requireNonBlank(errors, "alias.languageCode", alias.languageCode)
        }

        bundle.safetyGroups.forEach { group ->
            requireNonBlank(errors, "safetyGroup.id", group.id)
            requireNonBlank(errors, "safetyGroup.code", group.code)
            requireNonBlank(errors, "safetyGroup.displayName", group.displayName)
            if (group.conditionType !in conditionTypes) errors += "Safety group ${group.id} has unsupported conditionType ${group.conditionType}"
            requireNonBlank(errors, "safetyGroup.regulatoryStatus", group.regulatoryStatus)
            requireNonBlank(errors, "safetyGroup.jurisdiction", group.jurisdiction)
        }

        bundle.safetySources.forEach { source ->
            requireNonBlank(errors, "safetySource.id", source.id)
            requireNonBlank(errors, "safetySource.organization", source.organization)
            requireNonBlank(errors, "safetySource.title", source.title)
            requireNonBlank(errors, "safetySource.officialReference", source.officialReference)
            requireNonBlank(errors, "safetySource.jurisdiction", source.jurisdiction)
            requireDate(errors, "safetySource.reviewDate", source.reviewDate)
            source.publicationDate?.let { requireDate(errors, "safetySource.publicationDate", it) }
        }

        bundle.safetyRelations.forEach { relation ->
            if (relation.ingredientId !in ingredientIds) errors += "Safety relation ${relation.id} references missing ingredient ${relation.ingredientId}"
            if (relation.safetyGroupId !in groupIds) errors += "Safety relation ${relation.id} references missing safety group ${relation.safetyGroupId}"
            if (relation.sourceId !in sourceIds) errors += "Safety relation ${relation.id} references missing source ${relation.sourceId}"
            if (relation.relationType !in relationTypes) errors += "Safety relation ${relation.id} has unsupported relationType ${relation.relationType}"
            if (relation.evidenceLevel !in evidenceLevels) errors += "Safety relation ${relation.id} has unsupported evidenceLevel ${relation.evidenceLevel}"
            requireDate(errors, "safetyRelation.reviewedAt", relation.reviewedAt)
        }

        if (manifest.releaseStatus == "PRODUCTION_CANDIDATE") validateProductionCandidate(bundle, errors)

        return CatalogValidationResult(errors.distinct())
    }

    private fun validateFileLayout(files: CatalogFiles, schemaVersion: Int, errors: MutableList<String>) {
        requireNonBlank(errors, "manifest.files.categories", files.categories)
        requireNonBlank(errors, "manifest.files.safetyGroups", files.safetyGroups)
        requireNonBlank(errors, "manifest.files.safetySources", files.safetySources)
        requireNonBlank(errors, "manifest.files.safetyRelations", files.safetyRelations)

        val ingredientShards = files.ingredientShards.orEmpty()
        val aliasShards = files.aliasShards.orEmpty()
        val hasIngredientSingle = !files.ingredients.isNullOrBlank()
        val hasAliasSingle = !files.aliases.isNullOrBlank()

        if (hasIngredientSingle == ingredientShards.isNotEmpty()) {
            errors += "Manifest must declare exactly one ingredient file mode: ingredients or ingredientShards"
        }
        if (hasAliasSingle == aliasShards.isNotEmpty()) {
            errors += "Manifest must declare exactly one alias file mode: aliases or aliasShards"
        }
        if (schemaVersion == 1 && (ingredientShards.isNotEmpty() || aliasShards.isNotEmpty())) {
            errors += "Catalog schemaVersion 1 does not support sharded ingredient/alias files"
        }

        ingredientShards.forEach { requireNonBlank(errors, "manifest.files.ingredientShards", it) }
        aliasShards.forEach { requireNonBlank(errors, "manifest.files.aliasShards", it) }
        validateUnique(errors, "ingredient shard file", ingredientShards)
        validateUnique(errors, "alias shard file", aliasShards)
    }

    private fun validateCounts(bundle: IngredientCatalogBundle, errors: MutableList<String>) {
        val c = bundle.manifest.counts
        compareCount(errors, "categories", c.categories, bundle.categories.size)
        compareCount(errors, "ingredients", c.ingredients, bundle.ingredients.size)
        compareCount(errors, "aliases", c.aliases, bundle.aliases.size)
        compareCount(errors, "safetyGroups", c.safetyGroups, bundle.safetyGroups.size)
        compareCount(errors, "safetySources", c.safetySources, bundle.safetySources.size)
        compareCount(errors, "safetyRelations", c.safetyRelations, bundle.safetyRelations.size)
    }

    private fun validateProductionCandidate(bundle: IngredientCatalogBundle, errors: MutableList<String>) {
        if (bundle.categories.size < 20) errors += "Production candidate requires at least 20 categories"
        if (bundle.ingredients.size < 600) errors += "Production candidate requires at least 600 canonical ingredients"
        if (bundle.aliases.size < 1200) errors += "Production candidate requires at least 1200 aliases"
        if (bundle.manifest.jurisdiction.contains("EU")) {
            val present = bundle.safetyGroups.mapTo(mutableSetOf()) { it.code }
            val missing = euAnnexIiCodes - present
            if (missing.isNotEmpty()) errors += "Production candidate is missing EU Annex II groups: ${missing.sorted().joinToString()}"
        }
    }

    private fun compareCount(errors: MutableList<String>, label: String, expected: Int, actual: Int) {
        if (expected != actual) errors += "Manifest count mismatch for $label: expected $expected, actual $actual"
    }

    private fun validateUnique(errors: MutableList<String>, label: String, values: List<String>) {
        values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { duplicate ->
            errors += "Duplicate $label: $duplicate"
        }
    }

    private fun requireNonBlank(errors: MutableList<String>, field: String, value: String) {
        if (value.isBlank()) errors += "$field must not be blank"
    }

    private fun requirePositive(errors: MutableList<String>, field: String, value: Int) {
        if (value <= 0) errors += "$field must be greater than zero"
    }

    private fun requireDate(errors: MutableList<String>, field: String, value: String) {
        try {
            LocalDate.parse(value)
        } catch (_: Exception) {
            errors += "$field must use ISO date format yyyy-MM-dd"
        }
    }
}
