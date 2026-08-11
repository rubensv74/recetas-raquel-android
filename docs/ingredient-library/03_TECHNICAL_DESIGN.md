# 03 — TECHNICAL DESIGN

## Architecture

Keep the existing single-module layered architecture.

Feature boundaries:

```text
domain/ingredient
domain/foodsafety
data/catalog
data/repository
ui/ingredients
```

Exact package names may be flattened to match repository style.

## Repository contracts

Do not turn `RecipeRepository` into a catalog mega-interface. The implementation keeps `IngredientCatalogRepository` separate and reserves separate custom-ingredient and safety aggregation boundaries.

The catalog repository also exposes lineage traversal without exposing Room entities:

```text
getParentRelations(ingredientId)
getChildRelations(ingredientId)
```

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

Catalog schema v2 supports ingredient/alias shards. Catalog schema v3 adds optional lineage relation files/shards while retaining readers for older manifests.

## JSON parsing

Gson is used as the focused structured JSON parser under ADR-021. Ad-hoc string parsing is not used.

## Search

Persist canonical text plus precomputed normalized keys for names and aliases. Ranking is deterministic: exact canonical, exact alias, prefix, then substring. No fuzzy matching for identity.

## Ingredient lineage

The accepted lineage model is a separate non-clinical graph between catalog ingredients:

```text
VARIANT_OF
CUT_OF
DERIVED_FROM
FORM_OF
```

The graph is validated for references, duplicate edges, self-relations and cycles. It is stored separately from food-safety relations and cannot propagate them.

## Recipe ingredient origins

Persisted recipe ingredients must resolve to one master origin: catalog or custom. The legacy free-text editor is supported transitionally by creating `recipe-custom:<ingredientId>` custom masters before child ingredient insertion. Existing legacy/catalog/custom origins are preserved.

## Threading

Asset parsing/import/search off the main thread using Room/coroutines.

## Safety aggregation

Implement the core aggregator as pure domain logic so it can be unit tested without Android/Room.

Lineage information is not an input that automatically manufactures safety results. Any safety relation still requires its own evidence metadata.

## Navigation

`Add ingredient` should open a dedicated library destination/modal flow, not embed a full catalog inside every ingredient card.

## DI

Extend `AppContainer` explicitly. No Hilt.

## Failure handling

Corrupt catalog: validation fails; no partial import; previous valid imported catalog remains intact; recipes remain readable/editable.

Migration failure: no destructive fallback. Room migrations are explicit, with v1 -> v2 and v2 -> v3 validated independently and as a chained path before release.

## Deferred architecture

The lineage graph intentionally does not model compound composition, component proportions, optional constituents or brand-variable products. Those require a future explicit architecture decision.
