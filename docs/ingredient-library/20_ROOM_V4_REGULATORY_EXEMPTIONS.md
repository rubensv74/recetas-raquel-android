# 20 — ROOM V4: CAPA DE EXENCIONES REGULATORIAS

**Estado:** GATE DE EJECUCIÓN SUPERADO — revisión de `4.json` pendiente  
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

## 7. Pruebas de migración

`RecipeDatabaseMigrationTest` valida el camino real:

```text
Room v1 -> v2 -> v3 -> v4
```

Comprueba preservación de datos legacy, existencia vacía de `regulatory_exemptions`, integridad referencial y `PRAGMA user_version = 4`.

La prueba histórica `IngredientLibraryMigration23Test` abre una base física v2 con la versión actual de `RecipeDatabase` registrando la cadena completa:

```text
MIGRATION_2_3
MIGRATION_3_4
```

Conserva su objetivo original —verificar el contrato `sourceId/sourceDetails`— y añade la comprobación de que la nueva tabla regulatoria queda vacía al llegar a v4.

## 8. Catálogo

`ingredient-catalog/v6/` permanece inmutable y sigue siendo el catálogo activo durante este gate.

```text
Room schema version = 4
active catalogVersion = 6
active catalog schemaVersion = 3
```

La evolución del formato de catálogo para transportar exenciones se realizará únicamente después de revisar y versionar `4.json`.

## 9. Primera ejecución del gate — 2026-08-08

La primera ejecución terminó con 1 fallo instrumentado. La causa fue exclusivamente una prueba histórica que registraba solo `MIGRATION_2_3` aunque `RecipeDatabase` ya estaba en v4. Room solicitó correctamente una ruta completa `2 -> 4`.

Corrección aplicada:

```text
IngredientLibraryMigration23Test
  MIGRATION_2_3
  MIGRATION_3_4
  PRAGMA user_version = 4
  regulatory_exemptions count = 0
```

No fue necesario modificar código de producción para resolver ese fallo.

## 10. Segunda ejecución correctiva — SUPERADA

Evidencia local del 2026-08-08:

```text
compileDebugAndroidTestKotlin  PASS
migrate2To4... targeted test  PASS — 1/1
connectedDebugAndroidTest      PASS — 39/39, 0 skipped, 0 failed
```

Combinado con la primera ejecución del mismo cambio:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS — 39/39
assembleRelease                PASS
Room schema export             PASS — 4.json generado
```

Por tanto, el **gate de ejecución de Room v4 está verde**.

Estado local esperado y confirmado:

```text
?? app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/4.json
```

El archivo no se versionará hasta completar su revisión estructural.

## 11. Criterios de revisión de 4.json

Debe existir `regulatory_exemptions` con:

- 12 columnas previstas;
- tres claves foráneas `NO ACTION`;
- índices simples sobre ingrediente, grupo y fuente;
- índice único conceptual sobre ingrediente + grupo + jurisdicción + efecto;
- ausencia de cambios inesperados en las demás entidades.

La revisión también debe confirmar `version = 4` y que el cambio respecto de `3.json` es exclusivamente el esperado para esta entidad, además de los metadatos de Room.

## 12. Estado del gate

```text
MIGRACIÓN 3 -> 4                 PASS
TEST DIRIGIDO 2 -> 4             PASS — 1/1
SUITE INSTRUMENTADA              PASS — 39/39
BUILD / UNIT / LINT / RELEASE    PASS
GENERACIÓN 4.json                PASS
AUDITORÍA DE 4.json              PENDING
VERSIONADO DE 4.json             PENDING
```

## 13. Siguiente paso

Después de revisar y versionar `4.json`:

1. ampliar el formato de catálogo con una capa `regulatoryExemptions`;
2. ampliar reader, validator, importer y DAO;
3. crear pruebas sintéticas de separación entre exención y seguridad;
4. solo entonces preparar una nueva versión del catálogo con exenciones reales revisadas.
