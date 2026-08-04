# Arquitectura

## Enfoque local-first

Room es la fuente de verdad. Todas las operaciones esenciales se ejecutan en el dispositivo y no dependen de conectividad. `recipes.db` conserva datos entre ejecuciones y expone cambios mediante `Flow`.

## Capas y flujo

- **UI (`ui`, `app`)**: Compose representa estados inmutables y emite eventos a ViewModels.
- **Dominio (`domain`)**: modelos, read models, filtros y contratos sin dependencias de Room.
- **Datos (`data`)**: entidades, relaciones, DAO, mappers y `LocalRecipeRepository`.
- **Backup (`backup`)**: límite futuro no implementado.

El flujo es UI → ViewModel → Repository → DAO → Room/SQLite → Flow → ViewModel → UI. La UI no accede al DAO ni recibe entidades.

## Navegación y estados

Navigation Compose centraliza `catalog`, `recipe/{recipeId}`, `recipe/new`, `recipe/{recipeId}/edit` y `settings`; el inicio es `catalog`. El detalle obtiene `recipeId` mediante `SavedStateHandle` y no recibe objetos completos. El editor usa `RecipeEditorViewModel` con `SavedStateHandle` para determinar modo creación (`new`) o edición (`{recipeId}`).

`RecipeCatalogViewModel`, `RecipeDetailViewModel` y `RecipeEditorViewModel` usan `StateFlow`, `viewModelScope` y factories manuales. Compose recoge estado con `collectAsStateWithLifecycle`. Catálogo distingue carga, base vacía, sin resultados, contenido y error; detalle distingue carga, contenido, no encontrado y error; editor distingue carga, no encontrado, error y contenido con validación.

El editor usa `MutableSharedFlow<EditorNavigationEvent>` para navegación de un solo uso (creado, actualizado, eliminado). Los cambios sin guardar se detectan comparando `NormalizedEditorState` actual con el estado inicial normalizado. El doble guardado se previene con flags `isSaving`/`isDeleting`.

## Persistencia y catálogo

`RecipeDatabase` continúa en esquema 1 con `exportSchema = true`. No hay migraciones ni migración destructiva. La transacción `saveRecipeWithDetails` y las cascadas del Sprint 1 no cambian. El editor reutiliza la misma transacción para creación y actualización, preservando `createdAt`, `isFavorite` e IDs existentes de ingredientes/pasos. Los IDs nuevos se generan con `IdGenerator` en el mapper.

`RecipeSummary` evita representar recetas incompletas y cargar ingredientes/pasos para cada tarjeta. SQL parametrizado combina consulta, favoritas y categoría. `EXISTS` busca ingredientes sin duplicar recetas; Room ordena por `updatedAt DESC`. Las categorías usan `DISTINCT`, excluyen nulos/vacíos y se ordenan sin distinguir mayúsculas cuando SQLite lo permite. Los índices existentes `(recipeId, sortOrder)` siguen cubriendo relaciones y subconsulta.

## Identidad, tiempo e inyección

Los IDs siguen siendo UUID `String`; fechas en milisegundos Unix UTC. `IdGenerator` y `TimeProvider` son testeables. `RecetasRaquelApplication` crea un único `AppContainer`, una única `RecipeDatabase` y el repositorio. Las factories de ViewModel reciben dependencias explícitas; no existe framework de DI.

El source set `debug` aporta un controlador demo que reutiliza `RecipeRepository`. `release` aporta una factory nula, por lo que no se muestra la sección de desarrollo. No hay red, backend, sincronización ni backup.

## Un solo módulo

Se mantiene únicamente `app`; los paquetes expresan los límites internos.
