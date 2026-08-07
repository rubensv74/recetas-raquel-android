# 11 — CULINARY CATALOG BATCH 01

**Status:** IMPLEMENTED — local validation pending  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Purpose

Expand the ingredient library from the regulatory seed into a materially useful culinary catalog without inventing food-safety relationships.

This batch separates two different claims:

1. **culinary identity** — the ingredient exists as a canonical library/search item;
2. **food-safety evidence** — a safety relation is only present when separately sourced and reviewed.

An ingredient may therefore exist in the catalog with no safety relation. That absence must never be interpreted as absence of risk.

## Versioning

A new immutable catalog version is created:

```text
catalog schemaVersion   2
catalogVersion          3
releaseStatus           DRAFT
Room schema             remains v2
```

`ingredient-catalog/v1/` and `v2/` remain unchanged historical versions.

## Sharded catalog format

Catalog schema v2 adds backward-compatible sharding for ingredient and alias lists. The reader still supports schema-v1 manifests with a single `ingredients.json` / `aliases.json`, while v3 can declare ordered `ingredientShards` and `aliasShards`.

This avoids turning the future 600+ ingredient catalog into one monolithic JSON file and permits review by controlled culinary area.

Current v3 ingredient shards:

```text
ingredients-regulatory.json
ingredients-vegetables.json
ingredients-fruits.json
ingredients-herbs-spices.json
ingredients-cereals-legumes.json
```

Alias shards:

```text
aliases-regulatory.json
aliases-produce.json
aliases-pantry.json
```

## Catalog totals after Batch 01

```text
categories              20
canonical ingredients   127
aliases                 151
EU safety groups         14
safety sources            3
safety relations         27
```

The 27 previously validated regulatory anchor ingredients and their 27 safety relations are preserved unchanged.

Batch 01 adds **100 culinary ingredients** and **129 search aliases**.

## New culinary coverage

```text
vegetables / produce     30
fruits                    30
herbs                     12
spices                    12
cereals / grains           8
legumes                    8
TOTAL                    100
```

Examples include tomato, onion, garlic, potato, courgette, peppers, spinach, broccoli, artichoke, apple, citrus, berries, peach, melon, pineapple, avocado, parsley, basil, oregano, rosemary, black pepper, paprika, cumin, turmeric, rice, maize, quinoa, buckwheat, chickpea, lentil and several bean forms.

## Search aliases

Aliases include conservative plural, presentation and regional search forms such as:

- `papa` / `papas` → `Patata`;
- `batata` / `camote` → `Boniato`;
- `zucchini` → `Calabacín`;
- `morrón rojo/verde/amarillo` → the corresponding pepper entry;
- `ejote` / `habichuela verde` → `Judía verde`;
- `banana` → `Plátano`;
- `toronja` → `Pomelo`;
- `frutilla` → `Fresa`;
- `durazno` → `Melocotón`;
- `ananá` → `Piña`;
- `palta` → `Aguacate`;
- `alforfón` → `Trigo sarraceno`;
- regional `frijol ...` forms → the corresponding alubia entry.

Aliases are search/navigation aids only. They do not create food-safety relations.

## Safety guardrail

All 100 new culinary records are marked:

```text
verificationStatus = REVIEW_REQUIRED
```

No new safety relation is created for them in this batch.

This is deliberate. For example, `Trigo sarraceno` is now available as a culinary ingredient, but the catalog does not infer a relation merely from external priority-allergen discussions. Any future relation must be separately reviewed and carry `sourceId`, `evidenceLevel`, `relationType` and `reviewedAt`.

Likewise, no fish/crustacean/mollusc species expansion is performed here. Those regulatory families remain represented only by the already reviewed generic anchors until species-level mappings are explicitly evidenced.

## Automated guardrails added

The active catalog import test now verifies:

- schemaVersion 2;
- catalogVersion 3;
- exact v3 counts;
- all 14 EU Annex II regulatory group codes still present;
- the sulphite threshold relation remains unchanged;
- `Tomate` is `REVIEW_REQUIRED` and has no invented safety relation;
- `Trigo sarraceno` is `REVIEW_REQUIRED` and has no invented safety relation;
- first import succeeds;
- second import is idempotent;
- an invalid catalog version 4 does not partially replace v3.

Unit tests also cover both legacy single-file catalogs and schema-v2 sharded catalogs.

## Local validation gate

Run:

```powershell
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" clean assembleDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" assembleRelease

Get-ChildItem ".\app\schemas\com.rmm.recetasraquel.data.local.RecipeDatabase"
git status -sb
```

Expected invariants:

```text
connectedDebugAndroidTest     36/36 PASS
Room schemas                  1.json + 2.json only
working tree                  CLEAN
```

## Next batch after validation

The next reviewed population batch should expand other single-ingredient culinary families: seeds not covered by Annex II, meats/poultry, culinary oils/fats and additional produce/grains.

Fish and seafood species, dairy derivatives, flours/bakery ingredients, sauces, commercial products and compound ingredients require a more explicit safety/composition review before broad expansion.
