# 28 — CATÁLOGO V9: DERIVADOS DE SOJA EXENTOS DE DECLARACIÓN

**Estado:** CERRADO — VALIDADO Y ACTIVO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## 1. Objetivo

Incorporar de forma controlada las excepciones restantes del punto 6(b), 6(c) y 6(d) del Anexo II del Reglamento (UE) 1169/2011, después del `regulatory refresh` documentado en:

```text
docs/ingredient-library/27_REGULATORY_REFRESH_SOY_TOCOPHEROLS_PHYTOSTEROLS_STANOLS.md
```

## 2. Delta desde v8

```text
catalogVersion                8 -> 9
catalog schemaVersion         4 -> 4
categorías                    20 -> 20
ingredientes                  255 -> 262
alias                         245 -> 245
relaciones de linaje          28 -> 35
relaciones de seguridad       33 -> 33
exenciones regulatorias        3 -> 10
Room version                   4 -> 4
```

No se añade ninguna relación de seguridad alimentaria.

## 3. Nuevas identidades

```text
Tocoferoles naturales mezclados (E306) derivados de soja
D-alfa tocoferol natural derivado de soja
Acetato de D-alfa tocoferol natural derivado de soja
Succinato de D-alfa tocoferol natural derivado de soja
Fitosteroles derivados de aceites vegetales de soja
Ésteres de fitosterol derivados de aceites vegetales de soja
Éster de fitostanol derivado de fitosteroles de aceite de soja
```

Todas usan:

```text
categoryId             cat-prepared-compound
verificationStatus     VERIFIED
compositionVariability VARIABLE_BY_PREPARATION
sourceUpdatedAt         null
```

## 4. Linaje

Se crean siete relaciones `DERIVED_FROM`.

Seis identidades apuntan directamente a `ing-soybean`. El éster de fitostanol apunta a `ing-soy-phytosterols`, porque el texto normativo lo describe como derivado de fitosteroles de aceite de soja.

Todas las relaciones usan:

```text
sourceReference = EU_FIC_1169_2011
reviewedAt       = 2026-08-09
```

El linaje continúa siendo no clínico y no propaga relaciones de seguridad.

## 5. Exenciones regulatorias

Se crean siete registros adicionales con:

```text
safetyGroupId     sg-eu-soybeans
jurisdiction      EU-ES
regulatoryEffect  EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
sourceId          EU_FIC_1169_2011
effectiveFrom     null
effectiveTo       null
reviewedAt        2026-08-09
isActive          true
```

Cada registro conserva en `conditions` la identidad/origen concreto descrito por el punto correspondiente del Anexo II.

## 6. Separación clínica

Para las siete identidades nuevas:

```text
IngredientSafetyRelation = 0
RegulatoryExemption       = 1 por ingrediente
```

La relación de seguridad existente de `ing-soybean` con `sg-eu-soybeans` se conserva sin cambios.

Ninguna exención puede convertirse automáticamente en mensajes como `sin soja`, `sin riesgo`, `seguro` o `apto para alérgicos`.

## 7. Integridad histórica

V9 se creó como fork exacto del árbol validado de v8. Los bundles v1-v8 permanecen sin modificaciones. La prueba histórica de v8 lee explícitamente `ingredient-catalog/v8`, por lo que su cobertura no depende del catálogo activo.

## 8. Prueba instrumentada

`CatalogV9SoyDerivativeExemptionTest` comprueba schema/catalogVersion, conteos, identidades, linaje, exenciones, ausencia de relaciones de seguridad nuevas, persistencia Room y metadata final `catalogVersion = 9`.

Tras la activación, la prueba usa el lector por defecto para demostrar que v9 es realmente el catálogo activo.

## 9. Gate de staging

El gate autónomo mediante GitHub Actions quedó verde:

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

`IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY` cambió de `ingredient-catalog/v8` a `ingredient-catalog/v9`.

La prueba histórica de v8 quedó fijada a v8 y la prueba de v9 pasó a utilizar el catálogo predeterminado.

## 11. Gate post-activación — CERRADO

El gate post-activación ejecutado por `.github/workflows/android-ci.yml` finalizó completamente en verde:

```text
assembleDebug                     PASS
testDebugUnitTest                 PASS
lintDebug                         PASS
compileDebugAndroidTestKotlin     PASS
assembleRelease                   PASS
connectedDebugAndroidTest         PASS
Room schemas                      1.json..4.json únicamente
```

Resultado: v9 queda validado, activo e inmutable como versión histórica a partir de la preparación de v10.
