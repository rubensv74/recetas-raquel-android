# 41 — ROOM v5: HISTORIAL REGULATORIO — GATE

**Estado:** IMPLEMENTADO — VALIDACIÓN CI EN CURSO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-10  
**ADR:** ADR-028

## Objetivo

Validar la decisión B de ADR-028: conservar en Room snapshots históricos de exenciones regulatorias por versión de catálogo, sin permitir que el histórico contamine las reglas operativas actuales.

## Contrato de persistencia

Room queda en versión 5.

`regulatory_exemptions` incorpora:

```text
catalogVersion
```

La identidad del snapshot es:

```text
id + catalogVersion
```

La unicidad conceptual dentro de una fotografía de catálogo es:

```text
ingredientId
+ safetyGroupId
+ jurisdiction
+ regulatoryEffect
+ catalogVersion
```

## Comportamiento de importación

La importación de una versión nueva de catálogo:

1. asigna el `catalogVersion` del manifest a cada exención;
2. no elimina snapshots de versiones anteriores;
3. inserta/actualiza solo la fotografía de la versión importada;
4. actualiza `catalog_metadata.master` para indicar qué fotografía es operativa.

Si una regla desaparece de una versión posterior, permanece en el histórico pero deja de participar en las consultas actuales.

## Consultas operativas

Las lecturas usadas por la aplicación exigen que la exención:

- pertenezca al `catalogVersion` activo;
- corresponda a la identidad exacta del ingrediente;
- sea de la jurisdicción solicitada;
- esté activa;
- haya comenzado su vigencia, si existe `effectiveFrom`;
- no haya finalizado su vigencia, si existe `effectiveTo`.

Para la aplicación actual, el contexto regulatorio por defecto es `EU-ES` y la fecha se calcula mediante `TimeProvider` en zona `Europe/Madrid`.

## Consultas históricas

Se incorpora lectura explícita por versión de catálogo para auditoría. Esta lectura no participa en la agregación de seguridad de una receta.

También se diferencia entre:

```text
countActiveRegulatoryExemptions
```

que cuenta únicamente la fotografía operativa actual, y:

```text
countRegulatoryExemptionSnapshots
```

que permite auditar el volumen histórico persistido.

## Migraciones cubiertas

La batería de instrumentación valida:

```text
1 -> 2 -> 3 -> 4 -> 5
2 -> 3 -> 4 -> 5
4 -> 5
```

La prueba específica 4 -> 5 parte de una base Room v4 que contiene una exención realista y comprueba que se preservan:

- identificador;
- ingrediente;
- grupo;
- jurisdicción;
- efecto regulatorio;
- condiciones;
- fuente;
- `effectiveFrom`;
- `effectiveTo`;
- `reviewedAt`;
- notas;
- estado activo;
- y la versión de catálogo que estaba instalada.

También verifica la clave primaria compuesta y `PRAGMA foreign_key_check`.

## Inmutabilidad de fuentes

Los snapshots regulatorios históricos continúan apuntando a `safety_sources`. Para impedir una reescritura indirecta de la historia, el importador compara cualquier `sourceId` ya persistido con la nueva definición.

Regla:

```text
mismo sourceId = mismos metadatos completos
```

Un cambio material de organización, título, referencia oficial, jurisdicción, fechas, estado documental o URL exige publicar un `sourceId` nuevo. La importación se rechaza antes de modificar el catálogo si incumple esta regla.

Los catálogos v7, v8, v9 y v10 fueron revisados durante la implementación y las fuentes reutilizadas conservan contenido idéntico.

## Esquema Room

El esquema generado y versionado es:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/5.json
```

Identity hash:

```text
f032c684ca112fcab2b4eaa88acb94a2
```

El workflow de CI exige exactamente:

```text
1.json
2.json
3.json
4.json
5.json
```

y falla si Room genera diferencias respecto a esos archivos versionados.

## Gate CI

Ejecución de cierre:

```text
Workflow: Android CI
Run:      31373881075
Code SHA: 546a532b5ef820a887456aa89f72bf722f4ce57f
```

Estado confirmado hasta el momento de redactar este gate:

```text
assembleDebug                  PASS
unit tests                     PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
assembleRelease                PASS
Room schema guard              PASS
connectedDebugAndroidTest      EN CURSO
Room guard post-emulador       PENDIENTE
```

El gate no se considera cerrado hasta que los dos últimos controles estén en verde.

## Semántica de seguridad no modificada

ADR-028 no cambia el significado clínico ni visual de Fase 7.

Permanece vigente:

```text
exención regulatoria != ausencia de alérgeno
exención regulatoria != ausencia de riesgo
exención regulatoria != aptitud clínica
```

Una exención se mantiene en un canal legal separado. No elimina ni rebaja automáticamente una observación de seguridad, y el grafo culinario tampoco propaga exenciones.

## Criterio de cierre

Este documento podrá marcarse `VALIDADO` únicamente cuando el job de emulador y el guard posterior de Room finalicen con `success` en la ejecución indicada.
