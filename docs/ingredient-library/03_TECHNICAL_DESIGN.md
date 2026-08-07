# 03 — TECHNICAL DESIGN

## Architecture

Keep the existing single-module layered architecture.

Proposed feature boundaries:

```text
domain/ingredient
domain/foodsafety
data/catalog
data/repository
ui/ingredients
```

Exact package names may be flattened to match repository style.

## Repository contracts

Do not turn `RecipeRepository` into a catalog mega-interface. Prefer separate cohesive contracts such as `IngredientCatalogRepository`, `CustomIngredientRepository` and a recipe safety query/aggregator boundary.

## Catalog lifecycle

Shipped master data:

```text
app/src/main/assets/ingredient-catalog/vN/*.json
```

Runtime searchable copy: Room.

Import algorithm:

1. read manifest;
2. compare catalog version;
3. validate full bundle;
4. import in one Room transaction;
5. record imported version;
6. no-op if same version already imported.

Catalog version is independent of Room schema version.

## JSON parsing

Use a robust structured parser. Prefer Kotlin Serialization if a focused dependency is justified; avoid ad-hoc string parsing.

## Search

Persist canonical text plus precomputed normalized keys for names and aliases. Ranking is deterministic: exact canonical, exact alias, prefix, then substring. No fuzzy matching for identity.

## Threading

Asset parsing/import/search off the main thread using Room/coroutines.

## Safety aggregation

Implement the core aggregator as pure domain logic so it can be unit tested without Android/Room.

## Navigation

`Add ingredient` should open a dedicated library destination/modal flow, not embed a full catalog inside every ingredient card.

## DI

Extend `AppContainer` explicitly. No Hilt.

## Failure handling

Corrupt catalog: validation fails; no partial import; previous valid imported catalog remains intact; recipes remain readable/editable.
