# 24 — CATÁLOGO V7: PRIMERA EXENCIÓN REGULATORIA REAL

**Estado:** CERRADO — VALIDADO Y ACTIVO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-08

## 1. Objetivo

Crear la primera versión de catálogo que usa `schemaVersion = 4` con una exención regulatoria real, después de haber validado por separado:

1. Room v4 y la tabla `regulatory_exemptions`;
2. la infraestructura de catálogo schema v4;
3. el `regulatory refresh` específico del Reglamento Delegado (UE) 2024/2512.

## 2. Delta desde v6

```text
catalogVersion                6 -> 7
catalog schemaVersion         3 -> 4
categorías                    20 -> 20
ingredientes                  252 -> 253
alias                         245 -> 245
relaciones de linaje          25 -> 26
relaciones de seguridad       33 -> 33
exenciones regulatorias        0 -> 1
Room version                   4 -> 4
```

No se añadió ninguna nueva relación de seguridad alimentaria.

## 3. Nuevo ingrediente técnico

```text
id                     ing-mustard-behenic-acid
nombre                 Ácido behénico de semillas de mostaza
categoría              Aceites y grasas
verificationStatus     VERIFIED
compositionVariability VARIABLE_BY_PREPARATION
```

La identidad existe porque la excepción jurídica afecta a un derivado técnico concreto y condicionado; no sería correcto aplicar la excepción a `ing-mustard` de forma general.

## 4. Nuevo linaje

```text
ing-mustard-behenic-acid
  DERIVED_FROM
    ing-mustard
```

La relación referencia `EU_MUSTARD_2024_2512` y conserva explícitamente la regla de que el linaje no crea ni propaga relaciones de seguridad.

## 5. Exención regulatoria

```text
id                rex-eu-mustard-behenic-acid-2024-2512
ingredientId      ing-mustard-behenic-acid
safetyGroupId     sg-eu-mustard
jurisdiction      EU-ES
regulatoryEffect  EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
sourceId          EU_MUSTARD_2024_2512
effectiveFrom     2025-04-01
effectiveTo       null
reviewedAt        2026-08-08
isActive          true
```

Las condiciones codificadas exigen:

```text
pureza de ácido behénico >= 85 %
dos fases de destilación
uso para fabricar exclusivamente E470a, E471 y E477
```

## 6. Separación clínica obligatoria

El nuevo ingrediente mantiene:

```text
0 nuevas IngredientSafetyRelation
```

La exención no puede interpretarse automáticamente como:

```text
sin mostaza
sin alérgeno
sin riesgo
apto para personas alérgicas
puede consumirse
```

`ing-mustard` conserva su relación regulatoria existente con `sg-eu-mustard`.

## 7. Fuente y revisión

La fuente normativa es:

```text
EU_MUSTARD_2024_2512
Commission Delegated Regulation (EU) 2024/2512
CELEX 32024R2512
```

El `regulatory refresh` específico está documentado en:

```text
docs/ingredient-library/23_REGULATORY_REFRESH_MUSTARD_BEHENIC_ACID.md
```

## 8. Gates superados

Primero se validó v7 en staging con el lector activo todavía en v6. Después se activó v7 cambiando:

```text
IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY = ingredient-catalog/v7
```

El gate post-activación local del 2026-08-08 quedó:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS — 41/41, 0 skipped, 0 failed
assembleRelease                PASS
Room schemas                   1.json, 2.json, 3.json, 4.json únicamente
working tree                   clean
```

No se generó `5.json`, por lo que la activación del catálogo no produjo un cambio de esquema Room.

## 9. Nota sobre el intento de test dirigido

Un primer comando para ejecutar exclusivamente `CatalogV7RegulatoryExemptionTest` fue interpretado por Gradle como nombre de tarea y falló antes de lanzar instrumentación. Ese error era de sintaxis de invocación y no del código ni de los datos. La suite completa posterior ejecutó 41 pruebas, incluida la prueba nueva de v7, todas en verde.

## 10. Estado final

```text
v6 = histórico e inmutable
v7 = activo y validado
Room = v4
```

V7 demuestra en datos reales el principio:

```text
exención regulatoria != relación de seguridad
```

El siguiente bloque puede incorporar otras exenciones expresamente enumeradas por la normativa vigente, siempre mediante revisión oficial específica y sin inferencia clínica.
