# 14 — LINEAGE GRAPH IMPLEMENTATION

**Status:** CLOSED — Room v3 validated and schema versioned  
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

Room evolves from v2 to v3 with an explicit non-destructive migration.

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

The migration also corrects the previous custom-safety provenance discrepancy: `custom_ingredient_safety_relations` now requires `sourceId`; legacy free-text `sourceDescription` is retained as `sourceDetails` and associated with deterministic source `LOCAL_USER_DECLARED_MIGRATED_V2`.

No destructive migration or fallback is introduced.

## Catalog schema evolution

Catalog schema v3 adds optional lineage files while remaining backward-compatible with earlier catalog schemas.

Supported declarations:

```text
ingredientRelations: "ingredient-relations.json"
```

or:

```text
ingredientRelationShards: [ ... ]
```

Catalogs v1-v4 remain immutable historical bundles. Catalog v5 is the first bundle containing real persisted lineage edges.

## Import and repository support

The importer persists lineage relations in the same transaction as categories, ingredients, aliases and safety data.

Traversal is explicit in both directions:

```text
getParentRelations(ingredientId)
getChildRelations(ingredientId)
```

The domain uses dedicated `IngredientLineageRelation` / `IngredientLineageType`; food-safety types are not reused.

## Validation rules

The validator rejects:

- unknown child or parent ingredient IDs;
- self-relations;
- unsupported lineage relation types;
- duplicate relation IDs;
- duplicate `(child,parent,type)` edges;
- invalid review dates;
- lineage cycles;
- lineage file declarations before catalog schema v3.

Food-safety validation remains independent.

## Transitional editor compatibility

The pre-library editor still accepts free-text ingredient names. A new free-text ingredient without explicit origin is normalized before persistence as:

```text
free-text recipe ingredient without origin
    -> dedicated custom origin recipe-custom:<ingredientId>
    -> CustomIngredientEntity with ingredientType RECIPE_FREE_TEXT_COMPAT
    -> custom master upserted before IngredientEntity insert
```

Existing `legacy:*`, real custom and catalog origins are preserved. A dual origin is rejected before persistence.

## Validation evidence

The completed Room-v3 gate produced:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS — 39/39, 0 skipped, 0 failed
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

The generated `3.json` was reviewed against entities and migration SQL and is now versioned in the program branch.

Validated invariants include:

- v1 -> v3 chained migration;
- v2 -> v3 direct migration;
- preservation of custom safety source details;
- lineage reference/type/cycle validation;
- persistence and traversal of `CUT_OF`;
- zero automatic safety propagation from lineage;
- free-text editor compatibility with explicit custom origin.

See `15_ROOM_V3_VALIDATION_GATE.md` and `16_ROOM_V3_SCHEMA_REVIEW.md`.

## Closure

Room v3 infrastructure is formally closed. Further culinary lineage content is introduced only through new immutable catalog versions; the Room schema does not need to change merely to add new edges.
