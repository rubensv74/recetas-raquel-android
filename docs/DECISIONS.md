# Registro de decisiones

## ADR-001 — Kotlin, Compose y Material 3

**Aceptada.** Una base Kotlin y UI declarativa sin pantallas XML.

## ADR-002 — Room como fuente local

**Implementada.** Room 2.8.4, esquema v1 exportado, Flow, integridad y transacciones.

## ADR-003 — Aplicación offline

**Aceptada.** Casos principales sin red para disponibilidad y privacidad.

## ADR-004 — Un único módulo

**Aceptada.** Un solo `app`; límites mediante paquetes.

## ADR-005 — Inyección manual

**Aceptada.** `AppContainer` y factories explícitas, sin Hilt/Koin/Dagger.

## ADR-006 — GitHub solo como posible backup

**Aceptada como dirección futura.** Nunca será la base de datos; no existe Sync.

## ADR-007 — Sin backend en MVP

**Aceptada.** No hay cuentas ni infraestructura remota.

## ADR-008 — Navegación manual provisional

**Reemplazada en Sprint 2.** Fue suficiente para Sprint 0, pero detalle con argumento y back stack justifican Navigation Compose.

## ADR-009 — KSP y esquemas

**Aceptada.** Room Compiler usa KSP2 y exporta a `app/schemas`.

## ADR-010 — UUID y timestamps UTC

**Aceptada.** IDs `String`, timestamps Unix UTC y proveedores testeables.

## ADR-011 — Agregado transaccional

**Aceptada.** Receta e hijos se guardan como unidad y se eliminan en cascada.

## ADR-012 — Navigation Compose y ViewModels

**Aceptada.** Rutas `catalog`, `recipe/{recipeId}`, `recipe/new`, `recipe/{recipeId}/edit` y `settings`; estados `StateFlow`; ID desde `SavedStateHandle`. Navegación de un solo uso con `SharedFlow`.

## ADR-013 — Read model y consulta SQL

**Aceptada.** `RecipeSummary` evita cargas completas. SQL parametrizado con `EXISTS` busca ingredientes sin duplicados y combina filtros.

## ADR-014 — Datos demo por source set

**Aceptada.** Cinco recetas deterministas viven solo en debug, bajo acción explícita e idempotente. Release devuelve controlador nulo.

## ADR-015 — Editor de recetas sin esquema nuevo

**Aceptada en Sprint 3.** El editor funciona con el esquema v1 existente. `RecipeMapper.toUpdatedRecipe` preserva IDs de recetas hijas para actualizaciones in-place, y `LocalRecipeRepository.updateRecipeFromDraft` usa la estrategia de reemplazo transaccional del DAO. No se añaden dependencias ni migraciones.
