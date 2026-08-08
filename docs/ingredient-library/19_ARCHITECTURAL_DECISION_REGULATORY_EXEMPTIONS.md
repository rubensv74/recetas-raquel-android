# 19 — DECISIÓN ARQUITECTÓNICA: EXENCIONES REGULATORIAS

**Estado:** ACEPTADA — OPCIÓN B  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-08

## 1. Contexto

La evolución del catálogo desde ingredientes simples hacia derivados revela una diferencia que debe representarse de forma explícita:

```text
relación de seguridad positiva != excepción regulatoria != afirmación clínica de seguridad
```

El Anexo II del Reglamento (UE) 1169/2011 contiene grupos de sustancias/productos que deben declararse, pero también excepciones concretas y condicionadas. Entre otras, la versión consolidada vigente desde 2025-04-01 recoge:

- determinados jarabes de glucosa y maltodextrinas derivados de cereales;
- determinados usos de gelatina de pescado;
- aceite y grasa de soja **totalmente refinados**;
- determinados derivados de soja;
- determinados derivados de leche;
- frutos de cáscara usados para destilados;
- una exención específica para ácido behénico derivado de mostaza bajo condiciones de pureza/proceso.

Estas excepciones no deben interpretarse por la aplicación como `sin riesgo`, `apto` o equivalentes. Son decisiones regulatorias de etiquetado/alcance legal, no una garantía clínica universal.

## 2. Problema

El modelo previo dispone de:

- grafo culinario de linaje (`VARIANT_OF`, `CUT_OF`, `DERIVED_FROM`, `FORM_OF`);
- relaciones positivas/advertencias de seguridad con fuente y nivel de evidencia;
- notas libres.

No existía una estructura dedicada para expresar:

- que un derivado está excluido de una obligación regulatoria concreta;
- bajo qué condiciones exactas se aplica la excepción;
- jurisdicción;
- fuente legal;
- fecha de revisión/vigencia;
- diferencia entre exención legal y evaluación clínica.

Usar simplemente la ausencia de una relación de seguridad sería ambiguo y poco auditable.

## 3. Alternativas evaluadas

### Opción A — Añadir `REGULATORY_EXEMPTION` al grafo de seguridad

Reutilizaría `IngredientSafetyRelation`, pero mezclaría presencia/riesgo con efectos jurídicos de signo distinto y trasladaría condiciones complejas a campos poco estructurados.

### Opción B — Registro estructurado de exenciones regulatorias separado

Crear un concepto independiente `RegulatoryExemption`, relacionado con ingrediente/derivado, grupo regulatorio y fuente oficial.

Campos iniciales:

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

Efecto inicial previsto:

```text
EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
```

### Opción C — Mantener exenciones solo en documentación/notas

Evitaría cambios de esquema, pero ofrecería una base insuficiente para auditoría, actualización periódica y UI consistente.

## 4. Decisión

**Se adopta la Opción B.**

Las excepciones regulatorias se almacenarán en una capa independiente y estructurada. Se preservan tres conceptos separados:

```text
1. linaje culinario
2. evidencia/alertas de seguridad
3. exenciones regulatorias
```

Ninguna de las tres capas crea, hereda o propaga automáticamente datos hacia otra.

## 5. Reglas semánticas obligatorias

1. Una exención regulatoria describe un efecto jurídico concreto; no significa ausencia de alérgeno ni ausencia de riesgo clínico.
2. La ausencia de una relación de seguridad nunca significa que exista una exención.
3. La ausencia de una exención nunca significa que exista automáticamente una relación de seguridad.
4. Toda exención debe identificar ingrediente, grupo regulatorio, jurisdicción, efecto, condiciones, fuente oficial y fecha de revisión.
5. `effectiveFrom` y `effectiveTo` son opcionales cuando la fuente revisada no permite registrar una fecha concreta sin inventarla.
6. Las condiciones son obligatorias: no se admiten exenciones genéricas sin capturar el requisito que limita su aplicación.
7. La UI no podrá traducir una exención a expresiones como `seguro`, `apto`, `sin riesgo` o equivalentes.
8. Los cambios regulatorios posteriores deben producir nuevas revisiones/versiones; no se reescribe silenciosamente la historia.

## 6. Persistencia

La implementación inicial usa Room v4 y una tabla independiente:

```text
regulatory_exemptions
```

La clave técnica es `id`. Además se impide duplicar la misma combinación activa conceptual mediante un índice único sobre:

```text
ingredientId + safetyGroupId + jurisdiction + regulatoryEffect
```

Las referencias a ingrediente, grupo y fuente usan claves foráneas con `NO ACTION`, porque forman parte de la trazabilidad y no deben desaparecer en cascada.

## 7. Versionado de catálogo

Room v4 introduce únicamente la capacidad de persistencia. El catálogo activo v6 permanece inmutable y no se reescribe.

La incorporación de exenciones reales se hará en una versión posterior del catálogo, con una evolución explícita del formato del catálogo y validaciones propias. Hasta superar el gate de Room v4 no se añadirán datos regulatorios reales a esta nueva tabla.

## 8. Consecuencias

La decisión permite modelar posteriormente, entre otros casos, derivados cuya situación legal depende del proceso o pureza, sin confundir la excepción con evidencia clínica.

También mejora el futuro mecanismo de actualización anual: una revisión regulatoria podrá detectar cambios de vigencia, condiciones o fuentes en una capa específicamente diseñada para ello.

## 9. Estado de implementación

```text
Decisión                         ACEPTADA — B
Entidad RegulatoryExemption     IMPLEMENTADA
Migración Room 3 -> 4            IMPLEMENTADA
Schema Room v4                  PENDIENTE DE GENERACIÓN/REVISIÓN LOCAL
Catálogo con exenciones reales  NO INICIADO
Propagación automática          PROHIBIDA
```

Ver también `docs/ingredient-library/20_ROOM_V4_REGULATORY_EXEMPTIONS.md`.
