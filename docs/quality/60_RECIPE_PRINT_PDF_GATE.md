# Gate 60 — Impresión y exportación PDF de recetas

**Estado:** IMPLEMENTADO — PENDIENTE DE GATE  
**Fecha:** 2026-09-26  
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

## Gate requerido

Antes de marcar el incremento como validado deben superar:

- assembleDebug
- testDebugUnitTest
- lintDebug
- connectedDebugAndroidTest

Y debe comprobarse manualmente en Android:

1. Abrir una receta con alérgenos.
2. Pulsar "Imprimir / PDF".
3. Verificar la previsualización A4.
4. Seleccionar "Guardar como PDF".
5. Abrir el PDF generado.
6. Comprobar que el bloque de alérgenos destaca y no se divide.
7. Comprobar el mismo documento en escala de grises.
8. Repetir con una receta sin coincidencias y otra que requiera revisión.
9. Verificar una receta con exención regulatoria.

No se marcará este gate como VALIDADO hasta superar la validación automática y la comprobación de dispositivo.
