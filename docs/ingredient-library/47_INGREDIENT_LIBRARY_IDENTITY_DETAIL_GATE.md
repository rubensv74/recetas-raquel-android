# Biblioteca de ingredientes — detalle de identidad culinaria

Fecha de cierre: 2026-08-10

## Estado

**GATE SUPERADO**

Este bloque completa la ficha `Ver información` de la biblioteca con información de identidad culinaria que ya estaba modelada en el catálogo:

- descripción;
- alias;
- presentaciones relacionadas explícitamente modeladas.

No cambia el esquema de Room y no modifica las reglas de seguridad alimentaria ni de regulación.

## Objetivo funcional

La ficha del ingrediente ya mostraba información de seguridad y regulación. El diseño funcional también exige aprovechar los datos de identidad disponibles para ayudar a reconocer correctamente el ingrediente antes de seleccionarlo.

La ficha incorpora ahora, cuando existen datos:

1. descripción;
2. nombres alternativos o alias;
3. presentaciones culinarias relacionadas.

Estos datos se cargan únicamente cuando el usuario pulsa `Ver información`; no se añaden al resultado ligero de búsqueda.

## Modelo de dominio

Se incorporan:

```kotlin
enum class IngredientCatalogRelationDirection {
    PARENT,
    CHILD,
}

data class IngredientCatalogRelatedPresentation(
    val ingredientId: String,
    val canonicalName: String,
    val relationType: IngredientLineageType,
    val direction: IngredientCatalogRelationDirection,
)

data class IngredientCatalogDetail(
    val description: String? = null,
    val aliases: List<String> = emptyList(),
    val relatedPresentations: List<IngredientCatalogRelatedPresentation> = emptyList(),
)
```

`IngredientCatalogRepository` expone `getIngredientDetail(ingredientId)`.

## Consultas

`IngredientCatalogDao` incorpora consultas independientes para:

- descripción del ingrediente;
- alias;
- relaciones culinarias visibles.

La consulta de relaciones recorre tanto padres como hijos explícitamente registrados y los unifica para la presentación.

La condición crítica de visibilidad es:

```sql
related.isActive = 1
AND related.catalogRole = 'CULINARY'
```

Por tanto, una identidad `REGULATORY_TECHNICAL` puede seguir existiendo y ser utilizada por el motor interno, pero no aparece en la sección `Presentaciones relacionadas`.

## Separación entre identidad y seguridad

Las relaciones mostradas continúan siendo exclusivamente relaciones de identidad culinaria.

La ficha incluye la advertencia:

> Estas relaciones describen identidad culinaria. No heredan ni generan información de seguridad alimentaria.

No se utiliza el linaje para:

- asignar un grupo de seguridad;
- heredar una relación de alérgeno;
- crear una alerta;
- propagar una exención regulatoria;
- inferir aptitud clínica.

La información de seguridad y la información regulatoria permanecen en sus secciones independientes.

## Casos de referencia validados

### Harina de trigo

El catálogo v12 contiene:

- ingrediente: `ing-wheat-flour`;
- alias: `Harina trigo`;
- relación explícita `DERIVED_FROM` con `ing-wheat` / `Trigo`.

La ficha recupera esa información como:

- alias: `Harina trigo`;
- presentación relacionada: `Derivado de: Trigo`.

### Leche

El catálogo v12 ofrece un caso importante para probar la frontera de visibilidad.

`ing-milk` tiene, entre otras, relaciones con:

- `ing-lactitol`, que es `CULINARY`;
- `ing-whey-alcoholic-distillates`, que es `REGULATORY_TECHNICAL`.

La ficha de Leche:

- incluye `Lactitol` entre las relaciones visibles;
- excluye `Lactosuero utilizado para hacer destilados alcohólicos`.

Esto confirma que la separación introducida en v12 no queda limitada al buscador y se respeta también en la navegación visual del linaje.

## ViewModel e interfaz

`IngredientLibraryInfoUiState` incorpora `catalogDetail`.

Al abrir la ficha, el `ViewModel` recupera de forma separada:

1. detalle de identidad culinaria;
2. relaciones de seguridad;
3. exenciones regulatorias.

La interfaz presenta la identidad antes de la sección de seguridad y mantiene las fronteras conceptuales explícitas.

## Cobertura automática

### `IngredientCatalogIdentityDetailTest`

Valida dos contratos:

1. `ing-wheat-flour` recupera el alias `Harina trigo` y la relación explícita con `Trigo` como `DERIVED_FROM` / `PARENT`.
2. La ficha de `ing-milk` incluye `ing-lactitol` y excluye `ing-whey-alcoholic-distillates`.

### `IngredientLibraryUiTest`

Añade el caso `identityDetailShowsDescriptionAliasesAndRelatedCulinaryPresentations`, que comprueba:

- descripción;
- alias;
- cabecera `Presentaciones relacionadas`;
- texto `Derivado de: Trigo`;
- advertencia de que el linaje no genera ni hereda seguridad.

Las pruebas anteriores de seguridad, regulación, selección y consulta independiente permanecen activas.

## Room

Este bloque no introduce persistencia nueva.

Se mantiene:

- `RecipeDatabase` versión 6;
- esquemas `1.json` a `6.json`;
- identity hash v6: `7371cdda6f45ff9d90227b3b3b2d581d`.

El guard de CI confirmó que no se generó ningún esquema adicional ni se modificó un contrato existente.

## Evidencia final de CI

GitHub Actions:

- workflow: `Android CI`;
- run: `31401500296`;
- run number: `135`;
- commit validado: `e3772ffbb07f5e9bb720b9bf9b82c92b1663f6b2`;
- conclusión: `success`.

Fase de calidad:

- `assembleDebug`: success;
- unit tests: success;
- lint: success;
- compilación de tests instrumentados: success;
- `assembleRelease`: success;
- guard de Room: success.

Emulador Android API 36:

- 74 tests ejecutados;
- 0 omitidos;
- 0 fallidos;
- `BUILD SUCCESSFUL`;
- guard de Room posterior: success;
- conjunto final de esquemas: exactamente `1.json` a `6.json`, sin diferencias.

## Resultado

La ficha de la biblioteca ya reúne la identidad culinaria disponible y la información de seguridad/regulación sin mezclar sus significados.

Se mantienen los invariantes:

- linaje culinario != evidencia de seguridad;
- exención regulatoria != seguridad clínica;
- identidad técnica regulatoria != ingrediente visible;
- ausencia de relación directa != ausencia de riesgo;
- las relaciones se muestran solo cuando están explícitamente modeladas.

No queda una decisión de arquitectura abierta en este bloque.
