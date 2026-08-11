# REQUISITOS DE MANTENIMIENTO DEL CONOCIMIENTO DE SEGURIDAD ALIMENTARIA

**Estado:** REQUISITO FUNCIONAL/TÉCNICO REGISTRADO — arquitectura de distribución pendiente  
**Fecha:** 2026-08-08

## 1. Motivo

La información sobre alergias, intolerancias, celiaquía, reactividad cruzada, procesamiento, exenciones regulatorias, PAL y otros aspectos de seguridad alimentaria no es estática.

El catálogo no debe considerarse un bloque de conocimiento cerrado. Debe existir un proceso mantenible para revisar nuevas evidencias y publicar versiones posteriores sin perder trazabilidad ni modificar silenciosamente la historia.

## 2. Principio fundamental

Una publicación científica, alerta o noticia nueva **no debe modificar automáticamente** una relación de seguridad de la aplicación.

El flujo correcto es:

```text
nueva evidencia detectada
    -> revisión de fuente y alcance
    -> clasificación de evidencia
    -> evaluación del impacto
    -> propuesta de cambio
    -> validación humana/documental
    -> nueva versión inmutable del catálogo
    -> pruebas
    -> distribución controlada
```

La automatización futura puede ayudar a detectar cambios, recopilar metadatos o generar comparaciones, pero no debe convertir por sí sola una publicación nueva en una conclusión clínica.

## 3. Fuentes que deben vigilarse

Como mínimo, el proceso de mantenimiento debe contemplar:

### Marco regulatorio ES/EU

- EUR-Lex / legislación de la Unión Europea aplicable;
- Comisión Europea;
- BOE cuando exista regulación española aplicable;
- AESAN.

### Evidencia científica e institucional

- EFSA;
- FAO/WHO;
- Codex Alimentarius;
- WHO/IUIS para nomenclatura, respetando que nomenclatura no equivale a significación clínica.

### Evidencia específica de producto

- etiquetas oficiales de fabricante;
- comunicaciones o retiradas oficiales;
- cambios de formulación documentados.

Una fuente secundaria, una noticia generalista o una publicación no verificada puede servir como señal para investigar, pero no como evidencia final por sí sola.

## 4. Dos tipos de revisión

### Revisión periódica

Debe realizarse una revisión formal **al menos anual** del conocimiento sensible y del registro de fuentes.

La revisión anual debe comprobar:

- vigencia de las referencias legales;
- cambios del Anexo II o de sus exenciones;
- nuevas orientaciones de AESAN/EFSA;
- cambios relevantes en Codex/FAO/WHO;
- fuentes con fecha de revisión antigua;
- registros `UNVERIFIED` o `REVIEW_REQUIRED`;
- productos o datos de fabricante cuya etiqueta haya quedado obsoleta;
- huecos de investigación pendientes.

### Revisión por evento

No se debe esperar a la revisión anual cuando aparezca alguno de estos eventos:

- cambio legislativo o regulatorio aplicable;
- nueva exención o retirada de una exención;
- alerta sanitaria relevante para un ingrediente o producto registrado;
- actualización oficial que contradiga información vigente en el catálogo;
- nueva evidencia científica oficial con impacto potencial significativo;
- cambio de formulación conocido de un producto comercial;
- detección de un error de seguridad en los datos existentes.

## 5. Registro de cada revisión

Cada ciclo de revisión debe generar un registro reproducible con:

```text
reviewId
reviewDate
reviewType       # ANNUAL / EVENT_DRIVEN
sourcesChecked
sourcesChanged
catalogVersionBefore
proposedCatalogVersion
recordsAdded
recordsChanged
recordsDeprecated
relationsAdded
relationsChanged
relationsRemovedOrSuperseded
unresolvedQuestions
reviewer
validationStatus
releaseDecision
```

El detalle puede materializarse en Markdown/JSON según convenga, pero debe poder auditarse mediante Git.

## 6. Política de actualización de relaciones sensibles

Para cualquier relación que cambie:

- conservar la fuente anterior en la historia;
- registrar la nueva fuente;
- registrar fecha de revisión;
- explicar el motivo del cambio;
- no reutilizar una relación vieja cambiando su significado sin dejar rastro;
- no elevar evidencia `UNVERIFIED` automáticamente;
- no degradar o eliminar una alerta únicamente para reducir falsos positivos sin evidencia suficiente.

