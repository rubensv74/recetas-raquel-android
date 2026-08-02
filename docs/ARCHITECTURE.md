# Arquitectura

## Enfoque local-first

Room es la fuente de verdad de la aplicación. Todas las operaciones esenciales se ejecutan en el dispositivo y no dependen de conectividad. La base `recipes.db` conserva los datos entre ejecuciones y expone cambios mediante `Flow`.

## Capas

- **UI (`ui`, `app`)**: Compose representa estado y emite eventos. En Sprint 1 no consume aún el catálogo.
- **Dominio (`domain`)**: modelos, borradores, contrato `RecipeRepository` y validaciones sin anotaciones ni dependencias de Room.
- **Datos (`data`)**: entidades, relaciones, DAO, mappers e implementación `LocalRecipeRepository`.
- **Backup (`backup`)**: límite reservado para una fase futura; no está implementado.

El flujo previsto es UI → repositorio → DAO → Room/SQLite. La UI no recibe entidades ni accede al DAO. Los mappers concentran todas las conversiones entre persistencia y dominio.

## Flujo de datos

Las lecturas observables usan `Flow`; las operaciones se expresan como funciones `suspend`. Room planifica el acceso fuera del hilo principal y no se habilita `allowMainThreadQueries()`. Los fallos de validación y SQLite se devuelven como `Result` en las escrituras del repositorio.

## Persistencia y transacciones

`RecipeDatabase` tiene esquema versión 1 con `exportSchema = true`; el plugin de Room exporta el JSON versionado a `app/schemas`. `saveRecipeWithDetails` guarda la receta y sustituye ingredientes y pasos en una única transacción. Las claves foráneas eliminan hijos en cascada. No hay migraciones en la versión inicial y no se usa migración destructiva.

Los ingredientes y pasos poseen índice `(recipeId, sortOrder)`. Como `@Relation` no garantiza orden, los mappers ordenan explícitamente por `sortOrder` antes de exponer el dominio.

## Identidad y tiempo

Los IDs se generan como UUID y se almacenan como `String`, facilitando futuros backups e importaciones. `IdGenerator` permite sustituir la generación en pruebas. `createdAt` y `updatedAt` son milisegundos Unix UTC obtenidos mediante `TimeProvider`; una actualización conserva `createdAt` y renueva `updatedAt`.

## Inyección manual y ciclo de vida

`RecetasRaquelApplication` crea un único `AppContainer` por proceso. El contenedor mantiene una sola instancia de `RecipeDatabase` y expone `RecipeRepository`, `IdGenerator` y `TimeProvider`. No hay contenedor global mutable ni framework de inyección.

## Un solo módulo

El proyecto mantiene únicamente `app`. Los paquetes expresan los límites sin añadir coste de módulos Gradle; se reconsiderará solo ante una necesidad demostrable.

## Backup y sincronización futura

Un futuro backup será manual, versionado y validado. GitHub solo podría ser un destino opcional de copias, nunca la base de datos. No hay red, backend, cola de sincronización ni backup en Sprint 1.
