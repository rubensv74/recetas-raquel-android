# 06 — UI FLOW

## Editor integration

The default add flow is library-first:

```text
Añadir ingrediente -> Biblioteca de ingredientes
```

The recipe editor keeps the recipe as a draft until the person explicitly saves it.

## Library screen

Top bar: back/cancel, autofocus search, secondary `Crear ingrediente nuevo` action.

Empty query: Frequent (only with enough local recipe history) and Categories.

`Recent` is not shown. Gate 48 / Option A deliberately rejected deriving recency from `Recipe.updatedAt` because it is not a reliable ingredient-selection timestamp.

Query: ranked results and optional category filtering.

Rows show name, category and concise non-color-only safety indicator. Detailed clinical/evidence information belongs in the ingredient detail screen.

## Selection

Selecting a catalog ingredient returns to the recipe editor and creates a draft ingredient row containing:

- the catalog identity;
- canonical name;
- suggested default unit when available;
- empty quantity and notes ready for recipe-specific input.

The canonical name of a linked catalog identity is read-only in the editor and is labelled `Ingrediente de biblioteca`. Quantity, unit and observations remain editable because they belong to the recipe usage, not to the catalog identity.

The ingredient is persisted only when the recipe itself is saved.

## Custom ingredient

```text
Biblioteca -> Crear ingrediente nuevo -> información general -> tipo -> seguridad -> guardar -> editor de receta
```

Saving creates a custom identity and returns it to the recipe editor with its custom identifier, name and default unit. In the recipe editor its identity name is read-only and labelled `Ingrediente personalizado`; quantity, unit and observations remain editable for that recipe.

Safety form supports multiple relations. `Composición desconocida` is explicit and later generates recipe-level review warning.

## Identity transfer invariant

A recipe ingredient may carry one origin identity:

- `catalogIngredientId`, or
- `customIngredientId`.

Navigation transfers identity + name + default unit back to the editor. The editor preserves the origin ID when building the `IngredientDraft`, together with the recipe-specific quantity, unit and notes.

Linked identity names are not silently detached or renamed from the recipe editor.

## Recipe detail

Show `⚠ Información sobre seguridad alimentaria` when relevant. Aggregated panel orders confirmed presence separately from derivatives, PAL, cross-reactivity and unknown/review states.

Do not use a green `safe` certificate when no matches are found.

## Accessibility

All warning icons have descriptions; color is never the only signal; heading structure and touch targets are accessible; panel content is readable with TalkBack.