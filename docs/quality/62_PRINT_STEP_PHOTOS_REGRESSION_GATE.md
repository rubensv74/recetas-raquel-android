# Gate 62 — Fotografías de pasos en impresión/PDF

**Estado:** VALIDADO  
**Fecha:** 2026-09-27  
**Rama de validación:** `fix/print-step-photos`

## Incidencia

Una prueba real de Recetoria mostró que el PDF imprimía correctamente la fotografía de portada, pero la sección **Preparación** mostraba únicamente el texto de cada paso aunque esos pasos tuvieran fotografías persistidas.

## Causa

El flujo de impresión solo resolvía `recipe.coverPhotoPath` y enviaba esa imagen como data URI a `RecipePrintHtmlBuilder`.

Las propiedades `RecipeStep.photoPath` no se resolvían y el generador HTML no disponía de un contrato para renderizar imágenes de pasos.

## Corrección

`RecipePrintManager` ahora:

1. recorre los pasos de la receta;
2. toma cada `photoPath` existente;
3. resuelve el archivo privado de Recetoria;
4. lo convierte a data URI;
5. lo asocia al identificador del paso;
6. entrega el mapa completo al generador de impresión.

`RecipePrintHtmlBuilder` ahora renderiza la fotografía dentro del bloque del paso correspondiente.

La fotografía:

- mantiene proporción;
- se limita al ancho disponible;
- tiene altura máxima de 72 mm para una paginación A4 razonable;
- permanece dentro del bloque del paso, que conserva `break-inside: avoid`.

Los pasos sin fotografía continúan imprimiéndose como antes.

## Alcance

No se modifica:

- Room;
- persistencia de fotografías;
- captura de cámara;
- galería;
- seguridad alimentaria;
- información regulatoria.

La corrección consume las mismas fotografías persistidas que ya muestra la receta.

## Test de regresión

`RecipePrintHtmlBuilderTest.stepPhotosAreEmbeddedInPreparation` verifica que:

- una fotografía asociada a un paso se incluye en el HTML;
- se utiliza la data URI entregada;
- la imagen queda asociada al número de paso;
- el texto del paso precede a su fotografía.

## Evidencia automática

Workflow temporal: **PRINT-02 Step Photo Validation**  
Run: **1**  
Run ID: **36323735141**

Resultado:

```text
assembleDebug                  PASS
assembleRelease                PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
```

El workflow temporal se elimina tras recoger la evidencia.

## Cierre

**Gate 62: VALIDADO.**

La impresión/PDF de Recetoria incluye ahora la portada y las fotografías persistidas de los pasos de preparación.
