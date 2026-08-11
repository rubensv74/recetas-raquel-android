# Gate 51 — Integración de selección de ingrediente con el editor de receta

**Estado:** IMPLEMENTADO — GATE MANUAL DE CI PENDIENTE  
**Fecha:** 2026-08-11  
**Rama:** `program/ingredient-library-food-safety`

## 1. Objetivo

Auditar y cerrar el recorrido funcional completo desde la biblioteca de ingredientes hasta el borrador de receta, preservando la identidad de origen y dejando claramente separada la identidad del ingrediente de los datos específicos de uso en la receta.

## 2. Flujo auditado

### Ingrediente de catálogo

```text
Editor de receta
 -> Añadir ingrediente
 -> Biblioteca
 -> Seleccionar identidad de catálogo
 -> volver al editor
 -> completar cantidad / unidad / observaciones
 -> guardar receta
```

La navegación transfiere:

- `catalogIngredientId`;
- nombre canónico;
- unidad habitual sugerida, cuando existe.

El editor crea una fila de borrador. La selección no persiste por sí sola la receta.

### Ingrediente personalizado nuevo

```text
Editor de receta
 -> Biblioteca
 -> Crear ingrediente nuevo
 -> guardar identidad personalizada
 -> volver a Biblioteca
 -> transferir selección al editor
 -> completar cantidad / unidad / observaciones
 -> guardar receta
```

La navegación conserva:

- `customIngredientId`;
- nombre;
- unidad habitual, cuando existe.

Los valores transportados por `SavedStateHandle` se consumen y limpian después de añadir la fila, evitando que la misma selección vuelva a añadirse por recomposición.

## 3. Identidad bloqueada y datos de uso editables

El dominio ya impedía cambiar el nombre de una fila que estuviese vinculada a `catalogIngredientId` o `customIngredientId`.

La UI ahora refleja esa misma regla:

- identidad de catálogo: nombre de solo lectura + `Ingrediente de biblioteca`;
- identidad personalizada: nombre de solo lectura + `Ingrediente personalizado`;
- ingrediente sin identidad vinculada: nombre editable.

Siguen siendo editables para cada receta:

- cantidad;
- unidad;
- observaciones.

Esto elimina el comportamiento anterior en el que el nombre parecía editable aunque el `ViewModel` ignorase el cambio.

## 4. Persistencia de origen

`RecipeEditorViewModel.buildDraft()` conserva explícitamente en cada `IngredientDraft`:

- `catalogIngredientId`;
- `customIngredientId`;
- cantidad;
- unidad;
- nombre;
- observaciones.

Por tanto, la procedencia necesaria para la resolución posterior de seguridad no se pierde al guardar la receta.

## 5. Seguridad de ingredientes personalizados

La cobertura existente de `BuildRecipeSafetySummaryUseCaseTest` confirma que:

- las declaraciones personalizadas alcanzan el resumen de receta con su procedencia;
- `compositionKnown = false` genera aviso `UNKNOWN_COMPOSITION`;
- las evidencias de catálogo y personalizadas pueden coexistir;
- las exenciones regulatorias permanecen en una colección separada;
- una exención no reduce ni elimina las observaciones de seguridad.

No se añadió inferencia nueva en este gate.

## 6. Ajustes de UX y documentación

La acción secundaria de la biblioteca pasa de `Introducir manualmente` a `Crear ingrediente nuevo`, porque abre el formulario completo y crea una identidad personalizada.

`06_UI_FLOW.md` se alinea además con Gate 48 / Opción A:

- no se muestra `Recent`;
- `Frequent` se deriva de recetas guardadas cuando existe historial suficiente;
- se documenta la transferencia de identidad al editor y el carácter de borrador de la fila seleccionada.

## 7. Cobertura añadida

### Unitarias

`RecipeEditorCustomIngredientIdentityTest` cubre ahora también una identidad de catálogo y exige que:

- `catalogIngredientId` se preserve;
- `customIngredientId` permanezca nulo;
- el nombre canónico no cambie desde el editor;
- cantidad, unidad y observaciones sí puedan cambiar.

### Instrumentadas de UI

Se añade `RecipeEditorIngredientIdentityUiTest`:

- una identidad de catálogo muestra `Ingrediente de biblioteca`;
- una identidad personalizada muestra `Ingrediente personalizado`;
- la fila vinculada mantiene visibles cantidad, unidad y observaciones.

## 8. Arquitectura

No se modifica:

- versión Room;
- entidades Room;
- migraciones;
- schemas `1.json` a `6.json`;
- catálogo v12;
- roles `CULINARY` / `REGULATORY_TECHNICAL`;
- modelo de seguridad;
- modelo regulatorio;
- agregación de seguridad.

No aparece un gate de arquitectura.

## 9. Criterio de cierre

El bloque pasará a `VALIDADO` tras un único gate manual que confirme:

```text
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
Room schema guard                PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS
Room schema guard post-emulator  PASS
```

Hasta entonces este documento debe permanecer en estado **IMPLEMENTADO — GATE MANUAL DE CI PENDIENTE**.