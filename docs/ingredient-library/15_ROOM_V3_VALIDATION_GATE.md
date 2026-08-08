# 15 — ROOM V3 + LINEAGE VALIDATION GATE

**Status:** SECOND CORRECTIVE RERUN REQUIRED  
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

## First local execution — 2026-08-08

The first execution produced:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      FAIL — 39 tests, 3 failures
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

The three failures were diagnosed as two concrete fixture/catalog defects rather than a Room migration failure:

1. Catalog v4 contained 249 aliases while the manifest declared 220. A stale pantry shard contained 29 presentation aliases already removed during the validated v3 review. The v4 pantry shard was replaced with the reviewed version, restoring 220 aliases.
2. The synthetic schema-v3 lineage fixture omitted explicit `isActive` values. Gson materialized them as false instead of applying Kotlin constructor defaults. The fixture now declares `isActive=true` explicitly.
3. The rollback failure was a consequence of defect 1.

No migration SQL, Room entity, lineage type or food-safety propagation rule changed.

## Second local execution — 2026-08-08

The corrected catalog/lineage rerun produced:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      FAIL — UI test/process crash after 27/39
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

The previous three catalog/lineage failures did not recur before the run reached the UI suite. The only reported failing test was:

```text
RecipeCatalogUiTest.favoritesCategoryAndClearFiltersWork
```

The test clears a text query while the software keyboard may remain visible and then immediately requires the second recipe card to be displayed. On the current AVD this can reduce the `LazyColumn` viewport and make the assertion depend on keyboard/viewport state rather than on the filter behavior being tested. The test has therefore been stabilized without changing production UI behavior:

1. after clearing filters, wait for `2 resultados` to prove the unfiltered state is restored;
2. locate `recipe_tarta` by stable test tag;
3. `performScrollTo()` before asserting visibility.

This is test-harness hardening only; no application behavior, Room schema, catalog content, lineage model or safety rule changed.

Because the instrumentation process also reported a crash after the UI failure, a complete rerun is required rather than treating the remaining 12 tests as passed.

The generated `3.json` remains intentionally untracked until the complete instrumented gate is green and the schema is reviewed.

## Corrective rerun commands

Run from repository root:

```powershell
git pull --ff-only
git status -sb

.\gradlew -g "C:\Temp\gradle_home_ingredient_library" clean assembleDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" assembleRelease

Get-ChildItem ".\app\schemas\com.rmm.recetasraquel.data.local.RecipeDatabase"
git status -sb
```

## Expected schema result

```text
1.json
2.json
3.json
```

`3.json` must remain untracked until its generated structure is reviewed against the migration and entity model.

## Required PASS conditions

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS — all 39 tests, 0 failed, 0 unexpectedly skipped
assembleRelease                PASS
```

## Functional invariants exercised

1. v1 legacy recipe data survives the chained migration to v3.
2. v2 custom safety relations preserve their old free-text source as `sourceDetails` and gain a required `sourceId`.
3. `catalog_ingredient_relations` exists and has valid foreign keys/indexes.
4. a schema-v3 test catalog can persist and traverse a `CUT_OF` edge.
5. that lineage edge creates zero safety relations by itself.
6. catalog v4 imports with 227 ingredients, 220 aliases and 27 reviewed safety relations.
7. old free-text recipe editing persists a dedicated `recipe-custom:<ingredientId>` origin without foreign-key failure.
8. a dual recipe ingredient origin is rejected before persistence.

## After a green gate

1. inspect and version generated `3.json`;
2. update this document with actual validation evidence;
3. close the Room-v3 infrastructure gate;
4. create a new immutable catalog version for the first real lineage-backed derivatives/cuts;
5. continue autonomously until the next architectural boundary.
