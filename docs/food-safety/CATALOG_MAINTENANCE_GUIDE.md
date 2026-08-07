# CATALOG MAINTENANCE GUIDE — DRAFT

This guide becomes executable after the catalog schema/validator is implemented.

## Add a category

1. Create a stable code/ID.
2. Add localized name, sort order and active state.
3. Run full catalog validation.

## Add an ingredient

1. Stable ID.
2. Canonical name + normalized search key.
3. Existing category.
4. Optional default unit/description.
5. No safety relation unless separately sourced.

## Add an alias

Alias must reference an existing ingredient and have its own normalized search key. Aliases are not fuzzy rules.

## Register a source

Add source metadata first; only then create safety relations that reference it.

## Add a safety relation

Required: ingredient, group, relation type, evidence level, source and reviewed date. Do not infer missing data.

## Correct a relation

Do not silently overwrite provenance. Update review metadata and preserve version history in Git/catalog versioning.

## Deactivate an ingredient

Set inactive. Do not physically delete a master record used by recipes.

## Find UNVERIFIED records

The validator/report must provide a dedicated list/query.

## New catalog version

1. Change catalog version independently of Room schema.
2. Validate full bundle.
3. Compare counts and changed relations.
4. Review source deltas.
5. Import transactionally.
