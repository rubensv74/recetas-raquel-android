# 17 — CATALOG V5: FIRST REAL LINEAGE CONTENT

**Status:** VALIDATED — local gate green  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Purpose

Catalog v5 is the first immutable catalog bundle that uses the lineage architecture accepted in ADR-025 and persisted by Room v3.

This batch deliberately starts with a narrow, low-ambiguity case: **physical meat/poultry cuts**. It does not yet introduce flours, oils, dairy derivatives, processed products or compound ingredients because those areas require additional safety/composition review.

## Version contract

```text
catalogVersion        5
catalog schemaVersion 3
releaseStatus         DRAFT
Room version          3
```

Catalog v1-v4 remain immutable.

## Counts

```text
categories            20
ingredients           242
aliases               235
ingredientRelations   15
safetyGroups          14
safetySources          3
safetyRelations       27
```

Delta from v4:

```text
+15 canonical cut identities
+15 conservative aliases
+15 CUT_OF lineage edges
+0 food-safety relations
```

## New culinary identities

The batch adds:

### Poultry

```text
Pechuga de pollo
Muslo de pollo
Ala de pollo
Pechuga de pavo
Muslo de pavo
Pechuga de pato
```

### Meat

```text
Solomillo de vacuno
Costilla de vacuno
Entrecot de vacuno
Lomo de cerdo
Costilla de cerdo
Pierna de cordero
Paletilla de cordero
Muslo de conejo
Pierna de cabra
```

Every new identity is `REVIEW_REQUIRED`.

## Lineage examples

```text
Pechuga de pollo  CUT_OF  Pollo
Muslo de pollo    CUT_OF  Pollo
Solomillo de vacuno CUT_OF Vacuno
Lomo de cerdo     CUT_OF  Cerdo
Pierna de cordero CUT_OF  Cordero
```

All edges include:

```text
reviewedAt       2026-08-08
sourceReference  CULINARY_IDENTITY_REVIEW_2026_08
isActive         true
```

The source reference is an internal culinary-identity review marker. It is **not** a clinical or regulatory evidence source.

## Safety isolation

No safety relation was generated for any new cut.

This is intentional and verifies the architectural rule:

```text
lineage != safety evidence
```

A `CUT_OF` relation does not inherit, synthesize or infer `CONTAINS`, `DERIVED_FROM`, `POSSIBLE_CROSS_REACTIVITY` or any other food-safety relation.

The catalog still contains exactly the 27 previously reviewed regulatory safety relations.

Absence of a relation is not evidence of absence of risk.

## Why derivatives are not included yet

Items such as:

```text
Harina de trigo
Aceite de cacahuete
Aceite de sésamo
Nata
Tofu
Tahini
```

are high-value future lineage candidates, but they also intersect with regulatory derivatives, processing, exemptions, composition variability or clinically relevant allergen information.

They must not be added merely because the culinary parent relation is obvious. Before activation, safety evidence and processing/exemption implications must be reviewed independently.

This is a content-review gate, not a new architecture decision.

## Runtime activation

`IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY` points to:

```text
ingredient-catalog/v5
```

The importer remains transactional and Room remains v3.

## Automated coverage

The instrumented import test verifies:

- schemaVersion 3 and catalogVersion 5;
- exact 242/235/15/27 counts;
- presence of all 14 EU Annex II groups;
- sulphite threshold relation remains unchanged;
- `Pechuga de pollo` is `REVIEW_REQUIRED`;
- `Pechuga de pollo CUT_OF Pollo` is present;
- the new cut has zero automatically generated safety relations;
- v5 import persists 15 active lineage edges;
- parent traversal works after persistence;
- idempotent second import;
- invalid v6 rollback leaves v5 intact.

## Local validation result

The complete local gate was executed on 2026-08-08 and is green:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS — 39/39, 0 skipped, 0 failed
assembleRelease                PASS
Room schema history            PASS — 1.json, 2.json, 3.json only
working tree                   clean
```

No Room v4 schema was generated, confirming that catalog v5 is a content evolution independent of the Room schema.

## Conclusion

Catalog v5 is validated as the first real lineage-enabled bundle.

The next controlled content step is a separately reviewed derivative/form batch. Priority candidates are flours and single-source oils, but each clinically or regulatorily relevant derivative must pass the food-safety evidence gate independently of its lineage relation.
