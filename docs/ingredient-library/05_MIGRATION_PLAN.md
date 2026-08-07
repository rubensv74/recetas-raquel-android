# 05 — MIGRATION PLAN

## Current state

Room v1: `recipes`, `ingredients`, `recipe_steps`.

No destructive fallback.

## Preservation target

100% preservation of recipes, ingredients, quantities, units, notes, ordering, steps, timer values, photo paths, favorites and timestamps.

## Conservative strategy

Legacy ingredient linking:

- deterministic exact approved match -> catalog link may be allowed;
- ambiguous -> custom ingredient;
- no match -> custom ingredient.

Forbidden: fuzzy matching, dropping qualifiers, partial-string inference or model knowledge.

## Recommended safest sequence

1. Create new master/custom/safety tables.
2. Add nullable origin columns to `ingredients`.
3. Create custom master origins for legacy ingredient text.
4. Set exactly one custom origin for every legacy recipe ingredient.
5. Verify every row has one origin and no data changed/lost.
6. Import catalog independently after schema migration.
7. Do not silently relink legacy custom ingredients to catalog later; any relinking must be explicit/reviewed.

This deliberately favors correctness over automatic matching.

## Room vs catalog

Room migration creates schema. It must not embed hundreds of ingredient records. Catalog assets are imported by a versioned transactional importer.

## Required migration tests

Cover nulls, non-numeric quantities, accents, ambiguous names, compound names such as `nata cocina`, photos, reordering, multiple recipes using same text and inactive master references.

## Report

Implementation must create `08_LEGACY_INGREDIENT_MIGRATION_REPORT.md` with total, exact matches, ambiguous, custom, discarded and lost. Required `lost = 0`.
