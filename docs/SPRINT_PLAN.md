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
- **ViewModel**: Selección, eliminación, staging, promoción, limpieza y detección de cambios sin guardar en `RecipeEditorViewModel`.
- **Infraestructura**: Coil 2.7.0 y ExifInterface 1.4.1; `String` URI en domain (ADR-016); `unitTests.isReturnDefaultValues = true`.
- **Tests**: 58 pruebas unitarias (42 ViewModel + 16 existentes), 16 pruebas instrumentadas de catálogo + 10 de almacenamiento + 2 Compose UI de fotos. Validación completa: `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug`, `compileDebugAndroidTestKotlin`, `connectedDebugAndroidTest`.

### Fase futura

Backup exportación/importación, migraciones necesarias, accesibilidad y rendimiento. GitHub Sync fuera de alcance hasta diseño propio de seguridad y conflictos.
