# Biblioteca de ingredientes — ficha de seguridad y regulación

Fecha de cierre: 2026-08-10

## Estado

**GATE SUPERADO**

Este bloque habilita la consulta de la información de seguridad alimentaria y regulación de un ingrediente directamente desde la biblioteca, antes de seleccionarlo para una receta.

No introduce una nueva decisión de arquitectura y no modifica el esquema de Room.

## Objetivo funcional

La biblioteca ya indicaba si un ingrediente tenía información de seguridad o información regulatoria registrada. El siguiente paso era permitir consultar esa información sin alterar el flujo normal de selección.

El contrato de interacción queda así:

- pulsar la tarjeta del ingrediente: selecciona el ingrediente y vuelve al editor de receta;
- pulsar `Ver información`: abre una ficha informativa y **no selecciona** el ingrediente.

De este modo, consultar la información y decidir utilizar un ingrediente siguen siendo acciones independientes.

## Información de seguridad mostrada

Cuando existe una relación directa de seguridad registrada, la ficha muestra:

- grupo de seguridad alimentaria;
- tipo de relación;
- nivel de evidencia;
- ámbito o jurisdicción;
- fuente documentada;
- fecha de revisión;
- notas, cuando existan.

Ejemplo validado con `ing-wheat` / Trigo:

- grupo: `Cereales que contienen gluten`;
- relación: `INHERENT_SOURCE`;
- evidencia: `EU_LEGAL`;
- jurisdicción: `EU-ES`;
- fuente: `EU_FIC_1169_2011`;
- revisión: `2026-08-08`.

La jurisdicción se obtiene del grupo de seguridad almacenado en Room. No se deriva ni se infiere en la interfaz.

## Ausencia de relación directa

Si un ingrediente no tiene una relación directa de seguridad registrada, la interfaz no utiliza frases como `sin alérgenos`, `seguro`, `apto` o equivalentes.

El mensaje utilizado es:

> No hay una relación directa de seguridad registrada para este ingrediente. Esto no demuestra ausencia de alérgenos ni ausencia de riesgo.

Por tanto:

`ausencia de evidencia directa != evidencia de ausencia de riesgo`.

Esta regla permanece alineada con el modelo de seguridad del proyecto.

## Información regulatoria

Las exenciones regulatorias se muestran en una sección distinta de la información de seguridad alimentaria.

La ficha regulatoria incluye:

- efecto jurídico;
- condiciones de aplicación;
- jurisdicción;
- fuente regulatoria;
- fechas de vigencia, si están disponibles;
- fecha de revisión;
- notas.

La interfaz incluye expresamente la advertencia:

> Una excepción regulatoria no significa ausencia del alérgeno, ausencia de riesgo ni aptitud para una persona alérgica o intolerante.

La existencia de una exención legal no reduce, sustituye ni neutraliza la información de seguridad.

## Trazabilidad de las fuentes

La capa de datos conserva la fuente oficial tal como está documentada en el catálogo.

Para `EU_FIC_1169_2011`, la evidencia almacenada mantiene:

- título oficial: `Regulation (EU) No 1169/2011 — consolidated 2025-04-01`;
- referencia: `CELEX 02011R1169-20250401`.

Las traducciones o etiquetas más amigables pertenecen a la capa de presentación. La evidencia persistida y la trazabilidad no se reescriben para adaptarse a la interfaz.

## Flujo de datos

El recorrido validado es:

`ingredient-catalog/v12 → Room → IngredientCatalogDao → LocalIngredientCatalogRepository → IngredientLibraryViewModel → IngredientLibraryInfoDialog`

La consulta de detalle se realiza únicamente cuando el usuario solicita `Ver información`, de modo que la búsqueda normal de la biblioteca no incorpora esta carga adicional.

El `ViewModel` recupera por separado:

1. relaciones de seguridad;
2. exenciones regulatorias del snapshot actual.

Las dos colecciones permanecen separadas durante todo el flujo.

## Cambios principales

### Modelo de dominio

`CatalogIngredientSafetyRecord` incorpora el campo obligatorio:

```kotlin
val jurisdiction: String
```

### DAO

`CatalogIngredientSafetyRow` y su consulta incorporan la jurisdicción desde `food_safety_groups`.

### Repositorio

