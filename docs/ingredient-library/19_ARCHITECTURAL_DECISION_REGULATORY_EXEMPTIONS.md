# 19 — DECISIÓN ARQUITECTÓNICA: EXENCIONES REGULATORIAS

**Estado:** ABIERTA — decisión requerida  
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

El modelo actual dispone de:

- grafo culinario de linaje (`VARIANT_OF`, `CUT_OF`, `DERIVED_FROM`, `FORM_OF`);
- relaciones positivas/advertencias de seguridad con fuente y nivel de evidencia;
- notas libres.

No existe una estructura dedicada para expresar:

- que un derivado está excluido de una obligación regulatoria concreta;
- bajo qué condiciones exactas se aplica la excepción;
- jurisdicción;
- fuente legal;
- fecha de revisión/vigencia;
- diferencia entre exención legal y evaluación clínica.

Usar simplemente la ausencia de una relación de seguridad sería ambiguo y poco auditable.

## 3. Opción A — Añadir `REGULATORY_EXEMPTION` al grafo de seguridad

La excepción se convertiría en un nuevo tipo de `IngredientSafetyRelation`.

### Ventajas

- menor cambio estructural;
- reutiliza tabla, fuentes y trazabilidad existentes;
- implementación rápida.

### Inconvenientes

- mezcla relaciones que describen presencia/riesgo con excepciones legales;
- una misma tabla tendría semánticas de signo opuesto;
- las condiciones complejas acabarían probablemente en `notes`;
- aumenta el riesgo de que una capa de UI interprete la exención como ausencia de riesgo.

## 4. Opción B — Registro estructurado de exenciones regulatorias separado

Crear un concepto independiente, por ejemplo `RegulatoryExemption`, relacionado con ingrediente/derivado, grupo regulatorio y fuente oficial.

Campos conceptuales mínimos:

```text
id
ingredientId
safetyGroupId
jurisdiction
regulatoryEffect
conditions
sourceId
effectiveFrom
effectiveTo?
reviewedAt
notes?
isActive
```

`regulatoryEffect` podría comenzar con un valor explícito como:

```text
EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
```

### Ventajas

- separa claramente derecho alimentario de inferencia clínica;
- permite condiciones auditables;
- facilita futuras revisiones regulatorias;
- encaja mejor con el dossier de soporte de registro;
- evita que ausencia de relación se use como significado implícito;
- permite representar cambios de vigencia sin reescribir la historia.

### Inconvenientes

- requiere nueva entidad/tabla y migración Room;
- requiere ampliar formato de catálogo, validador, importador y pruebas;
- incrementa el coste de mantenimiento.

## 5. Opción C — Mantener exenciones solo en documentación/notas

No se modifica el modelo estructurado. Las excepciones quedan en documentos y, cuando proceda, notas de ingrediente.

### Ventajas

- coste técnico mínimo;
- no requiere migración.

### Inconvenientes

- baja capacidad de auditoría automática;
- difícil actualización anual;
- difícil mostrar información consistente en UI;
- alto riesgo de divergencia entre documentación y catálogo;
- insuficiente como base de largo plazo para conocimiento sensible versionado.

## 6. Recomendación técnica

**Se recomienda la Opción B.**

El objetivo de la aplicación no es solo almacenar una lista de alérgenos, sino conservar la procedencia y significado de información sensible. Una excepción legal tiene naturaleza distinta de una relación de seguridad y merece un registro propio.

La recomendación preserva tres capas independientes:

```text
1. linaje culinario
2. evidencia/alertas de seguridad
3. excepciones regulatorias
```

Ninguna de las tres se propaga automáticamente a otra.

## 7. Consecuencia sobre el catálogo

Hasta resolver esta ADR:

- catálogo v6 permanece válido e inmutable;
- pueden seguir estudiándose ingredientes simples sin exenciones;
- no se incorporarán derivados cuya interpretación correcta dependa de una excepción legal estructurada;
- en particular, no se modelará todavía `aceite de soja totalmente refinado` como si la mera ausencia de una alerta expresara la exención.

## 8. Decisión requerida

Elegir una de las siguientes opciones:

```text
A — Exención como tipo dentro del grafo de seguridad
B — Registro/entidad regulatoria independiente (recomendada)
C — Exenciones solo documentales
```

Tras la decisión se actualizará `DECISIONS.md` y, si procede, se diseñará la migración y la siguiente versión del catálogo.