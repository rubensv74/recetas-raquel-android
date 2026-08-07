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
- migración conservadora desde Room v1 con pérdida de datos = 0;
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
Fase 4 — infraestructura catálogo    SIGUIENTE
Generación masiva de catálogo        BLOQUEADA hasta gate específico
Merge a master                       NO AUTORIZADO todavía
```

### Gate Fase 3 superado

La rama `program/ingredient-library-food-safety` contiene Room v2, migración explícita no destructiva, esquema `2.json` versionado y prueba instrumentada de migración real v1 -> v2.

La migración conserva los datos legacy y asigna a cada ingrediente antiguo un origen personalizado `legacy:<ingredientId>` sin fuzzy matching ni asociación automática al catálogo.

Queda como requisito transitorio para fases posteriores asegurar que todo ingrediente nuevo creado por el flujo de biblioteca/personalizado persista exactamente un origen (`catalogIngredientId XOR customIngredientId`) antes de autorizar el merge completo del programa a `master`.

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
