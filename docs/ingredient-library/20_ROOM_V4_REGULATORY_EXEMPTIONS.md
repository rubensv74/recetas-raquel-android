# 20 — ROOM V4: CAPA DE EXENCIONES REGULATORIAS

**Estado:** CERRADO — ejecución, esquema y versionado validados  
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

`RecipeDatabase.build()` registra:

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

## 8. Catálogo durante el gate

`ingredient-catalog/v6/` permaneció inmutable y siguió siendo el catálogo activo durante la migración.

```text
Room schema version = 4
active catalogVersion = 6
active catalog schemaVersion = 3
```

Room y el formato/versionado del catálogo continúan siendo conceptos independientes.

## 9. Ejecución del gate

La primera ejecución terminó con 1 fallo instrumentado. La causa fue exclusivamente una prueba histórica que registraba solo `MIGRATION_2_3` aunque `RecipeDatabase` ya estaba en v4. Room solicitó correctamente una ruta completa `2 -> 4`.

Tras corregir el contrato de esa prueba, la ejecución final quedó:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
migrate2To4... targeted test   PASS — 1/1
connectedDebugAndroidTest      PASS — 39/39, 0 skipped, 0 failed
assembleRelease                PASS
Room schema export             PASS — 4.json generado
```

Los avisos de `stripDebugSymbols` / `stripReleaseDebugSymbols` para `libandroidx.graphics.path.so` no impidieron el empaquetado y ambos builds terminaron en `BUILD SUCCESSFUL`.

## 10. Auditoría estructural de `4.json` — SUPERADA

El esquema exportado y versionado confirma:

```text
formatVersion = 1
database.version = 4
entities = 15
```

Room v3 contenía 14 entidades. La nueva entidad es exclusivamente:

```text
regulatory_exemptions
```

La tabla contiene exactamente las 12 columnas previstas y mantiene nulabilidad/tipos coherentes con `RegulatoryExemptionEntity`.

Claves foráneas auditadas:

```text
ingredientId  -> catalog_ingredients.id  NO ACTION / NO ACTION
safetyGroupId -> food_safety_groups.id   NO ACTION / NO ACTION
sourceId      -> safety_sources.id        NO ACTION / NO ACTION
```

Índices auditados:

```text
index_regulatory_exemptions_ingredientId
index_regulatory_exemptions_safetyGroupId
index_regulatory_exemptions_sourceId
index_regulatory_exemptions_ingredientId_safetyGroupId_jurisdiction_regulatoryEffect  UNIQUE
```

La comparación de las entidades existentes con `3.json` no muestra un cambio funcional adicional esperado en Room v4. El cambio estructural deliberado es la incorporación de esta nueva tabla, además de los metadatos/identity hash propios de una nueva versión de Room.

## 11. Versionado

`4.json` está presente y versionado en la rama del programa:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/4.json
```

La historia de esquemas queda:

```text
1.json
2.json
3.json
4.json
```

## 12. Gate final

```text
MIGRACIÓN 3 -> 4                 PASS
TEST DIRIGIDO 2 -> 4             PASS — 1/1
SUITE INSTRUMENTADA              PASS — 39/39
BUILD / UNIT / LINT / RELEASE    PASS
GENERACIÓN 4.json                PASS
AUDITORÍA DE 4.json              PASS
VERSIONADO DE 4.json             PASS
ROOM V4                          CERRADO
```

## 13. Siguiente paso

Con Room v4 cerrado, el siguiente bloque es ampliar el **formato del catálogo** para transportar exenciones regulatorias de forma estructurada:

1. `CatalogBundle` y manifiesto;
2. reader;
3. validator;
4. DAO/importer;
5. modelo de dominio/repositorio;
6. pruebas de separación entre exención y seguridad;
7. solo después, nueva versión de catálogo con exenciones reales revisadas.

Ver también `21_ROOM_V4_SCHEMA_REVIEW.md`.
