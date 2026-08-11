# Especificación de producto

## Propósito

Recetas de Raquel es una aplicación Android privada y offline para conservar, consultar y cocinar recetas personales de forma simple y segura.

La aplicación está diseñada para una única usuaria y prioriza:

- funcionamiento sin conexión;
- privacidad local;
- consulta rápida;
- edición sencilla;
- seguridad ante errores de datos;
- información alimentaria prudente y trazable;
- conservación del archivo personal de recetas.

## Casos de uso principales

- Crear, editar y eliminar recetas.
- Añadir ingredientes, cantidades, unidades y observaciones.
- Añadir y reordenar pasos.
- Incorporar fotografía de portada y fotografías por paso.
- Buscar recetas por texto, ingrediente, categoría o favorito.
- Consultar recetas y ejecutar un modo cocina guiado.
- Elegir ingredientes desde una biblioteca maestra amplia.
- Crear ingredientes personalizados cuando no exista una identidad adecuada.
- Consultar información de seguridad y regulación asociada a ingredientes.
- Ver alertas consolidadas en una receta sin convertirlas en diagnósticos clínicos.
- Reutilizar ingredientes frecuentes derivados de recetas ya guardadas.
- Conservar y restaurar el archivo mediante Backup/Restore en la fase de cierre v1.

## Estado funcional actual

### Catálogo y consulta de recetas

El catálogo permite búsqueda parcial, filtros por categoría/favorito e identificación por ingrediente. El detalle muestra metadatos, ingredientes, pasos, fotografías e información alimentaria agregada.

### Editor de recetas

El editor permite creación y edición en una pantalla con scroll, validación, detección de cambios sin guardar, reordenamiento, eliminación y prevención de doble guardado.

Una identidad vinculada a la biblioteca o a un ingrediente personalizado mantiene bloqueado su nombre canónico, mientras cantidad, unidad y observaciones pertenecen al uso concreto dentro de la receta.

### Fotografías

Android Photo Picker permite seleccionar imágenes sin permiso global de galería. Las fotografías se corrigen por EXIF, se redimensionan y comprimen antes de almacenarse en espacio privado de la aplicación.

### Modo cocina

Desde el detalle puede iniciarse **Cocinar**. Se presenta un paso cada vez, progreso, fotografía opcional, tiempo configurado como referencia y acceso rápido a ingredientes. No existen temporizadores ejecutables ni notificaciones.

### Biblioteca de ingredientes

La biblioteca maestra incluye categorías, búsqueda, alias y detalle de identidad. El catálogo está versionado y la versión activa es v12.

Las identidades se distinguen entre:

- `CULINARY`: visibles/seleccionables;
- `REGULATORY_TECHNICAL`: internas, no seleccionables en la biblioteca normal.

La sección **Frecuentes** se deriva de recetas guardadas. No se implementa **Recientes** porque el modelo actual no dispone de un timestamp fiable de selección.

### Ingredientes personalizados

El formulario permite registrar identidad, categoría, unidad, alias, notas, composición conocida/desconocida, datos de producto comercial cuando aplican y declaraciones de seguridad aportadas por la usuaria.

### Seguridad alimentaria

La app conserva información estructurada sobre grupos, relaciones y fuentes. Las alertas se presentan como información de riesgo y revisión, no como diagnóstico ni certificación de aptitud.

Se mantienen separados:

```text
linaje culinario
seguridad alimentaria
regulación de etiquetado
```

Una exención regulatoria nunca se interpreta como ausencia de alérgeno ni elimina una observación de seguridad.

## Alcance de la primera versión estable

La v1 se considerará cerrada cuando se cumplan conjuntamente:

1. funciones actuales integradas en `master`;
2. documentación técnica coherente con la implementación;
3. aceptación básica en dispositivo real;
4. Backup/Restore de datos personales y fotografías;
5. corrección de defectos de aceptación bloqueantes;
6. build/release y regresión final verdes.

El rediseño visual premium no es condición de cierre de v1 si la experiencia actual resulta suficientemente clara en la prueba real.

## Fuera de alcance de v1

- cuentas, autenticación y perfiles;
- colaboración multiusuario;
- backend o servicios web;
- sincronización cloud/GitHub;
- analítica;
- catálogo público;
- lista de compra;
- cálculo nutricional;
- diagnóstico médico;
- cámara propia o edición avanzada de fotos;
- temporizadores ejecutables, alarmas y notificaciones de cocina;
- control por voz.

## Principios de experiencia

- La identidad del ingrediente no debe confundirse con cantidad/unidad de una receta.
- Las advertencias de seguridad deben ser visibles, prudentes y comprensibles.
- La información regulatoria debe mantenerse separada de la seguridad clínica.
- La usuaria no debe depender de formularios manuales para ingredientes comunes.
- La aplicación debe conservar los datos personales sin exigir conectividad.
- Durante el modo cocina se priorizan controles grandes, información esencial y mínimo cambio de contexto.

## Próxima capacidad bloqueante para cierre

**Backup/Restore** es la siguiente capacidad funcional prioritaria porque la aplicación almacena ya información personal de valor que no debe depender únicamente de una instalación concreta del dispositivo.