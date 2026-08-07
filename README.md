# Recetas de Raquel

Aplicación Android privada y local-first para guardar y consultar recetas sin conexión.

## Requisitos

- Android Studio compatible con AGP 9.0.1
- JDK de Android Studio o compatible con Gradle 9.2.1
- Android SDK 36.1 instalado

## Estado

El Sprint 3 incorpora editor de recetas con creación, edición y eliminación. La pantalla única del editor permite campos obligatorios, ingredientes y pasos con reordenamiento, validación en línea, detección de cambios sin guardar y eliminación con confirmación. La UI consume Room exclusivamente mediante ViewModels y `RecipeRepository`.

El Sprint 4 añade soporte para fotos de portada y por paso. El editor permite seleccionar, previsualizar y eliminar fotos con el Android Photo Picker; las imágenes se comprimen y almacenan en almacenamiento privado. El catálogo y el detalle muestran portadas con Coil.

El Sprint 5 incorpora un modo cocina guiado: acceso desde el detalle, un paso cada vez, progreso, navegación anterior/siguiente, fotografía del paso, consulta rápida de ingredientes, visualización del tiempo configurado y pantalla activa mientras se cocina. Los temporizadores ejecutables permanecen fuera de alcance.

La base de producción se llama `recipes.db`. El esquema continúa en versión 1 y se exporta a `app/schemas`. Los IDs son UUID almacenados como `String` y los timestamps son milisegundos Unix UTC (`Long`).

En debug, Ajustes permite cargar y retirar de forma idempotente cinco recetas de demostración. Nunca se insertan automáticamente y el controlador no existe en release.

## Validación

```shell
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew compileDebugAndroidTestKotlin
./gradlew connectedDebugAndroidTest
```

En Windows se puede usar `gradlew.bat`. La aplicación requiere Android 8.0 (API 26) o posterior.

La documentación se encuentra en [`docs`](docs/). El encargo y criterios del Sprint 5 están en [`docs/SPRINT_05_COOKING_MODE.md`](docs/SPRINT_05_COOKING_MODE.md).
