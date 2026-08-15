# Gate 56 — Estabilización funcional del editor

**Estado:** SUPERADO
**Fecha:** 2026-08-15  
**Rama:** `program/premium-experience-foundation`

## Alcance

Este gate cierra las incidencias funcionales detectadas durante la prueba manual del editor:

- `Cancelar` debe salir del editor cuando no existen cambios;
- `Descartar` debe limpiar el staging y salir cuando existen cambios;
- el botón o gesto Atrás de Android debe seguir el mismo flujo que `Cancelar`;
- categoría y unidad deben seleccionarse mediante listas controladas;
- una categoría o unidad ya elegida debe poder eliminarse;
- el catálogo de recetas debe conservar el filtro por categoría.

El trabajo de identidad visual queda expresamente fuera de este gate.

## Cambios implementados

1. `RecipeEditorScreen` registra un `BackHandler` y delega en el mismo callback usado por `Cancelar`.
2. `RecipeEditorViewModel.handleBack()` emite `EditorClosed` cuando no hay cambios y solicita confirmación cuando sí los hay.
3. `discardChanges()` elimina fotografías en staging antes de emitir `EditorClosed`.
4. `SelectionDropdown` permite una acción opcional de limpieza.
5. El selector de categoría ofrece `Sin categoría`.
6. El selector de unidad ofrece `Sin unidad`.

## Cobertura añadida

- cierre sin diálogo cuando el editor no tiene cambios;
- cierre después de confirmar el descarte;
- limpieza de categoría y unidad en el estado del editor;
- presencia de `Sin categoría` y `Sin unidad` en la UI instrumentada.

## Validaciones completadas en auditoría

- `git diff --check`: PASS;
- catálogo v13: conteos del manifiesto coherentes;
- 808 IDs únicos y 808 nombres normalizados únicos;
- referencias de categoría, alias, seguridad y composición: válidas;
- revisión estática de navegación y contratos del editor: PASS.

## Evidencia de cierre

Validación ejecutada en Windows con el AVD `Medium_Phone(AVD) - 16`:

```powershell
.\gradlew -g "C:\Temp\gradle_home_editor_gate56" assembleDebug testDebugUnitTest lintDebug compileDebugAndroidTestKotlin connectedDebugAndroidTest
```

- `BUILD SUCCESSFUL` en 2 min 14 s;
- 85 de 85 pruebas instrumentadas ejecutadas;
- 0 omitidas y 0 fallidas;
- aceptación manual completada para navegación, descarte, limpieza de selecciones y filtro por categoría.

## Criterio de cierre

El gate solo puede marcarse `SUPERADO` cuando:

1. Gradle termina con `BUILD SUCCESSFUL`;
2. todas las pruebas unitarias pasan;
3. lint no registra errores bloqueantes;
4. todas las pruebas instrumentadas se ejecutan, sin omitidas ni fallidas;
5. la aceptación manual confirma:
   - Atrás sin cambios sale;
   - Atrás con cambios muestra confirmación;
   - `Seguir editando` conserva el formulario;
   - `Descartar` sale y no conserva fotografías temporales;
   - `Sin categoría` y `Sin unidad` restauran valores vacíos;
   - el filtro por categoría devuelve únicamente recetas de la categoría elegida.

Los criterios de cierre han sido satisfechos. Gate 56 superado el 15 de agosto de 2026.
