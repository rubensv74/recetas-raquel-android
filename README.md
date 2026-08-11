# Recetas de Raquel

Aplicación Android privada, local-first y offline para guardar, consultar y cocinar recetas personales.

## Estado del producto

La base funcional de la aplicación está completa y se encuentra en fase de cierre de producto para una primera versión estable.

Incluye:

- catálogo local de recetas con búsqueda, categorías y favoritos;
- creación, edición y eliminación de recetas;
- ingredientes y pasos ordenables;
- fotografías de portada y por paso mediante Android Photo Picker;
- modo cocina guiado;
- biblioteca maestra de ingredientes por categorías y alias;
- ingredientes personalizados y productos comerciales;
- trazabilidad de seguridad alimentaria y fuentes;
- alertas de alérgenos y composición desconocida;
- información regulatoria separada de las advertencias de seguridad;
- catálogo culinario versionado hasta v12;
- soporte de identidades `CULINARY` y `REGULATORY_TECHNICAL`;
- sección de ingredientes frecuentes derivada de recetas guardadas.

La aplicación continúa funcionando completamente sin conexión y no requiere cuenta, backend ni permisos de red.

## Persistencia

La base de producción se llama `recipes.db` y usa Room **v6**.

Los esquemas históricos están versionados en:

```text
app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/
```

Se conservan `1.json` a `6.json` junto con migraciones explícitas y no destructivas.

Room almacena recetas, ingredientes utilizados en receta, pasos, catálogo estructurado, ingredientes personalizados, grupos/fuentes de seguridad y snapshots regulatorios. Las fotografías se guardan en almacenamiento privado de la aplicación y Room conserva únicamente rutas relativas.

## Seguridad alimentaria

La aplicación mantiene separados tres conceptos:

```text
identidad / linaje culinario
!= evidencia de seguridad
!= efecto regulatorio
```

Una exención regulatoria de etiquetado nunca se interpreta como ausencia de alérgeno ni como garantía de seguridad clínica. Las alertas de seguridad no se eliminan ni reducen por la existencia de una exención.

La biblioteca normal oculta identidades `REGULATORY_TECHNICAL`, aunque permanecen disponibles internamente para resolución y auditoría regulatoria.

## Requisitos de desarrollo

- Android Studio compatible con AGP 9.0.1
- JDK compatible con Gradle 9.2.1
- Android SDK 36.1
- Android 8.0 (API 26) o posterior

## Validación

Validación local ordinaria:

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Cuando un incremento afecta UI, persistencia, navegación o integración Android se ejecutan además las pruebas instrumentadas relevantes. El gate remoto completo se reserva para hitos deliberados mediante GitHub Actions.

La regresión funcional de la biblioteca y seguridad alimentaria cerró con:

```text
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
Room schema guard                PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS — 82/82
Room schema guard post-emulator  PASS
```

## Próximo objetivo

La etapa actual es **cierre de producto v1**:

1. documentación coherente con `master`;
2. prueba de aceptación en teléfono real;
3. diseño e implementación de Backup/Restore;
4. correcciones de aceptación;
5. congelación de una primera versión estable.

La documentación técnica se encuentra en [`docs`](docs/).