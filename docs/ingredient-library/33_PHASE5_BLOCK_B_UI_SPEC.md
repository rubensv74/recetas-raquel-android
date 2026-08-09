# 33 — FASE 5 / BLOQUE B: PANTALLA BIBLIOTECA DE INGREDIENTES

**Estado:** ESPECIFICACIÓN DE IMPLEMENTACIÓN CERRADA  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## Objetivo

Crear la pantalla que convierte el catálogo en la vía principal de selección de ingredientes desde el editor de recetas.

## Comportamiento

La pantalla tendrá:

- barra superior con volver;
- título `Biblioteca de ingredientes`;
- buscador con foco inicial;
- selector horizontal de categorías;
- resultados en lista;
- acción secundaria `Introducir manualmente`;
- estados de carga, vacío y error.

Con consulta y categoría vacías no se mostrará una lista arbitraria de cientos de elementos. La pantalla invitará a buscar o elegir una categoría. Al elegir una categoría sin escribir texto se mostrarán los ingredientes de esa categoría, ordenados alfabéticamente.

## Fila de resultado

Cada resultado mostrará:

- nombre canónico;
- categoría;
- unidad predeterminada cuando exista;
- indicador textual no basado solo en color.

Mensajes previstos:

```text
Información de seguridad registrada
Información regulatoria específica
La información disponible puede ser incompleta
```

El segundo mensaje no significa seguridad clínica; identifica únicamente que existe una regla regulatoria estructurada. El tercero evita interpretar la ausencia de relaciones directas como ausencia de riesgo.

## Selección

Al pulsar un resultado se devolverán al editor únicamente valores primitivos:

```text
ingredientId
canonicalName
defaultUnit
```

La pantalla no modifica directamente la receta ni persiste una fila por sí misma.

## Introducción manual

Mientras no esté implementado el formulario completo de ingredientes personalizados de la fase 6, `Introducir manualmente` conservará el comportamiento de compatibilidad existente: crear una fila libre que al guardar se persiste como ingrediente personalizado de compatibilidad.

No se realizará búsqueda silenciosa para intentar convertir esa fila libre en una identidad de catálogo.
