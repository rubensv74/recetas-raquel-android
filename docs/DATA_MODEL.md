# Modelo de datos — esquema 1

Los modelos de dominio son independientes de Room. Las entidades persistentes viven en `data.local.entity` y usan UUID como `String`. Los timestamps son `Long` con milisegundos Unix UTC.

## Recipe / RecipeEntity (`recipes`)

- `id: String`, clave primaria
- `name: String`
- `description: String?`
- `category: String?`
- `servings: Int?`
- `preparationMinutes: Int?`
- `cookingMinutes: Int?`
- `notes: String?`
- `isFavorite: Boolean`, inicialmente `false`
- `coverPhotoPath: String?`, solo almacenamiento de referencia en este sprint
- `ingredients: List<Ingredient>`, solo en dominio/agregado
- `steps: List<RecipeStep>`, solo en dominio/agregado
- `createdAt: Long`
- `updatedAt: Long`

## Ingredient / IngredientEntity (`ingredients`)

- `id: String`, clave primaria
- `recipeId: String`, clave foránea a `recipes.id` con borrado en cascada
- `quantity: String?`, admite `1/2`, `al gusto` y otras cantidades no numéricas
- `unit: String?`
- `name: String`
- `notes: String?`
- `sortOrder: Int`

Existe un índice `(recipeId, sortOrder)`.

## RecipeStep / RecipeStepEntity (`recipe_steps`)

- `id: String`, clave primaria
- `recipeId: String`, clave foránea a `recipes.id` con borrado en cascada
- `instruction: String`
- `timerMinutes: Int?`, sin temporizador funcional todavía
- `photoPath: String?`, sin selector de imágenes todavía
- `sortOrder: Int`

Existe un índice `(recipeId, sortOrder)`.

## Relación agregada

`RecipeWithDetails` combina una receta mediante `@Embedded` y sus ingredientes y pasos mediante dos `@Relation`. El mapper ordena ambas colecciones explícitamente por `sortOrder`.

## Escritura

La creación parte de `RecipeDraft`, `IngredientDraft` y `RecipeStepDraft`. El repositorio normaliza texto y orden, genera IDs y timestamps, y entrega todas las entidades al DAO. La actualización conserva IDs y `createdAt`, renueva `updatedAt` y reemplaza los hijos dentro de una transacción.

## Elementos futuros no implementados

`Tag`, `RecipeTag`, la gestión funcional de `Photo` y `SyncQueue` quedan fuera del esquema 1. Un cambio posterior exigirá una nueva versión y una migración real; no se permite `fallbackToDestructiveMigration()`.

El esquema JSON exportado está en `app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/1.json`.
