# PHASE 04 — CATALOG INFRASTRUCTURE

**Status:** IN PROGRESS — implementation written, local validation gate pending  
**Branch:** `program/ingredient-library-food-safety`  
**Started:** 2026-08-08

## Objective

Build the versioned, offline, validated catalog infrastructure before any mass ingredient population.

This phase deliberately separates **infrastructure readiness** from **content readiness**.

## Implemented

- Versioned asset root: `app/src/main/assets/ingredient-catalog/v1/`.
- `manifest.json` with catalog/schema version, locale, jurisdiction, review date, file map and declared counts.
- 20 structural culinary categories.
- Empty placeholders for canonical ingredients, aliases, safety groups, safety sources and safety relations.
- `releaseStatus = INFRASTRUCTURE` so an empty ingredient dataset cannot be mistaken for a production-ready catalog.
- Structured JSON parsing using Gson; no ad-hoc string parsing.
- Deterministic accent-insensitive normalization for search keys.
- Full structural validator covering:
  - manifest counts;
  - unique IDs/codes/normalized names;
  - category/ingredient/source/group references;
  - normalized-name/alias integrity;
  - supported relation types;
  - supported evidence levels;
  - supported condition types;
  - ISO review/publication dates;
  - production coverage gate.
- `PRODUCTION_CANDIDATE` gate requires at least:
  - 20 categories;
  - 600 canonical ingredients;
  - 1200 aliases;
  - the 14 EU Annex II group codes when jurisdiction includes EU.
- Transactional Room DAO for catalog import.
- Master rows that can already be referenced by user/recipe data are deactivated rather than destructively deleted.
- Version metadata stored independently from Room schema version.
- Importer behavior:
  1. read manifest/files;
  2. validate the complete bundle;
  3. compare imported catalog version;
  4. reject/no-op downgrade;
  5. import in one Room transaction;
  6. record metadata;
  7. no-op when already current.
- Separate `IngredientCatalogRepository`; `RecipeRepository` remains focused on recipes.
- Manual DI wired through `AppContainer`.
- Unit tests for validator, normalization and asset reader.
- Instrumented tests for real packaged asset import, idempotency and rollback-before-write when a new bundle fails validation.

## Intentionally not implemented yet

- Canonical ingredient population.
- Alias population.
- EU/extended safety group population.
- Ingredient-safety relation population.
- Ingredient library/search UI.
- Custom ingredient UI.
- Recipe-level safety aggregation UI.
- Automatic startup invocation of catalog import.

No clinical/safety relation has been invented to exercise the infrastructure.

## Important safety rule

The current v1 bundle contains structural categories but **zero canonical ingredients and zero safety relations**. This is intentional.

`INFRASTRUCTURE` means only that the parser/importer/validator contract is ready for testing. It must never be presented as catalog coverage.

## Local validation gate

Required commands:

```powershell
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" clean assembleDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" assembleRelease
```

## Gate criteria

Phase 4 cannot close until:

- all commands above are classified PASS/FAIL/NOT RUN;
- packaged v1 assets parse successfully;
- structural validator passes the infrastructure bundle;
- production release gate rejects the intentionally incomplete bundle;
- first import persists catalog metadata and 20 categories;
- second import is idempotent;
- invalid higher-version bundle leaves the previously imported catalog untouched;
- existing recipe/migration regressions remain green;
- Room schema remains v2 (no schema change expected from this phase);
- working tree is clean after any generated artifacts are reviewed.

## Next gate

Only after Phase 4 closes may controlled catalog population begin.

Mass generation remains blocked until each safety relation can satisfy the evidence/traceability policy already documented in `docs/food-safety/`.
