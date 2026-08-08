# 15 — ROOM V3 + LINEAGE VALIDATION GATE

**Status:** SCHEMA REVIEW APPROVED — exact generated artifact versioning pending  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Scope

This gate validates together:

- Batch 02 catalog v4;
- Room schema v3;
- non-destructive migration v2 -> v3;
- chained migration v1 -> v3;
- lineage graph storage/validation/traversal;
- preservation of custom safety relation source data;
- transitional free-text editor origin repair;
- no automatic safety propagation from lineage.

## Validation history

### First local execution

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      FAIL — 39 tests, 3 failures
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

The three failures were traced to two fixture/catalog defects:

1. v4 had a stale pantry alias shard, producing 249 aliases instead of the declared 220. The shard was replaced with the reviewed v3 version.
2. The synthetic schema-v3 lineage fixture omitted explicit `isActive=true`; Gson materialized those fields as false. The fixture was corrected.
3. The rollback failure was a consequence of defect 1.

No migration SQL, Room entity, lineage type or food-safety rule changed.

### Second local execution

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      FAIL — UI test/process crash after 27/39
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

The only reported test failure was `RecipeCatalogUiTest.favoritesCategoryAndClearFiltersWork`. The test was hardened to wait for the unfiltered result count and scroll the second recipe card into view before asserting visibility. No production behavior changed.

The subsequent single-test execution completed successfully:

```text
RecipeCatalogUiTest.favoritesCategoryAndClearFiltersWork  PASS — 1/1
```

The prior long-running attempt was attributable to an AVD/instrumentation state issue. After restarting ADB, the targeted test completed normally.

### Final instrumented rerun

```text
connectedDebugAndroidTest      PASS — 39/39
skipped                        0
failed                         0
execution time                 50s
```

The complete local gate is therefore green:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS — 39/39
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

## Functional invariants validated

1. v1 legacy recipe data survives the chained migration to v3.
2. v2 custom safety relations preserve their old free-text source as `sourceDetails` and gain a required `sourceId`.
3. `catalog_ingredient_relations` exists with the intended foreign keys and indexes.
4. a schema-v3 test catalog persists and traverses a `CUT_OF` edge.
5. lineage creates zero safety relations by itself.
6. catalog v4 imports with 227 ingredients, 220 aliases and 27 reviewed safety relations.
7. old free-text recipe editing persists a dedicated `recipe-custom:<ingredientId>` origin without foreign-key failure.
8. a dual recipe ingredient origin is rejected before persistence.

## Generated schema review

The locally generated `3.json` has now been reviewed against:

```text
RecipeDatabase.kt
IngredientLibraryEntity.kt
IngredientLibraryMigrations.kt
```

Review result: **APPROVED**.

Confirmed in the generated artifact:

- database version 3;
- 14 Room entities;
- `catalog_ingredient_relations` with child/parent FKs and unique `(child,parent,type)` index;
- corrected `custom_ingredient_safety_relations` with required `sourceId`, preserved `sourceDetails`, three FKs and indexes;
- recipe ingredient catalog/custom origin FKs preserved;
- safety relation provenance fields preserved;
- no unexpected table deletion.

Detailed review: `docs/ingredient-library/16_ROOM_V3_SCHEMA_REVIEW.md`.

## Remaining operational artifact step

The exact generated file remains local and untracked:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/3.json
```

It is approved for version control. Because this artifact was generated on the local Android build environment, the exact local file must be committed/pushed unchanged before the gate is formally closed.

## After artifact versioning

1. close the Room-v3 infrastructure gate;
2. update `SPRINT_PLAN.md` and implementation status;
3. create a new immutable catalog version for the first real lineage-backed derivatives/cuts;
4. continue autonomously until the next architectural boundary.
