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
| Pine nut | FAO/WHO Part 5 | 0 | 0 | targeted review |
| Fruit/vegetable culinary identities | safety mapping not inferred | 60 | 0 | Batch 01 identity coverage; REVIEW_REQUIRED |
| Herbs/spices culinary identities | safety mapping not inferred | 24 | 0 | Batch 01 identity coverage; REVIEW_REQUIRED |
| Additional cereals/legumes | safety mapping not inferred | 16 | 0 | Batch 01 identity coverage; REVIEW_REQUIRED |
| WHO/IUIS molecular allergens | nomenclature source | 0 | 0 | reference-only until mapped |
| Lactose intolerance | AESAN | 0 | 0 | condition design |
| Coeliac disease | AESAN/NIDDK + EU gluten rules | 0 | 0 | condition design |
| Non-coeliac sensitivity | public-health distinction | 0 | 0 | conservative design |
| Commercial products | label-specific only | 0 | 0 | design pending implementation |
| Compound ingredients | explicit unknown state | 0 | 0 | design ready |

Current active draft totals (`ingredient-catalog/v3`):

```text
127 canonical ingredients
151 aliases
14 EU Annex II regulatory groups
3 authoritative EU sources
27 reviewed safety relations
```

Composition of those 127 canonical entries:

```text
27 reviewed regulatory anchors
100 culinary identity entries marked REVIEW_REQUIRED
```

`ingredient-catalog/v1` remains the immutable infrastructure-only bundle. `ingredient-catalog/v2` remains the immutable validated regulatory seed. Catalog v3 adds culinary identity/search coverage without changing the 27 reviewed safety relations.

A zero in the relations column means no relation has yet been approved for that area. It does not mean absence of risk.

The 100 Batch 01 culinary ingredients deliberately receive no safety relation by inference. In particular, the presence of buckwheat, fruit, vegetables, herbs, spices, rice, maize, quinoa, chickpea or other legumes in the catalog is not used as evidence for or against an allergy/restriction.

Generic entries for crustaceans, fish and molluscs remain intentional: species expansion is deferred until identity/taxonomy and safety mapping can be reviewed explicitly rather than inferred from model knowledge.
