# Recetas de Raquel

Aplicación Android privada y local-first para guardar y consultar recetas de cocina sin conexión. El repositorio contiene la fundación técnica del Sprint 0; la gestión de recetas aún no está implementada.

## Requisitos

- Android Studio compatible con AGP 9.0.1
- JDK 11 o superior compatible con Gradle 9.2.1
- Android SDK 36.1 instalado

## Ejecutar y validar

Abre el proyecto raíz en Android Studio y ejecuta la configuración `app`, o usa:

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

En Windows también puedes usar `gradlew.bat`. La aplicación requiere Android 8.0 (API 26) o posterior.

## Estado

Incluye una única Activity, tema Material 3, Home, Ajustes y placeholders de las pantallas futuras. No incluye base de datos, red, autenticación, sincronización ni gestión real de recetas.

La documentación de producto y arquitectura se encuentra en [`docs`](docs/).
