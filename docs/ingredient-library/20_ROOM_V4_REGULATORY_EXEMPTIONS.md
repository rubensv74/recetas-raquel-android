# 20 — ROOM V4: CAPA DE EXENCIONES REGULATORIAS

**Estado:** IMPLEMENTADO — validación local pendiente  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-08

## 1. Objetivo

Implementar la decisión ADR-026 — Opción B: las exenciones regulatorias se almacenan en una capa estructurada independiente del grafo culinario y del grafo de seguridad.

La finalidad de Room v4 es crear capacidad de persistencia y trazabilidad. **No introduce todavía exenciones regulatorias reales en el catálogo activo v6.**

## 2. Nueva entidad

Se añade `RegulatoryExemptionEntity` con tabla:

```text
regulatory_exemptions
```

Campos:

```text
id                String PK
ingredientId      String FK -> catalog_ingredients.id
safetyGroupId     String FK -> food_safety_groups.id
jurisdiction      String
regulatoryEffect  String
conditions        String
sourceId          String FK -> safety_sources.id
effectiveFrom     String? ISO date
effectiveTo       String? ISO date
reviewedAt        String ISO date
notes             String?
isActive          Boolean
```

## 3. Principios semánticos

La tabla expresa únicamente efectos regulatorios documentados. No expresa ausencia de alérgeno, ausencia de riesgo ni aptitud clínica.

Regla central:

```text
exención regulatoria != seguridad clínica
```

No existe propagación automática entre:

```text
CatalogIngredientRelation
IngredientSafetyRelation
RegulatoryExemption
```

## 4. Integridad

Claves foráneas con `NO ACTION` hacia ingrediente, grupo regulatorio y fuente. Esto evita pérdida de trazabilidad por borrados en cascada.

Índices:

```text
ingredientId
safetyGroupId
sourceId
```

Además se impide duplicar una misma combinación conceptual:

```text
UNIQUE(ingredientId, safetyGroupId, jurisdiction, regulatoryEffect)
```

## 5. Migración 3 -> 4

`IngredientLibraryMigrations.MIGRATION_3_4` crea exclusivamente la nueva tabla y sus índices. No modifica recetas, ingredientes existentes, linaje, grupos, fuentes ni relaciones de seguridad.

La migración es aditiva y no destructiva.

## 6. RecipeDatabase

```text
Room version 3 -> 4
```

`RecipeDatabase.build()` registra ahora:

```text
MIGRATION_1_2
MIGRATION_2_3
MIGRATION_3_4
```

## 7. Prueba de migración

`RecipeDatabaseMigrationTest` se amplía para validar el camino real:

```text
Room v1 -> v2 -> v3 -> v4
```

Debe comprobar:

- preservación de la receta legacy;
- preservación de los ingredientes legacy y sus orígenes personalizados;
- preservación del grafo de linaje existente;
- existencia vacía de `regulatory_exemptions`;
- columnas regulatorias esperadas;
- `PRAGMA foreign_key_check` sin errores;
- `PRAGMA user_version = 4`.

## 8. Catálogo

`ingredient-catalog/v6/` permanece inmutable y sigue siendo el catálogo activo durante este gate.

Room v4 y `catalogVersion` continúan siendo conceptos independientes:

```text
Room schema version = 4
active catalogVersion = 6
active catalog schemaVersion = 3
```

La evolución del formato de catálogo para transportar exenciones se realizará únicamente después de validar Room v4.

## 9. Gate local requerido

Ejecutar:

```text
clean assembleDebug
testDebugUnitTest
lintDebug
compileDebugAndroidTestKotlin
connectedDebugAndroidTest
assembleRelease
```

Resultados esperados:

```text
39 instrumented tests PASS
0 failed
0 skipped
```

Room debe generar un nuevo:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/4.json
```

El archivo `4.json` deberá revisarse antes de versionarlo.

## 10. Criterios de revisión de 4.json

Debe existir `regulatory_exemptions` con:

- 12 columnas previstas;
- tres claves foráneas `NO ACTION`;
- índices simples sobre ingrediente, grupo y fuente;
- índice único conceptual sobre ingrediente + grupo + jurisdicción + efecto;
- ausencia de cambios inesperados en las demás entidades.

## 11. Siguiente paso tras gate verde

Después de revisar y versionar `4.json`:

1. ampliar el formato de catálogo con una capa `regulatoryExemptions`;
2. ampliar reader, validator, importer y DAO;
3. crear pruebas sintéticas de separación entre exención y seguridad;
4. solo entonces preparar una nueva versión del catálogo con exenciones reales revisadas.
