# Registro de decisiones

## ADR-001 — Kotlin y Jetpack Compose

**Estado:** aceptada. Se usa Kotlin con Compose y Material 3 para una UI declarativa, una sola base de lenguaje y pantallas sin XML. La contrapartida es depender del ciclo de versiones de Compose.

## ADR-002 — Room para persistencia

**Estado:** implementada en Sprint 1. Room 2.8.4 proporciona la fuente de verdad local, consultas con Flow, integridad referencial y transacciones. El esquema inicial es v1 y se exporta; todavía no existen migraciones.

## ADR-003 — Aplicación offline

**Estado:** aceptada. Todo caso de uso principal funcionará sin red. Mejora disponibilidad y privacidad, pero exige diseñar cuidadosamente backup y restauración.

## ADR-004 — Un único módulo

**Estado:** aceptada. Un solo módulo `app` reduce configuración y tiempos de coordinación en un producto pequeño. Los límites se mantienen mediante paquetes; modularizar requerirá una necesidad demostrable.

## ADR-005 — Inyección manual

**Estado:** aceptada. La raíz de aplicación construirá repositorios y casos de uso explícitamente. Evita Hilt y coste adicional; se revisará si el grafo de dependencias deja de ser sencillo.

## ADR-006 — GitHub como backup, no base de datos

**Estado:** aceptada como dirección futura. GitHub solo podría almacenar copias exportadas mediante una acción explícita y segura. Room conservará la autoridad local. No se implementa GitHub Sync en el MVP.

## ADR-007 — Sin backend en el MVP

**Estado:** aceptada. Una única usuaria y el requisito offline no justifican infraestructura remota, cuentas ni costes operativos. Las funciones que exijan backend quedan excluidas.

## ADR-008 — Navegación mínima sin dependencia adicional

**Estado:** aceptada para Sprint 0. Dos destinos activos se coordinan con estado guardable y `BackHandler`. Navigation Compose se evaluará cuando existan rutas con argumentos o una pila real.

## ADR-009 — KSP y exportación de esquemas

**Estado:** aceptada. Room Compiler se ejecuta con KSP2 y el plugin oficial de Room exporta esquemas reproducibles a `app/schemas`. Se evita kapt y se conserva sin cambios el toolchain global.

## ADR-010 — UUID y timestamps UTC

**Estado:** aceptada. Los IDs son UUID almacenados como `String`; `createdAt` y `updatedAt` son milisegundos Unix UTC. `IdGenerator` y `TimeProvider` hacen ambas decisiones deterministas en pruebas.

## ADR-011 — Agregado de receta transaccional

**Estado:** aceptada. Receta, ingredientes y pasos se guardan como una unidad: los hijos se reemplazan dentro de la misma transacción y se eliminan en cascada con la receta. El coste de reemplazar listas completas es aceptable para el tamaño previsto y evita estados parciales.
