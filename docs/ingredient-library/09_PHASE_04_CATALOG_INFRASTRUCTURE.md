# PHASE 04 — CATALOG INFRASTRUCTURE

**Status:** CLOSED — infrastructure validation gate passed  
**Branch:** `program/ingredient-library-food-safety`  
**Started:** 2026-08-08  
**Closed:** 2026-08-08

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

## Validation evidence

User-executed local validation on the branch completed with:

```text
clean assembleDebug                 PASS
testDebugUnitTest                   PASS
lintDebug                           PASS
compileDebugAndroidTestKotlin       PASS
connectedDebugAndroidTest           PASS — 36/36
skipped                             0
failed                              0
assembleRelease                     PASS
working tree                        CLEAN
```

The non-fatal `stripDebugDebugSymbols` / `stripReleaseDebugSymbols` warning for `libandroidx.graphics.path.so` did not block packaging.

## Gate criteria — result

- packaged v1 assets parse successfully: PASS;
- structural validator accepts the `INFRASTRUCTURE` bundle: PASS;
- production release gate rejects the intentionally incomplete bundle: PASS by automated coverage tests;
- first import persists metadata and 20 categories: PASS;
- second import is idempotent: PASS;
- invalid higher-version bundle leaves the previously imported catalog untouched: PASS;
- existing recipe/migration regressions remain green: PASS;
- Room schema remains v2; no v3 schema is introduced by this phase: PASS;
- working tree clean after validation: PASS.

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

`INFRASTRUCTURE` means only that the parser/importer/validator contract is ready. It must never be presented as catalog coverage.

## Gate decision

```text
PHASE 4 — CATALOG INFRASTRUCTURE     CLOSED ✅
CONTROLLED CATALOG POPULATION        AUTHORIZED TO START
MASS UNREVIEWED SAFETY MAPPING       NOT AUTHORIZED
MERGE TO master                      NOT AUTHORIZED YET
```

Controlled content population may now begin, but safety relations must continue to satisfy the evidence and traceability policy documented in `docs/food-safety/`.
