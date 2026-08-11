# INGREDIENT COVERAGE MATRIX

This matrix separates culinary catalog coverage from reviewed food-safety relations.

| Area | Official basis reviewed | Canonical catalog entries | Relations approved | Status |
|---|---:|---:|---:|---|
| Cereals containing gluten — source cereals | YES | 6 | 6 | controlled regulatory seed |
| Cereals containing gluten — reviewed flours | YES | 6 | 6 | catalog v6; explicit `DERIVED_FROM`; local gate pending |
| Crustaceans | YES | 1 | 1 | controlled generic seed |
| Eggs | YES | 1 | 1 | controlled generic seed |
| Fish | YES | 1 | 1 | controlled generic seed |
| Peanuts | YES | 1 | 1 | controlled regulatory seed |
| Soybeans | YES | 1 | 1 | controlled regulatory seed |
| Milk | YES | 1 | 1 | controlled generic seed; clinical split pending |
| EU nuts species | YES | 8 | 8 | controlled regulatory seed |
| Celery | YES | 1 | 1 | controlled regulatory seed |
| Mustard | YES + 2024/2512 | 1 | 1 | controlled seed with exemption note |
| Sesame | YES | 1 | 1 | controlled regulatory seed |
| Sulphites | YES + threshold | 2 | 2 | regulated-component seed; concentration context required |
| Lupin | YES | 1 | 1 | controlled regulatory seed |
| Molluscs | YES | 1 | 1 | controlled generic seed |
| Buckwheat | FAO/WHO Part 5 | 1 | 0 | culinary identity added; safety review still pending |
| Pine nut | FAO/WHO Part 5 | 1 | 0 | culinary identity added in Batch 02; safety review still pending |
| Fruit/vegetable culinary identities | safety mapping not inferred | 105 | 0 | Batch 01+02 identity coverage; REVIEW_REQUIRED |
| Mushrooms | safety mapping not inferred | 15 | 0 | Batch 02 identity coverage; REVIEW_REQUIRED |
| Herbs/spices culinary identities | safety mapping not inferred | 37 | 0 | Batch 01+02 identity coverage; REVIEW_REQUIRED |
| Additional seeds | safety mapping not inferred | 6 | 0 | Batch 02 identity coverage; REVIEW_REQUIRED |
| Meat/poultry source identities | safety mapping not inferred | 20 | 0 | Batch 02 source-level coverage; REVIEW_REQUIRED |
| Meat/poultry cut identities | lineage review only | 15 | 0 | catalog v5; 15 CUT_OF edges; REVIEW_REQUIRED |
| Additional cereals/legumes | safety mapping not inferred | 16 | 0 | Batch 01 identity coverage; REVIEW_REQUIRED |
| Non-Annex-II reviewed flour identities | identity + lineage reviewed | 4 | 0 | rice/corn/chickpea/buckwheat flours; no universal gluten relation |
| WHO/IUIS molecular allergens | nomenclature source | 0 | 0 | reference-only until mapped |
| Lactose intolerance | AESAN | 0 | 0 | condition design |
| Coeliac disease | AESAN/NIDDK + EU gluten rules | 0 | 0 | condition design |
| Non-coeliac sensitivity | public-health distinction | 0 | 0 | conservative design |
| Commercial products | label-specific only | 0 | 0 | design pending implementation |
| Compound ingredients | explicit unknown state | 0 | 0 | design ready |

Current implemented draft totals (`ingredient-catalog/v6`):

```text
252 canonical ingredients
245 aliases
25 culinary lineage relations
14 EU Annex II regulatory groups
3 authoritative EU runtime sources
33 reviewed safety relations
```

Composition of the 252 canonical entries:

```text
27 reviewed regulatory anchors
100 Batch 01 culinary identity entries marked REVIEW_REQUIRED
100 Batch 02 culinary identity entries marked REVIEW_REQUIRED
15 catalog v5 cut identities marked REVIEW_REQUIRED
10 catalog v6 flour identities marked REVIEW_REQUIRED
```

Lineage composition:

```text
15 CUT_OF relations
10 DERIVED_FROM relations
0 VARIANT_OF relations
0 FORM_OF relations
```

Food-safety delta in v6:

```text
+6 explicit DERIVED_FROM relations to sg-eu-cereals-gluten
  wheat flour
  spelt flour
  khorasan wheat flour
  rye flour
  barley flour
  oat flour

+0 universal gluten relations for
  rice flour
  corn flour
  chickpea flour
  buckwheat flour
```

Catalog v1-v5 remain immutable historical bundles. Catalog v6 is implemented but remains subject to its local execution gate before being marked validated.

A zero in the food-safety relations column means no relation has been approved for that area. It does not mean absence of risk. AESAN alert ES2026/431 concerning a specific chickpea-flour product is retained as evidence for this design principle; it is not generalized into a universal ingredient relation.

The v6 flour batch demonstrates that culinary lineage and safety evidence can coexist as separate records: a `DERIVED_FROM` lineage edge never manufactures the corresponding safety relation. The six regulated flour relations were added independently with `EU_LEGAL` evidence and `EU_FIC_1169_2011` provenance.
