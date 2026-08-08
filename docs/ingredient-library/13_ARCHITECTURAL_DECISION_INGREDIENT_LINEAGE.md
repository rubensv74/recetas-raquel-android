# 13 — ARCHITECTURAL DECISION: INGREDIENT LINEAGE AND VARIANTS

**Status:** ACCEPTED — OPTION B  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Decision

Adopt **Option B — non-clinical ingredient lineage graph**.

The master catalog will model explicit relationships between catalog ingredients without conflating those relationships with food-safety evidence.

Initial relation vocabulary:

```text
VARIANT_OF
CUT_OF
DERIVED_FROM
FORM_OF
```

Examples:

```text
Pechuga de pollo  CUT_OF       Pollo
Harina de trigo   DERIVED_FROM Trigo
Aceite de oliva   DERIVED_FROM Aceituna
```

Compound recipes/products are not modeled by this graph.

## Non-negotiable safety rule

An identity relationship must **never automatically create or inherit a food-safety relationship**.

For example:

```text
Harina de trigo DERIVED_FROM Trigo
```

is catalog lineage only. Any food-safety relation for `Harina de trigo` still requires its own reviewed record with:

```text
sourceId
evidenceLevel
relationType
reviewedAt
```

The identity graph and safety graph remain separate persisted concepts, separate validation paths and separate future UI semantics.

## Persisted model

Room v3 introduces:

```text
CatalogIngredientRelation
- id
- childIngredientId
- parentIngredientId
- relationType
- reviewedAt
- sourceReference nullable
- notes nullable
- isActive
```

Both ingredient references point to `CatalogIngredient`.

The graph is acyclic and duplicate `(child,parent,type)` edges are rejected.

## Migration consequence

The decision requires explicit Room v2 -> v3 migration. Existing recipe, catalog, custom ingredient and safety data must be preserved.

The same migration corrects the pre-existing custom safety relation source contract so every custom safety relation has a `sourceId`; legacy free-text source information is retained separately as `sourceDetails`.

## Catalog consequence

Catalog versions v1-v4 remain immutable. Catalog schema v3 adds optional lineage files. The first catalog version containing lineage edges will be a new immutable version after Room v3 validation.

## Deferred boundary

A full composition graph remains out of scope. Variable recipes, commercial products, compound sauces, constituent percentages/ranges and brand-specific composition require a separate future architectural decision rather than overloading lineage.

## Implementation record

See `14_LINEAGE_GRAPH_IMPLEMENTATION.md`.
