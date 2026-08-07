# 06 — UI FLOW

## Editor integration

Current `Añadir ingrediente` creates a blank manual row. New default:

```text
Añadir ingrediente -> Ingredient Library
```

## Library screen

Top bar: back/cancel, autofocus search, secondary `Crear ingrediente nuevo` action.

Empty query: Recent, Frequent (only with enough local history), Categories.

Query: ranked results and optional category filtering.

Rows show name, category and concise non-color-only safety indicator. Detailed clinical/evidence information belongs in the ingredient detail screen.

## Selection

Select ingredient -> quantity, unit and optional notes -> add to recipe. Default unit may be suggested but recipe usage stores the actual selected value.

## Custom ingredient

General info -> type -> safety information -> save -> add to recipe.

Safety form supports multiple relations. `Composición desconocida` is explicit and later generates recipe-level review warning.

## Recipe detail

Show `⚠ Información sobre seguridad alimentaria` when relevant. Aggregated panel orders confirmed presence separately from derivatives, PAL, cross-reactivity and unknown/review states.

Do not use a green `safe` certificate when no matches are found.

## Accessibility

All warning icons have descriptions; color is never the only signal; heading structure and touch targets are accessible; panel content is readable with TalkBack.
