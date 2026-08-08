# Plan de sprints

## Sprint 0 — Fundación (implementado)

Proyecto Compose/Material 3, Activity única, navegación provisional, placeholders y documentación.

## Sprint 1 — Dominio y persistencia (implementado)

Modelos, validaciones, Room v1, DAO transaccional, repositorio, mappers, inyección manual y pruebas.

## Sprint 2 — Catálogo y consulta (implementado)

Catálogo de recetas, búsqueda por receta/categoría/ingrediente, filtros combinables, detalle, favoritos, Navigation Compose, ViewModels y datos demo exclusivos de debug. Sin creación, edición ni cambios de esquema.

## Sprint 3 — Edición de recetas (implementado)

Editor de recetas con creación, edición y eliminación. Pantalla única con scroll, validación de campos obligatorios, detección de cambios sin guardar, reordenamiento de ingredientes/pasos, navegación con eventos de SharedFlow y pruebas unitarias del ViewModel.

## Sprint 4 — Fotos de recetas (completado)

Soporte para fotos de portada y fotos por paso dentro del editor y la consulta.

- **Domain**: `RecipePhotoStorage`, `PhotoDestination` (sealed), `StagedPhoto` en `domain/photos/`.
- **Data**: `LocalRecipePhotoStorage` con corrección EXIF, JPEG quality 85, max 2048px, staging/promote y almacenamiento privado en `recipe_photos/`.
- **UI**: `EditorPhotoState` para estado del editor; Photo Picker en editor; portada/pasos con Coil `AsyncImage`.
- **Guardado**: `SaveRecipeUseCase` promueve fotos, persiste rutas permanentes y compensa si Room falla.
- **Validación de cierre**: build debug/release, lint, pruebas unitarias/instrumentadas y aceptación manual superadas antes del merge.

## Sprint 5 — Modo cocina (completado)

Experiencia guiada para ejecutar una receta mientras se cocina.

- Entrada desde el detalle mediante **Cocinar**.
- Ruta `recipe/{recipeId}/cook` y `CookingModeViewModel` con `SavedStateHandle`.
- Un paso cada vez, contador y progreso.
- Navegación anterior/siguiente y finalización desde el último paso.
- Foto del paso cuando existe.
- Tiempo configurado mostrado como referencia, sin temporizador ejecutable.
- Ingredientes en hoja inferior sin abandonar el paso actual.
- Pantalla mantenida activa exclusivamente durante modo cocina.
- Sin cambios en Room v1, permisos, backend o almacenamiento.
- Validación automatizada y aceptación manual superadas; PR #6 fusionado en `master`.

Ver `SPRINT_05_COOKING_MODE.md` para el contrato de alcance y validación.

## Programa prioritario — Biblioteca maestra de ingredientes y seguridad alimentaria (ACTIVO)

Esta línea de trabajo tiene prioridad sobre los sprints de UX/polish previamente previstos.

Objetivos principales:

- biblioteca maestra amplia de ingredientes;
- separación entre ingrediente de catálogo, uso en receta e ingrediente personalizado;
- taxonomía de seguridad alimentaria extensible;
- trazabilidad de fuentes/evidencia;
- alertas no absolutas sobre alérgenos, derivados, PAL, reactividad cruzada e información desconocida;
- migración conservadora con pérdida de datos = 0;
- catálogo versionado independiente del esquema Room;
- funcionamiento offline-first;
- dossier técnico de decisiones de seguridad alimentaria reutilizable como soporte de auditoría/registro;
- proceso periódico y por eventos para revisar y actualizar conocimiento sensible.

Documentación de control:

```text
docs/ingredient-library/
docs/food-safety/
```

Estado actual:

