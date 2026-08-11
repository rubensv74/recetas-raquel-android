# 37 — FASE 6 / BLOQUE A: PERSISTENCIA DE INGREDIENTES PERSONALIZADOS

**Estado:** CERRADO — VALIDADO EN GITHUB ACTIONS  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09  
**Commit funcional validado:** `491593ffd9526e570da695593a2b5b6986935501`

## Resultado

El Bloque A de Fase 6 queda validado como base de dominio y persistencia para ingredientes personalizados.

GitHub Actions completó correctamente el gate completo:

```text
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
compileDebugAndroidTestKotlin    PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS — 54/54, 0 skipped, 0 failed
Room schema guard                PASS — 1.json, 2.json, 3.json, 4.json
```

No se creó `5.json`; se reutilizan las estructuras ya previstas en Room v4.

## Implementación validada

Se añadieron:

- `CustomIngredientRepository` separado del catálogo y del repositorio de recetas;
- modelos de dominio para `SIMPLE`, `COMPOUND` y `COMMERCIAL_PRODUCT`;
- `CustomIngredientDao` y operaciones transaccionales de agregado;
- `LocalCustomIngredientRepository`;
- acceso al repositorio desde `AppContainer`;
- lectura de categorías y grupos de seguridad existentes;
- creación y actualización explícitas de identidades personalizadas;
- alias normalizados y deduplicados;
- persistencia de declaraciones locales de seguridad.

## Reglas de seguridad verificadas

La API de escritura de ingredientes personalizados solo expone estos niveles de evidencia:

```text
USER_DECLARED
UNVERIFIED
```

No permite que una declaración local se autocalifique como evidencia legal, científica u oficial.

Las relaciones de seguridad requieren un grupo existente y se guardan con una fuente local explícita `LOCAL_USER_DECLARED`. No se crea ninguna relación por similitud de nombre, categoría o linaje.

## Atomicidad

La sustitución del agregado —identidad, alias y relaciones de seguridad— se realiza dentro de una transacción Room.

Las pruebas verifican que un grupo de seguridad inexistente produce error antes de dejar una identidad personalizada parcial en la base de datos.

## Identidad

Crear un ingrediente personalizado no crea una identidad en `catalog_ingredients`. El dominio personalizado permanece separado del catálogo maestro.

## Siguiente bloque

Bloque B — formulario de creación de ingrediente personalizado.
