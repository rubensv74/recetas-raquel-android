# 26 — CATÁLOGO V8: ACEITE Y GRASA DE SOJA TOTALMENTE REFINADOS

**Estado:** VALIDADO Y ACTIVO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## 1. Objetivo

Incorporar de forma controlada dos exenciones expresamente enumeradas en el Anexo II del Reglamento (UE) 1169/2011:

```text
aceite de soja totalmente refinado
grasa de soja totalmente refinada
```

La revisión normativa previa está documentada en:

```text
docs/ingredient-library/25_REGULATORY_REFRESH_FULLY_REFINED_SOY_OIL_FAT.md
```

## 2. Delta desde v7

```text
catalogVersion                7 -> 8
catalog schemaVersion         4 -> 4
categorías                    20 -> 20
ingredientes                  253 -> 255
alias                         245 -> 245
relaciones de linaje          26 -> 28
relaciones de seguridad       33 -> 33
exenciones regulatorias        1 -> 3
Room version                   4 -> 4
```

No se añaden relaciones de seguridad alimentaria.

## 3. Nuevas identidades técnicas

```text
ing-fully-refined-soybean-oil
Aceite de soja totalmente refinado

ing-fully-refined-soybean-fat
Grasa de soja totalmente refinada
```

Ambas usan:

```text
categoryId             cat-oils-fats
verificationStatus     VERIFIED
compositionVariability VARIABLE_BY_PREPARATION
sourceUpdatedAt         null
```

`sourceUpdatedAt` es un `Long?` en el contrato de catálogo/Room. La fecha de revisión normativa no se fuerza dentro de ese campo: se conserva en `reviewedAt` de la exención y en la documentación de revisión. No se inventa una marca temporal numérica.

La mención `totalmente refinado` forma parte de la identidad. No se crea una exención genérica para `aceite de soja` o `grasa de soja` sin esa condición.

## 4. Linaje

Se añaden dos aristas independientes:

```text
ing-fully-refined-soybean-oil
  DERIVED_FROM -> ing-soybean

ing-fully-refined-soybean-fat
  DERIVED_FROM -> ing-soybean
```

Ambas referencian:

```text
sourceReference = EU_FIC_1169_2011
```

El linaje no crea, hereda ni elimina relaciones de seguridad.

## 5. Exenciones regulatorias

Se añaden:

```text
rex-eu-fully-refined-soybean-oil
rex-eu-fully-refined-soybean-fat
```

Contrato común:

```text
safetyGroupId     sg-eu-soybeans
jurisdiction      EU-ES
regulatoryEffect  EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
sourceId          EU_FIC_1169_2011
effectiveFrom     null
effectiveTo       null
reviewedAt        2026-08-08
isActive          true
```

Las fechas de vigencia se mantienen nulas porque esta revisión no identificó una fecha material nueva que deba inventarse o atribuirse al consolidado 2025.

## 6. Separación clínica

Para las dos nuevas identidades se exige:

```text
IngredientSafetyRelation = 0
RegulatoryExemption       = 1 por ingrediente
```

El ingrediente padre:

```text
ing-soybean
```

conserva su relación existente con:

```text
sg-eu-soybeans
```

La exención regulatoria no debe convertirse en textos como `sin soja`, `sin riesgo`, `seguro para alérgicos` o equivalentes.

## 7. Integridad histórica

V8 se construye copiando íntegramente el bundle v7 y añadiendo únicamente:

```text
ingredients-reviewed-soy-exemptions.json
ingredient-relations-soy-exemptions.json
regulatory-exemptions-soy.json
```

Más el nuevo `manifest.json` de v8.

Los bundles v1-v7 permanecen sin modificaciones.

## 8. Prueba instrumentada específica

Se mantiene:

```text
CatalogV8SoyRegulatoryExemptionTest
```

La prueba demuestra:

- bundle schema 4 / catalogVersion 8 válido;
- 255 ingredientes;
- 245 alias;
- 28 relaciones de linaje;
- 33 relaciones de seguridad;
- 3 exenciones regulatorias;
- ambas identidades `DERIVED_FROM -> ing-soybean`;
- fuente `EU_FIC_1169_2011`;
- `sourceUpdatedAt = null` para las nuevas identidades, coherente con el contrato `Long?`;
- ausencia de relaciones de seguridad para las dos nuevas identidades;
- persistencia de una exención por cada identidad;
- conservación de la relación de seguridad de `ing-soybean`;
- metadata final `catalogVersion = 8`.

## 9. Incidencia detectada durante el primer gate

La primera ejecución que llegó a la suite instrumentada ejecutó 42 pruebas y detectó un único fallo en:

```text
CatalogV8SoyRegulatoryExemptionTest
```

Causa:

```text
JsonSyntaxException
NumberFormatException: For input string: "2026-08-08"
```

Las dos nuevas identidades habían escrito una fecha ISO en `sourceUpdatedAt`, pero el contrato existente define ese campo como `Long?` tanto en `CatalogIngredientRecord` como en `CatalogIngredientEntity`.

Corrección aplicada:

```text
sourceUpdatedAt = null
```

para ambas identidades. La fecha de revisión permanece correctamente en `reviewedAt = 2026-08-08` de las exenciones. Se añadió una aserción de regresión para impedir que vuelva a introducirse una cadena ISO en ese campo.

## 10. Gate de staging validado

Validación local confirmada el 2026-08-09:

```text
connectedDebugAndroidTest    42/42 PASS
skipped                      0
failed                       0
assembleRelease              PASS
Room schemas                 1.json, 2.json, 3.json, 4.json
Room 5.json                  no generado
```

Las fases ejecutadas antes del tramo final también habían quedado en verde:

```text
assembleDebug                 PASS
testDebugUnitTest             PASS
lintDebug                     PASS
compileDebugAndroidTestKotlin PASS
```

Por tanto, el staging v8 quedó aprobado.

## 11. Activación

Después del gate verde:

```text
IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY
```

pasó de:

```text
ingredient-catalog/v7
```

a:

```text
ingredient-catalog/v8
```

La prueba v7 quedó fijada explícitamente a `ingredient-catalog/v7` como regresión histórica. La prueba v8 usa la ruta predeterminada para demostrar que la aplicación resuelve realmente v8 como catálogo activo.

## 12. Gate post-activación validado

Validación local final confirmada el 2026-08-09:

```text
connectedDebugAndroidTest    42/42 PASS
skipped                      0
failed                       0
assembleRelease              PASS
Room schemas                 1.json, 2.json, 3.json, 4.json
Room 5.json                  no generado
git status                   limpio respecto al proyecto
```

Los únicos cambios `.idea` detectados antes de la ejecución se restauraron/eliminaron antes del `git pull` y no forman parte del producto.

## 13. Cierre definitivo

```text
CatalogValidator v8                  PASS
persistencia de 2 nuevas exenciones PASS
seguridad sin propagación            PASS
suite instrumentada                  42/42 PASS post-activación
assembleRelease                      PASS post-activación
Room                                 sin cambios
catálogo predeterminado              v8
```

**V8 queda cerrada, validada y activa.**
