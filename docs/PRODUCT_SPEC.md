# Especificación de producto

## Problema y usuaria

Las recetas personales pueden quedar dispersas o ser difíciles de localizar durante un servicio. Recetas de Raquel ofrece a una única usuaria un archivo privado, rápido y disponible sin conexión.

## Casos de uso principales

- Crear recetas con nombre, categoría, ingredientes, pasos y fotografías.
- Editar recetas existentes preservando favorito, fecha de creación y fotografías previas.
- Eliminar recetas con confirmación y cascada de ingredientes, pasos y fotografías.
- Encontrar recetas por texto, ingrediente, categoría o favorito.
- Consultar detalle, ingredientes, pasos y fotografías durante la cocina.
- Conservar y restaurar el archivo mediante backup futuro.

## Alcance del MVP

Catálogo local, búsqueda, CRUD de recetas, ingredientes/pasos con reordenamiento, validación, detección de cambios sin guardar, favoritos, modo cocina, fotos de portada y por paso, y backup manual futuro.

## Consulta implementada en Sprint 2

El catálogo busca parcialmente por nombre, categoría e ingrediente; combina favoritas y categoría; y ordena por modificación reciente. El detalle muestra metadatos, ingredientes y pasos ordenados y permite cambiar favorito. Se distinguen carga, vacío, sin resultados, error y receta no encontrada.

## Editor implementado en Sprint 3

El editor permite crear y editar recetas en una pantalla única con scroll. Incluye: campos obligatorios (nombre), ingredientes con cantidad/unidad/nombre/observaciones, pasos con instrucción y temporizador, reordenamiento con ↑/↓, validación de nombre e ingrediente parcial, detección de cambios sin guardar con confirmación al retroceder, eliminación con doble confirmación, y prevención de doble guardado. La navegación tras crear/editar/eliminar se gestiona con `SharedFlow` de un solo uso. Los IDs de ingredientes/pasos existentes se conservan; los nuevos se generan con `IdGenerator`.

## Fotografías implementadas en Sprint 4

Las recetas soportan una foto de portada y una foto por paso.

- **Selección**: Android Photo Picker (`PickVisualMedia`) sin permisos de cámara ni galería. Solo imágenes (`ImageOnly`).
- **Procesamiento**: Orientación EXIF corregida, redimensionado (máx. 2048px lado mayor, proporción conservada), compresión JPEG (calidad 85), eliminación de metadatos EXIF.
- **Almacenamiento**: Archivos JPEG privados en `filesDir/recipe_photos/{recipeId}/`. Rutas relativas en Room.
- **Ciclo de vida**: Staging en `cacheDir` → promoción tras guardado exitoso → limpieza de temporales. Fallos de limpieza silenciosos.
- **Editor**: Previsualización con Coil (`AsyncImage`), selección, cambio, eliminación con confirmación de estado anterior, indicador de procesamiento.
- **Catálogo y detalle**: Portadas mostradas con Coil desde archivos locales.

## Fuera de alcance

Cuentas, autenticación, perfiles, colaboración, backend, servicios web, analítica, catálogo público, sincronización, lista de compra y cálculo nutricional. Cámaras, filtros de imagen, edición de fotos, múltiples fotos por portada.

## Principios de experiencia

Consulta inmediata, legibilidad, seguridad ante pérdida, simplicidad, navegación previsible y privacidad local.
