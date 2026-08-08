# 18 — CATÁLOGO V6: HARINAS REVISADAS Y DERIVADOS REGULADOS

**Estado:** IMPLEMENTADO — validación local pendiente  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-08

## 1. Objetivo

Catalog v6 amplía el primer grafo de linaje validado en v5 con un lote controlado de harinas de un único origen. El propósito es comprobar que `DERIVED_FROM` puede convivir con relaciones de seguridad explícitas y documentadas sin convertir el linaje en una regla clínica automática.

## 2. Base oficial revisada

La revisión previa a este lote usa como referencia principal el Anexo II del Reglamento (UE) 1169/2011. El grupo de cereales que contienen gluten incluye los cereales enumerados y sus productos derivados, salvo las excepciones legales expresamente recogidas. Una harina genérica del cereal no se trata como una de esas excepciones.

Como apoyo interpretativo se mantiene la Comunicación de la Comisión 2017/C 428/01 y la guía de AESAN sobre etiquetado de alimentos que contienen cereales con gluten.

El Reglamento de Ejecución (UE) 828/2014 se mantiene separado: regula las condiciones de las declaraciones `sin gluten` y `muy bajo en gluten`; la identidad de una harina no autoriza a inferir por sí sola ninguna de esas declaraciones.

AESAN publicó además en julio de 2026 una alerta por gluten no declarado en una harina de garbanzo concreta. Se usa como evidencia de diseño para recordar que la identidad `harina de garbanzo` no demuestra ausencia de gluten o contacto cruzado. Esa alerta es específica de producto/lote y **no** se convierte en una relación universal garbanzo → gluten.

## 3. Nuevas identidades

Se añaden diez harinas de un único origen:

```text
Harina de trigo
Harina de espelta
Harina de trigo khorasan
Harina de centeno
Harina de cebada
Harina de avena
Harina de arroz
Harina de maíz
Harina de garbanzo
Harina de trigo sarraceno
```

Todas permanecen `REVIEW_REQUIRED`. El hecho de disponer de una relación legal revisada para un grupo regulado no convierte toda la identidad culinaria en `VERIFIED` para cualquier uso o contexto.

## 4. Linaje

Cada harina incorpora una arista no clínica `DERIVED_FROM` hacia su materia prima:

```text
Harina de trigo          DERIVED_FROM Trigo
Harina de espelta        DERIVED_FROM Espelta
Harina de trigo khorasan DERIVED_FROM Trigo khorasan
Harina de centeno        DERIVED_FROM Centeno
Harina de cebada         DERIVED_FROM Cebada
Harina de avena          DERIVED_FROM Avena
Harina de arroz          DERIVED_FROM Arroz
Harina de maíz           DERIVED_FROM Maíz
Harina de garbanzo       DERIVED_FROM Garbanzo
Harina de trigo sarraceno DERIVED_FROM Trigo sarraceno
```

Estas aristas usan una referencia interna de revisión de identidad culinaria. No son evidencia clínica ni regulatoria.

## 5. Relaciones de seguridad añadidas

Solo se añaden relaciones de seguridad para las seis harinas cuyo cereal padre está expresamente cubierto por el grupo regulado de cereales que contienen gluten:

```text
Harina de trigo
Harina de espelta
Harina de trigo khorasan
Harina de centeno
Harina de cebada
Harina de avena
```

Contrato de cada relación:

```text
relationType   DERIVED_FROM
evidenceLevel  EU_LEGAL
sourceId       EU_FIC_1169_2011
reviewedAt     2026-08-08
```

No se propaga ninguna relación desde el padre: las seis relaciones existen como registros independientes y trazables.

## 6. Harinas sin relación universal de gluten

No se crea relación de seguridad universal para:

```text
Harina de arroz
Harina de maíz
Harina de garbanzo
Harina de trigo sarraceno
```

Esto **no significa** que la aplicación afirme ausencia de gluten, contacto cruzado u otros riesgos. Significa únicamente que no existe una relación universal aprobada en este lote.

Las alertas oficiales de productos concretos, como la alerta AESAN ES2026/431 sobre harina de garbanzo, demuestran precisamente por qué identidad y ausencia de riesgo deben permanecer separadas.

## 7. Exclusiones deliberadas

No se incluyen todavía:

- mezclas de harinas;
- preparados de repostería;
- harinas etiquetadas `sin gluten` como categoría automática;
- aceites;
- derivados de soja con distinto nivel de refinado;
- lácteos y lactosa;
- tofu, tahini u otros ingredientes cuya composición/procesamiento requiera tratamiento adicional;
- productos comerciales o de marca.

## 8. Versionado esperado

```text
catalogVersion        6
catalog schemaVersion 3
Room version          3
categories            20
ingredients           252
aliases               245
ingredientRelations   25
safetyGroups          14
safetySources          3
safetyRelations       33
```

Room no cambia. Por tanto la historia de esquemas debe seguir siendo exactamente `1.json`, `2.json`, `3.json`.

## 9. Gate de validación

Antes de considerar v6 validado debe pasar:

```text
assembleDebug
testDebugUnitTest
lintDebug
compileDebugAndroidTestKotlin
connectedDebugAndroidTest
assembleRelease
```

Además se comprobará:

- importación 252/245/25/33;
- idempotencia;
- rollback frente a un bundle posterior inválido;
- navegación `Harina de trigo -> Trigo`;
- una relación legal explícita para `Harina de trigo`;
- cero relación universal de gluten para `Harina de garbanzo` y `Harina de trigo sarraceno`;
- ausencia de Room `4.json`.

## 10. Principio preservado

```text
linaje culinario != evidencia de seguridad
```

Incluso cuando ambos grafos describen un mismo ingrediente, cada arista se crea, revisa y versiona de forma independiente.
