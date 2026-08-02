# Recetas de Raquel

Aplicación Android privada y local-first para guardar y consultar recetas sin conexión.

## Requisitos

- Android Studio compatible con AGP 9.0.1
- JDK de Android Studio o JDK compatible con Gradle 9.2.1
- Android SDK 36.1 instalado

## Estado

El Sprint 1 incorpora la fuente de verdad local con Room: modelos de dominio, validación, entidades para recetas, ingredientes y pasos, CRUD mediante repositorio, transacciones y eliminación en cascada. La UI conserva los placeholders del Sprint 0; todavía no existe catálogo ni editor funcional.

La base de datos de producción se llama `recipes.db`. Su esquema inicial es la versión 1 y se exporta a `app/schemas`. Los IDs son UUID almacenados como `String` y los timestamps son milisegundos Unix UTC (`Long`).

## Validación

```shell
./gradlew clean
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew compileDebugAndroidTestKotlin
```

Con un dispositivo o emulador disponible:

```shell
./gradlew connectedDebugAndroidTest
```

En Windows se puede usar `gradlew.bat`. La aplicación requiere Android 8.0 (API 26) o posterior.

La documentación de producto y arquitectura se encuentra en [`docs`](docs/).
