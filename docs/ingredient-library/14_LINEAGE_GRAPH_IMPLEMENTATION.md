# 14 — LINEAGE GRAPH IMPLEMENTATION

**Status:** IMPLEMENTED — local validation pending  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Decision applied

Architectural option **B** from `13_ARCHITECTURAL_DECISION_INGREDIENT_LINEAGE.md` is accepted.

The master catalog uses a separate, non-clinical lineage graph for relationships between catalog ingredients. The lineage graph and food-safety graph remain independent.

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

Both child and parent reference `catalog_ingredients`. A unique index protects `(childIngredientId,parentIngredientId,relationType)`.

The migration also corrects a Phase-3 contract discrepancy in `custom_ingredient_safety_relations`: custom safety relations now require a `sourceId` just like shipped safety relations. Existing v2 rows, if present, are preserved through the deterministic migration source `LOCAL_USER_DECLARED_MIGRATED_V2`, and their former free-text `sourceDescription` is retained as `sourceDetails`.

No destructive migration or fallback is introduced.

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

The active content bundle remains immutable catalog v4/schema v2 during the Room-v3 infrastructure gate. The first catalog containing actual lineage edges must be a new catalog version.

## Import and repository support

The importer now persists lineage relations in the same catalog replacement transaction as categories, ingredients, aliases and safety data.

The catalog repository exposes explicit traversal in both directions:

```text
getParentRelations(ingredientId)
getChildRelations(ingredientId)
```

The domain model uses a dedicated `IngredientLineageRelation` / `IngredientLineageType`; food-safety types are not reused.

## Validation rules

The catalog validator rejects:

- unknown child or parent ingredient IDs;
- self-relations;
- unsupported lineage relation types;
- duplicate relation IDs;
- duplicate `(child,parent,type)` edges;
- invalid review dates;
- lineage cycles;
- lineage file declarations before catalog schema v3.

Food-safety validation remains independent and unchanged.

## Transitional editor compatibility fixed

The pre-library recipe editor still accepts free-text ingredient names. Before this change, a newly created free-text ingredient could reach persistence with both origin columns null, which conflicted with the intended origin invariant and could later break foreign-key-safe persistence.

The transitional compatibility path now works as follows:

```text
free-text recipe ingredient without origin
    -> dedicated custom origin recipe-custom:<ingredientId>
    -> CustomIngredientEntity with ingredientType RECIPE_FREE_TEXT_COMPAT
    -> custom master upserted before IngredientEntity insert
```

Existing `legacy:*`, real custom and catalog origins are preserved and are never rewritten by this compatibility mechanism. A dual origin (`catalogIngredientId` and `customIngredientId` both non-null) is rejected before persistence.

This keeps the old editor operational until the library-first/custom-ingredient UX replaces the transitional path in later phases.

## Automated evidence added

Tests now cover:

- schema-v3 lineage file parsing;
- lineage reference/type/self/duplicate/cycle validation;
- catalog v4 import under the lineage-capable importer with zero lineage edges;
- Room v1 -> v3 chained migration;
- Room v2 -> v3 preservation of existing custom safety source text;
- creation/update of free-text recipe ingredients with an explicit compatibility custom origin and clean foreign keys.

## Validation gate

Before catalog content starts using lineage, run and verify:

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

Expected schema history after the build:

```text
1.json
2.json
3.json
```

`3.json` must be reviewed and versioned only after the gate passes.
