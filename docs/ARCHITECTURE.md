# Arquitectura

## Estado

Arquitectura vigente para la candidata a **v1.0**.

La aplicación mantiene un enfoque **local-first y offline**: las funciones esenciales no dependen de conectividad, autenticación, backend ni servicios remotos.

## Capas y flujo

- **UI (`ui`, `app`)**: Jetpack Compose representa estados inmutables y emite eventos a ViewModels.
- **Dominio (`domain`)**: modelos, contratos, reglas, casos de uso y agregación de seguridad sin acceso directo a Room.
- **Datos (`data`)**: entidades Room, DAO, catálogo versionado, repositorios, mappers y almacenamiento local de fotografías.
- **Backup (`backup`)**: límite arquitectónico reservado; Backup/Restore todavía no está implementado.

Flujo ordinario de lectura:

```text
UI -> ViewModel -> Repository -> DAO -> Room/SQLite -> Flow -> ViewModel -> UI
```

La UI no accede directamente a DAO ni consume entidades Room.

## Persistencia

La base de producción es `recipes.db`.

`RecipeDatabase` está actualmente en **Room v6**, con `exportSchema = true` y migraciones explícitas no destructivas:

```text
1 -> 2
2 -> 3
3 -> 4
4 -> 5
5 -> 6
```

