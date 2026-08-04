# Recetas de Raquel

Aplicación Android privada y local-first para guardar y consultar recetas sin conexión.

## Requisitos

- Android Studio compatible con AGP 9.0.1
- JDK de Android Studio o compatible con Gradle 9.2.1
- Android SDK 36.1 instalado

## Estado

El Sprint 3 incorpora editor de recetas con creación, edición y eliminación. La pantalla única del editor permite campos obligatorios, ingredientes y pasos con reordenamiento, validación en línea, detección de cambios sin guardar y eliminación con confirmación. La UI consume Room exclusivamente mediante ViewModels y `RecipeRepository`.

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

La documentación se encuentra en [`docs`](docs/).
