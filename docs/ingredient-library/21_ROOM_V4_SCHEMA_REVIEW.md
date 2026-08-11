# 21 — REVISIÓN DEL ESQUEMA ROOM V4

**Estado:** APROBADO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha de revisión:** 2026-08-08

## 1. Objeto de la revisión

Auditar el archivo Room exportado:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/4.json
```

contra:

- `RegulatoryExemptionEntity`;
- `IngredientLibraryMigrations.MIGRATION_3_4`;
- `RecipeDatabase` versión 4;
- esquema histórico `3.json`.

El objetivo es detectar divergencias entre el modelo Kotlin, el SQL de migración y el esquema que Room considera canónico.

## 2. Resultado general

```text
REVISIÓN: PASS
```

El archivo exportado declara:

```text
formatVersion      1
database.version   4
entities           15
```

El esquema v3 contenía 14 entidades. La entidad adicional corresponde a `regulatory_exemptions`.

## 3. Tabla `regulatory_exemptions`

Room exporta las 12 columnas esperadas:

```text
id
ingredientId
safetyGroupId
jurisdiction
regulatoryEffect
conditions
sourceId
effectiveFrom
effectiveTo
reviewedAt
notes
isActive
```

Nulabilidad esperada:

```text
NOT NULL: id, ingredientId, safetyGroupId, jurisdiction,
          regulatoryEffect, conditions, sourceId, reviewedAt, isActive
NULLABLE: effectiveFrom, effectiveTo, notes
```

La clave primaria es `id` sin autogeneración.

## 4. Claves foráneas

Se han comprobado las tres relaciones esperadas:

```text
ingredientId  -> catalog_ingredients.id
safetyGroupId -> food_safety_groups.id
sourceId      -> safety_sources.id
```

Las tres usan:

```text
ON UPDATE NO ACTION
ON DELETE NO ACTION
```

Este comportamiento es coherente con el requisito de conservar trazabilidad y evitar borrados en cascada de una decisión regulatoria.

## 5. Índices

Índices simples:

```text
index_regulatory_exemptions_ingredientId
index_regulatory_exemptions_safetyGroupId
index_regulatory_exemptions_sourceId
```

Índice único conceptual:

```text
index_regulatory_exemptions_ingredientId_safetyGroupId_jurisdiction_regulatoryEffect
```

Columnas:

```text
ingredientId
safetyGroupId
jurisdiction
regulatoryEffect
```

El índice está marcado `unique = true`, como exige el contrato.

## 6. Comparación con Room v3

`3.json` declara `database.version = 3` y 14 entidades. No contiene `regulatory_exemptions`.

`4.json` declara `database.version = 4` y añade esa entidad. Las entidades anteriores conservan el contrato estructural previsto; no se ha identificado una mutación funcional adicional deliberada fuera de la nueva tabla. El `identityHash` cambia, como corresponde a una nueva versión del esquema Room.

## 7. Coherencia con la migración

`MIGRATION_3_4` es aditiva y crea la misma tabla e índices que aparecen en el esquema exportado. No reconstruye ni elimina tablas previas.

La suite de migración comprobó adicionalmente:

```text
PRAGMA foreign_key_check -> sin filas
PRAGMA user_version      -> 4
```

## 8. Conclusión

`4.json` se acepta como representación canónica de Room v4 y permanece versionado.

Room v4 queda habilitado como base estable para el siguiente bloque: transportar exenciones regulatorias desde el catálogo versionado hasta esta nueva capa de persistencia.

## 9. Principio que debe mantenerse

La existencia de una fila en `regulatory_exemptions` expresa un **efecto jurídico/regulatorio documentado** bajo condiciones concretas.

No expresa ni puede transformarse automáticamente en:

```text
"sin alérgeno"
"sin riesgo"
"apto para alérgicos"
"puede consumirlo"
```

La capa regulatoria, el grafo de seguridad y el linaje culinario continúan siendo independientes.
