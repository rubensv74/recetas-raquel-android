# 28 — CATÁLOGO V9: DERIVADOS DE SOJA EXENTOS DE DECLARACIÓN

**Estado:** IMPLEMENTADO EN STAGING — validación local pendiente  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## 1. Objetivo

Incorporar de forma controlada las excepciones restantes del punto 6(b), 6(c) y 6(d) del Anexo II del Reglamento (UE) 1169/2011, después del `regulatory refresh` documentado en:

```text
docs/ingredient-library/27_REGULATORY_REFRESH_SOY_TOCOPHEROLS_PHYTOSTEROLS_STANOLS.md
```

V8 permanece como catálogo activo mientras v9 está en staging.

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

Seis identidades apuntan directamente a:

```text
ing-soybean
```

El éster de fitostanol apunta a:

```text
ing-soy-phytosterols
```

porque el texto normativo lo describe como derivado de fitosteroles de aceite de soja.

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

V9 se ha creado como fork exacto del árbol validado de v8 y después se han añadido únicamente:

```text
ingredients-reviewed-soy-derivatives.json
ingredient-relations-soy-derivatives.json
regulatory-exemptions-soy-derivatives.json
```

más el `manifest.json` de v9.

Los bundles v1-v8 permanecen sin modificaciones.

## 8. Prueba instrumentada

Se añade:

```text
CatalogV9SoyDerivativeExemptionTest
```

Debe comprobar:

- schema 4 / catalogVersion 9;
- 262 ingredientes;
- 245 alias;
- 35 relaciones de linaje;
- 33 relaciones de seguridad;
- 10 exenciones regulatorias;
- siete identidades nuevas verificadas;
- `sourceUpdatedAt = null`;
- siete exenciones con `sg-eu-soybeans`;
- ausencia de relaciones de seguridad para las siete identidades;
- seis relaciones directas a `ing-soybean`;
- una relación del éster de fitostanol a `ing-soy-phytosterols`;
- importación transaccional en Room v4;
- metadata final `catalogVersion = 9`.

## 9. Catálogo activo durante el gate

Debe permanecer:

```text
IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY = ingredient-catalog/v8
```

La prueba de v9 lee explícitamente `ingredient-catalog/v9`. V9 no se activará hasta superar el gate.

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
43 instrumented tests
0 failed
0 skipped
```

Room debe continuar exactamente con:

```text
1.json
2.json
3.json
4.json
```

No debe aparecer `5.json`.

## 11. Criterio de aprobación

```text
CatalogValidator v9                    PASS
persistencia de 7 nuevas exenciones   PASS
seguridad sin propagación              PASS
suite instrumentada                    43/43 PASS
build/unit/lint/release                PASS
Room                                   sin cambios
catálogo predeterminado                v8 durante staging
```

Solo después del gate verde podrá activarse v9.
