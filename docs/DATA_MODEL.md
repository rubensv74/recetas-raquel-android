# Modelo de datos — esquema 1

Los modelos de dominio son independientes de Room. Entidades persistentes usan IDs `String` y timestamps `Long` UTC.

## Recipe / `recipes`

`id`, `name`, `description`, `category`, `servings`, `preparationMinutes`, `cookingMinutes`, `notes`, `isFavorite`, `coverPhotoPath`, `createdAt` y `updatedAt`. El dominio añade las listas `ingredients` y `steps`.

`coverPhotoPath` es un `String?` con la ruta relativa a `filesDir` (por ejemplo, `recipe_photos/{recipeId}/cover_{uuid}.jpg`). `null` indica ausencia de portada.

## Ingredient / `ingredients`

`id`, `recipeId`, `quantity: String?`, `unit`, `name`, `notes` y `sortOrder`. `recipeId` referencia la receta con borrado en cascada. El índice `(recipeId, sortOrder)` permanece intacto. La cantidad admite valores como `1/2`, `una pizca` o `al gusto`.

## RecipeStep / `recipe_steps`

`id`, `recipeId`, `instruction`, `timerMinutes`, `photoPath` y `sortOrder`. La clave foránea usa cascada y existe el índice `(recipeId, sortOrder)`.

`photoPath` es un `String?` con la ruta relativa a `filesDir` (por ejemplo, `recipe_photos/{recipeId}/steps/step_{stepId}_{uuid}.jpg`). `null` indica ausencia de foto del paso.

## Relación agregada

`RecipeWithDetails` combina receta, ingredientes y pasos. El mapper ordena hijos explícitamente por `sortOrder`.

## Read model del catálogo

`RecipeSummary` contiene `id`, `name`, `category`, `servings`, tiempos, favorito, referencia opcional de portada (`coverPhotoPath`) y `updatedAt`; nunca contiene listas vacías artificiales. `RecipeCatalogFilter` agrupa consulta, favoritas y categoría, recorta textos y convierte categorías vacías en `null`.

## Escritura y evolución

Creación usa `RecipeDraft.toNewRecipe()` que genera nuevo `id`, `createdAt` e `updatedAt` con `IdGenerator` y `TimeProvider`, y `isFavorite = false`. Actualización usa `RecipeDraft.toUpdatedRecipe()` que preserva `id`, `createdAt` e `isFavorite` del existente, genera nuevos IDs solo para hijos nuevos (`id ?: idGenerator.newId()`), y actualiza `updatedAt`. Ambas rutas pasan por `RecipeMapper.normalizeDraft()` → `RecipeValidator.normalize()` y se persisten con `saveRecipeWithDetails` (transacción `@Transaction`).

## Almacenamiento de fotografías

Las fotografías se almacenan en el sistema de archivos privado de la aplicación:

- **Portada**: `recipe_photos/{recipeId}/cover_{uuid}.jpg` bajo `context.filesDir`
- **Pasos**: `recipe_photos/{recipeId}/steps/step_{stepId}_{uuid}.jpg` bajo `context.filesDir`
- **Temporal (staging)**: `recipe_photo_staging/staging_{uuid}.jpg` bajo `context.cacheDir`

Las rutas en Room son relativas a `filesDir`. El URI externo del Photo Picker nunca se persiste: se decodifica, procesa y almacena como JPEG privado en una sola operación de staging. La promoción a almacenamiento permanente ocurre solo después de que Room confirma el guardado.

`LocalRecipePhotoStorage` gestiona el ciclo de vida completo: staging → promoción → eliminación. Los fallos de limpieza son silenciosos para no bloquear al usuario.

Sprint 4 no cambia entidades, tablas, columnas, tipos, claves, índices ni relaciones. `RecipeDatabase` continúa en versión 1. El único esquema es `app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/1.json`; no existe `2.json` ni migración.