```text
Fase 0 — auditoría                    CERRADA
Fase 1 — investigación               CERRADA para pasar a diseño
Fase 2 — diseño                      CERRADA como base de implementación
Fase 3 — Room v1 -> v2               CERRADA — 34/34 instrumented PASS
Fase 4 — infraestructura catálogo    CERRADA — 36/36 instrumented PASS
Catálogo regulatorio v2              VALIDADO
Batch 01 culinario v3                VALIDADO — 36/36 instrumented PASS
Batch 02 culinario v4                VALIDADO en gate combinado
Decisión de linaje                   OPCIÓN B ACEPTADA
Room v2 -> v3 + linaje               CERRADO — 39/39; 3.json revisado y versionado
Catálogo v5 con linaje real          IMPLEMENTADO — validación local pendiente
Dossier soporte regulatorio          BASE DOCUMENTAL CREADA — refresh oficial antes de uso externo
Mantenimiento conocimiento sensible REQUISITO REGISTRADO — revisión anual + ADR distribución pendiente
Mapeo masivo no revisado             NO AUTORIZADO
Merge a master                       NO AUTORIZADO todavía
```

### Gate Fase 3 superado

La migración Room v1 -> v2 conserva los datos legacy y asigna a cada ingrediente antiguo un origen personalizado `legacy:<ingredientId>` sin fuzzy matching ni asociación automática al catálogo. El esquema `2.json` está versionado y la prueba instrumentada real quedó validada.

### Gate Fase 4 superado

La infraestructura del catálogo versionado incluye manifiesto, categorías estructurales, parser JSON, normalización determinista, validador integral, DAO/importador transaccional, repositorio separado y pruebas de importación/idempotencia/rollback.

`ingredient-catalog/v1/` se conserva como bundle histórico e inmutable de infraestructura.

Ver `docs/ingredient-library/09_PHASE_04_CATALOG_INFRASTRUCTURE.md`.

### Catálogo regulatorio v2

`ingredient-catalog/v2/` es la primera versión poblada e inmutable. Su seed validado contiene 27 ingredientes regulatoriamente anclados, 22 alias, los 14 grupos del Anexo II, 3 fuentes oficiales UE y 27 relaciones de seguridad con evidencia `EU_LEGAL`.

Ver `docs/ingredient-library/10_CONTROLLED_CATALOG_SEED.md`.

### Batch 01 culinario v3

`ingredient-catalog/v3/` permanece como versión histórica inmutable. Añadió 100 ingredientes culinarios de verduras, frutas, hierbas, especias, cereales y legumbres sin inferir relaciones de seguridad.

```text
catalogVersion           3
categorías               20
ingredientes canónicos  127
alias                    122
relaciones de seguridad  27
```

El gate local quedó superado con `connectedDebugAndroidTest` 36/36 PASS y Room conservó únicamente `1.json` y `2.json`.

Ver `docs/ingredient-library/11_CULINARY_CATALOG_BATCH_01.md`.

### Batch 02 culinario v4

`ingredient-catalog/v4/` permanece como versión histórica inmutable. Añadió otras 100 identidades culinarias simples —fuentes de carne/aves, semillas/especias, setas, hortalizas/tubérculos y frutas— y 98 alias conservadores.

```text
catalogVersion           4
catalog schemaVersion    2
categorías               20
ingredientes canónicos  227
alias                    220
relaciones de linaje      0
relaciones de seguridad  27
```

Todos los nuevos registros permanecen `REVIEW_REQUIRED`.

Ver `docs/ingredient-library/12_CULINARY_CATALOG_BATCH_02.md`.

### ADR aceptada — linaje, variantes y derivados

Se adopta el **grafo de linaje no clínico** con tipos iniciales:

```text
VARIANT_OF
CUT_OF
DERIVED_FROM
FORM_OF
```

El grafo de linaje y el grafo de seguridad alimentaria permanecen separados. Ninguna relación de linaje crea, hereda o propaga una relación de seguridad.

Ver `docs/ingredient-library/13_ARCHITECTURAL_DECISION_INGREDIENT_LINEAGE.md` y ADR-025 en `DECISIONS.md`.

### Room v3 — CERRADO

