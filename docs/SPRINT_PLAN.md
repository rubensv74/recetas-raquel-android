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
- funcionamiento completamente offline.

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
Batch 02 culinario v4                IMPLEMENTADO — gate combinado pendiente
Decisión de linaje                   OPCIÓN B ACEPTADA
Room v2 -> v3 + linaje               IMPLEMENTADO — validación local pendiente
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

Estado validado:

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

`ingredient-catalog/v4/` es el bundle activo durante el gate de infraestructura de linaje. Añade 100 identidades culinarias simples —fuentes de carne y aves, semillas/especias, setas, hortalizas/tubérculos y frutas— y 98 alias conservadores.

Estado implementado:

```text
catalogVersion           4
catalog schemaVersion    2
categorías               20
ingredientes canónicos  227
alias                    220
relaciones de linaje      0
relaciones de seguridad  27
```

Todos los nuevos registros están `REVIEW_REQUIRED`; las relaciones de seguridad permanecen exactamente en 27.

Ver `docs/ingredient-library/12_CULINARY_CATALOG_BATCH_02.md`.

### ADR aceptada — linaje, variantes y derivados

Se ha aceptado la **Opción B: grafo de linaje no clínico**. Las relaciones iniciales son `VARIANT_OF`, `CUT_OF`, `DERIVED_FROM` y `FORM_OF`.

El grafo de linaje y el grafo de seguridad alimentaria son conceptos y tablas separados. Ninguna relación de linaje crea, hereda o propaga una relación de seguridad.

Ver `docs/ingredient-library/13_ARCHITECTURAL_DECISION_INGREDIENT_LINEAGE.md` y ADR-025 en `DECISIONS.md`.

### Room v3 — infraestructura de linaje implementada

Room evoluciona explícitamente v2 -> v3 sin migración destructiva. Se añade `catalog_ingredient_relations`, se amplían importer/validator/DAO/repository y el catalog schema v3 admite ficheros de linaje únicos o fragmentados.

La misma migración corrige el contrato de `custom_ingredient_safety_relations`: toda relación personalizada pasa a tener `sourceId`; el antiguo texto libre se conserva como `sourceDetails` mediante una fuente de migración determinista.

También se ha cerrado el riesgo transitorio del editor antiguo: un ingrediente nuevo de texto libre sin origen explícito se persiste como un `CustomIngredient` dedicado `recipe-custom:<ingredientId>` antes de crear la FK del ingrediente de receta. Los orígenes `legacy:*`, catálogo y personalizados reales se preservan; un origen doble se rechaza.

Pruebas añadidas cubren migración encadenada v1 -> v3, migración directa v2 -> v3 con preservación de fuente personalizada, validación de ciclos/referencias del grafo, importación de v4 con cero aristas y persistencia del editor libre sin fallo de FK.

El gate local combinado está pendiente. Hasta superarlo no se crea un catálogo v5 con aristas reales de linaje y no se considera cerrado Room v3.

Ver `docs/ingredient-library/14_LINEAGE_GRAPH_IMPLEMENTATION.md`.

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
