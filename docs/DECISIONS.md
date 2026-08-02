# Registro de decisiones

## ADR-001 — Kotlin y Jetpack Compose

**Estado:** aceptada. Se usa Kotlin con Compose y Material 3 para una UI declarativa, una sola base de lenguaje y pantallas sin XML. La contrapartida es depender del ciclo de versiones de Compose.

## ADR-002 — Room para persistencia

**Estado:** aceptada para una fase futura. Room aportará consultas observables, integridad y migraciones sobre SQLite, y será la fuente de verdad local. No se añade la dependencia hasta implementar persistencia.

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
