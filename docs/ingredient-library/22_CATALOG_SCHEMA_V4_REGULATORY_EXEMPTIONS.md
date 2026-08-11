# 22 — CATÁLOGO SCHEMA V4: EXENCIONES REGULATORIAS

**Estado:** VALIDADO — gate local verde  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-08

## 1. Objetivo

Extender el formato versionado del catálogo para transportar **exenciones regulatorias estructuradas** hasta Room v4, manteniéndolas separadas del linaje culinario y de las relaciones de seguridad.

Este bloque implementa infraestructura. **El catálogo activo continúa siendo `ingredient-catalog/v6` y no contiene todavía exenciones regulatorias reales.**

## 2. Evolución del schema del catálogo

Se admite:

```text
catalog schemaVersion = 4
```

Compatibilidad conservada:

```text
schema 1 — ficheros simples
schema 2 — shards de ingredientes/alias
schema 3 — grafo de linaje
schema 4 — exenciones regulatorias estructuradas
```

No se modifica Room en este bloque:

```text
Room version = 4
```

## 3. Nuevo contrato de manifiesto

`CatalogFiles` admite de forma opcional:

```text
regulatoryExemptions
regulatoryExemptionShards
```

Se aplica el mismo principio que en otros conjuntos de datos: fichero único o shards, nunca ambos simultáneamente.

`CatalogCounts` añade:

```text
regulatoryExemptions
```

con valor por defecto `0` para conservar compatibilidad con los manifiestos históricos.

## 4. Registro de catálogo

Nuevo `CatalogRegulatoryExemptionRecord`:

```text
id
ingredientId
safetyGroupId
jurisdiction
regulatoryEffect
conditions
sourceId
effectiveFrom?
effectiveTo?
reviewedAt
notes?
isActive
```

El primer efecto soportado es deliberadamente específico:

```text
EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
```

No se introduce un valor genérico como `SAFE`, `NO_RISK` o equivalente.

## 5. Reader

`IngredientCatalogAssetReader` lee exenciones desde fichero único o shards y devuelve una lista independiente:

```text
IngredientCatalogBundle.regulatoryExemptions
```

Los catálogos schema 1–3 que no declaran esos ficheros obtienen una lista vacía.

El lector activo sigue apuntando a:

```text
ingredient-catalog/v6
```

Por tanto esta infraestructura no activa contenido regulatorio nuevo antes del gate.

## 6. Validator

`CatalogValidator` admite schema 4 y valida cada exención antes de importar.

Controles mínimos:

- ID único;
- ingrediente conocido;
- grupo regulatorio conocido;
- fuente conocida;
- jurisdicción no vacía;
- condiciones no vacías;
- `regulatoryEffect` dentro del vocabulario permitido;
- `reviewedAt` ISO;
- fechas de vigencia ISO cuando existan;
- `effectiveFrom <= effectiveTo` cuando ambas fechas existan;
- ausencia de duplicados conceptuales por ingrediente + grupo + jurisdicción + efecto;
- recuento del manifiesto coherente;
- prohibición de declarar exenciones antes de schema 4.

El validador no genera ni infiere una relación de seguridad desde una exención.

## 7. Persistencia transaccional

`CatalogImporter` transforma cada registro a `RegulatoryExemptionEntity` y lo entrega a `IngredientCatalogDao.replaceShippedCatalog()`.

Orden de reemplazo relevante:

```text
borrar exenciones anteriores
borrar relaciones de seguridad/linaje/alias
actualizar maestros
insertar ingredientes/grupos/fuentes
insertar relaciones
insertar exenciones regulatorias
actualizar metadata
```

Todo permanece dentro de la transacción de sustitución del catálogo.

Un bundle inválido se rechaza antes de iniciar la sustitución.

## 8. Consulta desde dominio

Se añade un modelo independiente:

```text
RegulatoryExemption
RegulatoryEffect
```

`IngredientCatalogRepository` expone:

```text
getRegulatoryExemptions(ingredientId)
```

Esto permite que una futura UI muestre contexto regulatorio sin usar ausencia de alertas como significado implícito.

## 9. Pruebas añadidas

### Unitarias

`CatalogValidatorTest` verifica:

- schema 4 válido;
- rechazo de exenciones en schema 3;
- referencias inexistentes;
- efecto no soportado;
- rango de vigencia incoherente;
- duplicado conceptual.

`IngredientCatalogAssetReaderTest` verifica lectura ordenada de shards de exenciones en schema 4.

### Instrumentada sintética

`IngredientCatalogImportTest` añade un bundle **sintético**, explícitamente no productivo, con una exención regulatoria y cero relaciones de seguridad.

Debe demostrar simultáneamente:

```text
regulatory_exemptions = 1
ingredient_safety_relations = 0
catalog_ingredient_relations = 0
```

La finalidad es probar en Room que:

```text
exención regulatoria != relación de seguridad
```

El fixture no constituye evidencia legal para contenido real.

## 10. Catálogo v6

`ingredient-catalog/v6` permanece sin cambios funcionales y con schema 3.

La compatibilidad validada es:

```text
bundle.regulatoryExemptions = 0
Room regulatory_exemptions = 0
```

La infraestructura schema 4 solo se utilizará para contenido real en una nueva versión de catálogo posterior al gate.

## 11. Gate de ejecución — SUPERADO

Evidencia local comunicada el 2026-08-08:

```text
clean assembleDebug             PASS
testDebugUnitTest               PASS
lintDebug                       PASS
compileDebugAndroidTestKotlin   PASS
connectedDebugAndroidTest       PASS — 40/40, 0 skipped, 0 failed
assembleRelease                 PASS
```

Room permaneció estable y no se generó una nueva versión de esquema:

```text
1.json
2.json
3.json
4.json
```

No apareció `5.json`, como correspondía.

Los mensajes de Gradle sobre `libandroidx.graphics.path.so` indican que esa librería se empaqueta sin strip de símbolos; no bloquearon debug ni release y no forman parte de este gate funcional.

El árbol de trabajo quedó limpio respecto a `origin/program/ingredient-library-food-safety` al finalizar la ejecución.

## 12. Resultado del gate

```text
CATALOG SCHEMA V4                 PASS
READER                            PASS
VALIDATOR                         PASS
IMPORTACIÓN TRANSACCIONAL         PASS
SEPARACIÓN EXENCIÓN/SEGURIDAD     PASS
SUITE INSTRUMENTADA               PASS — 40/40
ROOM                              SIN CAMBIOS — v4
```

La infraestructura queda aceptada como base estable para crear una nueva versión de catálogo con exenciones reales revisadas.

## 13. Próximo paso

El siguiente bloque es un `regulatory refresh` específico antes de introducir contenido real. La primera candidata es la excepción del punto 10 del Anexo II para ácido behénico procedente de semillas de mostaza bajo las condiciones establecidas por el Reglamento Delegado (UE) 2024/2512.

Solo después de confirmar texto vigente, alcance, condiciones y fecha de aplicación se preparará `ingredient-catalog/v7` con schema 4.

Ninguna exención se convertirá en una afirmación clínica de seguridad.
