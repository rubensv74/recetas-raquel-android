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

**Aceptada.** Rutas de aplicación mediante Navigation Compose; estados `StateFlow`; IDs desde `SavedStateHandle`. Navegación de un solo uso con `SharedFlow` cuando procede.

## ADR-013 — Read model y consulta SQL

**Aceptada.** `RecipeSummary` evita cargas completas. SQL parametrizado con `EXISTS` busca ingredientes sin duplicados y combina filtros.

## ADR-014 — Datos demo por source set

**Aceptada.** Cinco recetas deterministas viven solo en debug, bajo acción explícita e idempotente. Release devuelve controlador nulo.

## ADR-015 — Editor de recetas sin esquema nuevo

**Aceptada en Sprint 3.** El editor funciona con el esquema v1 existente. `RecipeMapper.toUpdatedRecipe` preserva IDs de recetas hijas para actualizaciones in-place y `LocalRecipeRepository.updateRecipeFromDraft` usa la estrategia transaccional del DAO.

## ADR-016 — Strings URI en el domain layer

**Aceptada en agosto de 2026.** `RecipePhotoStorage` usa `String` en lugar de `android.net.Uri` para mantener tipos Android fuera de la capa de dominio.

## ADR-017 — Modo cocina como estado efímero de UI

**Aceptada en Sprint 5.** El modo cocina no crea entidades ni cambia Room. La receta se observa mediante `RecipeRepository`, el paso actual se conserva en `SavedStateHandle` y los ingredientes se muestran sin abandonar la ruta. Mantener la pantalla encendida se resuelve en Compose con `LocalView.keepScreenOn` y `DisposableEffect`, restaurando el valor anterior al salir. Los temporizadores ejecutables quedan para una fase posterior.
