# 10 — CONTROLLED CATALOG SEED

**Status:** IMPLEMENTED — local validation pending  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Purpose

Start catalog population with a deliberately small, high-confidence batch whose food-safety relations are directly traceable to current EU legal sources.

This is not the 600-ingredient production catalog. The manifest remains `DRAFT`.

## Seed contents

```text
categories              20
ingredients              27
aliases                  22
EU safety groups         14
safety sources            3
safety relations         27
```

The 27 canonical entries are regulatory anchor ingredients/categories taken directly from Annex II terminology:

- wheat, spelt, khorasan wheat, rye, barley and oats;
- crustaceans;
- egg;
- fish;
- peanut;
- soybean;
- milk;
- almond, hazelnut, walnut, cashew, pecan, Brazil nut, pistachio and macadamia;
- celery;
- mustard;
- sesame;
- sulphur dioxide and sulphites;
- lupin;
- molluscs.

Spanish canonical names are used in the asset bundle.

## Authoritative sources embedded in the catalog

1. `EU_FIC_1169_2011` — Regulation (EU) No 1169/2011, consolidated text applicable from 2025-04-01, Annex II.
2. `EU_ALLERGEN_NOTICE_2017` — Commission Notice 2017/C 428/01, including interpretative guidance on the exhaustive cereal/nut lists and the meaning of egg/milk.
3. `EU_MUSTARD_2024_2512` — Commission Delegated Regulation (EU) 2024/2512, current mustard/behenic-acid exemption.

## Safety mapping policy used

Each relation has:

```text
sourceId
evidenceLevel
relationType
reviewedAt
```

For direct Annex II source ingredients, the relation is `INHERENT_SOURCE` with `EU_LEGAL` evidence.

Sulphur dioxide/sulphites use `REGULATED_COMPONENT`, not an unconditional inherent-source rule, because Annex II regulation depends on the total SO2 concentration exceeding 10 mg/kg or 10 mg/l in the ready-to-consume or reconstituted food.

Mustard points to the current 2024/2512 source and explicitly records that the behenic-acid exemption is narrow: minimum 85% purity, two distillation steps and use in E470a/E471/E477. It is not generalized to mustard as a culinary ingredient.

## Clinical taxonomy guardrail

The 14 Annex II groups are regulatory groups, not a complete clinical taxonomy.

Where an Annex II group spans more than one clinical mechanism, the seed avoids pretending that the regulatory label itself is a diagnosis. In particular:

- cereals containing gluten are not reduced to a generic `gluten allergy` concept;
- the milk group does not collapse milk-protein allergy and lactose intolerance;
- sulphites are not forced into a single allergy diagnosis.

Separate condition-specific modeling remains required for later celiac, lactose-intolerance and other clinical flows.

## Deliberate non-expansion

The seed does **not** expand broad legal categories into species through model knowledge or taxonomy inference. Therefore it currently contains generic `Crustáceos`, `Pescado` and `Moluscos`, rather than automatically generating shrimp, crab, salmon, mussel, etc.

Likewise it does not automatically generate derivatives such as flour, bread, cheese, butter, soy sauce or tahini. Those require explicit identity/composition rules and source-backed relation review.

This is intentional and follows `DATA_QUALITY_POLICY.md`.

## Aliases

The initial 22 aliases are conservative search variants: singular/plural forms, accent-insensitive-compatible forms, `soya`, `SO2`, `trigo espelta`, `Khorasan`, and directly equivalent common forms such as `nuez de macadamia`.

Aliases improve search only; they do not create or infer food-safety relations.

## Validation gate

Before this seed is accepted as the baseline for further population, run:

```powershell
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" clean assembleDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
.\gradlew -g "C:\Temp\gradle_home_ingredient_library" assembleRelease
```

The instrumented catalog test now asserts the exact seed counts, the complete 14-code Annex II set, the sulphite threshold relation, import idempotency and invalid-bundle rollback.

## Next content step

After the seed passes locally, expand the culinary catalog in reviewed batches. Priority should be common raw ingredients with no inferred safety relations, followed by source-backed derivatives/compound ingredients. Mass unreviewed allergen mapping remains forbidden.
