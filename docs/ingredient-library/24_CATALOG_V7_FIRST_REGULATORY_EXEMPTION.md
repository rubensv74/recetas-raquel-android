# 24 — CATÁLOGO V7: PRIMERA EXENCIÓN REGULATORIA REAL

**Estado:** IMPLEMENTADO EN STAGING — validación local pendiente  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-08

## 1. Objetivo

Crear la primera versión de catálogo que usa `schemaVersion = 4` con una exención regulatoria real, después de haber validado por separado:

1. Room v4 y la tabla `regulatory_exemptions`;
2. la infraestructura de catálogo schema v4;
3. el `regulatory refresh` específico del Reglamento Delegado (UE) 2024/2512.

El catálogo v6 permanece inmutable y sigue siendo el catálogo activo hasta que v7 supere su gate.

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

No se añade ninguna nueva relación de seguridad alimentaria.

## 3. Nuevo ingrediente técnico

```text
id                     ing-mustard-behenic-acid
nombre                 Ácido behénico de semillas de mostaza
categoría              Aceites y grasas
verificationStatus     VERIFIED
compositionVariability VARIABLE_BY_PREPARATION
```

La identidad se crea porque la excepción jurídica afecta a un derivado técnico concreto y condicionado; no sería correcto aplicar la excepción a `ing-mustard` de forma general.

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

El nuevo ingrediente tiene:

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

La revisión confirmó el efecto, las condiciones y la fecha de aplicación antes de crear el registro del catálogo.

## 8. Prueba instrumentada específica

Se añade:

```text
CatalogV7RegulatoryExemptionTest
```

La prueba lee explícitamente `ingredient-catalog/v7` sin cambiar todavía el catálogo activo. Debe verificar:

- validación completa del bundle;
- 253 ingredientes;
- 245 alias;
- 26 relaciones de linaje;
- 33 relaciones de seguridad;
- 1 exención regulatoria;
- fuente y condiciones de la exención;
- `DERIVED_FROM` hacia `ing-mustard`;
- ausencia de relación de seguridad para `ing-mustard-behenic-acid`;
- persistencia transaccional en Room v4;
- metadata final con `catalogVersion = 7`.

## 9. Catálogo activo durante el gate

Se mantiene deliberadamente:

```text
IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY = ingredient-catalog/v6
```

Así, una versión v7 todavía no validada no se convierte en catálogo predeterminado de la aplicación.

Después del gate verde se hará un cambio separado para activar v7 y se repetirá la validación necesaria.

## 10. Gate requerido

Ejecutar:

```text
clean assembleDebug
testDebugUnitTest
lintDebug
compileDebugAndroidTestKotlin
connectedDebugAndroidTest
assembleRelease
```

Con la nueva prueba se esperan:

```text
41 instrumented tests
0 failed
0 skipped
```

Room no cambia:

```text
1.json
2.json
3.json
4.json
```

No debe aparecer `5.json`.

## 11. Criterio de aprobación

El staging v7 solo podrá activarse si:

```text
CatalogValidator v7                PASS
persistencia de exención           PASS
separación exención/seguridad      PASS
suite instrumentada                41/41 PASS
build/unit/lint/release            PASS
Room                               sin cambios
```

Hasta entonces:

```text
v6 = activo
v7 = staging
```
