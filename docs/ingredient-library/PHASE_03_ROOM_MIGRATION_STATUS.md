# PHASE 03 — ROOM MIGRATION STATUS

**Status:** CLOSED — Room v2 migration gate passed  
**Branch:** `program/ingredient-library-food-safety`  
**Closed:** 2026-08-08

## Implemented

- Room database version raised from 1 to 2.
- New master-data entities for categories, catalog ingredients, aliases, safety groups, sources, safety relations, custom ingredients and catalog metadata.
- `ingredients` now supports nullable `catalogIngredientId` and `customIngredientId` references while preserving the legacy `name` snapshot.
- Explicit non-destructive `MIGRATION_1_2` registered in `RecipeDatabase`.
- Every v1 ingredient is migrated conservatively to a deterministic custom origin `legacy:<ingredientId>`.
- No fuzzy or automatic catalog matching is performed by the migration.
- Mapper compatibility protects migrated origins when an existing recipe is edited through the pre-library editor.
- Room v2 schema exported and versioned at `app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/2.json`.
- Instrumented migration test opens a real schema-v1 SQLite database through Room v2 and validates migrated data and foreign keys.

## Validation evidence

```text
clean assembleDebug                 PASS
testDebugUnitTest                   PASS
lintDebug                           PASS
compileDebugAndroidTestKotlin       PASS
connectedDebugAndroidTest           PASS — 34/34
skipped                             0
failed                              0
assembleRelease                     PASS — user-confirmed in closure sequence
Room v2 schema versioned            PASS
```

The first migration-test implementation using `MigrationTestHelper` was replaced after a binary incompatibility in Room test serialization produced an `AbstractMethodError`. The final test does not bypass Room migration validation: it creates a physical v1 SQLite database and reopens that same database through `RecipeDatabase` v2 with `MIGRATION_1_2` registered.

## Schema review

The versioned `2.json` declares Room database version 2 and the expected tables:

```text
recipes
ingredients
recipe_steps
ingredient_categories
catalog_ingredients
ingredient_aliases
food_safety_groups
safety_sources
ingredient_safety_relations
custom_ingredients
custom_ingredient_aliases
custom_ingredient_safety_relations
catalog_metadata
```

The legacy `recipes` and `recipe_steps` structures remain unchanged. `ingredients` preserves all legacy columns and adds indexed nullable foreign keys to catalog/custom origins.

## Migration invariants verified

- legacy recipe/ingredient/step data preserved;
- quantity, unit, notes and ordering preserved;
- photo paths preserved;
- favorites and timestamps preserved;
- every migrated legacy ingredient receives `legacy:<ingredientId>` custom origin;
- no catalog origin is invented;
- `PRAGMA foreign_key_check` returns no rows;
- no destructive migration fallback exists.

## Transitional condition

The database columns `catalogIngredientId` and `customIngredientId` intentionally remain nullable in Room v2. The migration guarantees an origin for migrated v1 rows, but the pre-library editor can still create a new recipe ingredient without either origin until the catalog/custom-ingredient runtime flow is implemented.

Therefore the final logical invariant:

```text
exactly one origin = catalogIngredientId XOR customIngredientId
```

must be enforced by the application before this priority program is merged to `master`. This does not reopen the v1 -> v2 migration gate; it is a tracked integration requirement for the following phases.

## Intentionally not implemented in Phase 3

- Catalog JSON/assets.
- Catalog importer.
- Catalog DAOs/repositories.
- Ingredient library UI.
- Safety aggregation UI.
- Automatic legacy relinking to catalog.

## Gate decision

```text
PHASE 3 — ROOM v1 -> v2             CLOSED ✅
PHASE 4 — CATALOG INFRASTRUCTURE     AUTHORIZED TO START
MASS CATALOG POPULATION              STILL BLOCKED BY ITS OWN GATE
MERGE TO master                      NOT AUTHORIZED YET
```
