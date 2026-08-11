# 19 — DECISIÓN ARQUITECTÓNICA: EXENCIONES REGULATORIAS

**Estado:** ACEPTADA — OPCIÓN B · EVOLUCIONADA POR ADR-028  
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

## 6. Persistencia inicial y evolución

ADR-026 introdujo inicialmente `regulatory_exemptions` en Room v4 como tabla separada. Ese diseño resolvió la separación semántica, pero su clave única inicial solo permitía una fotografía de cada regla.

La auditoría posterior detectó que esa restricción no satisfacía completamente la regla 8: una actualización de catálogo podía reemplazar la fotografía anterior.

Por ello **ADR-028 evoluciona la persistencia a Room v5** y añade `catalogVersion` como dimensión histórica. La identidad del snapshot pasa a ser:

```text
id + catalogVersion
```

La unicidad conceptual se aplica dentro de cada versión:

```text
ingredientId + safetyGroupId + jurisdiction + regulatoryEffect + catalogVersion
```

Las referencias a ingrediente, grupo y fuente continúan usando claves foráneas con `NO ACTION`.

## 7. Versionado y vigencia

Desde Room v5 se distinguen explícitamente dos ejes:

```text
catalogVersion              -> fotografía conocida por la app
effectiveFrom / effectiveTo -> vigencia jurídica
```

Las consultas operativas leen únicamente el snapshot correspondiente a `catalog_metadata.master.catalogVersion` y después aplican jurisdicción y fechas de efecto. Los snapshots anteriores permanecen disponibles solo para auditoría.

El importador ya no borra el registro regulatorio histórico. Cada nueva versión de catálogo incorpora su propia fotografía.

## 8. Consecuencias

La decisión permite modelar derivados cuya situación legal depende del proceso o pureza sin confundir la excepción con evidencia clínica, y permite además reconstruir el estado regulatorio conocido por versiones anteriores del catálogo.

La actualización regulatoria deja de reescribir silenciosamente la historia local. Si una regla desaparece de un catálogo posterior, su snapshot histórico permanece, pero no participa en las consultas operativas actuales.

## 9. Estado de implementación

```text
Decisión ADR-026                         ACEPTADA — B
Separación regulatoria                   IMPLEMENTADA
Entidad RegulatoryExemption              IMPLEMENTADA
Room v4 inicial                          IMPLEMENTADO
Evolución histórica ADR-028              ACEPTADA — B
Room v5                                  IMPLEMENTADO EN CÓDIGO
Migración Room 4 -> 5                    IMPLEMENTADA
Snapshots por catalogVersion             IMPLEMENTADOS
Filtro por snapshot actual               IMPLEMENTADO
Filtro por jurisdicción + vigencia        IMPLEMENTADO
Catálogo con exenciones reales           IMPLEMENTADO hasta v10
Propagación automática                   PROHIBIDA
Schema Room v5 / gate CI                 EN VALIDACIÓN
```

Ver también:

- `20_ROOM_V4_REGULATORY_EXEMPTIONS.md` — implementación inicial histórica.
- `40_ARCHITECTURAL_DECISION_REGULATORY_HISTORY.md` — ADR-028, diseño vigente del historial regulatorio.
