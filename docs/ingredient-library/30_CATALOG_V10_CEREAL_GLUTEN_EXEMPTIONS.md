# 30 — CATÁLOGO V10: EXENCIONES DE CEREALES CON GLUTEN

**Estado:** ACTIVO — staging validado; gate post-activación automático pendiente  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## 1. Objetivo

Incorporar de forma controlada cuatro identidades técnicas cubiertas por las excepciones del punto 1(a), 1(b) y 1(c) del Anexo II del Reglamento (UE) 1169/2011, según la revisión documentada en `29_REGULATORY_REFRESH_CEREAL_GLUTEN_EXEMPTIONS.md`.

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

Todas usan `cat-prepared-compound`, `VERIFIED`, `VARIABLE_BY_PREPARATION` y `sourceUpdatedAt = null`.

No se crean alias genéricos para evitar que expresiones como `jarabe de glucosa`, `dextrosa` o `maltodextrina` se asignen silenciosamente a una procedencia cereal concreta.

## 4. Linaje no clínico

```text
ing-wheat-glucose-syrup   DERIVED_FROM ing-wheat
ing-wheat-dextrose        DERIVED_FROM ing-wheat
ing-wheat-maltodextrin    DERIVED_FROM ing-wheat
ing-barley-glucose-syrup  DERIVED_FROM ing-barley
```

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

Las tres identidades derivadas de trigo conservan en `conditions` la limitación de la nota legal aplicable a productos derivados. El catálogo no evalúa procesos industriales ni propaga la exención a descendientes del grafo.

## 6. Separación clínica

Para las cuatro identidades nuevas:

```text
IngredientSafetyRelation = 0
RegulatoryExemption       = 1
```

La exención es una regla de declaración regulatoria. No demuestra ausencia de gluten, trigo o cebada ni aptitud para una condición médica concreta.

## 7. Fuera de v10

No se modela la excepción del punto 1(d) para cereales utilizados en la elaboración de destilados alcohólicos. Su formulación depende del uso/proceso y no se representa como una identidad genérica para evitar una aplicación excesivamente amplia.

Tampoco reciben exención los nombres genéricos sin procedencia cereal demostrada ni productos derivados posteriores sin trazabilidad suficiente.

## 8. Prueba instrumentada

`CatalogV10CerealRegulatoryExemptionTest` comprueba los conteos, las cuatro identidades, linaje, exenciones, ausencia de nuevas relaciones de seguridad, conservación de las relaciones de trigo/cebada, importación Room y metadata `catalogVersion = 10`.

## 9. Gate de staging — SUPERADO

GitHub Actions validó el staging completo:

```text
assembleDebug                     PASS
testDebugUnitTest                 PASS
lintDebug                         PASS
compileDebugAndroidTestKotlin     PASS
assembleRelease                   PASS
connectedDebugAndroidTest         PASS
Room schemas 1..4                 PASS
```

## 10. Activación

Se cambió `IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY` de `ingredient-catalog/v9` a `ingredient-catalog/v10`.

La prueba histórica de v9 quedó fijada explícitamente a v9 y la prueba de v10 pasó a utilizar el lector/importador predeterminado.

## 11. Gate post-activación

La activación dispara automáticamente `.github/workflows/android-ci.yml`.

Para cerrar v10 definitivamente deben volver a pasar build, unit, lint, compilación AndroidTest, release, pruebas instrumentadas y guard de Room con schemas exclusivamente `1.json` a `4.json`.
