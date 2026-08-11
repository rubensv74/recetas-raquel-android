# 02 — FUNCTIONAL DESIGN

## Objective

Replace the current manual-first ingredient entry with a library-first flow while preserving custom ingredient creation.

## Add ingredient flow

```text
Recipe editor
 -> Add ingredient
 -> Ingredient library
 -> search / category / frequent
 -> select catalog ingredient
 -> quantity + unit + notes
 -> add to recipe
```

Secondary path:

```text
Library -> Create new ingredient
```

## Library

When query is empty: frequent ingredients when meaningful local recipe history exists, then categories. With query: ranked local results plus optional category filter.

`Frequent` is derived from distinct saved recipes that reference the catalog ingredient. It does not require additional behavioral history.

`Recent` is deliberately not implemented under Gate 48 / Option A because the current model does not store an explicit ingredient-selection timestamp. Recipe modification time must not be used as a surrogate for recent ingredient use.

Search supports canonical name, aliases, accent-insensitive and case-insensitive matching. No fuzzy identity matching.

## Ingredient detail

Show name, category, aliases, description, related presentations when explicitly modelled, and safety information with group, relation type, evidence level, jurisdiction, source and reviewed date.

## Custom ingredient

Types: `SIMPLE`, `COMPOUND`, `COMMERCIAL_PRODUCT`.

Fields: name, category, default unit, aliases, notes; optional brand/trade name and label-read date; composition known/unknown; zero or more safety relations.

User-created evidence is `USER_DECLARED` or `UNVERIFIED`; user cannot self-assign official evidence levels.

## Recipe safety aggregation

Groups:

- Contains
- Identified derivatives
- May contain
- Possible cross-reactivity (separate)
- Requires review
- Unknown information

Deduplicate group display but preserve originating ingredients and source types. Unknown composition must create a warning. Absence of matches uses neutral language, not a green certificate.

## Offline and accessibility

All browsing/search/safety display works offline. Warnings use text + icon, not color alone, and must be TalkBack-readable.
