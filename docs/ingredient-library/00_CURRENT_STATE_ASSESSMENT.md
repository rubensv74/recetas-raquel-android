# 00 — CURRENT STATE ASSESSMENT

**Programa prioritario:** Biblioteca maestra de ingredientes y seguridad alimentaria  
**Fecha:** 2026-08-07  
**Repositorio:** `rubensv74/recetas-raquel-android`  
**Paquete:** `com.rmm.recetasraquel`

## Estado Git verificado

```text
branch: master
status: ## master...origin/master
HEAD: 5a5d7679da060b4410be7ffbd2d37ac5e7a5cc1a
working tree: clean
```

El commit de partida corresponde al merge del Sprint 5. Los sprints futuros de UX/polish quedan congelados por decisión de producto mientras se ejecuta esta línea prioritaria.

## Arquitectura detectada

- Un único módulo Android `app`.
- Una única `MainActivity`.
- Kotlin + Jetpack Compose + Material 3.
- Navigation Compose.
- Room como fuente local de verdad.
- Capas `ui/app` → `domain` → `data`.
- Inyección manual mediante `AppContainer`.
- Coroutines/Flow y ViewModels con `StateFlow`.
- `SharedFlow` para eventos del editor.
- Fotografías privadas + Coil.
- Sin backend ni requisito de red para los casos principales.

Paquetes principales:

```text
com.rmm.recetasraquel/
├── app/
├── data/
├── domain/
├── ui/
└── util/
```

UI existente:

```text
ui/components
ui/cooking
ui/detail
ui/editor
ui/home
ui/navigation
ui/settings
ui/theme
```

## Room — versión real

```text
Database: recipes.db
Schema version: 1
```

`RecipeDatabase` contiene exactamente:

- `RecipeEntity`
- `IngredientEntity`
- `RecipeStepEntity`

Existe el esquema exportado `app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/1.json`.

No existe todavía una cadena de migraciones de producción ni `fallbackToDestructiveMigration`.

## Modelo actual de ingredientes

Persistencia:

```text
IngredientEntity
- id: String
- recipeId: String
- quantity: String?
- unit: String?
- name: String
- notes: String?
- sortOrder: Int
```

Dominio equivalente: `Ingredient` con los mismos conceptos.

Conclusión: el ingrediente actual es un **uso de receta basado en texto libre**. No existe aún separación entre ingrediente maestro de catálogo, ingrediente de receta e ingrediente/producto personalizado.

## Flujo existente

La app permite actualmente catálogo de recetas, búsqueda/filtros, detalle, favoritos, CRUD, ingredientes y pasos ordenables, fotografías y modo cocina. `Añadir ingrediente` sigue dependiendo del editor manual; no existe biblioteca maestra.

## Repositorios y casos de uso

`AppContainer` instancia explícitamente `RecipeDatabase`, `LocalRecipeRepository`, `LocalRecipePhotoStorage`, `SaveRecipeUseCase`, proveedores de ID/tiempo y datos demo en debug.

Riesgo de diseño: el catálogo y la seguridad alimentaria no deben inflar indiscriminadamente `RecipeRepository`; se propone un boundary específico.

## Baseline de validación ejecutado

```text
assembleDebug                     PASS
testDebugUnitTest                 PASS
lintDebug                         PASS
compileDebugAndroidTestKotlin     PASS
connectedDebugAndroidTest         NOT RUN
```

Motivo de `NOT RUN`:

```text
DeviceException: No connected devices!
```

No se produjo un fallo de test. No había dispositivo disponible.

Referencia histórica inmediatamente anterior al merge del Sprint 5:

```text
connectedDebugAndroidTest = 33/33 PASS
```

La cifra exacta de pruebas unitarias se obtendrá del reporte de una ejecución que fuerce/recoja resultados; no se reutilizarán cifras antiguas como si fueran actuales.

## Riesgos principales

1. Primera migración explícita de Room desde v1.
2. Ingredientes legacy sin identidad maestra: coincidencias ambiguas deben conservarse como personalizados.
3. Pérdida admisible de recetas/ingredientes/cantidades/unidades/pasos/fotos/metadatos: **0**.
4. La estrategia actual de guardado de hijos debe seguir siendo compatible con las futuras FK.
5. Anexo II UE no equivale a “todas las alergias”.
6. Derivados, procesado y exenciones requieren reglas trazables, no inferencias.
7. Productos compuestos/comerciales tienen composición variable.
8. Reactividad cruzada nunca equivale automáticamente a presencia confirmada.
9. Catálogo de gran tamaño requiere importador transaccional, índices y validador.
10. No hay una dependencia de serialización de catálogo claramente establecida; decidir antes de implementar el importador.
11. `docs/SPRINT_PLAN.md` quedó desactualizado respecto al cierre real de Sprint 5 y debe corregirse en esta rama.
12. Evitar incorporar artefactos locales `.idea`/Device Manager.

## Impacto previsto

```text
app/src/main/java/com/rmm/recetasraquel/data/local/
app/src/main/java/com/rmm/recetasraquel/data/repository/
app/src/main/java/com/rmm/recetasraquel/domain/model/
app/src/main/java/com/rmm/recetasraquel/domain/repository/
app/src/main/java/com/rmm/recetasraquel/domain/usecase/
app/src/main/java/com/rmm/recetasraquel/ui/editor/
app/src/main/java/com/rmm/recetasraquel/ui/detail/
app/src/main/java/com/rmm/recetasraquel/ui/navigation/
app/src/main/java/com/rmm/recetasraquel/ui/ingredients/   # probable feature nueva
app/src/main/java/com/rmm/recetasraquel/app/AppContainer.kt
app/src/main/assets/ingredient-catalog/
app/schemas/
app/src/test/
app/src/androidTest/
docs/food-safety/
docs/ingredient-library/
```

## Restricciones arquitectónicas

1. Offline-first.
2. Room como fuente local de verdad.
3. Sin migración destructiva.
4. UI sin DAO/filesystem directo.
5. Framework Android fuera de dominio cuando sea razonable.
6. DI manual explícita.
7. I/O fuera del hilo principal.
8. Ninguna relación clínica sin evidencia trazada.
9. Catálogo versionado independientemente de Room.
10. No push/merge/publicación sin autorización expresa.

## Gate Fase 0

```text
Git local                    PASS
Commit de partida            PASS
Working tree                 PASS
Arquitectura                 PASS
Room v1                      PASS
Modelo legacy                PASS
Build baseline               PASS
Unit baseline                PASS
Lint baseline                PASS
AndroidTest compile          PASS
Instrumented baseline        NOT RUN — entorno

FASE 0                       CLOSED
```
