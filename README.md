# Recetas de Raquel

Aplicación Android privada, local-first y completamente offline para guardar, consultar y cocinar recetas personales.

## Estado del producto

**Estado actual: candidata a v1.0 — cierre de producto en curso.**

La aplicación ya incluye:

- catálogo local de recetas con búsqueda, filtros y favoritas;
- creación, edición y eliminación de recetas;
- ingredientes y pasos ordenables;
- fotografías de portada y por paso mediante Android Photo Picker;
- modo cocina guiado, un paso cada vez;
- biblioteca maestra de ingredientes por categorías y alias;
- ingredientes personalizados y productos comerciales;
- información de seguridad alimentaria con fuentes y jurisdicción;
- tratamiento explícito de composición desconocida;
- información regulatoria separada de las advertencias de seguridad;
- roles `CULINARY` y `REGULATORY_TECHNICAL` para evitar que identidades jurídicas artificiales aparezcan como ingredientes culinarios normales;
- sección de ingredientes frecuentes derivada de recetas guardadas;
- preservación del origen de cada ingrediente al guardar una receta.

La base de producción es `recipes.db` y actualmente utiliza **Room v6**. Los esquemas históricos `1.json` a `6.json` están versionados en `app/schemas`. El catálogo maestro activo es **ingredient-catalog v12**.

La evolución de biblioteca y seguridad alimentaria se integró en `master` mediante PR #9 después de una validación completa con 82 pruebas instrumentadas, 0 fallos, build debug/release, unitarias, lint y guards de Room en verde.

## Qué falta para declarar v1.0 cerrada

La funcionalidad principal está terminada. El cierre de producto se concentra en:

1. aceptación manual en un teléfono real;
2. decidir e implementar Backup/Restore para proteger los datos de la usuaria;
3. corregir únicamente incidencias encontradas durante aceptación;
4. generar y validar la build candidata a v1.0.

Mejoras como selectores premium, rediseño visual adicional o temporizadores ejecutables no bloquean v1.0.

## Principios

- **Offline-first:** no existe backend, autenticación ni dependencia de red para las funciones esenciales.
- **Privacidad local:** recetas, fotografías y datos personalizados se almacenan en el dispositivo.
- **Seguridad alimentaria conservadora:** una exención regulatoria nunca significa ausencia de alérgeno, ausencia de riesgo ni aptitud clínica.
- **Trazabilidad:** catálogo, fuentes, revisiones y cambios regulatorios se versionan y documentan.
- **Evolución no destructiva:** Room utiliza migraciones explícitas y conserva sus contratos históricos.

## Requisitos de desarrollo

- Android Studio compatible con AGP 9.0.1
- JDK de Android Studio o compatible con Gradle 9.2.1
- Android SDK 36.1 instalado
- Android 8.0 / API 26 o posterior en dispositivo

## Validación

Flujo local ordinario:

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Para cambios que afecten UI, navegación, persistencia o integración Android se ejecutan además las pruebas instrumentadas relevantes. GitHub Actions se reserva como gate remoto de Pull Request o validación manual deliberada, según `AGENTS.md`.

## Documentación

- [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) — alcance funcional vigente.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — arquitectura actual.
- [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) — modelo de datos Room v6.
- [`docs/SPRINT_PLAN.md`](docs/SPRINT_PLAN.md) — estado y próximos bloques.
- [`docs/ingredient-library`](docs/ingredient-library) — diseño, ADR y gates de biblioteca/seguridad.
- [`docs/food-safety`](docs/food-safety) — dossier y mantenimiento de conocimiento sensible.

En `debug`, Ajustes permite cargar y retirar recetas de demostración de forma idempotente. No se insertan automáticamente y el controlador no existe en `release`.