Los contratos históricos `1.json` a `6.json` están versionados en:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/
```

No se usa fallback destructivo.

La base contiene 15 entidades distribuidas en cuatro áreas lógicas:

1. recetas y uso culinario;
2. catálogo maestro de ingredientes;
3. seguridad alimentaria y regulación;
4. ingredientes personalizados.

## Recetas

El agregado receta mantiene:

- `recipes`;
- `ingredients`;
- `recipe_steps`.

`IngredientEntity` conserva, además de cantidad/unidad/nombre/notas, el origen de identidad mediante:

- `catalogIngredientId`, o
- `customIngredientId`.

Una fila vinculada a una identidad no permite cambiar silenciosamente su nombre canónico desde el editor. Cantidad, unidad y observaciones siguen siendo datos específicos de cada receta.

La transacción `saveRecipeWithDetails` sigue siendo la unidad de persistencia del agregado receta.

## Biblioteca maestra de ingredientes

El catálogo está separado del uso de un ingrediente dentro de una receta.

Infraestructura persistente principal:

- `ingredient_categories`;
- `catalog_ingredients`;
- `ingredient_aliases`;
- `catalog_ingredient_relations`;
- `catalog_metadata`.

El catálogo se distribuye como assets inmutables y versionados. La fotografía activa es **ingredient-catalog v12**.

El rol de catálogo distingue:

```text
CULINARY
REGULATORY_TECHNICAL
```

La biblioteca normal solo expone identidades `CULINARY`. Las identidades `REGULATORY_TECHNICAL` permanecen accesibles para resolución, trazabilidad y regulación, pero no se ofrecen como ingredientes culinarios normales.

## Linaje culinario, seguridad y regulación

La arquitectura mantiene tres grafos/conceptos separados:

```text
identidad y linaje culinario
!=
evidencia de seguridad alimentaria
!=
exenciones regulatorias
```

### Linaje culinario

`catalog_ingredient_relations` representa relaciones como `VARIANT_OF`, `CUT_OF`, `DERIVED_FROM` o `FORM_OF`.

Estas relaciones describen identidad culinaria. **Nunca crean ni heredan automáticamente una relación de seguridad.**

### Seguridad alimentaria

Infraestructura:

- `food_safety_groups`;
- `safety_sources`;
- `ingredient_safety_relations`;
- `custom_ingredient_safety_relations`.

Las relaciones conservan grupo, tipo, evidencia, fuente y fecha de revisión. La composición desconocida de un ingrediente personalizado se representa explícitamente y puede producir un estado de revisión en el resumen de receta.

La agregación de receta conserva todas las observaciones relevantes y utiliza precedencia únicamente para presentación. No existe scoring clínico ni inferencia automática por linaje.

### Regulación

`regulatory_exemptions` representa efectos legales condicionados de forma independiente de seguridad y linaje.

Una exención regulatoria:

- no significa ausencia de alérgeno;
- no significa ausencia de riesgo;
- no significa que el alimento sea apto para una persona alérgica o intolerante;
- nunca elimina ni reduce observaciones de seguridad de una receta.

El historial regulatorio se conserva por `catalogVersion`; las consultas operativas usan el snapshot vigente, jurisdicción y fechas de aplicabilidad.

## Ingredientes personalizados

Los ingredientes que no existen en el catálogo maestro se almacenan en:

- `custom_ingredients`;
- `custom_ingredient_aliases`;
- `custom_ingredient_safety_relations`.

Pueden representar ingredientes genéricos o productos comerciales. Marca, nombre comercial y fecha de lectura de etiqueta son metadatos propios de `COMMERCIAL_PRODUCT` y no se conservan para tipos no comerciales.

La capa de persistencia normaliza alias y protege relaciones de seguridad duplicadas.

## Navegación

Navigation Compose centraliza, entre otras, las rutas:

```text
catalog
recipe/{recipeId}
recipe/new
recipe/{recipeId}/edit
recipe/{recipeId}/cook
ingredient-library
custom-ingredient/new
settings
```

Los ViewModels usan `SavedStateHandle` para IDs y estado puntual de navegación. No se transportan agregados Room completos entre pantallas.

La selección desde biblioteca transfiere al editor `id + nombre + unidad sugerida`; el editor crea una fila de borrador y la receta solo se persiste cuando se guarda explícitamente.

## Fotografías

`RecipePhotoStorage` mantiene el ciclo:

```text
selección -> staging -> promoción -> persistencia -> limpieza
```

`LocalRecipePhotoStorage`:

- corrige EXIF;
- limita imágenes a 2048 px;
- comprime a JPEG calidad 85;
- usa almacenamiento privado;
- compensa archivos promovidos si Room falla.

Room persiste rutas relativas permanentes, nunca `Bitmap`, URI externas ni rutas temporales de caché.

## Modo cocina

`CookingModeViewModel` observa una receta existente y mantiene el paso actual mediante `SavedStateHandle`.

La pantalla de cocina:

- muestra un paso cada vez;
- conserva progreso;
- permite consultar ingredientes sin abandonar el paso;
- mantiene la pantalla encendida solo durante la sesión;
- no escribe en Room;
- no crea temporizadores ejecutables ni notificaciones.

## Identidad, tiempo e inyección

- IDs persistentes: UUID almacenados como `String`.
- Timestamps técnicos: milisegundos Unix UTC (`Long`).
- Fechas regulatorias/de revisión: strings ISO cuando corresponde al contrato de catálogo.
- `IdGenerator` y `TimeProvider` son sustituibles en pruebas.
- `AppContainer` realiza inyección manual; no existe Hilt ni otro contenedor DI.

## Red y privacidad

La app no incorpora clientes de red ni permisos de conectividad para su funcionamiento esencial. No existe backend, login, sincronización, analítica ni catálogo remoto.

La distribución futura de actualizaciones sensibles del catálogo requerirá una ADR específica antes de introducir cualquier mecanismo remoto.

## Backup/Restore

Backup/Restore es el siguiente límite de arquitectura de producto, todavía no implementado.

Su diseño deberá preservar como mínimo:

- recetas;
- pasos;
- ingredientes usados;
- identidades personalizadas;
- declaraciones de seguridad personalizadas;
- fotografías;
- versión de formato de backup;
- compatibilidad con futuras versiones Room.

No debe implementarse copiando de forma opaca `recipes.db` sin definir antes formato, validación, estrategia de restauración y manejo de incompatibilidades.

## Un solo módulo

Se mantiene un único módulo Android `app`; los paquetes expresan límites internos y no se crean módulos vacíos para anticipar trabajo futuro.
