# Registro de decisiones

## ADR-001 — Kotlin, Compose y Material 3

**Aceptada.** Una base Kotlin y UI declarativa sin pantallas XML.

## ADR-002 — Room como fuente local

**Implementada.** Room 2.8.4, esquema v2 exportado, Flow, integridad y transacciones. La evolución v1 -> v2 se realiza mediante migración explícita no destructiva.

## ADR-003 — Aplicación offline

**Aceptada.** Casos principales sin red para disponibilidad y privacidad.

## ADR-004 — Un único módulo

**Aceptada.** Un solo `app`; límites mediante paquetes.

## ADR-005 — Inyección manual

**Aceptada.** `AppContainer` y factories explícitas, sin Hilt/Koin/Dagger.

## ADR-006 — GitHub solo como posible backup

**Aceptada como dirección futura.** Nunca será la base de datos; no existe Sync.

## ADR-007 — Sin backend en MVP

**Aceptada.** No hay cuentas ni infraestructura remota.

## ADR-008 — Navegación manual provisional

**Reemplazada en Sprint 2.** Fue suficiente para Sprint 0, pero detalle con argumento y back stack justifican Navigation Compose.

## ADR-009 — KSP y esquemas

**Aceptada.** Room Compiler usa KSP2 y exporta a `app/schemas`.

## ADR-010 — UUID y timestamps UTC

**Aceptada.** IDs `String`, timestamps Unix UTC y proveedores testeables.

## ADR-011 — Agregado transaccional

**Aceptada.** Receta e hijos se guardan como unidad y se eliminan en cascada.

## ADR-012 — Navigation Compose y ViewModels

**Aceptada.** Rutas de aplicación mediante Navigation Compose; estados `StateFlow`; IDs desde `SavedStateHandle`. Navegación de un solo uso con `SharedFlow` cuando procede.

## ADR-013 — Read model y consulta SQL

**Aceptada.** `RecipeSummary` evita cargas completas. SQL parametrizado con `EXISTS` busca ingredientes sin duplicados y combina filtros.

## ADR-014 — Datos demo por source set

**Aceptada.** Cinco recetas deterministas viven solo en debug, bajo acción explícita e idempotente. Release devuelve controlador nulo.

## ADR-015 — Editor de recetas sin esquema nuevo

**Aceptada en Sprint 3.** El editor funcionó inicialmente con el esquema v1. La evolución posterior a Room v2 conserva compatibilidad con ese editor durante la transición al flujo de biblioteca.

## ADR-016 — Strings URI en el domain layer

**Aceptada en agosto de 2026.** `RecipePhotoStorage` usa `String` en lugar de `android.net.Uri` para mantener tipos Android fuera de la capa de dominio.

## ADR-017 — Modo cocina como estado efímero de UI

**Aceptada en Sprint 5.** El modo cocina no crea entidades ni cambia Room. La receta se observa mediante `RecipeRepository`, el paso actual se conserva en `SavedStateHandle` y los ingredientes se muestran sin abandonar la ruta. Mantener la pantalla encendida se resuelve en Compose con `LocalView.keepScreenOn` y `DisposableEffect`, restaurando el valor anterior al salir. Los temporizadores ejecutables quedan para una fase posterior.

## ADR-018 — Catálogo de ingredientes versionado independiente de Room

**Aceptada en el programa de biblioteca de ingredientes.** El catálogo distribuido vive bajo `app/src/main/assets/ingredient-catalog/vN/` y dispone de `catalogVersion` propio. Room conserva una copia indexable para ejecución offline, pero `catalogVersion` no se confunde con la versión del esquema de base de datos.

## ADR-019 — Validar antes de importar y no degradar datos

**Aceptada.** El importer lee el bundle completo, ejecuta validación estructural y solo entonces inicia la sustitución lógica dentro de una transacción Room. Un bundle inválido no modifica el catálogo previamente importado. Si la base contiene una versión de catálogo superior a la del APK, no se fuerza un downgrade.

## ADR-020 — Datos maestros referenciados se desactivan, no se destruyen

**Aceptada.** Categorías, grupos de seguridad e ingredientes de catálogo pueden estar referenciados por ingredientes personalizados o recetas. Las actualizaciones de catálogo desactivan registros ausentes en lugar de eliminarlos físicamente cuando una eliminación podría romper trazabilidad o integridad referencial.

