# Arquitectura

## Enfoque local-first

Room es la fuente de verdad. Todas las operaciones esenciales se ejecutan en el dispositivo y no dependen de conectividad. `recipes.db` conserva datos entre ejecuciones y expone cambios mediante `Flow`.

## Capas y flujo

- **UI (`ui`, `app`)**: Compose representa estados inmutables y emite eventos a ViewModels.
- **Dominio (`domain`)**: modelos, read models, filtros, contratos y `RecipePhotoStorage` sin dependencias de Android framework.
- **Datos (`data`)**: entidades, relaciones, DAO, mappers, `LocalRecipeRepository` y `LocalRecipePhotoStorage`.
- **Backup (`backup`)**: límite futuro no implementado.

El flujo es UI → ViewModel → Repository → DAO → Room/SQLite → Flow → ViewModel → UI. La UI no accede al DAO ni recibe entidades.

## Navegación y estados

Navigation Compose centraliza `catalog`, `recipe/{recipeId}`, `recipe/new`, `recipe/{recipeId}/edit` y `settings`; el inicio es `catalog`. El detalle obtiene `recipeId` mediante `SavedStateHandle` y no recibe objetos completos. El editor usa `RecipeEditorViewModel` con `SavedStateHandle` para determinar modo creación (`new`) o edición (`{recipeId}`).

`RecipeCatalogViewModel`, `RecipeDetailViewModel` y `RecipeEditorViewModel` usan `StateFlow`, `viewModelScope` y factories manuales. Compose recoge estado con `collectAsStateWithLifecycle`. Catálogo distingue carga, base vacía, sin resultados, contenido y error; detalle distingue carga, contenido, no encontrado y error; editor distingue carga, no encontrado, error y contenido con validación.

El editor usa `MutableSharedFlow<EditorNavigationEvent>` para navegación de un solo uso (creado, actualizado, eliminado). Los cambios sin guardar se detectan comparando `NormalizedEditorState` actual con el estado inicial normalizado. El doble guardado se previene con flags `isSaving`/`isDeleting`.

## Persistencia y catálogo

`RecipeDatabase` continúa en esquema 1 con `exportSchema = true`. No hay migraciones ni migración destructiva. La transacción `saveRecipeWithDetails` y las cascadas del Sprint 1 no cambian. El editor reutiliza la misma transacción para creación y actualización, preservando `createdAt`, `isFavorite` e IDs existentes de ingredientes/pasos. Los IDs nuevos se generan con `IdGenerator` en el mapper.

`RecipeSummary` evita representar recetas incompletas y cargar ingredientes/pasos para cada tarjeta. SQL parametrizado combina consulta, favoritas y categoría. `EXISTS` busca ingredientes sin duplicar recetas; Room ordena por `updatedAt DESC`. Las categorías usan `DISTINCT`, excluyen nulos/vacíos y se ordenan sin distinguir mayúsculas cuando SQLite lo permite. Los índices existentes `(recipeId, sortOrder)` siguen cubriendo relaciones y subconsulta.

## Fotografías de recetas (Sprint 4)

### dominio

`RecipePhotoStorage` define el contrato: `stagePhoto`, `promotePhoto`, `delete`, `deleteStaged`, `resolve`, `cleanStaging`, `getRecipePhotoPaths`. `PhotoDestination` (sealed) y `StagedPhoto` (data class) completan el modelo. Las rutas se manejan como `String` relativo a `filesDir`, nunca `android.net.Uri` (ADR-016).

### Almacenamiento y ciclo de vida

1. **Selección**: `ActivityResultContracts.PickVisualMedia` obtiene un `Uri` del sistema. No se solicitan permisos de cámara ni galería.
2. **Staging**: El `Uri` se convierte a `String` en el ViewModel (`uri.toString()`), se pasa a `stagePhoto()` que decodifica, corrige orientación EXIF, redimensiona (máx. 2048px), comprime a JPEG (calidad 85) y escribe en `context.cacheDir/recipe_photo_staging/`. El resultado es un `StagedPhoto` con `stagedFile` (File) y `relativePath` (String bajo `recipe_photos/`).
3. **Promoción**: Tras guardar en Room con éxito, `promotePhoto()` mueve el archivo de staging a `context.filesDir/recipe_photos/{recipeId}/cover_{uuid}.jpg` o `.../steps/step_{stepId}_{uuid}.jpg`.
4. **Limpieza**: `cleanStaging()` elimina los archivos temporales. Los fallos de limpieza son silenciosos: archivos huérfanos no bloquean la app.

### Garantías de integridad

- **Room nunca apunta a cacheDir**: El draft solo incluye rutas cuando el estado es `Persisted` (ya en `filesDir`). Los `Staged` producen `null` en el draft.
- **Imagen anterior se conserva si falla el guardado**: `updateRecipe()` captura `oldCover` y `oldStepPaths` antes del intento; solo `deleteOldPhotos()` se ejecuta si el resultado es éxito.
- **Eliminación de fotos de pasos retirados**: `removeStep()` elimina la clave del paso de `stepPhotoStates`. `deleteOldPhotos()` comprueba rutas antiguas vs. estado actual y elimina las que ya no están presentes.
- **Eliminación de todas las fotos de una receta borrada**: `confirmDelete()` llama `getRecipePhotoPaths(id)` antes de borrar Room, luego elimina cada archivo.
- **Prevención de afectación cruzada**: `getRecipePhotoPaths()` filtra por prefijo `recipeId`; `buildRelativePath()` scope a `recipe_photos/{recipeId}/`.
- **Doble pulsación**: `isSaving`/`isDeleting` flags previenen doble ejecución de `save()` y `confirmDelete()`.
- **Reordenamiento**: Paso y foto se asocian por `key` (UUID), no por posición. `moveStepUp`/`moveStepDown` conservan la clave.
- **Limpieza al descartar**: `discardChanges()` → `cleanStaging()` elimina todos los `StagedPhoto` del estado actual.
- **Eliminación de pasos**: `removeStep()` elimina la entrada de `stepPhotoStates` para esa clave.

### Almacenamiento en UI

`EditorPhotoState` (sealed interface) modela: `None`, `Persisted(relativePath)`, `Staged(stagedPhoto)`, `Processing(previous)`, `Removed(previous)`, `Error(previous, message)`. El ViewModel solo almacena `StagedPhoto` (File + String) o `String` — nunca `Bitmap` en `StateFlow`.

### Renderizado

Coil 2.7.0 (`AsyncImage`) carga imágenes desde archivos locales: `stagedFile` (File) para previsualización en editor, `relativePath` (String) para portadas en catálogo y detalle. No se usa para URLs remotas.

### Permisos

No se solicitan permisos de cámara, lectura de almacenamiento o galería. El Android Photo Picker (`PickVisualMedia`) accede al contenido seleccionado mediante un URI temporal que el sistema gestiona.

## Identidad, tiempo e inyección

Los IDs siguen siendo UUID `String`; fechas en milisegundos Unix UTC. `IdGenerator` y `TimeProvider` son testeables. `AppContainer` crea un único `RecipeDatabase`, un `LocalRecipeRepository`, un `LocalRecipePhotoStorage` y el controlador demo. Las factories de ViewModel reciben dependencias explícitas; no existe framework de DI.

El source set `debug` aporta un controlador demo que reutiliza `RecipeRepository`. `release` aporta una factory nula, por lo que no se muestra la sección de desarrollo. No hay red, backend, sincronización ni backup.

## Un solo módulo

Se mantiene únicamente `app`; los paquetes expresan los límites internos.
