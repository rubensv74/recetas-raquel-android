# Arquitectura

## Enfoque local-first

Room es la fuente de verdad. Todas las operaciones esenciales se ejecutan en el dispositivo y no dependen de conectividad. `recipes.db` conserva datos entre ejecuciones y expone cambios mediante `Flow`.

## Capas y flujo

- **UI (`ui`, `app`)**: Compose representa estados inmutables y emite eventos a ViewModels.
- **Dominio (`domain`)**: modelos, read models, filtros, contratos, casos de uso y `RecipePhotoStorage` sin dependencias de Android framework.
- **Datos (`data`)**: entidades, relaciones, DAO, mappers, `LocalRecipeRepository` y `LocalRecipePhotoStorage`.
- **Backup (`backup`)**: límite futuro no implementado.

El flujo de lectura es UI → ViewModel → Repository → DAO → Room/SQLite → Flow → ViewModel → UI. La UI no accede al DAO ni recibe entidades Room.

## Navegación y estados

Navigation Compose centraliza `catalog`, `recipe/{recipeId}`, `recipe/new`, `recipe/{recipeId}/edit`, `recipe/{recipeId}/cook` y `settings`; el inicio es `catalog`. Detalle, edición y modo cocina obtienen `recipeId` mediante `SavedStateHandle` y no reciben objetos completos por navegación.

`RecipeCatalogViewModel`, `RecipeDetailViewModel`, `RecipeEditorViewModel` y `CookingModeViewModel` usan `StateFlow`, `viewModelScope` y factories manuales. Compose recoge estado con `collectAsStateWithLifecycle`.

El editor usa `MutableSharedFlow<EditorNavigationEvent>` para navegación de un solo uso. Los cambios sin guardar se detectan comparando estado normalizado y el doble guardado se previene mediante flags `isSaving`/`isDeleting`.

## Persistencia y catálogo

`RecipeDatabase` continúa en esquema 1 con `exportSchema = true`. No hay migraciones ni migración destructiva. La transacción `saveRecipeWithDetails` y las cascadas del Sprint 1 siguen siendo la unidad de persistencia del agregado receta.

`RecipeSummary` evita cargar el agregado completo en cada tarjeta. SQL parametrizado combina consulta, favoritas y categoría; `EXISTS` busca ingredientes sin duplicar recetas y Room ordena por `updatedAt DESC`.

## Fotografías de recetas (Sprint 4)

### Dominio

`RecipePhotoStorage` define el contrato `stagePhoto`, `promotePhoto`, `delete`, `deleteStaged`, `resolve`, `cleanStaging` y `getRecipePhotoPaths`. `PhotoDestination` y `StagedPhoto` completan el modelo. Las URI se cruzan al dominio como `String` para evitar tipos Android (ADR-016).

### Almacenamiento

1. `PickVisualMedia` obtiene una imagen seleccionada sin permisos globales de galería.
2. `stagePhoto()` corrige orientación EXIF, limita a 2048 px, comprime JPEG a calidad 85 y escribe en `cacheDir/recipe_photo_staging/`.
3. `SaveRecipeUseCase` conoce previamente `recipeId` y los identificadores estables de pasos, promueve los staged a `filesDir/recipe_photos/{recipeId}/...` y obtiene rutas relativas permanentes.
4. El draft final contiene esas rutas permanentes y se persiste en Room.
5. Si Room falla, el caso de uso elimina los archivos recién promovidos; las fotos anteriores permanecen intactas.
6. Si Room tiene éxito, se eliminan las fotos sustituidas y se limpia staging.

Room nunca debe persistir rutas de `cacheDir`, URI del picker ni `Bitmap`.

### UI de fotos

`EditorPhotoState` representa el ciclo de vida de la selección sin almacenar bitmaps en `StateFlow`. Coil (`AsyncImage`) se utiliza para previsualización y consulta de imágenes locales. No existe cámara ni carga remota.

## Modo cocina (Sprint 5)

`CookingModeViewModel` observa una receta con `RecipeRepository.observeRecipe(recipeId)`. Su estado es `CookingUiState`: `Loading`, `NotFound`, `Error` o `Content`.

`Content` contiene la receta, los pasos ordenados por `sortOrder`, el índice actual y la visibilidad de ingredientes. El índice se almacena en `SavedStateHandle`, por lo que una recreación del ViewModel conserva el paso activo. No se escribe ningún dato en Room durante la sesión de cocina.

`CookingModeScreen` presenta un único paso, contador `Paso X de N`, progreso, fotografía opcional y `timerMinutes` únicamente como información. Los ingredientes se abren en una `ModalBottomSheet` sin alterar el paso actual.

La política de pantalla encendida pertenece a UI: `LocalView.keepScreenOn` se activa con `DisposableEffect` al entrar y se restaura al salir. No requiere permisos ni servicios en background.

El modo cocina no edita recetas, no inicia temporizadores, no crea notificaciones y no añade persistencia propia.

## Identidad, tiempo e inyección

Los IDs son UUID `String`; fechas en milisegundos Unix UTC. `IdGenerator` y `TimeProvider` son testeables. `AppContainer` crea una única `RecipeDatabase`, `LocalRecipeRepository`, `LocalRecipePhotoStorage` y los casos de uso necesarios. Las factories de ViewModel reciben dependencias explícitas; no existe framework de DI.

El source set `debug` aporta el controlador de datos demo. `release` devuelve controlador nulo. No hay red, backend, sincronización ni backup.

## Un solo módulo

Se mantiene únicamente `app`; los paquetes expresan los límites internos.
