# 40 — DECISIÓN ARQUITECTÓNICA: HISTORIAL REGULATORIO VERSIONADO

**Estado:** ACEPTADA — OPCIÓN B  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-10  
**ADR:** ADR-028

## 1. Contexto

ADR-026 separó correctamente las exenciones regulatorias de las relaciones de seguridad alimentaria. La integración de Fase 7 añadió además la evaluación por jurisdicción y fecha efectiva.

Al revisar la persistencia se detectó una incoherencia: Room v4 solo podía conservar una fila por combinación conceptual de exención y el importador reemplazaba las exenciones al actualizar el catálogo. Eso permitía conocer el estado actual, pero no reconstruir qué información regulatoria conocía una versión anterior de la aplicación.

La regla ya aceptada en ADR-026 exige que los cambios regulatorios produzcan nuevas revisiones y que la historia no se reescriba silenciosamente.

## 2. Alternativas evaluadas

### Opción A — Historial únicamente en Git y en los assets de catálogo

Room conservaría solo la fotografía actual. Reduce complejidad local, pero impide una auditoría completa desde la base instalada y obliga a reconstruir el pasado desde el repositorio.

### Opción B — Snapshots regulatorios versionados en Room

Cada versión de catálogo conserva su propia fotografía de las exenciones. Las consultas operativas usan exclusivamente la versión de catálogo activa, mientras las consultas de auditoría pueden recuperar versiones anteriores.

### Opción C — Tabla actual + tabla histórica independiente

Separaría físicamente las filas actuales de las históricas. Es explícito, pero duplica contratos, aumenta el riesgo de desincronización y no aporta una ventaja suficiente para este caso.

## 3. Decisión

**Se adopta la Opción B.**

Room v5 conserva las exenciones regulatorias como snapshots asociados a `catalogVersion`.

La identidad técnica de una fila pasa a ser:

```text
id + catalogVersion
```

La restricción conceptual pasa a ser:

```text
ingredientId + safetyGroupId + jurisdiction + regulatoryEffect + catalogVersion
```

Una misma exención puede, por tanto, existir en varias versiones del catálogo sin que una actualización sobrescriba la fotografía anterior.

## 4. Dos ejes temporales distintos

`catalogVersion` y `effectiveFrom/effectiveTo` no representan lo mismo y no deben mezclarse.

```text
catalogVersion
= qué fotografía regulatoria conocía la aplicación

effectiveFrom / effectiveTo
= cuándo produce efectos jurídicos la regla descrita
```

Una exención puede aparecer por primera vez en una versión de catálogo y tener una fecha de efecto anterior o posterior. La consulta operativa debe satisfacer ambos criterios:

1. pertenecer al snapshot de catálogo actualmente activo;
2. ser aplicable a la jurisdicción y fecha consultadas.

## 5. Política de importación

La importación deja de borrar `regulatory_exemptions`.

Al importar un catálogo:

1. cada exención recibe el `catalogVersion` del `manifest`;
2. se inserta o actualiza únicamente el snapshot de esa versión;
3. los snapshots de versiones anteriores permanecen intactos;
4. `catalog_metadata.master.catalogVersion` define qué snapshot es operativo.

Si una exención existía en v10 y desaparece en v11, la fila v10 permanece para auditoría, pero las consultas operativas de v11 no la devolverán.

Si una exención cambia en v11, la fila v10 permanece sin cambios y v11 contiene la nueva fotografía.

## 6. Consultas

Se distinguen dos familias:

### Operativas

Solo consideran el `catalogVersion` activo y, cuando procede, jurisdicción y ventana de vigencia.

Ejemplos:

```text
getRegulatoryExemptionsForIngredient
getApplicableRegulatoryExemptionsForIngredient
countActiveRegulatoryExemptions
```

### Auditoría

Permiten consultar una fotografía histórica concreta.

