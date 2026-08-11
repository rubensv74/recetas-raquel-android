# Arquitectura

## Enfoque local-first

Room es la fuente de verdad de los datos estructurados de la aplicación. Todas las operaciones esenciales se ejecutan en el dispositivo y no dependen de conectividad. `recipes.db` conserva datos entre ejecuciones y expone cambios mediante `Flow`.

La aplicación no usa backend, autenticación, sincronización remota ni permisos de red. Las fotografías se almacenan en el sistema de archivos privado de la aplicación.

## Capas y flujo

- **UI (`ui`, `app`)**: Jetpack Compose representa estados inmutables y emite eventos a ViewModels.
- **Dominio (`domain`)**: modelos, read models, filtros, contratos, casos de uso y reglas independientes del framework Android cuando es posible.
- **Datos (`data`)**: entidades Room, DAO, repositorios, importador del catálogo, mappers y almacenamiento local de fotografías.
- **Backup (`backup`)**: límite arquitectónico reservado para la fase de cierre v1; todavía no implementado.

El flujo habitual es:

```text
UI → ViewModel → Repository / UseCase → DAO → Room
                                      → almacenamiento privado de fotos
```

La UI no accede directamente a DAO ni consume entidades Room.

## Persistencia — Room v6

`RecipeDatabase` usa actualmente **versión 6** con `exportSchema = true` y migraciones explícitas:

```text
1 -> 2
2 -> 3
3 -> 4
4 -> 5
5 -> 6
```

Los esquemas `1.json` a `6.json` permanecen versionados. No se utiliza migración destructiva.

La base contiene 15 entidades agrupables en cuatro áreas:

1. **Recetas**: receta, ingredientes utilizados y pasos.
2. **Catálogo culinario**: categorías, ingredientes, alias, relaciones de linaje y metadatos de catálogo.
3. **Seguridad alimentaria**: grupos, fuentes y relaciones de seguridad.
4. **Personalización y regulación**: ingredientes personalizados, alias/declaraciones personalizadas y snapshots de exenciones regulatorias.

La transacción de guardado de receta sigue siendo la unidad de persistencia del agregado receta.

## Identidad de ingrediente

Un ingrediente utilizado en una receta conserva, cuando procede, uno de estos orígenes:

- `catalogIngredientId`: identidad procedente de la biblioteca maestra;
- `customIngredientId`: identidad personalizada creada por la usuaria;
- ninguno: ingrediente legacy/no vinculado cuando corresponda.

El nombre de una identidad vinculada se trata como identidad, no como texto libre editable. Cantidad, unidad y observaciones pertenecen al uso concreto dentro de la receta y sí son editables.

## Catálogo versionado

El catálogo maestro se distribuye como assets inmutables versionados. La versión activa es **v12**.

El catálogo tiene su propio `catalogVersion`, separado de la versión de Room. Incluye categorías, identidades canónicas, alias, relaciones culinarias, fuentes/grupos de seguridad, relaciones de seguridad y exenciones regulatorias.

Las identidades pueden tener rol:

- `CULINARY`: visibles y seleccionables en la biblioteca normal;
- `REGULATORY_TECHNICAL`: disponibles para resolución regulatoria interna pero ocultas en la biblioteca culinaria.

## Separación de seguridad y regulación

La arquitectura mantiene tres grafos/conceptos independientes:

```text
linaje culinario
!= evidencia de seguridad
!= exención regulatoria
```

Las relaciones de linaje (`VARIANT_OF`, `CUT_OF`, `DERIVED_FROM`, `FORM_OF`) no propagan automáticamente seguridad.

Las observaciones de seguridad se agregan por evidencia explícita. Una exención regulatoria representa únicamente un efecto jurídico condicionado de etiquetado y nunca implica ausencia de alérgeno, ausencia de riesgo ni aptitud clínica.

Los snapshots regulatorios se conservan por versión de catálogo para evitar reescritura silenciosa del historial.

## Ingredientes personalizados

Los ingredientes personalizados viven fuera del catálogo maestro y pueden almacenar:

- identidad y categoría;
- unidad habitual;
- tipo de ingrediente;
- alias;
- notas;
- marca/nombre comercial cuando corresponde;
- composición conocida/desconocida;
- fecha de lectura de etiqueta;
- declaraciones de seguridad aportadas por la usuaria con fuente local explícita.

Los metadatos comerciales solo son válidos para `COMMERCIAL_PRODUCT`.

## Fotografías

`RecipePhotoStorage` abstrae staging, promoción, resolución y eliminación.

1. Android Photo Picker obtiene la imagen sin permiso global de galería.
2. Se corrige EXIF, se limita a 2048 px y se comprime a JPEG calidad 85.
3. El staging usa `cacheDir`.
4. El guardado definitivo usa `filesDir/recipe_photos/...`.
5. Room persiste solo rutas relativas permanentes.
6. Si la escritura de Room falla, el caso de uso compensa eliminando archivos recién promovidos.

Room nunca persiste `Bitmap`, URI externas ni rutas temporales de caché.

## Modo cocina

El modo cocina es de solo lectura. Presenta un paso cada vez, progreso, fotografía opcional, tiempo configurado como referencia y una hoja inferior de ingredientes. El paso actual se conserva en `SavedStateHandle` y la pantalla se mantiene encendida únicamente mientras el modo está visible.

No crea temporizadores ejecutables, servicios, notificaciones ni persistencia propia.

## Inyección y estado

Se mantiene inyección manual mediante `AppContainer`; no existe Hilt ni otro contenedor DI. Los ViewModels reciben dependencias explícitas mediante factories y exponen estado observable con `StateFlow`.

Los IDs son `String` UUID y los timestamps operativos usan milisegundos Unix UTC. Los contratos `IdGenerator` y `TimeProvider` permiten pruebas deterministas.

## Un solo módulo

Se mantiene únicamente el módulo Android `app`; los paquetes expresan límites internos sin multiplicar módulos anticipadamente.

## Siguiente límite arquitectónico

El siguiente bloque de producto es **Backup/Restore**. Su diseño debe preservar el funcionamiento offline y cubrir conjuntamente los datos estructurados de Room y las fotografías privadas. La estrategia de formato, alcance y restauración se decidirá antes de implementar persistencia de backup.