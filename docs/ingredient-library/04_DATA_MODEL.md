# 04 — DATA MODEL

## Required separation

1. Catalog ingredient — reusable master definition.
2. Recipe ingredient — concrete use in a recipe.
3. Custom ingredient/product — user-created master definition.

## Proposed entities

- `IngredientCategoryEntity`
- `CatalogIngredientEntity`
- `IngredientAliasEntity`
- `FoodSafetyGroupEntity`
- `SafetySourceEntity`
- `IngredientSafetyRelationEntity`
- `CustomIngredientEntity`
- `CustomIngredientAliasEntity`
- `CustomIngredientSafetyRelationEntity`
- `CatalogMetadataEntity`

## Recipe ingredient evolution

Keep current `ingredients` table as the recipe-use table and add nullable:

```text
catalogIngredientId
customIngredientId
```

Retain `name` as a snapshot/display fallback so historical recipes remain readable if a master item is renamed/deactivated.

Final application invariant:

```text
catalogIngredientId XOR customIngredientId
```

During v1->v2 migration, both new columns initially exist nullable, then every legacy row receives a valid custom origin unless an exact reviewed mapping is explicitly approved.

## Stable codes

Store enum-like values as stable codes, never localized display text.

## Deactivation

Master records referenced by recipes are logically deactivated, not physically deleted.

## Exemptions

Do not hardcode legal exemptions as ingredient-name checks. If machine-readable exemption logic is required, use source-backed structured data with effective dates/conditions.
