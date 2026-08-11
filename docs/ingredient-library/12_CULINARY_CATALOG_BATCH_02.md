# 12 — CULINARY CATALOG BATCH 02

**Status:** IMPLEMENTED — local validation pending  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Purpose

Continue expanding the master ingredient library with single-source culinary identities that do not require composition or derivation modeling.

The same safety rule remains in force:

> Catalog presence is an identity/search statement. It is not a food-safety statement.

No safety relation is created unless it has been separately reviewed and traced to evidence.

## Versioning

A new immutable catalog version is created:

```text
catalog schemaVersion   2
catalogVersion          4
releaseStatus           DRAFT
Room schema             remains v2
```

`ingredient-catalog/v1/`, `v2/` and `v3/` remain unchanged historical versions.

## Catalog totals after Batch 02

```text
categories              20
canonical ingredients   227
aliases                 220
EU safety groups         14
safety sources            3
safety relations         27
```

Batch 02 adds **100 canonical culinary identities** and **98 conservative search aliases** while preserving the 27 previously reviewed safety relations unchanged.

## New coverage

The new canonical identities are grouped as follows:

```text
meat source identities             12
poultry source identities           8
seeds / extra spices               20
mushrooms                          15
additional vegetables/tubers       25
additional fruits                  20
TOTAL                             100
```

Examples include:

- source-level meats: vacuno, cerdo, cordero, cabra, conejo, venado, jabalí, caballo, bisonte, búfalo, liebre and corzo;
- poultry: pollo, pavo, pato, ganso, codorniz, pintada, faisán and perdiz;
- seeds/spices: sunflower, pumpkin, flax, chia, poppy, hemp, pine nut, fennel seed, fenugreek, nigella, annatto, cardamom and several whole spices;
- mushrooms: champiñón, portobello, shiitake, oyster mushroom, king oyster mushroom, boletus, níscalo, rebozuelo, colmenilla, enoki, shimeji, maitake and truffles;
- additional produce: kale, pak choi, endive, arugula, parsnip, celeriac, cassava, yam, malanga, okra, chayote, jícama and others;
- additional fruits: persimmon, passion fruit, guava, lychee, star fruit, dragon fruit, loquat, currants, cranberry, tamarind, custard apple, rambutan, mangosteen, jackfruit and others.

## Alias policy

The new aliases remain identity-conservative. Typical accepted forms are plural variants, established regional names and generic expressions such as `Carne de pollo` → `Pollo`.

During preparation, aliases that would collapse a meaningful presentation or subtype into a broader ingredient were deliberately avoided. For example, this batch does not make `pechuga de pollo` an alias of `Pollo`, nor `ternera` an alias of `Vacuno`.

This distinction becomes important for the architectural decision described in `13_ARCHITECTURAL_DECISION_INGREDIENT_LINEAGE.md`.

## Safety guardrail

All 100 new ingredients are stored as:

```text
verificationStatus = REVIEW_REQUIRED
```

No new safety relation is added. The totals remain:

```text
reviewed safety relations = 27
```

In particular, no relation is inferred for pine nut, seeds, mushrooms, meats, poultry, fruits or vegetables merely because an external clinical taxonomy may discuss them.

## Automated gate changes

The active catalog reader now points to `ingredient-catalog/v4`.

The instrumented catalog test expects:

```text
schemaVersion            2
catalogVersion           4
canonical ingredients  227
aliases                 220
safety relations         27
```

It also verifies that representative Batch 02 ingredients such as `Pollo`, `Piñón` and `Champiñón` remain `REVIEW_REQUIRED` and have no invented safety relation. Invalid catalog version 5 must not partially replace v4.

## Local validation gate

Run before Batch 02 is declared validated:

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

## Boundary reached

Further high-value catalog expansion now begins to require relationships between ingredients rather than additional flat identities. Examples include animal cuts, flours, oils, dairy derivatives, tofu, tahini and other derived forms.

That is an architectural boundary and is intentionally not crossed in this batch without an explicit decision.
