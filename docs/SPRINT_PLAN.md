# Plan de sprints

## Sprint 0 — Fundación (implementado)

Proyecto Compose/Material 3, Activity única, navegación provisional, placeholders y documentación.

## Sprint 1 — Dominio y persistencia (implementado)

Modelos, validaciones, Room v1, DAO transaccional, repositorio, mappers, inyección manual y pruebas.

## Sprint 2 — Catálogo y consulta (implementado)

Catálogo, búsqueda por receta/categoría/ingrediente, filtros combinables, detalle, favoritos, Navigation Compose, ViewModels y datos demo exclusivos de debug. Sin creación, edición ni cambios de esquema.

## Sprint 3 — Edición de recetas (implementado)

Editor de recetas con creación, edición y eliminación. Pantalla única con scroll, validación de campos obligatorios, detección de cambios sin guardar, reordenamiento de ingredientes/pasos, navegación con eventos de SharedFlow y pruebas unitarias del ViewModel.

## Sprint 4 — Fotos de recetas (completado)

Soporte para fotos de portada y fotos por paso dentro del editor y la consulta.

- **Domain**: `RecipePhotoStorage`, `PhotoDestination` (sealed), `StagedPhoto` en `domain/photos/`.
- **Data**: `LocalRecipePhotoStorage` con fix EXIF, JPEG quality 85, max 2048px, staging/promote, almacenamiento privado en `recipe_photos/`.
- **UI**: `EditorPhotoState` para estado del editor; `CoverPhotoSection`, `StepPhotoSection` con `PickVisualMedia` en `RecipeEditorScreen`; portada con Coil `AsyncImage` en `HomeScreen` y `RecipeDetailScreen`.
- **Guardado**: `SaveRecipeUseCase` promueve las fotos antes de la escritura Room, persiste únicamente rutas permanentes y compensa eliminando archivos recién promovidos si Room falla. Fotos sustituidas se eliminan solo tras éxito.
- **Infraestructura**: Coil 2.7.0 y ExifInterface 1.4.1; `String` URI en domain (ADR-016); `unitTests.isReturnDefaultValues = true`.
- **Validación de cierre**: 91 pruebas unitarias y 28 instrumentadas, build debug/release y lint superados antes del merge del PR #5.

## Sprint 5 — Modo cocina (implementado en rama; validación pendiente)

Experiencia guiada para ejecutar una receta mientras se cocina.

- Entrada desde el detalle mediante **Cocinar**.
- Ruta `recipe/{recipeId}/cook` y `CookingModeViewModel` con `SavedStateHandle`.
- Un paso cada vez, contador y progreso.
- Navegación anterior/siguiente y finalización desde el último paso.
- Foto del paso en tamaño grande cuando exista.
- Tiempo configurado mostrado como referencia, sin temporizador ejecutable.
- Ingredientes en hoja inferior sin abandonar el paso actual.
- Pantalla mantenida activa exclusivamente durante modo cocina.
- Sin cambios en Room v1, permisos, backend o almacenamiento.
- Pruebas unitarias e instrumentadas específicas añadidas; falta ejecutar la suite completa y aceptación manual antes de merge.

Ver `SPRINT_05_COOKING_MODE.md` para el contrato de alcance y validación.

### Fases futuras

Temporizadores ejecutables, backup exportación/importación, migraciones cuando sean necesarias, accesibilidad y rendimiento. GitHub Sync fuera de alcance hasta diseño propio de seguridad y conflictos.
