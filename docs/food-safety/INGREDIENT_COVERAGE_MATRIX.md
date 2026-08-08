# INGREDIENT COVERAGE MATRIX

This matrix separates culinary catalog coverage from reviewed food-safety relations.

| Area | Official basis reviewed | Canonical catalog entries | Relations approved | Status |
|---|---:|---:|---:|---|
| Cereals containing gluten | YES | 6 | 6 | controlled regulatory seed |
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
| Additional cereals/legumes | safety mapping not inferred | 16 | 0 | Batch 01 identity coverage; REVIEW_REQUIRED |
| WHO/IUIS molecular allergens | nomenclature source | 0 | 0 | reference-only until mapped |
| Lactose intolerance | AESAN | 0 | 0 | condition design |
| Coeliac disease | AESAN/NIDDK + EU gluten rules | 0 | 0 | condition design |
| Non-coeliac sensitivity | public-health distinction | 0 | 0 | conservative design |
| Commercial products | label-specific only | 0 | 0 | design pending implementation |
| Compound ingredients | explicit unknown state | 0 | 0 | design ready |

Current active draft totals (`ingredient-catalog/v4`):

```text
227 canonical ingredients
220 aliases
14 EU Annex II regulatory groups
3 authoritative EU sources
27 reviewed safety relations
```

Composition of those 227 canonical entries:

```text
27 reviewed regulatory anchors
100 Batch 01 culinary identity entries marked REVIEW_REQUIRED
100 Batch 02 culinary identity entries marked REVIEW_REQUIRED
```

Alias composition:

```text
22 aliases inherited from the regulatory v2 seed
100 new identity-conservative aliases in Batch 01
98 new identity-conservative aliases in Batch 02
```

`ingredient-catalog/v1`, `v2` and `v3` remain immutable historical bundles. Catalog v4 expands culinary identity/search coverage without changing the 27 reviewed safety relations.

A zero in the relations column means no relation has yet been approved for that area. It does not mean absence of risk.

Batch 02 deliberately adds source-level meat/poultry, seeds, mushrooms and additional produce without inferring food-safety relations. In particular, the presence of pine nut or any seed in the catalog is not treated as evidence for or against a clinical restriction until a relation is reviewed and sourced.

The next high-value expansion area includes cuts and derived ingredients. That work is paused at the architectural boundary documented in `docs/ingredient-library/13_ARCHITECTURAL_DECISION_INGREDIENT_LINEAGE.md`.
