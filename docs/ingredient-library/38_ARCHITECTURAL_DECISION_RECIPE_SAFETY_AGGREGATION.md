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

El diseño funcional exige:

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

La lógica pura de dominio queda implementada mediante:

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

## 6. Resolución de evidencia implementada

La integración de datos se realiza mediante:

```text
BuildRecipeSafetySummaryUseCase
RecipeSafetySummaryResolver
```

El resolver:

- fuerza la disponibilidad del catálogo versionado antes de leer evidencia;
- consulta únicamente relaciones de seguridad explícitas del ingrediente exacto de catálogo;
- consulta declaraciones explícitas de ingredientes personalizados;
- conserva grupo, relación, evidencia, fuente, notas y fecha de revisión;
- conserva exenciones regulatorias en el canal separado definido por ADR-026;
- genera avisos globales si una identidad no puede resolverse o si la composición personalizada está marcada como desconocida;
- nunca consulta el grafo de linaje para inferir seguridad.

El DAO incorpora una lectura con `JOIN` a grupo y fuente para que la UI no pierda la procedencia de la evidencia de catálogo.

## 7. Presentación en RecipeDetail

`RecipeDetailViewModel` resuelve el resumen para la receta observada y entrega a la pantalla:

```text
safetySummary
safetyMessage
```

`RecipeDetailScreen` muestra el panel `⚠ Información sobre seguridad alimentaria` con:

- grupo y estado principal;
- ingrediente que origina cada observación;
- tipo de relación;
- nivel de evidencia;
- fuente y fecha de revisión cuando están disponibles;
- avisos de revisión globales;
- texto neutral cuando no hay coincidencias;
- recordatorio de que la información disponible puede ser incompleta.

No se muestran certificados verdes ni expresiones de seguridad absoluta.

Las exenciones regulatorias se conservan en el resumen, pero esta primera UI no las mezcla con la advertencia clínica. Una futura presentación legal detallada deberá mantener el canal visual separado.

## 8. Pruebas implementadas

La batería cubre:

- precedencia de presentación sin pérdida de observaciones;
- `DERIVED_FROM` como estado propio;
- reactividad cruzada separada de presencia;
- `UNKNOWN` como revisión;
- avisos globales sin grupos inventados;
- exenciones regulatorias sin supresión de alertas;
- orden determinista;
- combinación de evidencia de catálogo y personalizada;
- composición desconocida como aviso global;
- ingrediente sin identidad como revisión, sin inferencia;
- identidad de catálogo no disponible como revisión;
- renderizado del panel de seguridad;
- lenguaje neutral cuando no se detectan coincidencias.

## 9. Estado de implementación

```text
Decisión ADR-027                ACEPTADA — B
Modelo de dominio Fase 7        IMPLEMENTADO
Agregador puro                  IMPLEMENTADO
Adaptador catálogo              IMPLEMENTADO
Adaptador custom                IMPLEMENTADO
Integración con RecipeDetail    IMPLEMENTADA
UI de información/alertas       IMPLEMENTADA
Pruebas unitarias               IMPLEMENTADAS
Pruebas UI                      IMPLEMENTADAS
Gate Gradle local               PENDIENTE
Room schema                     SIN CAMBIOS — debe permanecer v4
```

No se ha introducido ninguna puntuación clínica, inferencia por linaje ni supresión automática basada en exenciones.
