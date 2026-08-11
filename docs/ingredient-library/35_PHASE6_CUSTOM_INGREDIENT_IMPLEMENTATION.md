# 35 — FASE 6: INGREDIENTES PERSONALIZADOS

**Estado:** DISEÑO DE IMPLEMENTACIÓN CERRADO — ejecución pendiente del gate de Fase 5  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## 1. Objetivo

Sustituir la introducción manual de compatibilidad por un formulario explícito de ingrediente personalizado, manteniendo completamente separadas las identidades del catálogo maestro y las declaraciones creadas por la persona usuaria.

La fase no convierte texto libre en identidades de catálogo ni realiza inferencias de seguridad.

## 2. Base de datos existente

Room v4 ya contiene las estructuras necesarias:

- `custom_ingredients`;
- `custom_ingredient_aliases`;
- `custom_ingredient_safety_relations`;
- `ingredient_categories`;
- `food_safety_groups`;
- `safety_sources`.

Por tanto, la Fase 6 no requiere una migración Room ni un schema `5.json` para el alcance definido aquí.

## 3. Tipos de ingrediente personalizado

Se mantienen los tipos ya aprobados:

```text
SIMPLE
COMPOUND
COMMERCIAL_PRODUCT
```

No se introduce un grafo de composición. `compositionKnown` describe únicamente si la composición declarada por el usuario se considera conocida o desconocida. La descomposición estructurada de productos compuestos permanece fuera de alcance y requerirá una decisión arquitectónica específica si llega a ser necesaria.

## 4. Campos del formulario

Campos principales:

- nombre — obligatorio;
- tipo — obligatorio;
- categoría — opcional;
- unidad habitual — opcional;
- alias — opcionales;
- notas — opcionales;
- composición conocida/desconocida — obligatoria como estado explícito.

Campos adicionales para producto comercial:

- marca — opcional;
- nombre comercial — opcional;
- fecha de lectura de etiqueta — opcional.

## 5. Información de seguridad declarada

El formulario permitirá registrar cero o más relaciones con grupos de seguridad existentes.

Cada relación deberá especificar como mínimo:

```text
grupo
relationType
evidenceLevel
sourceId
reviewedAt
```

Reglas:

- la persona usuaria solo podrá generar evidencia `USER_DECLARED` o `UNVERIFIED`;
- no podrá autoasignarse `EU_LEGAL`, `OFFICIAL_SCIENTIFIC` ni otros niveles oficiales;
- no se heredarán relaciones del catálogo por similitud, nombre, categoría o linaje;
- una composición desconocida deberá permanecer identificable como información que requiere revisión posterior;
- la ausencia de relaciones declaradas no se interpretará como ausencia de alérgenos o riesgo.

Para declaraciones locales se reutilizará una fuente local explícita y trazable; no se falsificará una fuente oficial.

## 6. Flujo de navegación

```text
Editor de receta
  -> Añadir ingrediente
  -> Biblioteca de ingredientes
  -> Introducir manualmente
  -> Nuevo ingrediente personalizado
  -> Guardar ingrediente personalizado
  -> volver al editor con customIngredientId
```

El formulario crea primero la identidad personalizada. El editor recibe únicamente la identidad necesaria para crear el uso en receta.

No se serializan entidades Room completas entre destinos.

## 7. Integridad de identidad

Una fila de receta originada desde un ingrediente personalizado conservará `customIngredientId`.

Se mantiene el invariante:

```text
catalogIngredientId XOR customIngredientId
```

Nunca ambos y nunca una asociación implícita a catálogo.

La edición futura de una identidad personalizada deberá ser explícita; cambiar el texto de una fila de receta no debe alterar silenciosamente el maestro personalizado.

## 8. Repositorio y capas

Se creará un `CustomIngredientRepository` separado de `IngredientCatalogRepository` y `RecipeRepository`, siguiendo la arquitectura ya aprobada.

Responsabilidades iniciales:

- crear ingrediente personalizado;
- leer ingrediente personalizado por id;
- actualizarlo de forma explícita;
- listar categorías y grupos necesarios para el formulario;
- guardar alias;
- guardar relaciones de seguridad declaradas por usuario;
- ejecutar el guardado agregado de forma transaccional.

## 9. Bloques de implementación

### Bloque A — dominio + persistencia

- modelos de entrada/salida de dominio;
- DAO para custom ingredients, alias y relaciones;
- repositorio separado;
- validaciones de tipo/evidencia/integridad;
- transacción atómica;
- pruebas instrumentadas de persistencia.

### Bloque B — formulario

- nueva ruta;
- formulario SIMPLE / COMPOUND / COMMERCIAL_PRODUCT;
- campos condicionales de producto comercial;
- selector de categoría;
- selector de estado de composición;
- selector de grupos de seguridad;
- lenguaje de advertencia neutral;
- estados de guardado/error.

### Bloque C — integración con biblioteca/editor

- sustituir la compatibilidad `Introducir manualmente` por la nueva ruta;
- devolver `customIngredientId`, nombre y unidad al editor;
- conservar identidad personalizada en `RecipeDraft`;
- pruebas end-to-end de navegación Compose.

## 10. Pruebas mínimas

Debe comprobarse que:

- crear un ingrediente personalizado no crea una identidad de catálogo;
- `SIMPLE`, `COMPOUND` y `COMMERCIAL_PRODUCT` se conservan;
- una evidencia de usuario no puede convertirse en evidencia oficial;
- composición desconocida se persiste como tal;
- alias y relaciones se guardan de forma atómica;
- un fallo intermedio no deja datos parciales;
- el editor conserva `customIngredientId`;
- Room sigue en versión 4 con exclusivamente `1.json` a `4.json`.

## 11. Fuera de alcance

- composición estructurada por componentes;
- proporciones de ingredientes internos;
- OCR de etiquetas;
- códigos de barras;
- descarga remota de productos;
- inferencia automática de alérgenos desde el nombre;
- certificación de que un producto es seguro para una persona concreta.
