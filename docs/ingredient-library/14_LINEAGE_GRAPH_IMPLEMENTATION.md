# 14 — LINEAGE GRAPH IMPLEMENTATION

**Status:** IMPLEMENTATION IN PROGRESS — local validation pending  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Decision applied

Architectural option **B** from `13_ARCHITECTURAL_DECISION_INGREDIENT_LINEAGE.md` is accepted.

The master catalog will use a separate, non-clinical lineage graph for relationships between catalog ingredients. The lineage graph and food-safety graph remain independent.

Initial lineage relation types are:

```text
VARIANT_OF
CUT_OF
DERIVED_FROM
FORM_OF
```

No lineage edge may create, inherit or imply a food-safety relation.

## Room evolution

The implementation evolves Room from v2 to v3 with an explicit non-destructive migration.

New table:

```text
catalog_ingredient_relations
- id
- childIngredientId
- parentIngredientId
- relationType
- reviewedAt
- sourceReference nullable
- notes nullable
- isActive
```

Both child and parent reference `catalog_ingredients`.

The migration also corrects a Phase-3 contract discrepancy in `custom_ingredient_safety_relations`: custom safety relations now require a `sourceId` just like shipped safety relations. Existing v2 rows, if present, are preserved through a migration source record and their former free-text source is retained as `sourceDetails`.

## Catalog schema evolution

Catalog schema v3 adds optional lineage files while remaining backward-compatible with catalog schema v1/v2.

A future catalog version may use either:

```text
ingredientRelations: "ingredient-relations.json"
```

or sharded files:

```text
ingredientRelationShards: [ ... ]
```

Older catalog versions have zero lineage relations.

## Validation rules

The catalog validator must reject:

- unknown child or parent ingredient IDs;
- self-relations;
- unsupported lineage relation types;
- duplicate relation IDs;
- duplicate `(child,parent,type)` edges;
- invalid review dates;
- lineage cycles.

Food-safety validation remains independent and unchanged.

## Gate

Before catalog content starts using lineage, validate:

```text
Room v2 -> v3 migration
Room v1 -> v3 chained migration
schema 3.json export
catalog v4 import under Room v3
unit tests
lint
instrumented tests
release build
```

Catalog v4 remains the active flat catalog during this infrastructure gate. The first catalog that actually contains lineage edges must be a new immutable catalog version.
