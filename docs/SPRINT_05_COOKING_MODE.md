# Sprint 5 — Modo Cocina

## Objetivo

Convertir el detalle de una receta en una experiencia de ejecución durante la cocina: legible a distancia corta, paso a paso, sin perder contexto y completamente offline.

## Alcance funcional

1. Desde el detalle de una receta con pasos se muestra la acción **Cocinar**.
2. El modo cocina abre la ruta `recipe/{recipeId}/cook`.
3. Se presenta un único paso cada vez, con texto de alta legibilidad.
4. Se muestra progreso `Paso X de N` y una barra de progreso.
5. Los botones **Anterior** y **Siguiente** permiten recorrer los pasos sin modificar la receta.
6. En el último paso, **Siguiente** se sustituye por **Terminar** y vuelve al detalle.
7. Si el paso tiene fotografía, se muestra en tamaño grande.
8. Si el paso tiene `timerMinutes`, se muestra como referencia de tiempo, pero no se ejecuta un temporizador en este sprint.
9. Los ingredientes están disponibles desde una hoja inferior sin abandonar el paso actual.
10. Mientras el modo cocina está visible, la pantalla se mantiene encendida; al salir se restaura el valor anterior.
11. El índice del paso se conserva en `SavedStateHandle` para sobrevivir a recreaciones del ViewModel.
12. Se contemplan estados de carga, receta eliminada, error y receta sin pasos.

## Arquitectura

- `CookingModeViewModel` depende exclusivamente de `RecipeRepository` y recibe `recipeId` mediante `SavedStateHandle`.
- `CookingUiState` es inmutable y modela `Loading`, `NotFound`, `Error` y `Content`.
- La receta continúa viniendo de `Room` a través de `RecipeRepository.observeRecipe()`; no se crea almacenamiento adicional.
- La UI no accede a DAO, Room ni filesystem.
- `CookingModeScreen` no modifica receta, pasos, fotos ni tiempos.
- El mantenimiento de pantalla encendida es una preocupación de UI (`LocalView.keepScreenOn`) y se revierte con `DisposableEffect`.

## Fuera de alcance

- Temporizadores ejecutables, alarmas o notificaciones.
- Voz, lectura de pasos o control por voz.
- Gestos de swipe obligatorios.
- Edición desde modo cocina.
- Escalado automático de cantidades.
- Lista de compra.
- Backup, importación/exportación o GitHub Sync.
- Cambios en entidades Room, esquema o migraciones.
- Nuevos permisos Android.

## Criterios de aceptación

- Una receta con pasos puede abrir el modo cocina desde detalle.
- El primer paso se muestra como `Paso 1 de N`.
- `Anterior` está deshabilitado en el primer paso.
- `Siguiente` no puede avanzar más allá del último paso.
- `Anterior` no puede retroceder antes del primero.
- El último paso muestra `Terminar`.
- Reordenamiento persistido de pasos se respeta mediante `sortOrder`.
- Ingredientes se consultan sin cambiar el paso activo.
- Una foto de paso, cuando exista, se muestra en el paso correspondiente.
- Un tiempo configurado se muestra solo como referencia.
- Una receta eliminada durante la sesión pasa a estado `NotFound`.
- Una receta sin pasos presenta un estado vacío seguro.
- La pantalla permanece encendida únicamente mientras esta ruta está visible.
- No se modifica el esquema Room v1.

## Pruebas

### Unitarias

`CookingModeViewModelTest` debe cubrir:

- carga y orden de pasos;
- navegación y límites;
- visibilidad de ingredientes;
- restauración del paso desde `SavedStateHandle`;
- receta sin pasos;
- desaparición de receta;
- error de observación.

### Instrumentadas Compose

`CookingModeUiTest` debe cubrir:

- renderizado del paso y controles;
- hoja de ingredientes sin perder el paso;
- finalización en último paso;
- estado seguro sin pasos.

## Validación obligatoria antes del merge

```shell
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew compileDebugAndroidTestKotlin
./gradlew connectedDebugAndroidTest
git diff --check
git status
```

## Prueba manual mínima

1. Abrir una receta con al menos tres pasos, uno con foto y uno con tiempo.
2. Pulsar **Cocinar**.
3. Avanzar y retroceder comprobando contador, texto y foto.
4. Abrir/cerrar ingredientes y confirmar que se mantiene el paso.
5. Girar/recrear la Activity y comprobar que se conserva el paso.
6. Llegar al último paso y pulsar **Terminar**.
7. Confirmar que se vuelve al detalle.

## Regla Git

Implementar en `sprint/05-cooking-mode`. No fusionar a `master` hasta que la validación automatizada y la prueba manual estén aprobadas.
