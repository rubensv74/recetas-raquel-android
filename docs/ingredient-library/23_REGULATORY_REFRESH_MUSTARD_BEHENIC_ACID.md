# 23 — REGULATORY REFRESH: ÁCIDO BEHÉNICO PROCEDENTE DE MOSTAZA

**Estado:** REVISIÓN OFICIAL SUPERADA — apto para preparar catálogo v7  
**Fecha de revisión:** 2026-08-08  
**Jurisdicción:** Unión Europea / aplicación directa en España  
**Objeto:** excepción del punto 10 del Anexo II del Reglamento (UE) n.º 1169/2011

## 1. Propósito

Este documento registra la revisión inmediatamente anterior a introducir la primera exención regulatoria real en el catálogo estructurado de la aplicación.

La revisión no pretende convertir una exención de etiquetado en una conclusión clínica. Su único objetivo es comprobar que el efecto jurídico, las condiciones y la fuente que se van a codificar coinciden con una fuente oficial vigente.

Regla permanente:

```text
exención de declaración obligatoria != ausencia de alérgeno
exención de declaración obligatoria != ausencia de riesgo
exención de declaración obligatoria != aptitud clínica
```

## 2. Fuentes oficiales revisadas

### Fuente jurídica primaria

**Reglamento Delegado (UE) 2024/2512 de la Comisión, de 17 de abril de 2024**  
CELEX: `32024R2512`  
Publicación: DO L, 25-09-2024  
Estado consultado el 2026-08-08: en vigor  
Aplicación: desde 01-04-2025

EUR-Lex:

```text
https://eur-lex.europa.eu/eli/reg_del/2024/2512/oj
```

El artículo 1 sustituye el punto 10 del Anexo II y establece la excepción para ácido behénico procedente de mostaza bajo condiciones concretas.

### Texto consolidado de apoyo

**Reglamento (UE) n.º 1169/2011 — texto consolidado 01-04-2025**  
CELEX: `02011R1169-20250401`

```text
https://eur-lex.europa.eu/legal-content/EN/ALL/?uri=CELEX:02011R1169-20250401
```

La versión consolidada incorpora el Reglamento Delegado (UE) 2024/2512 como modificación M4. EUR-Lex advierte que el texto consolidado es una herramienta documental; para la fuente jurídica se conserva como referencia principal el acto publicado en el Diario Oficial.

### Base científica citada por el acto jurídico

El Reglamento Delegado (UE) 2024/2512 cita la reevaluación de EFSA de 2023:

```text
Re-evaluation of behenic acid from mustard seeds to be used in the
manufacturing of certain emulsifiers pursuant to Article 21(2) of
Regulation (EU) No 1169/2011 – for permanent exemption from labelling.
EFSA Journal 2023;21(9):8240
DOI: 10.2903/j.efsa.2023.8240
```

La opinión científica sustenta la decisión regulatoria, pero el registro de exención en la aplicación utilizará como `sourceId` el acto jurídico oficial `EU_MUSTARD_2024_2512`.

## 3. Texto material que debe modelarse

El alcance normativo es limitado al ácido behénico procedente de semillas de mostaza cuando se cumplen simultáneamente estas condiciones:

```text
pureza mínima:             85 %
proceso:                   obtenido tras dos fases de destilación
uso:                       fabricación de los emulgentes E470a, E471 y E477
grupo del Anexo II:        MOSTAZA
fecha de aplicación:       2025-04-01
efecto regulatorio:        excepción de declaración obligatoria del Anexo II
```

No se codificará una exención genérica para:

```text
mostaza
aceite de mostaza
ácido behénico de procedencia desconocida
ácido behénico de mostaza con pureza no acreditada
ácido behénico de mostaza sin las dos fases de destilación
otros emulgentes diferentes de E470a, E471 y E477
```

## 4. Representación prevista en catálogo v7

### Ingrediente técnico regulatorio

Se creará una identidad específica:

```text
id:                 ing-mustard-behenic-acid
nombre:             Ácido behénico de semillas de mostaza
categoría:          Aceites y grasas
verificación:       VERIFIED
variabilidad:       VARIABLE_BY_PREPARATION
```

La identidad no contiene por sí sola la conclusión regulatoria. Las condiciones permanecen en `RegulatoryExemption`.

### Linaje no clínico

Se registrará:

```text
ing-mustard-behenic-acid
  DERIVED_FROM
    ing-mustard
```

La arista tendrá referencia documental al Reglamento Delegado (UE) 2024/2512.

La existencia de esta arista no propagará ninguna relación de seguridad.

### Exención regulatoria

Registro previsto:

```text
ingredientId:       ing-mustard-behenic-acid
safetyGroupId:      sg-eu-mustard
jurisdiction:       EU-ES
regulatoryEffect:   EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
sourceId:           EU_MUSTARD_2024_2512
effectiveFrom:      2025-04-01
effectiveTo:        null
reviewedAt:         2026-08-08
isActive:           true
```

`conditions` conservará de forma expresa pureza mínima, dos fases de destilación y uso exclusivo en E470a, E471 y E477.

## 5. Decisiones deliberadas de seguridad

### No se añadirá una relación clínica positiva o negativa

El catálogo v7 no añadirá una relación `IngredientSafetyRelation` para este ingrediente técnico como consecuencia automática de la exención.

Motivo: el contrato del proyecto separa el efecto regulatorio de la interpretación clínica. La evaluación científica explica por qué se adoptó la excepción, pero la aplicación no debe transformar el acto jurídico en frases como:

```text
"no causa alergia"
"es seguro"
"apto para alérgicos a la mostaza"
"sin riesgo"
```

### No se aplicará la exención a la mostaza padre

`ing-mustard` conserva su relación regulatoria con el grupo `sg-eu-mustard`. La excepción pertenece al derivado técnico bajo condiciones, no a la mostaza en general.

### No se deducirán productos comerciales

La presencia de E470a, E471 o E477 en un producto no permitirá inferir automáticamente que se utilizó este ácido behénico ni que se cumplen sus condiciones de producción.

## 6. Vigencia y mantenimiento

A fecha de revisión 2026-08-08:

- el Reglamento Delegado (UE) 2024/2512 figura en EUR-Lex como **en vigor**;
- su fecha de aplicación es **01-04-2025**;
- el texto consolidado del Reglamento 1169/2011 incorpora esta modificación;
- no se ha identificado en la revisión una modificación posterior del punto 10 que sustituya esta excepción.

Este resultado no elimina la obligación de repetir el `regulatory refresh` antes de una publicación, registro o actualización futura del catálogo sensible.

## 7. Criterio de aprobación para v7

La incorporación podrá avanzar si el bundle v7 conserva simultáneamente:

```text
1 exención regulatoria real
1 ingrediente técnico nuevo
1 relación DERIVED_FROM nueva
0 inferencias de seguridad automáticas
0 cambios en las 33 relaciones de seguridad existentes
Room sigue en versión 4
```

El catálogo v6 permanece inmutable.

## 8. Resultado

```text
FUENTE JURÍDICA                 CONFIRMADA
ESTADO EN VIGOR                 CONFIRMADO
FECHA DE APLICACIÓN             CONFIRMADA — 2025-04-01
CONDICIONES                     CONFIRMADAS
GRUPO MOSTAZA                   CONFIRMADO
EFECTO REGULATORIO              DELIMITADO
INTERPRETACIÓN CLÍNICA          NO CODIFICADA
AUTORIZACIÓN PARA PREPARAR v7   SÍ
```
