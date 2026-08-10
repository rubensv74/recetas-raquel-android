# 38 — DECISIÓN ARQUITECTÓNICA: AGREGACIÓN DE SEGURIDAD EN RECETA

**Estado:** ACEPTADA — OPCIÓN B  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09  
**ADR:** ADR-027

## 1. Contexto

Las fases anteriores ya separan tres conceptos que no deben mezclarse:

```text
identidad / linaje culinario
relaciones de seguridad alimentaria
exenciones regulatorias
```

La Fase 7 debe construir la información de seguridad de una receta completa a partir de todos sus ingredientes de catálogo y personalizados.

El diseño funcional ya exige:

- agrupar por grupo de seguridad;
- evitar duplicar visualmente el mismo grupo;
- conservar los ingredientes que originan cada observación;
- conservar el tipo y la procedencia de cada relación;
- mantener `POSSIBLE_CROSS_REACTIVITY` separada de presencia identificada;
- tratar la composición desconocida como información que requiere revisión;
- no convertir ausencia de coincidencias en una certificación de seguridad.

## 2. Invariantes no negociables

1. Ninguna relación de linaje crea o hereda seguridad.
2. Ninguna exención regulatoria elimina, rebaja o neutraliza automáticamente una observación de seguridad.
3. Una exención es información legal/auditable y no una afirmación clínica.
4. No se asignará una puntuación clínica ni una probabilidad de reacción.
5. No se perderá la trazabilidad de los ingredientes y relaciones que originan el resultado.
6. `POSSIBLE_CROSS_REACTIVITY` nunca se presentará como presencia confirmada.
7. La composición desconocida puede producir un estado global `REQUIERE_REVISIÓN` aunque no exista un grupo concreto que pueda asignarse sin evidencia.
8. La ausencia de coincidencias solo permite afirmar que no se detectaron coincidencias en los datos registrados; no permite declarar la receta segura.

## 3. Decisión

**Se adopta la Opción B: estado principal de presentación + evidencia completa conservada.**

Cada grupo se muestra una sola vez con un estado principal determinista, pero el modelo conserva todas las observaciones que lo originan.

La precedencia aprobada es exclusivamente de presentación:

```text
1. PRESENCIA_IDENTIFICADA
   <- INHERENT_SOURCE / CONTAINS / REGULATED_COMPONENT

2. DERIVADO_IDENTIFICADO
   <- DERIVED_FROM

3. PUEDE_CONTENER_DECLARADO
   <- DECLARED_MAY_CONTAIN

4. POSIBLE_REACTIVIDAD_CRUZADA
   <- POSSIBLE_CROSS_REACTIVITY

5. REQUIERE_REVISION
   <- UNKNOWN u otra información insuficiente
```

Esta precedencia no representa gravedad médica, probabilidad de reacción ni recomendación de consumo.

Si el mismo grupo tiene `CONTAINS` y `DECLARED_MAY_CONTAIN`, el resumen muestra `PRESENCIA_IDENTIFICADA`, pero el detalle conserva ambas observaciones, sus ingredientes, fuentes, evidencia y notas.

La composición desconocida que no pueda vincularse legítimamente a un grupo concreto permanece como advertencia global de receta y no se asigna artificialmente a los grupos regulatorios.

## 4. Exenciones regulatorias

Las exenciones de ADR-026 permanecen en un canal separado.

```text
Resumen de seguridad        -> observaciones de seguridad
Información regulatoria     -> exenciones aplicables a la identidad exacta
```

Una exención puede mostrarse como información legal contextual, pero no puede ejecutar una operación equivalente a:

```text
warning = warning - exemption
```

Cualquier futura regla que pretendiese modificar una advertencia clínica en función de una exención requeriría una ADR independiente y evidencia específica.

## 5. Contrato de dominio implementado

La primera pieza de Fase 7 queda implementada como lógica pura de dominio:

```text
RecipeSafetyAggregator
RecipeSafetySummary
RecipeSafetyGroupSummary
RecipeSafetyObservation
RecipeReviewNotice
RecipeSafetyPresentationState
RecipeSafetyRelationType
```

Archivo principal:

```text
app/src/main/java/com/rmm/recetasraquel/domain/ingredient/RecipeSafetyAggregation.kt
```

El agregador:

- agrupa observaciones por `safetyGroupId`;
- calcula el estado principal usando exclusivamente la precedencia aprobada;
- conserva todas las observaciones subyacentes;
- mantiene advertencias globales fuera de grupos artificiales;
- transporta las exenciones regulatorias en una colección independiente;
- ordena el resultado de forma determinista para facilitar pruebas y presentación estable.

## 6. Pruebas implementadas

Se añadió:

```text
app/src/test/java/com/rmm/recetasraquel/domain/ingredient/RecipeSafetyAggregatorTest.kt
```

La batería cubre inicialmente:

- `CONTAINS` prevalece visualmente sobre `DECLARED_MAY_CONTAIN` y `POSSIBLE_CROSS_REACTIVITY`, sin perder observaciones;
- `DERIVED_FROM` conserva un estado propio;
- reactividad cruzada no se convierte en presencia;
- `UNKNOWN` produce `REQUIERE_REVISION`;
- composición/información no asignable puede permanecer como aviso global;
- una exención regulatoria no suprime una observación de seguridad;
- orden determinista de grupos.

## 7. Estado de implementación

```text
Decisión ADR-027                ACEPTADA — B
Modelo de dominio Fase 7        IMPLEMENTADO
Agregador puro                  IMPLEMENTADO
Pruebas deterministas           IMPLEMENTADAS
Gate Gradle local               PENDIENTE
Adaptadores catálogo/custom     PENDIENTE
Integración con RecipeDetail    PENDIENTE
UI final de alertas              PENDIENTE
```

No se ha introducido ninguna puntuación clínica, inferencia por linaje ni supresión automática basada en exenciones.
