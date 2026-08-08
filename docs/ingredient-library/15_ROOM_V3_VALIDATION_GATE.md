# 15 — ROOM V3 + LINEAGE VALIDATION GATE

**Status:** CLOSED — complete gate green and schema versioned  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Scope

This gate jointly validated:

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

The failures were traced to a stale v4 alias shard and missing explicit `isActive=true` values in a synthetic Gson fixture. Both were corrected without changing the Room migration or lineage architecture.

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

The UI test was made independent of software-keyboard viewport state. The targeted test subsequently passed 1/1 after restarting the AVD/ADB instrumentation state.

### Final instrumented rerun

```text
connectedDebugAndroidTest      PASS — 39/39
skipped                        0
failed                         0
execution time                 50s
```

Complete gate:

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

1. v1 legacy recipe data survives chained migration to v3.
2. v2 custom safety relations preserve their old free-text source as `sourceDetails` and gain required `sourceId`.
3. `catalog_ingredient_relations` exists with intended foreign keys and indexes.
4. schema-v3 catalog data persists and traverses a `CUT_OF` edge.
5. lineage creates zero safety relations by itself.
6. catalog v4 imports with 227 ingredients, 220 aliases and 27 reviewed safety relations.
7. free-text recipe editing persists a dedicated `recipe-custom:<ingredientId>` origin without FK failure.
8. a dual recipe ingredient origin is rejected before persistence.

## Generated schema review

The generated artifact:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/3.json
```

was reviewed against entities and migration SQL. Review result: **APPROVED**.

Observed metadata:

```text
databaseVersion  3
identityHash      3208d5d131ab4f673316fec25ec61986
entityCount       14
```

The exact generated artifact is tracked in the program branch. See `16_ROOM_V3_SCHEMA_REVIEW.md`.

## Closure decision

```text
ROOM V3 MIGRATION              CLOSED ✅
SCHEMA 3.JSON                  REVIEWED + VERSIONED ✅
LINEAGE PERSISTENCE            VALIDATED ✅
AUTOMATIC SAFETY PROPAGATION   PROHIBITED / 0 ✅
CATALOG V5 LINEAGE CONTENT     AUTHORIZED ✅
```

Room v3 is no longer a blocker for catalog evolution. New lineage content must be introduced through new immutable catalog versions and their own validation gates.
