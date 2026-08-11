# 31 — FASE 5: IMPLEMENTACIÓN DE LA BIBLIOTECA DE INGREDIENTES

**Estado:** DISEÑO DE IMPLEMENTACIÓN CERRADO — ejecución incremental en curso  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## 1. Objetivo

Convertir el catálogo versionado de ingredientes en la vía principal para añadir ingredientes a una receta, manteniendo la introducción manual como opción secundaria y sin alterar las reglas de seguridad alimentaria ya congeladas.

## 2. Alcance de esta fase

La fase se implementará en tres bloques verificables:

### Bloque A — Consulta del catálogo

- API de dominio para categorías e ingredientes de catálogo.
- Búsqueda determinista por nombre canónico y alias.
- Orden de coincidencia: exacta → prefijo → contiene.
- Normalización existente sin fuzzy matching.
- Filtro opcional por categoría.
- Resumen explícito de la información registrada, sin inferencias clínicas.
- Sin cambios de esquema Room.

### Bloque B — Pantalla Biblioteca de ingredientes

- Nueva ruta de navegación.
- Campo de búsqueda con foco inicial.
- Categorías navegables.
- Resultados con nombre, categoría y estado informativo.
- Acción secundaria para mantener la introducción manual durante la transición hacia la fase de ingredientes personalizados.
- Estados de carga, vacío y error.

### Bloque C — Integración con el editor de recetas

- `Añadir ingrediente` abrirá la biblioteca por defecto.
- La selección devolverá la identidad de catálogo, nombre canónico y unidad predeterminada al editor.
- Se persistirá `catalogIngredientId` en `RecipeIngredient`.
- La identidad canónica de un ingrediente seleccionado de biblioteca no se editará como texto libre dentro de la fila del editor; cantidad, unidad y notas sí seguirán siendo editables.
- Los ingredientes históricos o introducidos manualmente conservarán su vía `customIngredientId`/compatibilidad existente.
- No se realizará conversión silenciosa entre texto libre e identidades de catálogo.

## 3. Regla de búsqueda

La consulta usa `IngredientTextNormalizer` y no incorpora coincidencia aproximada.

Orden:

```text
0  nombre canónico exacto o alias exacto
1  nombre canónico o alias que empiece por la consulta
2  nombre canónico o alias que contenga la consulta
```

A igualdad de rango se ordena de forma estable por nombre canónico.

No se utilizará distancia de edición, IA, similitud semántica ni reglas de familia para decidir identidades.

## 4. Estado informativo en resultados

El listado distinguirá al menos:

```text
SAFETY_RELATIONS_RECORDED
REGULATORY_EXEMPTION_RECORDED
NO_DIRECT_SAFETY_RELATION_RECORDED
```

Estos estados describen únicamente qué datos existen en el catálogo. No equivalen a una evaluación de aptitud o riesgo individual.

Especialmente:

- una exención regulatoria no es una relación clínica;
- la ausencia de una relación directa no significa ausencia de alérgeno;
- el linaje no propaga seguridad;
- no se mostrará `seguro`, `sin riesgo`, `apto para alérgicos` ni equivalentes.

## 5. Integridad de identidad en el editor

Una fila procedente del catálogo debe conservar `catalogIngredientId` hasta que se elimine o sustituya explícitamente.

No se permitirá modificar solo el nombre y mantener detrás un identificador de catálogo diferente, porque produciría una discrepancia peligrosa entre el texto visible y la identidad usada por las reglas de seguridad.

Por ello el nombre canónico será de solo lectura para filas de catálogo. El usuario podrá eliminar la fila y seleccionar otra identidad o utilizar la vía manual.

## 6. Navegación y estado del editor

La biblioteca se abrirá encima del editor en la pila de navegación. El `ViewModel` del editor permanecerá asociado a su entrada y conservará los cambios no guardados.

La selección se devolverá mediante el `SavedStateHandle` de la entrada anterior usando valores primitivos. No se serializarán modelos Room ni objetos de infraestructura entre pantallas.

## 7. Pruebas mínimas

El bloque de datos debe probar:

- importación del catálogo activo;
- 20 categorías;
- coincidencia exacta antes que prefijo/contiene;
- búsqueda sin acentos conforme al normalizador existente;
- ausencia de fuzzy matching;
- filtro por categoría;
- identificación separada de relaciones de seguridad y exenciones regulatorias.

La integración del editor debe probar que una selección de catálogo conserva `catalogIngredientId` en el `RecipeDraft` y que el flujo manual no crea una asociación de catálogo implícita.

## 8. Gate

Cada bloque que modifica código se valida mediante `.github/workflows/android-ci.yml`.

Room debe permanecer en versión 4 con exclusivamente `1.json` a `4.json` durante toda la fase 5.
