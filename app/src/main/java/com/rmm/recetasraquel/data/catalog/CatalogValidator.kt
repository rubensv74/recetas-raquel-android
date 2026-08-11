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
    private val supportedSchemaVersions = setOf(1, 2, 3, 4, 5)
    private val releaseStatuses = setOf("INFRASTRUCTURE", "DRAFT", "PRODUCTION_CANDIDATE")
    private val verificationStatuses = setOf("VERIFIED", "REVIEW_REQUIRED", "UNVERIFIED")
    private val compositionVariability = setOf("STABLE", "VARIABLE_BY_BRAND", "VARIABLE_BY_PREPARATION", "UNKNOWN")
    private val compositionCoverage = setOf("NONE", "COMPLETE", "PARTIAL")
    private val componentPresenceTypes = setOf("REQUIRED", "POSSIBLE")
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
    private val ingredientLineageRelationTypes = setOf(
        "VARIANT_OF",
        "CUT_OF",
        "DERIVED_FROM",
        "FORM_OF",
    )
    private val evidenceLevels = setOf(
        "EU_LEGAL",
        "OFFICIAL_SCIENTIFIC",
        "OFFICIAL_HEALTH_AUTHORITY",
        "MANUFACTURER_LABEL",
        "USER_DECLARED",
        "UNVERIFIED",
    )
    private val regulatoryEffects = setOf(
        "EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION",
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
        validateUnique(errors, "ingredient relation id", bundle.ingredientRelations.map { it.id })
        validateUnique(errors, "ingredient component id", bundle.ingredientComponents.map { it.id })
        validateUnique(errors, "safety group id", bundle.safetyGroups.map { it.id })
        validateUnique(errors, "safety group code", bundle.safetyGroups.map { it.code })
        validateUnique(errors, "safety source id", bundle.safetySources.map { it.id })
        validateUnique(errors, "safety relation id", bundle.safetyRelations.map { it.id })
        validateUnique(errors, "regulatory exemption id", bundle.regulatoryExemptions.map { it.id })

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
            val coverage = ingredient.compositionCoverage ?: CatalogImporter.DEFAULT_COMPOSITION_COVERAGE
            if (coverage !in compositionCoverage) {
                errors += "Ingredient ${ingredient.id} has unsupported compositionCoverage $coverage"
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

        validateIngredientRelations(bundle.ingredientRelations, ingredientIds, errors)
        validateIngredientComponents(bundle, ingredientIds, errors)

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

        validateRegulatoryExemptions(
            exemptions = bundle.regulatoryExemptions,
            ingredientIds = ingredientIds,
            groupIds = groupIds,
            sourceIds = sourceIds,
            errors = errors,
        )

        if (manifest.releaseStatus == "PRODUCTION_CANDIDATE") validateProductionCandidate(bundle, errors)

        return CatalogValidationResult(errors.distinct())
    }

    private fun validateIngredientRelations(
        relations: List<CatalogIngredientRelationRecord>,
        ingredientIds: Set<String>,
        errors: MutableList<String>,
    ) {
        val edgeKeys = mutableSetOf<String>()
        relations.forEach { relation ->
            requireNonBlank(errors, "ingredientRelation.id", relation.id)
            if (relation.childIngredientId !in ingredientIds) {
                errors += "Ingredient relation ${relation.id} references missing child ingredient ${relation.childIngredientId}"
            }
            if (relation.parentIngredientId !in ingredientIds) {
                errors += "Ingredient relation ${relation.id} references missing parent ingredient ${relation.parentIngredientId}"
            }
            if (relation.childIngredientId == relation.parentIngredientId) {
                errors += "Ingredient relation ${relation.id} cannot reference the same ingredient as child and parent"
            }
            if (relation.relationType !in ingredientLineageRelationTypes) {
                errors += "Ingredient relation ${relation.id} has unsupported relationType ${relation.relationType}"
            }
            requireDate(errors, "ingredientRelation.reviewedAt", relation.reviewedAt)

            val edgeKey = "${relation.childIngredientId}|${relation.parentIngredientId}|${relation.relationType}"
            if (!edgeKeys.add(edgeKey)) errors += "Duplicate ingredient relation edge: $edgeKey"
        }

        detectLineageCycles(relations, errors)
    }

    private fun validateIngredientComponents(
        bundle: IngredientCatalogBundle,
        ingredientIds: Set<String>,
        errors: MutableList<String>,
    ) {
        val edgeKeys = mutableSetOf<String>()
        val activeComponentsByParent = bundle.ingredientComponents
            .filter { it.isActive }
            .groupBy { it.parentIngredientId }

        bundle.ingredientComponents.forEach { component ->
            requireNonBlank(errors, "ingredientComponent.id", component.id)
            if (component.parentIngredientId !in ingredientIds) {
                errors += "Ingredient component ${component.id} references missing parent ingredient ${component.parentIngredientId}"
            }
            if (component.componentIngredientId !in ingredientIds) {
                errors += "Ingredient component ${component.id} references missing component ingredient ${component.componentIngredientId}"
            }
            if (component.parentIngredientId == component.componentIngredientId) {
                errors += "Ingredient component ${component.id} cannot reference the same ingredient as parent and component"
            }
            if (component.presenceType !in componentPresenceTypes) {
                errors += "Ingredient component ${component.id} has unsupported presenceType ${component.presenceType}"
            }
            requireDate(errors, "ingredientComponent.reviewedAt", component.reviewedAt)

            val edgeKey = "${component.parentIngredientId}|${component.componentIngredientId}"
            if (!edgeKeys.add(edgeKey)) errors += "Duplicate ingredient component edge: $edgeKey"
        }

        bundle.ingredients.forEach { ingredient ->
            val coverage = ingredient.compositionCoverage ?: CatalogImporter.DEFAULT_COMPOSITION_COVERAGE
            val activeComponents = activeComponentsByParent[ingredient.id].orEmpty()
            if (coverage == "NONE" && activeComponents.isNotEmpty()) {
                errors += "Ingredient ${ingredient.id} has active components but compositionCoverage is NONE"
            }
            if (coverage != "NONE" && activeComponents.isEmpty()) {
                errors += "Ingredient ${ingredient.id} declares compositionCoverage $coverage but has no active components"
            }
        }

        detectComponentCycles(bundle.ingredientComponents.filter { it.isActive }, errors)
    }

    private fun validateRegulatoryExemptions(
        exemptions: List<CatalogRegulatoryExemptionRecord>,
        ingredientIds: Set<String>,
        groupIds: Set<String>,
        sourceIds: Set<String>,
        errors: MutableList<String>,
    ) {
        val conceptualKeys = mutableSetOf<String>()
        exemptions.forEach { exemption ->
            requireNonBlank(errors, "regulatoryExemption.id", exemption.id)
            if (exemption.ingredientId !in ingredientIds) {
                errors += "Regulatory exemption ${exemption.id} references missing ingredient ${exemption.ingredientId}"
            }
            if (exemption.safetyGroupId !in groupIds) {
                errors += "Regulatory exemption ${exemption.id} references missing safety group ${exemption.safetyGroupId}"
            }
            if (exemption.sourceId !in sourceIds) {
                errors += "Regulatory exemption ${exemption.id} references missing source ${exemption.sourceId}"
            }
            requireNonBlank(errors, "regulatoryExemption.jurisdiction", exemption.jurisdiction)
            requireNonBlank(errors, "regulatoryExemption.conditions", exemption.conditions)
            if (exemption.regulatoryEffect !in regulatoryEffects) {
                errors += "Regulatory exemption ${exemption.id} has unsupported regulatoryEffect ${exemption.regulatoryEffect}"
            }
            requireDate(errors, "regulatoryExemption.reviewedAt", exemption.reviewedAt)
            exemption.effectiveFrom?.let { requireDate(errors, "regulatoryExemption.effectiveFrom", it) }
            exemption.effectiveTo?.let { requireDate(errors, "regulatoryExemption.effectiveTo", it) }

            val from = exemption.effectiveFrom?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val to = exemption.effectiveTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (from != null && to != null && from.isAfter(to)) {
                errors += "Regulatory exemption ${exemption.id} has effectiveFrom after effectiveTo"
            }

            val key = "${exemption.ingredientId}|${exemption.safetyGroupId}|${exemption.jurisdiction}|${exemption.regulatoryEffect}"
            if (!conceptualKeys.add(key)) errors += "Duplicate regulatory exemption: $key"
        }
    }

    private fun detectLineageCycles(
        relations: List<CatalogIngredientRelationRecord>,
        errors: MutableList<String>,
    ) {
        val adjacency = relations.groupBy { it.childIngredientId }.mapValues { (_, edges) -> edges.map { it.parentIngredientId } }
        if (hasCycle(adjacency)) errors += "Ingredient lineage contains a cycle"
    }

    private fun detectComponentCycles(
        components: List<CatalogIngredientComponentRecord>,
        errors: MutableList<String>,
    ) {
        val adjacency = components.groupBy { it.parentIngredientId }.mapValues { (_, edges) -> edges.map { it.componentIngredientId } }
        if (hasCycle(adjacency)) errors += "Ingredient composition contains a cycle"
    }

    private fun hasCycle(adjacency: Map<String, List<String>>): Boolean {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(node: String): Boolean {
            if (node in visiting) return true
            if (node in visited) return false
            visiting += node
            val cycle = adjacency[node].orEmpty().any(::visit)
            visiting -= node
            visited += node
            return cycle
        }

        return adjacency.keys.any(::visit)
    }

    private fun validateFileLayout(files: CatalogFiles, schemaVersion: Int, errors: MutableList<String>) {
        requireNonBlank(errors, "manifest.files.categories", files.categories)
        requireNonBlank(errors, "manifest.files.safetyGroups", files.safetyGroups)
        requireNonBlank(errors, "manifest.files.safetySources", files.safetySources)
        requireNonBlank(errors, "manifest.files.safetyRelations", files.safetyRelations)

        val ingredientShards = files.ingredientShards.orEmpty()
        val aliasShards = files.aliasShards.orEmpty()
        val ingredientRelationShards = files.ingredientRelationShards.orEmpty()
        val ingredientComponentShards = files.ingredientComponentShards.orEmpty()
        val regulatoryExemptionShards = files.regulatoryExemptionShards.orEmpty()
        val hasIngredientSingle = !files.ingredients.isNullOrBlank()
        val hasAliasSingle = !files.aliases.isNullOrBlank()
        val hasIngredientRelationSingle = !files.ingredientRelations.isNullOrBlank()
        val hasIngredientComponentSingle = !files.ingredientComponents.isNullOrBlank()
        val hasRegulatoryExemptionSingle = !files.regulatoryExemptions.isNullOrBlank()

        if (hasIngredientSingle == ingredientShards.isNotEmpty()) {
            errors += "Manifest must declare exactly one ingredient file mode: ingredients or ingredientShards"
        }
        if (hasAliasSingle == aliasShards.isNotEmpty()) {
            errors += "Manifest must declare exactly one alias file mode: aliases or aliasShards"
        }
        if (hasIngredientRelationSingle && ingredientRelationShards.isNotEmpty()) {
            errors += "Manifest cannot declare both ingredientRelations and ingredientRelationShards"
        }
        if (hasIngredientComponentSingle && ingredientComponentShards.isNotEmpty()) {
            errors += "Manifest cannot declare both ingredientComponents and ingredientComponentShards"
        }
        if (hasRegulatoryExemptionSingle && regulatoryExemptionShards.isNotEmpty()) {
            errors += "Manifest cannot declare both regulatoryExemptions and regulatoryExemptionShards"
        }
        if (schemaVersion == 1 && (ingredientShards.isNotEmpty() || aliasShards.isNotEmpty())) {
            errors += "Catalog schemaVersion 1 does not support sharded ingredient/alias files"
        }
        if (schemaVersion < 3 && (hasIngredientRelationSingle || ingredientRelationShards.isNotEmpty())) {
            errors += "Catalog schemaVersion $schemaVersion does not support ingredient lineage files"
        }
        if (schemaVersion < 4 && (hasRegulatoryExemptionSingle || regulatoryExemptionShards.isNotEmpty())) {
            errors += "Catalog schemaVersion $schemaVersion does not support regulatory exemption files"
        }
        if (schemaVersion < 5 && (hasIngredientComponentSingle || ingredientComponentShards.isNotEmpty())) {
            errors += "Catalog schemaVersion $schemaVersion does not support ingredient component files"
        }

        ingredientShards.forEach { requireNonBlank(errors, "manifest.files.ingredientShards", it) }
        aliasShards.forEach { requireNonBlank(errors, "manifest.files.aliasShards", it) }
        ingredientRelationShards.forEach { requireNonBlank(errors, "manifest.files.ingredientRelationShards", it) }
        ingredientComponentShards.forEach { requireNonBlank(errors, "manifest.files.ingredientComponentShards", it) }
        regulatoryExemptionShards.forEach { requireNonBlank(errors, "manifest.files.regulatoryExemptionShards", it) }
        validateUnique(errors, "ingredient shard file", ingredientShards)
        validateUnique(errors, "alias shard file", aliasShards)
        validateUnique(errors, "ingredient relation shard file", ingredientRelationShards)
        validateUnique(errors, "ingredient component shard file", ingredientComponentShards)
        validateUnique(errors, "regulatory exemption shard file", regulatoryExemptionShards)
    }

    private fun validateCounts(bundle: IngredientCatalogBundle, errors: MutableList<String>) {
        val c = bundle.manifest.counts
        compareCount(errors, "categories", c.categories, bundle.categories.size)
        compareCount(errors, "ingredients", c.ingredients, bundle.ingredients.size)
        compareCount(errors, "aliases", c.aliases, bundle.aliases.size)
        compareCount(errors, "ingredientRelations", c.ingredientRelations, bundle.ingredientRelations.size)
        compareCount(errors, "ingredientComponents", c.ingredientComponents, bundle.ingredientComponents.size)
        compareCount(errors, "safetyGroups", c.safetyGroups, bundle.safetyGroups.size)
        compareCount(errors, "safetySources", c.safetySources, bundle.safetySources.size)
        compareCount(errors, "safetyRelations", c.safetyRelations, bundle.safetyRelations.size)
        compareCount(errors, "regulatoryExemptions", c.regulatoryExemptions, bundle.regulatoryExemptions.size)
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
