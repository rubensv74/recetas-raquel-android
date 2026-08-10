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

Queda por decidir cómo representar un mismo grupo cuando una receta produce simultáneamente observaciones de distinta naturaleza.

Ejemplo conceptual:

```text
Grupo X
- ingrediente A -> CONTAINS
- ingrediente B -> DECLARED_MAY_CONTAIN
- ingrediente C -> POSSIBLE_CROSS_REACTIVITY
```

La decisión afecta al modelo de dominio del agregador, a la presentación de la receta y a las pruebas de seguridad. Por ello no debe fijarse como un detalle incidental de UI.

## 2. Invariantes no negociables

Cualquier opción deberá cumplir:

1. Ninguna relación de linaje crea o hereda seguridad.
2. Ninguna exención regulatoria elimina, rebaja o neutraliza automáticamente una observación de seguridad.
3. Una exención es información legal/auditable y no una afirmación clínica.
4. No se asignará una puntuación clínica ni una probabilidad de reacción.
5. No se perderá la trazabilidad de los ingredientes y relaciones que originan el resultado.
6. `POSSIBLE_CROSS_REACTIVITY` nunca se presentará como presencia confirmada.
7. La composición desconocida puede producir un estado global `REQUIERE_REVISIÓN` aunque no exista un grupo concreto que pueda asignarse sin evidencia.
8. La ausencia de coincidencias solo permite afirmar que no se detectaron coincidencias en los datos registrados; no permite declarar la receta segura.

## 3. Opción A — Grupo único con todos los estados independientes

Para cada grupo se muestra una única tarjeta o sección, pero dentro de ella se presentan todas las observaciones aplicables sin elegir una principal.

Ejemplo:

```text
Grupo X
- Presencia registrada: ingrediente A
- Puede contener declarado: ingrediente B
- Posible reactividad cruzada: ingrediente C
```

### Ventajas

- máxima fidelidad a los datos;
- no introduce una jerarquía entre tipos de relación;
- muy sencilla de auditar.

### Inconvenientes

- puede producir una interfaz densa;
- obliga a la persona usuaria a interpretar varias señales simultáneas;
- dificulta una vista resumen compacta cuando una receta contiene muchos grupos.

## 4. Opción B — Estado principal de presentación + evidencia completa conservada

Cada grupo se muestra una sola vez con un **estado principal de presentación**, pero el modelo conserva y permite consultar todas las observaciones que lo originan.

La prioridad propuesta es exclusivamente de presentación, no una escala clínica de gravedad:

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

Si el mismo grupo tiene `CONTAINS` y `DECLARED_MAY_CONTAIN`, el resumen mostraría `PRESENCIA_IDENTIFICADA`, pero el detalle seguiría conservando ambas observaciones, sus ingredientes, fuentes, evidencia y notas.

La composición desconocida que no pueda vincularse legítimamente a un grupo concreto permanecerá como advertencia global de receta y no se asignará artificialmente a los 14 grupos.

### Ventajas

- permite una vista resumen compacta y comprensible;
- cumple el requisito de no duplicar visualmente grupos;
- no pierde evidencia porque el colapso solo afecta a la presentación;
- facilita pruebas deterministas del agregador;
- evita convertir tipos heterogéneos en una puntuación numérica.

### Inconvenientes

- introduce una regla de precedencia que debe documentarse y probarse;
- el término `principal` podría confundirse con gravedad si la UI no explica correctamente su significado;
- obliga a conservar explícitamente dos niveles: resumen y observaciones completas.

## 5. Opción C — Puntuación o semáforo de riesgo

Transformar las relaciones en un valor, nivel numérico o semáforo único por grupo/receta.

### Ventajas

- interfaz aparentemente sencilla;
- comparación rápida.

### Inconvenientes

- introduce falsa precisión;
- mezcla evidencia, presencia, PAL, incertidumbre y reactividad cruzada;
- puede interpretarse como evaluación clínica de riesgo;
- requeriría criterios clínicos y cuantitativos que el producto no posee;
- aumenta considerablemente el riesgo de una conclusión engañosa.

**No recomendada.**

## 6. Exenciones regulatorias

Independientemente de A o B, las exenciones de ADR-026 permanecen en un canal separado.

Ejemplo conceptual:

```text
Resumen de seguridad        -> observaciones de seguridad
Información regulatoria     -> exenciones aplicables a la identidad exacta
```

Una exención puede mostrarse como información legal contextual, pero no puede ejecutar una operación equivalente a:

```text
warning = warning - exemption
```

Cualquier futura regla que pretendiese modificar una advertencia clínica en función de una exención requeriría una ADR independiente y evidencia específica.

## 7. Decisión

**Se adopta la Opción B.**

La Fase 7 utilizará un estado principal determinista **solo para presentación**, conservando siempre el conjunto completo de observaciones y su trazabilidad.

La precedencia aprobada es:

```text
PRESENCIA_IDENTIFICADA
DERIVADO_IDENTIFICADO
PUEDE_CONTENER_DECLARADO
POSIBLE_REACTIVIDAD_CRUZADA
REQUIERE_REVISION
```

Esta precedencia no representa gravedad médica, probabilidad de reacción ni recomendación de consumo.

## 8. Consecuencia de la decisión

La Fase 7 implementará inicialmente:

```text
RecipeSafetyAggregator        función pura de dominio
RecipeSafetySummary           resultado global
RecipeSafetyGroupSummary      un grupo + estado principal + observaciones
RecipeSafetyObservation       ingrediente + relación + evidencia/procedencia
RecipeReviewNotice            advertencias globales no asignables a un grupo
```

Las exenciones regulatorias se transportarán en una colección/canal separado y no participarán en el cálculo del estado principal.

La UI y el wording se diseñarán después sobre este contrato, manteniendo las expresiones de seguridad ya aprobadas.

## 9. Estado de implementación

```text
Decisión ADR-027                ACEPTADA — B
Modelo de dominio Fase 7        PENDIENTE
Agregador puro                  PENDIENTE
Pruebas deterministas           PENDIENTE
Integración UI                   PENDIENTE
```