```text
getRegulatoryExemptionsForIngredientAtCatalogVersion
countRegulatoryExemptionSnapshots
```

Ninguna consulta histórica se usa para calcular las alertas actuales de una receta.

## 7. Migración Room 4 -> 5

La migración:

1. crea la nueva estructura versionada;
2. toma `catalog_metadata.master.catalogVersion` como versión de la fotografía existente;
3. copia íntegramente las filas v4;
4. conserva condiciones, fuente, fechas de vigencia, fecha de revisión, notas y estado;
5. sustituye la tabla antigua por la versionada;
6. recrea índices y claves foráneas.

Si una base v4 no dispone excepcionalmente de metadata, se usa `catalogVersion = 0` como marcador técnico de procedencia desconocida. Esa fila queda preservada para auditoría y no se convierte en una regla actual de un catálogo posterior.

## 8. Invariantes

1. Una actualización de catálogo no borra historia regulatoria.
2. Una fila histórica nunca se mezcla con la fotografía operativa actual.
3. La vigencia jurídica sigue dependiendo de `effectiveFrom/effectiveTo`, no del número de catálogo.
4. Una exención continúa sin significar `seguro`, `apto` ni ausencia de riesgo clínico.
5. El grafo culinario no modifica ni propaga exenciones.
6. Las relaciones de seguridad no se neutralizan automáticamente por una exención.
7. Los identificadores de fuentes regulatorias deben mantenerse estables; una referencia oficial materialmente distinta debe recibir un identificador de fuente nuevo.
8. Una misma versión de catálogo se considera inmutable una vez publicada; cualquier revisión de contenido regulatorio exige una nueva versión de catálogo.

## 9. Pruebas obligatorias

Room v5 debe cubrir como mínimo:

- migración v4 -> v5 con una exención existente;
- conservación de todos sus campos y asignación del `catalogVersion` previo;
- clave primaria compuesta `id + catalogVersion`;
- coexistencia de snapshots históricos y actuales;
- exclusión de snapshots históricos en consultas operativas;
- consulta explícita de una versión histórica;
- filtrado por jurisdicción;
- exclusión de reglas futuras, caducadas e inactivas;
- migración completa desde instalaciones antiguas hasta v5;
- `PRAGMA foreign_key_check` sin incidencias.

## 10. Consecuencias

La base instalada pasa a ser auditable sin depender de reconstruir el estado desde commits de GitHub. El coste es un crecimiento acumulativo de un conjunto regulatorio pequeño y controlado, asumible para una aplicación local.

Esta decisión no versiona todo el catálogo culinario. El histórico persistente se introduce específicamente para el registro regulatorio porque ADR-026 exige trazabilidad temporal y porque una reescritura silenciosa podría alterar el significado legal de información ya revisada.

## 11. Estado de implementación

```text
ADR-028                                  ACEPTADA — B
Room                                     v5
RegulatoryExemption.catalogVersion       IMPLEMENTADO
Clave snapshot id + catalogVersion       IMPLEMENTADA
Migración 4 -> 5                         IMPLEMENTADA
Importación sin borrar historial         IMPLEMENTADA
Consulta snapshot actual                 IMPLEMENTADA
Consulta histórica explícita             IMPLEMENTADA
Filtro jurisdicción + vigencia            IMPLEMENTADO
Pruebas unitarias/DAO                    IMPLEMENTADAS
Prueba migración 4 -> 5                  IMPLEMENTADA
Schema Room v5                           PENDIENTE DE GENERAR/VALIDAR EN CI
Gate CI completo                         PENDIENTE
```

Relacionados:

- `19_ARCHITECTURAL_DECISION_REGULATORY_EXEMPTIONS.md` — ADR-026.
- `38_ARCHITECTURAL_DECISION_RECIPE_SAFETY_AGGREGATION.md` — ADR-027.
- `39_PHASE_07_RECIPE_SAFETY_INTEGRATION_GATE.md` — integración de Fase 7.
