# 25 — REGULATORY REFRESH: ACEITE Y GRASA DE SOJA TOTALMENTE REFINADOS

**Estado:** REVISADO — apto para staging de catálogo  
**Fecha:** 2026-08-08  
**Jurisdicción:** Unión Europea / España

## 1. Objeto

Revisar una exención expresa del Anexo II del Reglamento (UE) n.º 1169/2011 antes de incorporarla como dato estructurado al catálogo.

El objetivo es responder únicamente a esta cuestión regulatoria:

> ¿El Anexo II vigente excluye de la obligación de declaración de soja al aceite y la grasa de soja totalmente refinados?

Esta revisión no pretende establecer seguridad clínica ni tolerancia individual.

## 2. Fuente primaria

Fuente oficial vigente consultada:

```text
Regulation (EU) No 1169/2011
versión consolidada actual: 01/04/2025
Anexo II, punto 6 — Soybeans and products thereof
CELEX 02011R1169-20250401
```

La versión consolidada oficial continúa mostrando en el punto 6:

```text
Soybeans and products thereof, except:
(a) fully refined soybean oil and fat
```

La versión española expresa la misma excepción como aceite y grasa de soja totalmente refinados.

## 3. Fuente interpretativa complementaria

La Comunicación de la Comisión 2017/C 428/01 continúa siendo una referencia oficial para la aplicación de las reglas de información sobre alérgenos del Anexo II y contiene una sección específica sobre exenciones.

Esta comunicación no sustituye al Reglamento. Para el dato del catálogo, la fuente normativa primaria sigue siendo el Anexo II del Reglamento 1169/2011.

## 4. Resultado regulatorio

Se confirma que la exención es específica para:

```text
aceite de soja totalmente refinado
grasa de soja totalmente refinada
```

No debe generalizarse a:

```text
aceite de soja sin especificar refinado total
grasa de soja sin especificar refinado total
soja
otros derivados de soja
productos elaborados con soja
```

La expresión `totalmente refinado` forma parte del alcance material de la exención y debe conservarse en la identidad del ingrediente.

## 5. Modelado acordado

Se crearán dos identidades técnicas distintas:

```text
ing-fully-refined-soybean-oil
ing-fully-refined-soybean-fat
```

Ambas tendrán linaje culinario/técnico:

```text
DERIVED_FROM -> ing-soybean
```

Y cada una tendrá una fila independiente de `RegulatoryExemption` vinculada a:

```text
safetyGroupId = sg-eu-soybeans
sourceId      = EU_FIC_1169_2011
jurisdiction  = EU-ES
regulatoryEffect = EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
```

## 6. Fecha de vigencia

La excepción forma parte del régimen histórico del Anexo II y no se ha identificado en esta revisión una fecha de inicio específica que deba representarse como cambio nuevo de 2025 o 2026.

Para evitar inventar una fecha material, el staging utilizará:

```text
effectiveFrom = null
effectiveTo   = null
```

La fecha de revisión sí se conserva:

```text
reviewedAt = 2026-08-08
```

## 7. Separación clínica

No se crearán nuevas `IngredientSafetyRelation` para estas dos identidades como resultado de la exención.

Esto es deliberado. Una exención de declaración obligatoria no equivale a una conclusión clínica universal y no autoriza textos como:

```text
seguro para alérgicos a la soja
sin riesgo
sin soja
puede consumirse
```

## 8. Regla de implementación

La exención solo se asocia a las identidades que contienen explícitamente `totalmente refinado`.

El ingrediente padre:

```text
ing-soybean
```

mantiene su relación de seguridad existente con `sg-eu-soybeans`.

Ninguna relación se hereda ni se elimina por el grafo de linaje.

## 9. Fuentes verificadas

- EUR-Lex, Reglamento (UE) 1169/2011, versión consolidada actual a 01/04/2025, Anexo II, punto 6(a).
- Comunicación de la Comisión 2017/C 428/01 sobre información relativa a sustancias o productos que causan alergias o intolerancias.

## 10. Conclusión

```text
REGULATORY REFRESH: PASS
```

Existe evidencia normativa oficial suficiente para preparar un catálogo posterior con dos exenciones regulatorias específicas para aceite y grasa de soja totalmente refinados.

La incorporación seguirá siendo staging hasta superar validación de catálogo, persistencia, separación de seguridad y suite completa.
