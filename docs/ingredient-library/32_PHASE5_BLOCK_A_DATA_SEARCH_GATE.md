# 32 — FASE 5 / BLOQUE A: CONSULTA DETERMINISTA DEL CATÁLOGO

**Estado:** IMPLEMENTADO — gate automático en curso  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09

## Alcance implementado

Se ha añadido la capa de consulta necesaria para convertir el catálogo versionado en una biblioteca utilizable desde UI, sin cambios de esquema Room.

Incluye:

- modelos de dominio para categoría y entrada de catálogo;
- consulta de categorías activas;
- búsqueda por nombre canónico y alias;
- ranking exacta → prefijo → contiene;
- normalización mediante `IngredientTextNormalizer`;
- filtro opcional por categoría;
- límite acotado entre 1 y 200 resultados;
- consulta directa de una identidad por `ingredientId`;
- estado informativo diferenciado entre relaciones de seguridad, exenciones regulatorias y ausencia de relación directa registrada.

## Regla de búsqueda

La consulta SQL no usa `LIKE` con patrones aportados por el usuario. Emplea igualdad, `substr` e `instr` sobre los valores ya normalizados, evitando que `%` o `_` adquieran semántica de comodín.

No existe fuzzy matching.

## Seguridad

El estado mostrado por la API solo describe la presencia de datos:

```text
SAFETY_RELATIONS_RECORDED
REGULATORY_EXEMPTION_RECORDED
NO_DIRECT_SAFETY_RELATION_RECORDED
```

No se propaga seguridad por linaje y una exención regulatoria sigue separada de una relación de seguridad.

## Prueba instrumentada

`IngredientLibrarySearchRepositoryTest` comprueba:

- importación del catálogo activo;
- 20 categorías;
- `Trigo` como coincidencia exacta prioritaria;
- filtro `cat-cereals-flours`;
- clasificación regulatoria del jarabe de glucosa a base de trigo;
- búsqueda insensible a acentos (`sesamo` → `Sésamo`);
- ausencia de fuzzy matching (`trigp` no se convierte en `Trigo`).

## Room

No se añaden entidades, columnas, índices ni migraciones. El gate debe conservar exclusivamente schemas `1.json` a `4.json`.
