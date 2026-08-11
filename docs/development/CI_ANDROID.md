# CI Android — validación autónoma

## Objetivo

Evitar que la validación habitual del proyecto dependa de ejecutar manualmente Gradle y el emulador en el equipo local.

El workflow principal está en:

```text
.github/workflows/android-ci.yml
```

## Cuándo se ejecuta

Se ejecuta automáticamente cuando hay cambios relevantes en ramas `program/**` y cuando una pull request hacia `master` modifica código Android, Gradle o el propio workflow.

También puede lanzarse manualmente mediante `workflow_dispatch`.

Los cambios únicamente documentales no disparan el gate pesado.

## Gate automatizado

### Job `quality`

Ejecuta en un runner estándar `ubuntu-latest`:

```text
assembleDebug
testDebugUnitTest
lintDebug
compileDebugAndroidTestKotlin
assembleRelease
```

Además comprueba que Room conserva exactamente:

```text
1.json
2.json
3.json
4.json
```

y que las pruebas/builds no modifican silenciosamente los schemas versionados.

El proyecto compila contra Android SDK 36.1, coherente con `compileSdk` API 36 + `minorApiLevel = 1`.

### Job `instrumented`

Solo se ejecuta si `quality` ha terminado correctamente.

Levanta un emulador Android API 36 mediante un runner estándar de GitHub y ejecuta:

```text
connectedDebugAndroidTest
```

Al terminar vuelve a comprobar la invariancia de los schemas Room.

## Principios de seguridad del workflow

- `permissions: contents: read` por defecto.
- No publica APK.
- No crea releases.
- No modifica `master`.
- No hace merge automático.
- No utiliza secretos del repositorio.
- Las actions externas están fijadas a commits concretos para reducir cambios inesperados de dependencias CI.
- Se usa `concurrency` para cancelar un gate anterior cuando la misma rama recibe un commit más nuevo.

## Coste

Para repositorios públicos, GitHub documenta que los runners estándar hospedados por GitHub no consumen la cuota de minutos incluida del plan. Los runners de mayor tamaño sí son facturables, por lo que este workflow utiliza exclusivamente `ubuntu-latest` estándar.

La política vigente debe revisarse si el repositorio vuelve a ser privado o si se cambia el tipo de runner.

## Método de trabajo desde esta implantación

El flujo previsto pasa a ser:

```text
cambio en rama program/**
        ↓
push
        ↓
GitHub Actions
  quality
        ↓
  instrumented
        ↓
resultado del gate
```

El desarrollo puede continuar sin pedir al usuario que replique rutinariamente los comandos Gradle en su PC. La validación local queda reservada para incidencias específicas del entorno, pruebas en hardware real o verificaciones UX que no puedan representarse correctamente en CI.

## Criterio de gate verde

```text
assembleDebug                     PASS
testDebugUnitTest                 PASS
lintDebug                         PASS
compileDebugAndroidTestKotlin     PASS
assembleRelease                   PASS
connectedDebugAndroidTest         PASS
Room schemas                      1..4 únicamente
git diff de schemas               vacío
```

Cualquier fallo detiene el avance del bloque afectado hasta corregirlo.