## ADR-021 — Gson como parser del catálogo v1

**Aceptada.** Se usa Gson como dependencia focalizada para JSON estructurado. Evita parsing manual y no introduce el plugin/runtime de Kotlin Serialization en esta fase, especialmente después de la incompatibilidad binaria observada en el tooling de pruebas de migración de Room. La elección puede revisarse si cambian las restricciones técnicas.

## ADR-022 — Infraestructura no equivale a cobertura de catálogo

**Aceptada.** `releaseStatus = INFRASTRUCTURE` permite validar parser, manifest, importador y transacciones con cero ingredientes canónicos y cero relaciones de seguridad. Solo `PRODUCTION_CANDIDATE` activa los mínimos de cobertura y los gates regulatorios definidos por el validador.

## ADR-023 — Las versiones del catálogo son inmutables

**Aceptada.** Una vez creado un directorio `ingredient-catalog/vN/`, su contenido no se reutiliza para representar una versión funcionalmente distinta. El bundle de infraestructura permanece como v1 y el primer seed regulatorio se publica como v2. El lector apunta a la versión activa más reciente y `catalogVersion` debe coincidir con esa evolución, de modo que Room pueda detectar actualizaciones reales e impedir que contenido nuevo sea tratado erróneamente como `AlreadyCurrent`.

## ADR-024 — El catálogo escalable admite shards manteniendo compatibilidad

**Aceptada.** Desde `catalog schemaVersion = 2`, ingredientes y alias pueden declararse como listas ordenadas de shards en el manifiesto. El lector conserva compatibilidad explícita con los catálogos schema-v1 de fichero único. Los shards son una unidad de revisión y organización del contenido, no una relajación de las reglas de validación: una vez concatenados, el bundle completo sigue pasando los mismos controles de IDs, nombres normalizados, referencias, recuentos y evidencia antes de importarse transaccionalmente.

## ADR-025 — Grafo de linaje de ingredientes separado de seguridad alimentaria

**Aceptada.** Se adopta un grafo no clínico de linaje entre `CatalogIngredient` con relaciones `VARIANT_OF`, `CUT_OF`, `DERIVED_FROM` y `FORM_OF`. Room v3 persiste estas relaciones en una tabla independiente. El grafo debe ser acíclico, no admite auto-relaciones ni duplicados equivalentes y puede conservar metadatos de revisión de identidad. Ninguna arista de linaje propaga, crea ni hereda relaciones de seguridad alimentaria. El grafo de seguridad sigue exigiendo evidencia propia y trazable. La composición de productos/ingredientes compuestos permanece fuera de alcance hasta una decisión arquitectónica específica.

## ADR-026 — Exenciones regulatorias en capa independiente

**Aceptada — Opción B.** Las exenciones legales no se representan como ausencia de una alerta ni como un tipo de relación clínica. Se crea una capa estructurada independiente `RegulatoryExemption`, relacionada con ingrediente, grupo regulatorio y fuente oficial. Debe conservar jurisdicción, efecto regulatorio, condiciones, fuente, fechas de vigencia cuando puedan establecerse sin inventarlas, fecha de revisión, notas y estado activo. Una exención regulatoria nunca implica `seguro`, `apto`, `sin riesgo` ni otra conclusión clínica. Room v4 introduce la tabla `regulatory_exemptions`; el catálogo activo v6 permanece inmutable y los datos regulatorios reales se incorporarán solo en una versión posterior tras validar el nuevo esquema. Ver `docs/ingredient-library/19_ARCHITECTURAL_DECISION_REGULATORY_EXEMPTIONS.md`.

## ADR-027 — Agregación de seguridad de una receta

**ABIERTA — decisión requerida antes de Fase 7.** Debe decidirse cómo resumir un mismo grupo de seguridad cuando distintas líneas de ingrediente producen relaciones heterogéneas. Se comparan: **A)** una tarjeta de grupo con todos los estados independientes; **B)** un estado principal determinista de presentación conservando todas las observaciones y trazabilidad; **C)** puntuación/semáforo de riesgo. Se recomienda **B**. Ninguna opción puede utilizar exenciones regulatorias para suprimir observaciones de seguridad, convertir reactividad cruzada en presencia confirmada ni emitir una conclusión clínica. Ver `docs/ingredient-library/38_ARCHITECTURAL_DECISION_RECIPE_SAFETY_AGGREGATION.md`.
