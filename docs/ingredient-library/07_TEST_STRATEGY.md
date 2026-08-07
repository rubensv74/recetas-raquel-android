# 07 — TEST STRATEGY

## Baseline

Preserve all existing tests. Record real counts from generated reports; never reuse old numbers blindly.

## Unit

Cover normalization, accent-insensitive search, aliases, deterministic ranking, category filter, import/idempotency/version update/corruption, aggregation by relation type, custom ingredients, unknown composition, user-declared evidence and non-absolute messages.

## Room

Use exported v1 schema to test migration. Verify exact preservation of recipe/ingredient/step/photo data, FK integrity, one origin per recipe ingredient, transactional catalog import, rollback, idempotency and search queries.

## Compose

Cover library opening, autofocus search, category navigation, no results, selection/cancel, ingredient detail, custom creation, multi-relation safety entry, unknown composition, recipe alert, aggregated panel, cross-reactivity separation, restoration and accessibility semantics.

## Catalog validator

Must load the complete shipped bundle. Fail on missing EU groups, duplicate IDs, normalized-name conflicts, missing category/source, orphan alias/relation, invalid `EU_LEGAL`, `UNVERIFIED` misuse and manifest-count mismatch.

Report target:

```text
build/reports/ingredient-catalog-validation/
```

## Regression commands

```powershell
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" assembleDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" assembleRelease
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
```

Report each as PASS / FAIL / NOT RUN.
