# Gate 61 — Captura directa de fotografías

**Estado:** VALIDADO  
**Fecha:** 2026-09-27  
**Rama de validación:** `feature/direct-camera-capture`

## Objetivo

Permitir tomar fotografías desde Recetoria durante la creación o edición de una receta, tanto para la portada como para cada paso, sin obligar a preparar previamente las imágenes en la galería.

## Experiencia

Al pulsar `Seleccionar foto`, `Añadir foto` o `Cambiar`, la app ofrece:

- **Hacer foto**;
- **Elegir de galería**.

La captura vuelve al mismo editor y utiliza la fotografía para la portada o para el paso que inició la acción.

## Decisión técnica

La app utiliza `ActivityResultContracts.TakePicture` para abrir la aplicación de cámara disponible en Android.

No se incorpora CameraX ni una cámara propia dentro del proceso de Recetoria.

La fotografía se escribe en un URI temporal suministrado por `FileProvider`:

- provider no exportado;
- permisos URI temporales;
- directorio limitado a `cache/recipe_camera_captures/`;
- sin exposición del almacenamiento privado de recetas.

Recetoria no solicita el permiso Android `CAMERA`, porque la captura la realiza la aplicación de cámara del sistema mediante el contrato de Activity Result.

## Procesamiento

Después de la captura se reutiliza el pipeline existente de `LocalRecipePhotoStorage`:

1. lectura desde `content://Uri`;
2. corrección EXIF;
3. reducción a un máximo de 2048 px;
4. conversión JPEG quality 85;
5. staging;
6. promoción al almacenamiento privado al guardar la receta.

El archivo temporal de captura se elimina después de completar el staging. Las capturas canceladas también se descartan y los temporales antiguos tienen limpieza defensiva.

## Persistencia

CAM-01 no modifica:

- Room;
- esquema de receta;
- `RecipeDraft`;
- rutas permanentes de fotografías;
- `SaveRecipeUseCase`.

Por tanto, cámara y galería producen exactamente el mismo tipo de fotografía persistida.

## Archivos principales

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/main/java/com/rmm/recetasraquel/ui/editor/RecipeCameraCapture.kt`
- `app/src/main/java/com/rmm/recetasraquel/ui/editor/RecipeEditorScreen.kt`
- `app/src/main/java/com/rmm/recetasraquel/data/photos/LocalRecipePhotoStorage.kt`
- `app/src/androidTest/java/com/rmm/recetasraquel/ui/editor/RecipeEditorPhotoUiTest.kt`
- `app/src/androidTest/java/com/rmm/recetasraquel/data/photos/LocalRecipePhotoStorageTest.kt`

## Evidencia automatizada

Workflow temporal: **CAM-01 Direct Camera Validation**  
Run: **9**  
Run ID: **36321511085**  
Entorno instrumentado: **Android 36 / Pixel 6 emulator**

Resultado:

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS
```

La batería instrumentada incluye:

- presencia de las opciones `Hacer foto` y `Elegir de galería`;
- creación de URI real mediante `FileProvider`;
- staging de una captura simulada;
- eliminación del temporal de cámara después del staging;
- regresión de la batería Android existente.

El workflow temporal se elimina tras registrar la evidencia.

## Cierre

**Gate 61: VALIDADO.**

CAM-01 queda aprobado para incorporarse a la candidata RC1.
