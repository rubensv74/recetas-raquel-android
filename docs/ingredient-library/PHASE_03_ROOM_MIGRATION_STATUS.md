# PHASE 03 — ROOM MIGRATION STATUS

**Status:** IN PROGRESS — implementation written, validation gate pending  
**Branch:** `program/ingredient-library-food-safety`

## Implemented

- Room database version raised from 1 to 2.
- New master-data entities for categories, catalog ingredients, aliases, safety groups, sources, safety relations, custom ingredients and catalog metadata.
- `ingredients` now supports nullable `catalogIngredientId` and `customIngredientId` references while preserving the legacy `name` snapshot.
- Explicit non-destructive `MIGRATION_1_2` registered in `RecipeDatabase`.
- Every v1 ingredient is migrated conservatively to a deterministic custom origin `legacy:<ingredientId>`.
- No fuzzy or automatic catalog matching is performed by the migration.
- Mapper compatibility protects migrated origins when an existing recipe is edited through the pre-library editor.
- Migration test created using Room `MigrationTestHelper`.
- Room schema directory exposed to Android instrumentation tests.

## Intentionally not implemented yet

- Catalog JSON/assets.
- Catalog importer.
- Catalog DAOs/repositories.
- Ingredient library UI.
- Safety aggregation UI.
- Automatic legacy relinking to catalog.

These belong to later phases after the Room v1 -> v2 gate passes.

## Pending validation gate

The implementation is not considered complete until the local checkout generates and versions `2.json` and the following commands pass:

```powershell
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" clean assembleDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
```

`connectedDebugAndroidTest` requires a connected/authorized emulator or device.

## Gate criteria

- `app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/2.json` generated and reviewed.
- Room migration schema validation passes.
- Legacy recipe/ingredient/step data preserved.
- Photo paths preserved.
- Favorites/timestamps preserved.
- Every migrated legacy ingredient has a custom origin.
- `PRAGMA foreign_key_check` returns no rows.
- Existing application regressions remain green.
- No destructive migration fallback exists.

## Merge policy

Do not merge this branch into `master` until the Phase 3 gate is fully green and manually reviewed.
