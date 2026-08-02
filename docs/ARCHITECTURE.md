# Arquitectura

## Enfoque local-first

Todas las operaciones esenciales se ejecutarán en el dispositivo y no dependerán de conectividad. Room será la futura fuente de verdad: la UI observará datos locales y las escrituras se confirmarán localmente antes de cualquier mecanismo opcional de backup.

## Capas

- **UI (`ui`, `app`)**: Compose representa estado, emite eventos y coordina navegación. ViewModels se incorporarán cuando existan casos de uso con estado real.
- **Dominio (`domain`)**: modelos, contratos de repositorio y validaciones independientes de la interfaz y de Room.
- **Datos (`data`)**: entidades y DAO de Room, mappers y repositorios que implementan los contratos del dominio.
- **Backup (`backup`)**: importación y exportación explícitas, aisladas de la persistencia ordinaria.

Las dependencias apuntarán desde UI hacia dominio y desde datos hacia los contratos de dominio. La raíz `app` compondrá manualmente las implementaciones.

## Flujo de datos

Se usará flujo unidireccional: la UI envía eventos, el controlador o ViewModel valida y solicita operaciones al dominio, el repositorio actualiza Room y un flujo observable produce un nuevo estado inmutable para la UI. Los errores se modelarán como estado o eventos explícitos.

## Un solo módulo

El tamaño, la distribución privada y la única usuaria no justifican aún el coste de módulos Gradle adicionales. Los límites se expresan mediante paquetes y contratos Kotlin. Se reconsiderará solo si aparecen tiempos de compilación, equipos o componentes reutilizables que lo requieran.

## Navegación del Sprint 0

Home y Ajustes usan un contenedor de estado guardable y el botón atrás del sistema. Las pantallas restantes son placeholders no conectados. Se valorará Navigation Compose cuando rutas con argumentos y una pila más profunda lo justifiquen.

## Backup y sincronización futura

El primer mecanismo será un backup manual, versionado y validado, exportado mediante APIs del sistema sin secretos. Una restauración se validará antes de aplicar cambios y deberá definir una política ante conflictos. GitHub podría actuar más adelante como destino opcional de copias, nunca como base de datos ni fuente de verdad. Cualquier sincronización futura consumirá una cola persistente y se diseñará en un sprint y ADR propios; no existe en el MVP inicial.
