# CATALOG MAINTENANCE GUIDE

**Estado:** operativo para mantenimiento manual/versionado; mecanismo de distribución remota todavía no decidido.

## Principio de mantenimiento

El catálogo no se considera conocimiento estático. Cada versión publicada es inmutable y cualquier corrección o nueva evidencia debe producir una versión posterior trazable.

Ninguna publicación científica, alerta o resultado de IA modifica automáticamente una relación de seguridad.

Ver también:

```text
docs/food-safety/SAFETY_KNOWLEDGE_MAINTENANCE_REQUIREMENTS.md
docs/food-safety/REGISTRATION_SUPPORT_FOOD_SAFETY_DOSSIER.md
```

## Añadir una categoría

1. Crear código/ID estable.
2. Añadir nombre localizado, orden y estado activo.
3. Ejecutar validación completa del catálogo.

## Añadir un ingrediente

1. ID estable.
2. Nombre canónico + clave normalizada de búsqueda.
3. Categoría existente.
4. Unidad/descripción opcionales.
5. No crear ninguna relación de seguridad salvo que exista evidencia separada y trazable.

## Añadir un alias

El alias debe referenciar un ingrediente existente y disponer de su propia clave normalizada de búsqueda. Los alias no son reglas fuzzy ni sustituyen relaciones de identidad/linaje.

## Registrar una fuente

Añadir primero la fuente con metadatos suficientes; después crear las relaciones de seguridad que la referencien.

La fuente debe poder distinguir, cuando aplique:

- organización;
- documento o referencia oficial;
- jurisdicción;
- fecha de publicación;
- fecha de revisión;
- estado/vigencia;
- URL oficial o referencia estable.

## Añadir una relación de seguridad

Campos mínimos obligatorios:

```text
ingredientId
safetyGroupId
relationType
evidenceLevel
sourceId
reviewedAt
```

No inferir datos ausentes.

## Corregir una relación

1. Identificar la fuente que justifica el cambio.
2. Registrar la nueva evidencia y fecha de revisión.
3. Conservar el historial en Git/versiones del catálogo.
4. No reutilizar silenciosamente una relación histórica cambiando su significado.
5. Comparar el efecto sobre alertas antes de publicar.

## Desactivar un ingrediente

Marcarlo inactivo. No eliminar físicamente un maestro utilizado por recetas.

## Localizar registros pendientes

El validador y los informes de mantenimiento deben poder localizar como mínimo:

```text
UNVERIFIED
REVIEW_REQUIRED
UNKNOWN
fuentes con revisión antigua
relaciones con fuente sustituida
```

## Revisión periódica de conocimiento sensible

Realizar una revisión formal **al menos anual**, además de revisiones extraordinarias por evento cuando aparezcan cambios relevantes.

La revisión periódica debe comprobar:

- legislación UE aplicable y Anexo II;
- exenciones vigentes;
- documentación y alertas AESAN;
- actualizaciones EFSA;
- cambios relevantes FAO/WHO y Codex;
- registros de fuentes obsoletos;
- evidencia `UNVERIFIED`;
- ingredientes `REVIEW_REQUIRED`;
- etiquetas de fabricante con antigüedad relevante;
- huecos documentados en `RESEARCH_GAPS.md`.

## Revisión extraordinaria por evento

Abrir revisión sin esperar al ciclo anual ante:

- modificación normativa aplicable;
- nueva exención o retirada de una exención;
- alerta sanitaria relevante;
- corrección oficial de una fuente ya utilizada;
- nueva evidencia científica oficial con impacto material;
- error detectado en una relación sensible;
- cambio documentado de formulación de un producto comercial.

## Nueva versión del catálogo

1. Crear nueva versión sin modificar una versión publicada existente.
2. Actualizar el registro de fuentes cuando corresponda.
3. Aplicar cambios de ingredientes/alias/relaciones.
4. Ejecutar validación completa del bundle.
5. Comparar recuentos y relaciones contra la versión anterior.
6. Revisar deltas de fuentes y evidencia.
7. Ejecutar pruebas de importación/rollback y regresión.
8. Registrar decisión de publicación.
9. Importar transaccionalmente.

El `catalogVersion` continúa siendo independiente de la versión de Room.

## Informe de diferencias obligatorio

Para cambios sensibles, generar un resumen auditable:

```text
versión anterior -> versión nueva
fuentes añadidas/modificadas
relaciones añadidas/modificadas/sustituidas
evidencia elevada/degradada
ingredientes desactivados
exenciones afectadas
registros que requieren revisión
preguntas sin resolver
```

## Uso futuro para auditoría/registro

La historia de mantenimiento debe permitir demostrar qué versión de conocimiento utilizaba la aplicación, qué fuentes respaldaban cada relación, cuándo se revisaron y qué cambios se introdujeron posteriormente.

El mecanismo técnico para distribuir actualizaciones del catálogo fuera de una nueva release de la app queda pendiente de una decisión arquitectónica específica. No se implementa implícitamente desde esta guía.
