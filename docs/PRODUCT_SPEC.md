# Especificación de producto

## Problema y usuaria

Las recetas personales pueden quedar dispersas o ser difíciles de localizar durante un servicio. Recetas de Raquel ofrece a una única usuaria un archivo privado, rápido y disponible sin conexión.

## Casos de uso principales

- Crear recetas con nombre, categoría, ingredientes y pasos.
- Editar recetas existentes preservando favorito y fecha de creación.
- Eliminar recetas con confirmación y cascada de ingredientes/pasos.
- Encontrar recetas por texto, ingrediente, categoría o favorito.
- Consultar detalle, ingredientes y pasos durante la cocina.
- Conservar y restaurar el archivo mediante backup futuro.

## Alcance del MVP

Catálogo local, búsqueda, CRUD de recetas, ingredientes/pasos con reordenamiento, validación, detección de cambios sin guardar, favoritos, modo cocina y backup manual futuro.

## Consulta implementada en Sprint 2

El catálogo busca parcialmente por nombre, categoría e ingrediente; combina favoritas y categoría; y ordena por modificación reciente. El detalle muestra metadatos, ingredientes y pasos ordenados y permite cambiar favorito. Se distinguen carga, vacío, sin resultados, error y receta no encontrada.

## Editor implementado en Sprint 3

El editor permite crear y editar recetas en una pantalla única con scroll. Incluye: campos obligatorios (nombre), ingredientes con cantidad/unidad/nombre/observaciones, pasos con instrucción y temporizador, reordenamiento con ↑/↓, validación de nombre e ingrediente parcial, detección de cambios sin guardar con confirmación al retroceder, eliminación con doble confirmación, y prevención de doble guardado. La navegación tras crear/editar/eliminar se gestiona con `SharedFlow` de un solo uso. Los IDs de ingredientes/pasos existentes se conservan; los nuevos se generan con `IdGenerator`.

## Fuera de alcance

Cuentas, autenticación, perfiles, colaboración, backend, servicios web, analítica, catálogo público, sincronización, lista de compra y cálculo nutricional.

## Principios de experiencia

Consulta inmediata, legibilidad, seguridad ante pérdida, simplicidad, navegación previsible y privacidad local.
