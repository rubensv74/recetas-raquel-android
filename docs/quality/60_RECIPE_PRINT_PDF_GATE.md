# Gate 60 — Impresión y exportación PDF de recetas

**Estado:** VALIDADO  
**Fecha:** 2026-09-27  
**Rama:** feature/recipe-print-pdf

## Objetivo

Permitir imprimir cualquier receta desde su detalle o guardarla como PDF mediante el diálogo nativo de impresión de Android, manteniendo Recetoria completamente offline.

## Decisión técnica

Flujo implementado:

RecipeDetailScreen → RecipePrintManager → HTML local de impresión → WebView local sin JavaScript, file access ni content access → Android PrintManager → Impresora / Guardar como PDF.

No se añaden dependencias, permisos, clientes de red, cambios de Room ni cambios en persistencia.

## Contrato de seguridad alimentaria

La impresión reutiliza RecipeSafetySummary. No vuelve a inferir alérgenos desde nombres o texto libre.

Se mantiene la separación:

**Alérgenos y seguridad alimentaria ≠ Información regulatoria de etiquetado**

### Bloque prioritario de alérgenos

Se coloca antes de ingredientes y preparación e incluye:

- presencia identificada;
- derivados identificados;
- "puede contener" declarado;
- posible reactividad cruzada;
- grupos que requieren revisión;
- resumen de avisos pendientes.

El bloque tiene borde reforzado, fondo contrastado, título en mayúsculas, etiquetas textuales independientes del color y reglas break-inside/page-break-inside para evitar que quede dividido entre páginas.

### Ingredientes responsables

Cada ingrediente con evidencia agregada muestra una línea adicional con estado y grupo de seguridad. Los avisos asociados al ingrediente se muestran de forma separada como "Requiere revisión".

### Exenciones regulatorias

Las exenciones permanecen en un panel independiente. El PDF conserva explícitamente que una excepción de etiquetado no significa ausencia del alérgeno, ausencia de riesgo ni aptitud para una persona alérgica o intolerante.

## Contenido A4

Cuando la receta dispone de los datos correspondientes, la impresión incluye:

- marca Recetoria;
- categoría;
- título y descripción;
- fotografía principal;
- raciones y tiempos;
- bloque prioritario de alérgenos y seguridad;
- ingredientes y notas;
- pasos numerados y temporizadores;
- notas de receta;
- avisos globales de revisión;
- información regulatoria de etiquetado;
- recordatorio final de comprobar el etiquetado actual.

## Seguridad del renderizado

Todo texto procedente de los datos de receta se escapa antes de insertarse en HTML.

La WebView se usa solo como motor local de impresión y se configura con JavaScript deshabilitado, acceso a archivos deshabilitado y acceso a contenido deshabilitado. La fotografía principal se incrusta como data URI cuando está disponible.

## Archivos

- app/src/main/java/com/rmm/recetasraquel/app/printing/RecipePrintHtmlBuilder.kt
- app/src/main/java/com/rmm/recetasraquel/app/printing/RecipePrintManager.kt
- app/src/main/java/com/rmm/recetasraquel/app/RecetasRaquelApp.kt
- app/src/main/java/com/rmm/recetasraquel/ui/detail/RecipeDetailScreen.kt
- app/src/test/java/com/rmm/recetasraquel/app/printing/RecipePrintHtmlBuilderTest.kt
- app/src/androidTest/java/com/rmm/recetasraquel/ui/detail/RecipePrintUiTest.kt

## Gate de cierre

El incremento se valida con dos capas automáticas y una revisión estructural del contrato de impresión.

### Gate estándar

Workflow: **Android CI**  
Run: **190**  
Run ID: **36299580593**

Resultado:

- assembleDebug: PASS
- testDebugUnitTest: PASS
- lintDebug: PASS
- guard de esquemas Room: PASS

### Gate instrumentado específico de PRINT-01

Workflow temporal: **PR16 Print PDF Validation**  
Run: **1**  
Run ID: **36299580613**  
Entorno: **Android 36 / Pixel 6 emulator**

Resultado:

- arranque del emulador: PASS
- instalación/ejecución de tests instrumentados: PASS
- paquete `com.rmm.recetasraquel.ui.detail`: PASS
- acción `detail_print_pdf`: PASS
- paneles de seguridad alimentaria y regulación del detalle: PASS

El workflow temporal se elimina tras obtener la evidencia y no forma parte del CI permanente del repositorio.

### Validación de release

Durante la preparación del gate se ejecutó además `assembleRelease` correctamente. Un intento posterior del gate completo se detuvo por una incidencia de sintaxis en un script temporal del workflow, antes de ejecutar los tests; esa incidencia era de CI, no del producto, fue aislada y sustituida por el gate instrumentado específico anterior.

### Sustitución de la comprobación manual

La comprobación manual en un teléfono se sustituye para este incremento por:

1. tests unitarios del generador HTML;
2. compilación y lint del proyecto;
3. tests instrumentados reales en Android 36;
4. revisión estructural de la plantilla A4 y sus reglas de paginación;
5. verificación de que el flujo usa el `PrintManager` y `PrintDocumentAdapter` nativos de Android.

Se acepta como riesgo residual únicamente la posible variación visual del diálogo o del render final de impresión introducida por un servicio de impresión/OEM concreto. Este riesgo no afecta al cálculo de alérgenos, al contenido de la receta ni al contrato de seguridad alimentaria.

## Cierre

**Gate 60: VALIDADO.**

PRINT-01 queda aprobado para integración en `master`. El bloque de alérgenos conserva prioridad visual, semántica independiente del color y protección frente a saltos de página, y la información regulatoria permanece separada de la información de seguridad alimentaria.
