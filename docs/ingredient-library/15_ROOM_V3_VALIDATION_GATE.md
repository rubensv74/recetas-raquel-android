# 15 — ROOM V3 + LINEAGE VALIDATION GATE

**Status:** CORRECTIVE RERUN REQUIRED  
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

The first execution produced the following result:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      FAIL — 39 tests, 3 failures
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

The three failures were diagnosed as two concrete defects rather than a Room migration failure:

1. Catalog v4 contained 249 aliases while its immutable-version plan and manifest declared 220. The source was a stale `aliases-pantry.json` copied from an earlier, pre-review draft. It contained exactly 29 presentation aliases that had already been removed during the validated v3 review. Catalog v4 now reuses the validated v3 pantry alias shard, restoring the intended total of 220 aliases.
2. The synthetic schema-v3 lineage fixture omitted explicit `isActive` fields. Gson does not apply Kotlin constructor defaults when materializing these records reflectively, so the test ingredients/relation were imported inactive. The fixture now declares `isActive=true` explicitly, matching the shipped catalog contract.
3. The rollback test failed only because the initial real v4 import was blocked by the alias-count mismatch; it should recover automatically after correction 1.

No migration SQL, Room entity, lineage relation type or safety propagation rule was changed as a consequence of these failures.

The generated `3.json` remains intentionally untracked until the corrective rerun is completely green and the schema is reviewed.

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

After compilation Room should export:

```text
1.json
2.json
3.json
```

`3.json` is expected to remain untracked until its generated structure is reviewed against the migration and entity model.

## Required PASS conditions

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS
assembleRelease                PASS
```

No exact instrumented-test total is frozen in this document because new migration/origin/lineage tests have been added since the previous 36-test gate. The acceptance criterion is zero failed and zero unexpectedly skipped tests.

## Functional invariants exercised

1. v1 legacy recipe data survives the chained migration to v3.
2. v2 custom safety relations preserve their old free-text source as `sourceDetails` and gain a required `sourceId`.
3. `catalog_ingredient_relations` exists and has valid foreign keys/indexes.
4. a schema-v3 test catalog can persist and traverse a `CUT_OF` edge.
5. that lineage edge creates zero safety relations by itself.
6. catalog v4 still imports with 227 ingredients, 220 aliases and 27 reviewed safety relations.
7. old free-text recipe editing persists a dedicated `recipe-custom:<ingredientId>` origin without foreign-key failure.
8. a dual recipe ingredient origin is rejected before persistence.

## After a green gate

1. inspect and version generated `3.json`;
2. update this document with actual validation evidence;
3. close the Room-v3 infrastructure gate;
4. create a new immutable catalog version for the first real lineage-backed derivatives/cuts;
5. continue autonomously until the next architectural boundary.
