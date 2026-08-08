# 16 — ROOM V3 GENERATED SCHEMA REVIEW

**Status:** APPROVED — local artifact versioning pending  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Reviewed artifact

Generated Room schema:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/3.json
```

Observed metadata:

```text
formatVersion  1
databaseVersion 3
identityHash 3208d5d131ab4f673316fec25ec61986
entityCount 14
```

## Review result

The generated schema is consistent with the Room v3 entity model and the explicit v2 -> v3 migration currently implemented in the branch.

No destructive fallback is present and no unexpected table removal was observed.

## `catalog_ingredient_relations`

The generated table matches the accepted lineage design:

```text
id                  TEXT NOT NULL PRIMARY KEY
childIngredientId   TEXT NOT NULL
parentIngredientId  TEXT NOT NULL
relationType        TEXT NOT NULL
reviewedAt          TEXT NOT NULL
sourceReference     TEXT nullable
notes               TEXT nullable
isActive            INTEGER NOT NULL
```

Foreign keys:

```text
childIngredientId  -> catalog_ingredients.id  ON DELETE NO ACTION
parentIngredientId -> catalog_ingredients.id  ON DELETE NO ACTION
```

Indexes:

```text
childIngredientId
parentIngredientId
UNIQUE(childIngredientId,parentIngredientId,relationType)
```

This matches `CatalogIngredientRelationEntity` and `IngredientLibraryMigrations.createCatalogIngredientRelations()`.

## `custom_ingredient_safety_relations`

The generated Room v3 table contains the corrected provenance contract:

```text
id                  TEXT NOT NULL PRIMARY KEY
customIngredientId  TEXT NOT NULL
safetyGroupId       TEXT NOT NULL
relationType        TEXT NOT NULL
evidenceLevel       TEXT NOT NULL
sourceId            TEXT NOT NULL
sourceDetails       TEXT nullable
notes               TEXT nullable
reviewedAt          TEXT NOT NULL
```

Foreign keys:

```text
customIngredientId -> custom_ingredients.id    ON DELETE CASCADE
safetyGroupId      -> food_safety_groups.id    ON DELETE NO ACTION
sourceId           -> safety_sources.id         ON DELETE NO ACTION
```

Indexes exist for all three FK columns.

This matches `CustomIngredientSafetyRelationEntity` and the v2 -> v3 migration that preserves legacy `sourceDescription` as `sourceDetails` while assigning the deterministic migration source `LOCAL_USER_DECLARED_MIGRATED_V2`.

## Recipe ingredient origins

The `ingredients` table retains both nullable origin columns:

```text
catalogIngredientId
customIngredientId
```

with FKs to their respective master tables and indexes on both fields.

The XOR invariant remains an application/domain invariant rather than a SQLite CHECK constraint, as already designed and covered by persistence validation tests.

## Safety provenance

`ingredient_safety_relations` continues to require:

```text
sourceId
evidenceLevel
relationType
reviewedAt
```

and `sourceId` remains a foreign key to `safety_sources`.

The generated schema therefore preserves the evidence-first safety model.

## Existing catalog structures

The review confirms continued presence of:

```text
ingredient_categories
catalog_ingredients
ingredient_aliases
food_safety_groups
safety_sources
ingredient_safety_relations
custom_ingredients
custom_ingredient_aliases
catalog_metadata
```

alongside recipe tables and the new lineage table.

## Conclusion

The generated `3.json` is approved for version control.

The only remaining operational action for the Room-v3 gate is to add the exact locally generated artifact to Git and push it to the program branch. Once the exact generated file is versioned, Room v3 can be formally closed and catalog v5 with real lineage edges may begin.
