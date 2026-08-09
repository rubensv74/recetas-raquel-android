# 30 — CATÁLOGO V10: EXENCIONES DE CEREALES CON GLUTEN

**Estado:** IMPLEMENTADO EN STAGING — validación automática pendiente  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## 1. Objetivo

Incorporar de forma controlada cuatro identidades técnicas cubiertas por las excepciones del punto 1(a), 1(b) y 1(c) del Anexo II del Reglamento (UE) 1169/2011, según la revisión documentada en:

```text
docs/ingredient-library/29_REGULATORY_REFRESH_CEREAL_GLUTEN_EXEMPTIONS.md
```

V9 permanece como catálogo activo durante el gate de staging.

## 2. Delta desde v9

```text
catalogVersion                 9 -> 10
catalog schemaVersion          4 -> 4
categorías                    20 -> 20
ingredientes                  262 -> 266
alias                         245 -> 245
relaciones de linaje           35 -> 39
relaciones de seguridad        33 -> 33
exenciones regulatorias        10 -> 14
Room version                    4 -> 4
```

No se añade ninguna relación de seguridad alimentaria.

## 3. Nuevas identidades

```text
Jarabe de glucosa a base de trigo
Dextrosa a base de trigo
Maltodextrina a base de trigo
Jarabe de glucosa a base de cebada
```

Todas usan:

```text
categoryId             cat-prepared-compound
verificationStatus     VERIFIED
compositionVariability VARIABLE_BY_PREPARATION
sourceUpdatedAt         null
```

No se crean alias genéricos para evitar que expresiones como `jarabe de glucosa`, `dextrosa` o `maltodextrina` se asignen silenciosamente a una procedencia cereal concreta.

## 4. Linaje no clínico

```text
ing-wheat-glucose-syrup   DERIVED_FROM ing-wheat
ing-wheat-dextrose        DERIVED_FROM ing-wheat
ing-wheat-maltodextrin    DERIVED_FROM ing-wheat
ing-barley-glucose-syrup  DERIVED_FROM ing-barley
```

Todas las relaciones usan `EU_FIC_1169_2011` y `reviewedAt = 2026-08-09`.

El linaje no propaga relaciones de seguridad ni exenciones regulatorias.

## 5. Exenciones regulatorias

Cada nueva identidad tiene exactamente una exención con:

```text
safetyGroupId     sg-eu-cereals-gluten
jurisdiction      EU-ES
regulatoryEffect  EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
sourceId          EU_FIC_1169_2011
effectiveFrom     null
effectiveTo       null
reviewedAt        2026-08-09
isActive          true
```

Las tres identidades derivadas de trigo conservan además en `conditions` la limitación de la nota legal aplicable a productos derivados: no se extiende la excepción automáticamente si un proceso posterior pudiera aumentar la alergenicidad evaluada para el producto de origen.

El catálogo no intenta evaluar procesos industriales ni propagar esa excepción a descendientes del grafo.

## 6. Separación clínica

Para las cuatro identidades nuevas:

```text
IngredientSafetyRelation = 0
RegulatoryExemption       = 1
```

La exención es una regla de declaración regulatoria. No demuestra ausencia de gluten, trigo o cebada ni aptitud para una condición médica concreta.

## 7. Fuera de v10

No se modela todavía la excepción del punto 1(d) para cereales utilizados en la elaboración de destilados alcohólicos. Su formulación depende del uso/proceso y no se representa como una identidad genérica para evitar una aplicación excesivamente amplia.

Tampoco reciben exención:

```text
jarabe de glucosa genérico
dextrosa genérica
maltodextrina genérica
productos derivados posteriores sin trazabilidad suficiente
```

## 8. Prueba instrumentada

Se añade:

```text
CatalogV10CerealRegulatoryExemptionTest
```

Debe comprobar:

- schema 4 / catalogVersion 10;
- 266 ingredientes;
- 245 alias;
- 39 relaciones de linaje;
- 33 relaciones de seguridad;
- 14 exenciones regulatorias;
- cuatro identidades nuevas verificadas;
- cuatro relaciones de linaje con los padres esperados;
- cuatro exenciones con `sg-eu-cereals-gluten`;
- ausencia de relaciones de seguridad en las cuatro identidades;
- conservación de las relaciones de seguridad de trigo y cebada;
- importación transaccional en Room v4;
- metadata final `catalogVersion = 10`.

## 9. Gate automático

`.github/workflows/android-ci.yml` ejecuta automáticamente:

```text
assembleDebug
testDebugUnitTest
lintDebug
compileDebugAndroidTestKotlin
assembleRelease
connectedDebugAndroidTest
Room schema guard
```

Con la nueva prueba se esperan 44 pruebas instrumentadas. Room debe permanecer exactamente en `1.json` a `4.json`.

V10 no se activará hasta que el gate de staging quede completamente verde.
