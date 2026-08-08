# 04 — DATA MODEL

## Required separation

1. Catalog ingredient — reusable master definition.
2. Recipe ingredient — concrete use in a recipe.
3. Custom ingredient/product — user-created master definition.
4. Catalog ingredient lineage — non-clinical relationship between catalog identities.
5. Food-safety relation — separately evidenced relationship to a safety group.

## Implemented entities

- `IngredientCategoryEntity`
- `CatalogIngredientEntity`
- `IngredientAliasEntity`
- `CatalogIngredientRelationEntity`
- `FoodSafetyGroupEntity`
- `SafetySourceEntity`
- `IngredientSafetyRelationEntity`
- `CustomIngredientEntity`
- `CustomIngredientAliasEntity`
- `CustomIngredientSafetyRelationEntity`
- `CatalogMetadataEntity`

## Recipe ingredient evolution

The existing `ingredients` table remains the recipe-use table and contains nullable:

```text
catalogIngredientId
customIngredientId
```

`name` remains a snapshot/display fallback so historical recipes remain readable if a master item is renamed or deactivated.

Final persisted application invariant:

```text
catalogIngredientId XOR customIngredientId
```

Room does not currently express this XOR as an arbitrary table CHECK constraint. It is therefore enforced in the application persistence path and covered by tests.

Migration v1 -> v2 maps every legacy row conservatively to a dedicated `legacy:<ingredientId>` custom origin. During the transition to the library-first editor, newly entered free text is assigned a dedicated `recipe-custom:<ingredientId>` custom master before its recipe-use row is inserted.

## Catalog lineage graph

Room v3 introduces:

```text
CatalogIngredientRelationEntity
- id
- childIngredientId
- parentIngredientId
- relationType
- reviewedAt
- sourceReference nullable
- notes nullable
- isActive
```

Initial stable relation codes:

```text
VARIANT_OF
CUT_OF
DERIVED_FROM
FORM_OF
```

The graph is acyclic and rejects self-relations and duplicate `(child,parent,type)` edges.

Lineage describes identity/culinary provenance only. It does not create or inherit food-safety relations.

## Safety relation source contract

Every shipped or custom safety relation requires a `sourceId` plus its evidence/relation/review metadata.

Room v3 corrects the legacy custom-relation shape by replacing free-text-only `sourceDescription` with:

```text
sourceId      required
sourceDetails optional
```

Existing v2 source text is preserved through a deterministic migration source record rather than discarded.

## Stable codes

Store enum-like values as stable codes, never localized display text.

## Deactivation

Master records referenced by recipes are logically deactivated, not physically deleted.

## Exemptions

Do not hardcode legal exemptions as ingredient-name checks. If machine-readable exemption logic is required, use source-backed structured data with effective dates/conditions.

## Explicitly deferred model

The lineage graph is not a composition graph. Compound ingredients, recipes-as-components, optional constituents, percentages/ranges and brand-variable products remain outside this model until a separate architectural decision is made.
