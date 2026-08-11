# 43 — CATÁLOGO v11: COBERTURA DE EXENCIONES DEL ANEXO II — GATE

**Estado:** VALIDADO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-10  
**Catálogo:** v11  
**Relacionado:** ADR-026 / ADR-028

## Objetivo

Cerrar la auditoría de las excepciones explícitas del Anexo II del Reglamento (UE) n.º 1169/2011 que todavía no estaban representadas en el catálogo v10, manteniendo separadas la evidencia de seguridad alimentaria y la semántica legal de etiquetado.

## Fuente jurídica revisada

Se utilizó como fuente de verdad la versión consolidada vigente a 2025-04-01 del Reglamento (UE) n.º 1169/2011, CELEX `02011R1169-20250401`.

El catálogo conserva la fuente oficial existente:

```text
EU_FIC_1169_2011
Regulation (EU) No 1169/2011 — consolidated 2025-04-01
CELEX 02011R1169-20250401
```

Sus metadatos no se modificaron, respetando la inmutabilidad de fuentes de ADR-028.

## Cobertura añadida

v10 cubría ya las excepciones modeladas para cereales, soja y mostaza, pero quedaban seis cláusulas expresas sin identidad regulatoria propia.

v11 añade:

1. cereales con gluten utilizados para hacer destilados alcohólicos;
2. gelatina de pescado utilizada como soporte de vitaminas o preparados de carotenoides;
3. gelatina de pescado o ictiocola utilizada como clarificante en cerveza y vino;
4. lactosuero utilizado para hacer destilados alcohólicos;
5. lactitol;
6. frutos de cáscara utilizados para hacer destilados alcohólicos.

## Modelado conservador

Las excepciones condicionadas no se asignan a ingredientes genéricos como trigo, pescado, leche o frutos de cáscara.

El motor regulatorio actual no interpreta semánticamente el texto libre de `conditions`; por ello la condición de uso o proceso forma parte de la identidad técnica cuando es necesaria para determinar la excepción.

Ejemplo:

```text
NO  -> Lactosuero + condición textual "si se usa para destilados"
SÍ  -> Lactosuero utilizado para hacer destilados alcohólicos
```

Esto evita que una receta con lactosuero ordinario reciba una excepción legal que no le corresponde.

El linaje culinario continúa sin propagar ni evidencia de seguridad ni exenciones regulatorias.

## Catálogo v11

```text
schemaVersion           4
catalogVersion          11
releaseStatus           DRAFT
categories              20
ingredients             272
aliases                 245
ingredientRelations      43
safetyGroups             14
safetySources             3
safetyRelations          33
regulatoryExemptions     20
```

Las seis identidades nuevas no reciben relaciones de seguridad. Su información legal vive exclusivamente en el registro regulatorio independiente.

## Historial regulatorio

La prueba de actualización importa primero v10 y posteriormente v11.

Resultado esperado y validado:

```text
v10 activa
- exenciones operativas       14
- snapshots totales           14

v11 activa
- exenciones operativas       20
- snapshots v10 conservados   14
- snapshots v11               20
- snapshots totales           34
```

La fotografía v10 no se reescribe y las consultas operativas v11 no devuelven las filas históricas v10.

## Controles contra falsos positivos

La batería verifica expresamente que no se creen exenciones nuevas sobre las identidades genéricas de:

- trigo, espelta, khorasan, centeno, cebada y avena;
- pescado;
- leche;
- almendra, avellana, nuez, anacardo, pacana, nuez de Brasil, pistacho y macadamia.

También comprueba que las seis identidades técnicas nuevas tengan cero relaciones de seguridad directas.

## Sulfitos

El umbral del Anexo II para dióxido de azufre y sulfitos no se modela como `RegulatoryExemption`. Se trata de una condición cuantitativa de declaración y no de una excepción nominal equivalente a las registradas en este bloque.

## Productos derivados

No se introduce herencia automática de las excepciones hacia productos derivados. La nota del Anexo II condiciona determinados derivados a que el procesamiento no sea probable que aumente el nivel de alergenicidad evaluado por la autoridad competente. Cualquier ampliación futura requerirá revisión y evidencia explícitas.

## Gate automatizado

Ejecución de cierre:

```text
Workflow: Android CI
Run:      31380379183
Code SHA: 9d2cb50ba6ab154feedce9c0a5936314106b60a1
```

Resultado:

```text
assembleDebug                  PASS
unit tests                     PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
assembleRelease                PASS
Room schema guard              PASS
connectedDebugAndroidTest      PASS — 64/64
Room guard post-emulador       PASS
```

La ejecución instrumentada finalizó con:

```text
Starting 64 tests on emulator-5554 - 16
Finished 64 tests on emulator-5554 - 16
BUILD SUCCESSFUL
```

## Cierre

El catálogo v11 completa la representación explícita de las excepciones del Anexo II dentro del alcance definido, sin convertir ninguna excepción legal en una afirmación clínica de seguridad y sin perder el histórico regulatorio introducido por ADR-028.

El siguiente incremento puede centrarse en presentar esta información en RecipeDetail mediante un bloque visual separado de las alertas de seguridad.
