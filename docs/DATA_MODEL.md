# Modelo de datos — Room v6

Los modelos de dominio son independientes de Room. Las entidades persistentes usan IDs `String`; los timestamps operativos de aplicación usan `Long` UTC cuando corresponde.

## Agregado receta

### Recipe / `recipes`

Campos principales:

`id`, `name`, `description`, `category`, `servings`, `preparationMinutes`, `cookingMinutes`, `notes`, `isFavorite`, `coverPhotoPath`, `createdAt`, `updatedAt`.

`coverPhotoPath` almacena una ruta relativa a `filesDir`. `null` indica ausencia de portada.

### Ingredient / `ingredients`

Campos:

`id`, `recipeId`, `quantity`, `unit`, `name`, `notes`, `sortOrder`, `catalogIngredientId`, `customIngredientId`.

`recipeId` referencia la receta con borrado en cascada.

El origen del ingrediente utilizado se conserva mediante:

- `catalogIngredientId` cuando procede de la biblioteca maestra;
- `customIngredientId` cuando procede de una identidad personalizada;
- ambos nulos cuando existe un registro no vinculado/legacy permitido por la evolución histórica.

Las FKs de origen usan `NO ACTION` para impedir eliminar una identidad que siga referenciada por recetas.

La cantidad permanece como `String?` para admitir expresiones culinarias como `1/2`, `una pizca` o `al gusto`.

### RecipeStep / `recipe_steps`

`id`, `recipeId`, `instruction`, `timerMinutes`, `photoPath`, `sortOrder`.

`recipeId` usa cascada. `photoPath` es una ruta relativa a almacenamiento privado.

## Catálogo maestro de ingredientes

### `ingredient_categories`

Categorías estructurales del catálogo, con código, nombre, orden, icono opcional y estado activo.

### `catalog_ingredients`

Identidades canónicas del catálogo. Conservan nombre normalizado, categoría, unidad habitual, descripción, versión de catálogo, estado de verificación, variabilidad de composición, estado activo y `catalogRole`.

Roles actuales:

- `CULINARY`;
- `REGULATORY_TECHNICAL`.

La biblioteca normal consulta únicamente identidades culinarias.

### `ingredient_aliases`

Alias versionados asociados a identidades de catálogo, con normalización, idioma y tipo de alias.

### `catalog_ingredient_relations`

Grafo de linaje no clínico entre ingredientes. Tipos iniciales:

```text
VARIANT_OF
CUT_OF
DERIVED_FROM
FORM_OF
```

Estas relaciones no crean ni heredan seguridad alimentaria.

### `catalog_metadata`

Registra la fotografía de catálogo importada actualmente (`catalogVersion`, locale, jurisdicción, fecha de revisión e importación).

## Seguridad alimentaria

### `food_safety_groups`

Taxonomía de grupos de seguridad: código, nombre, tipo de condición, estado regulatorio, jurisdicción, descripción y estado activo.

### `safety_sources`

Fuentes de evidencia con organización, título, referencia oficial, jurisdicción, fechas de publicación/revisión, estado documental y URL oficial cuando existe.

### `ingredient_safety_relations`

Relaciones explícitas ingrediente ↔ grupo de seguridad. Conservan tipo de relación, nivel de evidencia, fuente, notas y fecha de revisión.

La ausencia de relación no equivale a una exención ni a una afirmación de seguridad.

## Exenciones regulatorias

### `regulatory_exemptions`

Snapshots regulatorios inmutables por versión de catálogo.

La clave identifica conjuntamente la exención y su `catalogVersion`. Cada registro conserva ingrediente, grupo, jurisdicción, efecto regulatorio, condiciones, fuente, fechas de vigencia cuando están disponibles, revisión, notas y estado activo.

El efecto soportado inicialmente es:

`EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION`.

Una exención es un efecto jurídico condicionado y no modifica las relaciones de seguridad de una receta.

## Ingredientes personalizados

### `custom_ingredients`

Identidades creadas por la usuaria. Incluyen nombre, normalización, categoría opcional, unidad habitual, tipo, marca/nombre comercial, estado de composición conocida, fecha de lectura de etiqueta, notas y timestamps.

Marca, nombre comercial y fecha de lectura de etiqueta se consideran metadatos del tipo `COMMERCIAL_PRODUCT`.

### `custom_ingredient_aliases`

Alias asociados a una identidad personalizada.

### `custom_ingredient_safety_relations`

Declaraciones de seguridad aportadas por la usuaria: grupo, tipo de relación, nivel de evidencia, `sourceId`, detalle de fuente, notas y fecha de revisión.

Estas relaciones permanecen separadas de las relaciones maestras del catálogo.

## Relación agregada de receta

`RecipeWithDetails` combina receta, ingredientes y pasos. Los hijos se ordenan por `sortOrder`.

`RecipeSummary` es el read model ligero del catálogo de recetas y evita cargar el agregado completo para cada tarjeta.

## Escritura y evolución

Creación y actualización pasan por normalización/validación de draft y terminan en una transacción Room de guardado del agregado.

La evolución de esquema es explícita y no destructiva:

```text
Room v1 -> v2  origen de ingredientes / biblioteca
Room v2 -> v3  linaje y procedencia de seguridad personalizada
Room v3 -> v4  exenciones regulatorias
Room v4 -> v5  historial regulatorio versionado
Room v5 -> v6  rol de identidad de catálogo
```

Los contratos históricos se conservan en `app/schemas/.../1.json` a `6.json`.

## Almacenamiento de fotografías

Las fotografías no viven dentro de SQLite:

- portada: `recipe_photos/{recipeId}/...` bajo `filesDir`;
- pasos: `recipe_photos/{recipeId}/steps/...`;
- staging: `recipe_photo_staging/...` bajo `cacheDir`.

Room persiste rutas relativas, nunca `Bitmap`, URI externas ni contenido binario.

## Implicación para Backup/Restore

Una copia completa de la aplicación no puede limitarse a `recipes.db`: debe preservar de forma coherente los **datos estructurados que pertenecen a la usuaria** y las **fotografías privadas referenciadas por las recetas**.

El catálogo maestro empaquetado puede reconstruirse desde los assets de la versión de la app, pero las identidades personalizadas, sus declaraciones y las referencias de recetas son datos de usuario y no deben perderse.