Room v2 -> v3 se implementó mediante migración explícita no destructiva. Se añadió `catalog_ingredient_relations` y se corrigió la procedencia de `custom_ingredient_safety_relations` para exigir `sourceId` y preservar `sourceDetails`.

Gate final:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS — 39/39
assembleRelease                PASS
Room schema export             PASS — 1.json, 2.json, 3.json
```

`3.json` fue revisado contra entidades y SQL de migración y está versionado. Room v3 ya no bloquea la evolución del catálogo.

Ver `docs/ingredient-library/14_LINEAGE_GRAPH_IMPLEMENTATION.md`, `15_ROOM_V3_VALIDATION_GATE.md` y `16_ROOM_V3_SCHEMA_REVIEW.md`.

### Catálogo v5 — primer contenido real de linaje

`ingredient-catalog/v5/` es el primer bundle que usa catalog schema v3 con aristas reales.

```text
catalogVersion           5
catalog schemaVersion    3
categorías               20
ingredientes canónicos  242
alias                    235
relaciones de linaje     15
relaciones de seguridad  27
```

Delta desde v4:

```text
+15 cortes de carne/aves
+15 alias conservadores
+15 relaciones CUT_OF
+0 relaciones de seguridad
```

La primera expansión se limita a cortes físicos de baja ambigüedad. El objetivo es validar en datos reales la arquitectura de linaje sin introducir derivados con consecuencias regulatorias o clínicas todavía no revisadas.

`IngredientCatalogAssetReader` apunta ahora a v5. El test instrumentado valida recuentos, persistencia, recorrido `Pechuga de pollo -> Pollo`, idempotencia, rollback y que el linaje no genere relaciones de seguridad.

Ver `docs/ingredient-library/17_CATALOG_V5_FIRST_LINEAGE_CONTENT.md`.

### Dossier técnico de seguridad alimentaria para soporte de registro

`docs/food-safety/REGISTRATION_SUPPORT_FOOD_SAFETY_DOSSIER.md` consolida decisiones sobre los 14 grupos UE, papel de AESAN, separación alergia/intolerancia/celiaquía, evidencia, inferencias prohibidas, lenguaje seguro, trazabilidad y artefactos para una futura auditoría o registro.

El dossier no afirma cumplimiento regulatorio por sí solo. Antes de cualquier uso externo debe realizarse un `regulatory refresh` con fuentes oficiales vigentes en la fecha de presentación.

### Mantenimiento periódico del conocimiento sensible

`docs/food-safety/SAFETY_KNOWLEDGE_MAINTENANCE_REQUIREMENTS.md` establece revisión formal anual y revisión por evento cuando existan cambios regulatorios, alertas o nueva evidencia oficial material. La revisión anual de AESAN, EFSA y normativa ya está programada.

Una publicación nueva nunca cambia automáticamente una relación clínica: la evidencia debe pasar por revisión, clasificación, validación y una nueva versión inmutable del catálogo.

El mecanismo de distribución de catálogos actualizados **no está decidido**. Cuando llegue el momento se abrirá una ADR para comparar actualización empaquetada con la app, catálogo remoto firmado o solución híbrida. Esta será una decisión arquitectónica por su impacto en offline-first, autenticidad, rollback y seguridad.

Ver `docs/food-safety/SAFETY_KNOWLEDGE_MAINTENANCE_REQUIREMENTS.md` y `CATALOG_MAINTENANCE_GUIDE.md`.

## Sprints futuros congelados

Quedan expresamente pospuestos hasta nueva decisión de producto:

- mejora UX del editor;
- selector premium de unidades;
- pickers/steppers de tiempos;
- rediseño visual y nueva paleta premium;
- temporizadores ejecutables;
- otras mejoras visuales detectadas durante Sprint 5.

## Fases futuras generales

Backup exportación/importación, accesibilidad y rendimiento seguirán evaluándose cuando corresponda. GitHub Sync continúa fuera de alcance hasta disponer de un diseño específico de seguridad y resolución de conflictos.
