# Especificación de producto

## Problema y usuaria

Las recetas personales pueden quedar dispersas o ser difíciles de localizar durante un servicio. Recetas de Raquel ofrece a una única usuaria un archivo privado, rápido y disponible sin conexión.

## Casos de uso principales

- Crear recetas con nombre, categoría, ingredientes, pasos y fotografías.
- Editar recetas existentes preservando favorito, fecha de creación y fotografías previas.
- Eliminar recetas con confirmación y cascada de ingredientes, pasos y fotografías.
- Encontrar recetas por texto, ingrediente, categoría o favorito.
- Consultar detalle, ingredientes, pasos y fotografías.
- Ejecutar una receta en modo cocina, un paso cada vez, con progreso y acceso rápido a ingredientes.
- Conservar y restaurar el archivo mediante backup futuro.

## Alcance del MVP

Catálogo local, búsqueda, CRUD de recetas, ingredientes/pasos con reordenamiento, validación, detección de cambios sin guardar, favoritos, modo cocina, fotos de portada y por paso, y backup manual futuro.

## Consulta implementada en Sprint 2

El catálogo busca parcialmente por nombre, categoría e ingrediente; combina favoritas y categoría; y ordena por modificación reciente. El detalle muestra metadatos, ingredientes y pasos ordenados y permite cambiar favorito. Se distinguen carga, vacío, sin resultados, error y receta no encontrada.

## Editor implementado en Sprint 3

El editor permite crear y editar recetas en una pantalla única con scroll. Incluye campos obligatorios, ingredientes, pasos, reordenamiento, validación, detección de cambios sin guardar, eliminación y prevención de doble guardado. Los IDs persistentes se preservan.

## Fotografías implementadas en Sprint 4

Las recetas soportan una foto de portada y una foto por paso mediante Android Photo Picker. Las imágenes se corrigen según EXIF, se redimensionan a un máximo de 2048 px, se comprimen a JPEG calidad 85 y se guardan en almacenamiento privado. Room conserva únicamente rutas relativas permanentes. `SaveRecipeUseCase` coordina promoción, persistencia y compensación ante errores.

## Modo cocina implementado en Sprint 5

Desde el detalle de una receta con pasos se puede iniciar **Cocinar**. La experiencia muestra un único paso por pantalla, contador `Paso X de N`, progreso, fotografía del paso cuando exista y el tiempo configurado como referencia. Los ingredientes se consultan en una hoja inferior sin abandonar el paso. El último paso ofrece **Terminar** y vuelve al detalle.

El paso actual se conserva mediante `SavedStateHandle` y la pantalla permanece encendida únicamente mientras el modo cocina está visible. El modo cocina es de solo lectura: no modifica Room, no edita recetas y no crea temporizadores ni notificaciones.

## Fuera de alcance

Cuentas, autenticación, perfiles, colaboración, backend, servicios web, analítica, catálogo público, sincronización, lista de compra y cálculo nutricional. Cámara, filtros de imagen, edición de fotos, múltiples fotos por portada, temporizadores ejecutables, alarmas y control por voz.

## Principios de experiencia

Consulta inmediata, legibilidad, seguridad ante pérdida, simplicidad, navegación previsible y privacidad local. Durante la cocina se priorizan controles grandes, información esencial y mínimo cambio de contexto.
