# Plan de sprints

## Sprint 0 — Fundación

Proyecto compilable, paquete definitivo, Compose/Material 3, Activity única, navegación básica Home–Ajustes, placeholders y documentación inicial. Sin persistencia ni CRUD.

## Sprint 1 — Dominio y persistencia (implementado)

Modelos y validaciones, esquema Room v1, DAO transaccional, repositorio local, mappers e inyección manual. Incluye pruebas unitarias e instrumentadas; no existe migración porque esta es la versión inicial.

## Sprint 2 — Catálogo y edición

Implementar catálogo, estados vacío/error, creación y edición de recetas, ingredientes y pasos. Incorporar navegación con argumentos cuando sea necesaria.

## Sprint 3 — Consulta y modo cocina

Detalle de receta, búsqueda y filtros básicos, etiquetas, modo cocina accesible y comportamiento de pantalla adecuado al servicio.

## Sprint 4 — Fotos, backup y robustez

Gestión segura de fotos locales, exportación/importación manual versionada, restauración, pruebas de migración y endurecimiento de accesibilidad y rendimiento.

La sincronización con GitHub queda fuera de estos sprints hasta contar con un diseño específico de seguridad, conflictos y consentimiento.