`LocalIngredientCatalogRepository` propaga la jurisdicción al modelo de dominio.

### ViewModel

Se incorpora `IngredientLibraryInfoUiState`, que mantiene:

- ingrediente;
- relaciones de seguridad;
- exenciones regulatorias;
- estado de carga;
- error de carga.

### Interfaz

Se incorpora `IngredientLibraryInfoDialog` y la acción secundaria `Ver información` en cada resultado de la biblioteca.

## Room

Este bloque **no cambia el esquema de base de datos**.

Se mantiene:

- `RecipeDatabase` versión 6;
- esquemas versionados `1.json` a `6.json`;
- identity hash de Room v6: `7371cdda6f45ff9d90227b3b3b2d581d`.

El guard de CI exige que no aparezca ningún esquema adicional ni diferencias en los seis contratos existentes.

## Cobertura automática

### `IngredientCatalogSafetyDetailTest`

Valida el recorrido real desde el catálogo v12 hasta el repositorio para `ing-wheat` y comprueba:

- catálogo 12 importado;
- grupo correcto;
- jurisdicción `EU-ES`;
- relación `INHERENT_SOURCE`;
- evidencia `EU_LEGAL`;
- fuente `EU_FIC_1169_2011`;
- título oficial de Regulation (EU) No 1169/2011;
- referencia CELEX `02011R1169-20250401`;
- fecha de revisión `2026-08-08`.

### `IngredientLibraryUiTest`

Amplía la cobertura para comprobar:

- la consulta de información no selecciona el ingrediente;
- la selección normal continúa funcionando;
- la ficha de seguridad presenta grupo, relación, evidencia, jurisdicción, fuente y revisión;
- un ingrediente con exención pero sin relación directa muestra ambas situaciones por separado;
- la advertencia regulatoria está presente;
- el mensaje de información incompleta no implica seguridad;
- las rutas de categorías e introducción manual continúan separadas.

## Incidencias detectadas durante el gate

El proceso de CI detectó tres ajustes en contratos de prueba. Ninguno requirió debilitar la lógica de producción.

### 1. Fixture unitaria sin jurisdicción

Una fixture histórica de `BuildRecipeSafetySummaryUseCaseTest` construía `CatalogIngredientSafetyRecord` antes de que `jurisdiction` fuese obligatorio.

Se actualizó la fixture para utilizar la jurisdicción real de su grupo de seguridad.

### 2. API de Compose Test

La versión de Compose Test utilizada por el proyecto no incluye `assertExists()`.

Las comprobaciones de existencia en contenido desplazable se adaptaron a `fetchSemanticsNode()`, manteniendo la misma intención de prueba.

### 3. Fuente oficial en idioma original

La primera versión del test de datos esperaba la palabra española `Reglamento` dentro del título de la fuente.

El catálogo conserva correctamente la fuente oficial de EUR-Lex en su título original en inglés. El test se corrigió para comprobar el título oficial y la referencia CELEX, en lugar de alterar la fuente almacenada.

## Evidencia final de CI

GitHub Actions:

- workflow: `Android CI`;
- run: `31398999063`;
- run number: `127`;
- commit validado: `e18494140e9ad7a63566df3bcbc722324fd6586e`;
- conclusión: `success`.

Fase de calidad:

- `assembleDebug`: success;
- unit tests: success;
- lint: success;
- compilación de tests instrumentados: success;
- `assembleRelease`: success;
- guard de Room: success.

Emulador Android API 36:

- 71 tests ejecutados;
- 0 omitidos;
- 0 fallidos;
- `BUILD SUCCESSFUL`;
- guard de Room posterior: success;
- conjunto final de esquemas: exactamente `1.json` a `6.json`, sin diferencias.

## Resultado

La biblioteca permite ahora **informarse antes de seleccionar** sin mezclar consulta con acción, y muestra de forma auditable la evidencia disponible.

Se mantienen intactos los invariantes principales:

- exención regulatoria != seguridad clínica;
- ausencia de relación directa != ausencia de riesgo;
- no hay inferencia automática de seguridad por linaje;
- las exenciones regulatorias no suprimen evidencia de seguridad;
- la fuente oficial se conserva fielmente en la capa de datos.

No queda una decisión de arquitectura abierta en este bloque.