Cuando una fuente quede superada, debe poder identificarse como histórica/sustituida sin destruir el registro previo.

## 7. Versionado

El conocimiento sensible debe seguir la regla ya adoptada:

```text
una versión publicada del catálogo es inmutable
```

Los cambios derivados de nueva evidencia deben producir una **nueva versión de catálogo**, aunque Room no cambie.

Esto permite saber exactamente qué conocimiento utilizaba una versión concreta de la aplicación en una fecha determinada.

## 8. Validación previa a publicación

Toda nueva versión del catálogo debe pasar, como mínimo:

- validación de integridad del manifiesto;
- referencias de ingredientes/grupos/fuentes válidas;
- `sourceId`, `evidenceLevel`, `relationType` y `reviewedAt` en relaciones sensibles;
- ausencia de relaciones huérfanas;
- control de duplicados;
- revisión de registros `UNVERIFIED`;
- comparación automática contra la versión anterior;
- pruebas de importación/rollback;
- pruebas de regresión de la aplicación.

Los cambios sensibles deben tener una revisión documental adicional antes de marcar el catálogo como candidato a producción.

## 9. Comparación entre versiones

El mantenimiento debe poder producir un informe del tipo:

```text
vN -> vN+1
- fuentes nuevas/modificadas
- ingredientes nuevos/desactivados
- relaciones de seguridad nuevas
- relaciones modificadas
- evidencia elevada/degradada
- exenciones nuevas/cambiadas
- registros que pasan a REVIEW_REQUIRED
- cambios regulatorios con impacto
```

Este informe formará parte de la evidencia de mantenimiento y del futuro dossier de registro/auditoría.

## 10. Arquitectura de distribución — decisión futura

El requisito de mantener actualizado el conocimiento está aceptado, pero **no se selecciona todavía el mecanismo técnico de distribución**.

La decisión futura deberá comparar al menos:

### Opción A — actualización solo con nueva versión de la app

El catálogo actualizado viaja empaquetado en una nueva release Android.

### Opción B — catálogo remoto firmado y versionado

La aplicación puede obtener un bundle de conocimiento actualizado de un origen controlado sin necesitar una nueva versión binaria.

### Opción C — modelo híbrido

La app incluye una versión base offline y puede instalar versiones de catálogo posteriores verificadas cuando exista conectividad.

La elección afecta a la arquitectura offline-first, superficie de ataque, firma/verificación, rollback, disponibilidad sin red, soporte y proceso de publicación. Por tanto se tratará como **ADR específica y gate arquitectónico** cuando llegue el momento.

No debe implementarse ninguna de estas opciones por defecto sin esa decisión.

## 11. Requisitos de seguridad para cualquier futura distribución remota

Si en el futuro se autoriza un mecanismo remoto, deberá diseñarse para impedir que información sensible sea aceptada sin autenticidad verificable.

Como requisitos mínimos a evaluar:

- autenticidad del origen;
- integridad criptográfica del bundle;
- versión monotónica y protección frente a downgrade accidental;
- validación completa antes de activar una actualización;
- importación transaccional;
- conservación de la última versión válida;
- rollback controlado;
- auditoría de versión instalada;
- ausencia de dependencia de red para leer recetas existentes;
- comportamiento seguro cuando no exista conectividad.

Estos son requisitos, no una implementación aprobada.

## 12. Relación con IA

La IA puede ayudar en tareas auxiliares como:

- localizar publicaciones nuevas;
- clasificar documentos para revisión;
- resumir diferencias;
- proponer candidatos a actualización;
- detectar inconsistencias o fuentes caducadas;
- preparar informes comparativos.

Pero una salida del modelo no constituye una fuente de seguridad alimentaria. Ningún cambio clínico/regulatorio debe entrar en el catálogo únicamente porque lo haya propuesto una IA.

## 13. Resultado esperado

El objetivo no es prometer que la aplicación siempre contiene la ciencia más reciente. El objetivo es poder demostrar:

1. qué información contenía una versión;
2. de qué fuente procedía;
3. cuándo fue revisada;
4. qué cambió posteriormente;
5. por qué cambió;
6. quién o qué proceso validó el cambio;
7. cómo puede desplegarse una nueva versión sin destruir la anterior.

Este proceso forma parte de la mantenibilidad de la seguridad alimentaria y del futuro soporte documental para auditoría/registro.
