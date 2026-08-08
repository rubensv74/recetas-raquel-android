# 13 — ARCHITECTURAL DECISION: INGREDIENT LINEAGE AND VARIANTS

**Status:** DECISION REQUIRED  
**Branch:** `program/ingredient-library-food-safety`  
**Date:** 2026-08-08

## Why this decision is required now

The catalog can continue growing safely while entries are simple culinary identities: tomato, apple, chicken, sunflower seed, mushroom, etc.

The next high-value areas are different. They contain relationships between ingredients:

```text
Pollo        -> pechuga de pollo / muslo de pollo
Vacuno       -> solomillo / entrecot / carne picada
Trigo        -> harina de trigo
Aceituna     -> aceite de oliva
Leche        -> nata / mantequilla / queso
Sésamo       -> tahini
Soja         -> tofu / bebida de soja / salsa de soja
```

The current `CatalogIngredient` model is flat. It can store each of these strings as an independent canonical ingredient, but it cannot express why or how they are related.

That becomes important for navigation, deduplication, maintenance and especially future food-safety traceability.

## Non-negotiable safety rule

An identity relationship must **never automatically create or inherit a food-safety relationship**.

For example:

```text
Harina de trigo DERIVED_FROM Trigo
```

may be useful catalog lineage, but that lineage alone must not silently manufacture an allergen result. A safety relation still requires its own reviewed record with:

```text
sourceId
evidenceLevel
relationType
reviewedAt
```

The identity graph and the safety graph must remain separate.

## Option A — Keep the catalog flat

Every cut, derivative and form becomes an independent `CatalogIngredient`.

### Advantages

- no Room schema change;
- simplest importer and DAO;
- fastest short-term population.

### Disadvantages

- no explicit relationship between source and derivative;
- repeated maintenance and duplicated evidence;
- difficult grouping in the future UI;
- increased risk of inconsistent safety review between related ingredients;
- weak traceability for derivatives and variants.

### Assessment

Not recommended for a long-lived master catalog.

## Option B — Add a non-clinical ingredient lineage graph

Introduce a separate relationship entity/table between catalog ingredients, for example:

```text
CatalogIngredientRelation
- id
- childIngredientId
- parentIngredientId
- relationType
- reviewedAt
- optional identity-source metadata
```

Initial relation types could be deliberately limited to:

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

Compound recipes/products would **not** yet be modeled with this table.

### Advantages

- preserves a clean canonical catalog;
- supports hierarchy/grouping without conflating identity with safety;
- enables explicit source/derivative traceability;
- keeps compound composition out of scope until needed;
- scales better than a purely flat catalog.

### Disadvantages

- requires a new persisted relationship table;
- therefore requires a Room v2 -> v3 migration if the graph is stored in the runtime database;
- importer, validator, schema assets and tests must evolve;
- relation semantics must be frozen carefully.

### Assessment

**Recommended.** It is the smallest architecture that solves the problem currently blocking further high-value catalog population.

## Option C — Implement a full ingredient composition graph now

Model parent/child lineage plus compound components, optional/required constituents, proportions/ranges and possibly commercial variants.

### Advantages

- highest theoretical expressiveness;
- can eventually describe sauces, breads, stocks, prepared foods and products.

### Disadvantages

- substantially larger domain model;
- composition is often variable by recipe/brand;
- could create false precision;
- would require much more source governance and UI complexity;
- premature for the current offline personal recipe application.

### Assessment

Not recommended now. Compound/product composition should remain a later bounded problem.

## Recommended decision

Adopt **Option B — non-clinical ingredient lineage graph**, with these constraints:

1. lineage and safety relations remain separate tables and separate concepts;
2. no automatic propagation of safety classifications along lineage edges;
3. initial relation vocabulary is small and versioned;
4. compound composition remains out of scope;
5. Room migration is explicit and non-destructive;
6. existing catalog versions v1-v4 remain immutable;
7. the first catalog using lineage becomes a new catalog version after the Room migration is validated.

## Consequence if approved

The next implementation step becomes a controlled Room v2 -> v3 evolution containing the lineage table, DAO/importer/validator changes and migration tests. Only after that gate passes should the catalog add cuts, flours, oils and other derivatives.

## Decision requested

Choose one:

```text
A — keep catalog flat
B — add lineage graph (recommended)
C — full composition graph now
```

No implementation beyond this architectural boundary should proceed until the option is explicitly selected.
