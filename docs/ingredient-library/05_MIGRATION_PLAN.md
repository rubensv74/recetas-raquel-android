# 05 — MIGRATION PLAN

## Current state

Room v1: `recipes`, `ingredients`, `recipe_steps`.

Target for the first schema evolution: **Room v2**.

No destructive fallback.

## Preservation target

100% preservation of recipes, ingredients, quantities, units, notes, ordering, steps, timer values, photo paths, favorites and timestamps.

## Frozen legacy decision for v1 -> v2

The migration will **not attempt any automatic catalog matching**.

Every legacy recipe ingredient receives a deterministic custom origin:

```text
customIngredientId = "legacy:" + ingredientId
catalogIngredientId = null
```

The new custom master row preserves the original ingredient name and suggested unit. It is marked internally as:

```text
ingredientType = LEGACY_UNCLASSIFIED
compositionKnown = false
```

This is intentionally conservative. A later explicit/reviewed process may link a legacy custom ingredient to a catalog ingredient, but the schema migration itself will never infer that identity.

Forbidden during migration:

- fuzzy matching;
- dropping qualifiers;
- partial-string inference;
- botanical/family inference;
- model knowledge;
- silent relinking.

## Implemented sequence

1. Create new category/catalog/alias/safety/source/custom/catalog-metadata tables.
2. Create one deterministic custom origin for each existing recipe ingredient.
3. Rebuild `ingredients` with nullable `catalogIngredientId` and `customIngredientId` foreign keys.
4. Copy every v1 row byte-for-byte for recipe-use fields and set its custom origin.
5. Recreate indexes.
6. Validate foreign keys.
7. Import the future catalog independently after schema migration.

The recipe ingredient keeps its existing `name` as a snapshot/fallback even after origin references exist.

## Compatibility safeguard

The domain/persistence mapper carries `catalogIngredientId` and `customIngredientId` so editing an already migrated recipe does not accidentally erase the legacy custom origin.

When the current pre-library editor sends no origin information, the mapper preserves the existing pair. When a future library flow explicitly supplies an origin, that explicit pair replaces the prior origin.

## Room vs catalog

Room migration creates schema. It does not embed hundreds of ingredient records.

Catalog assets will be imported by a later versioned transactional importer.

## Required migration tests

The v1 -> v2 migration suite must verify at minimum:

- recipe metadata;
- favorites;
- cover photo paths;
- ingredient count;
- non-numeric quantities;
- units;
- accented names;
- notes;
- ordering;
- step timers;
- step photo paths;
- deterministic custom origins;
- zero invalid foreign keys;
- exactly one origin for migrated legacy ingredients.

Additional fixtures will cover nulls, compound names such as `nata cocina`, multiple recipes using the same text and inactive master references before the migration gate closes.

## Report

Implementation must create `08_LEGACY_INGREDIENT_MIGRATION_REPORT.md` with total, exact matches, ambiguous, custom, discarded and lost.

For this strategy:

```text
exact automatic catalog matches = 0
legacy rows migrated to custom = total legacy rows
lost = 0
```

The actual counts must come from migration validation data, not assumptions.
