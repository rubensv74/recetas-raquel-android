# Gate 55 — Validación instrumentada de v13 bloqueada por el runner Android

**Estado:** SUPERADO — VALIDACIÓN LOCAL COMPLETA  
**Fecha:** 2026-08-13  
**Rama:** `program/culinary-catalog-v13`

## Resultado alcanzado

La fotografía v13 ha quedado ampliada mediante curación editorial independiente:

- 808 ingredientes efectivos;
- 20 categorías;
- 228 identidades editoriales nuevas;
- 4 duplicados históricos de harinas eliminados;
- identificadores únicos;
- nombres normalizados únicos;
- manifiesto y conteos coherentes;
- entradas editoriales marcadas `REVIEW_REQUIRED`;
- sin relaciones de seguridad inventadas;
- pruebas JVM superadas;
- pruebas Android compiladas correctamente;
- esquema Room v7 presente.

## Bloqueo

La ejecución instrumentada no ha llegado a ejecutar ninguna prueba debido a fallos del emulador alojado en GitHub Actions.

### Intento API 36

Run: `31721420133`

El emulador declaró el arranque completado después de aproximadamente quince minutos, pero Android no había iniciado el servicio de paquetes:

```
Starting 0 tests on emulator-5554
Can't find service: package
```

### Intento API 35

Run: `31723549277`

El emulador no completó el arranque dentro del tiempo de la acción:

```
Timeout waiting for emulator to boot.
```

En ambos intentos pasaron previamente:

- validación estructural del catálogo;
- conteo mínimo de 800;
- comprobación de duplicados;
- pruebas JVM;
- compilación de pruebas instrumentadas.

## Interpretación

No existe evidencia de fallo funcional de v13. Tampoco existe evidencia suficiente para aprobarla: las pruebas instrumentadas no se ejecutaron.

El problema observado pertenece al entorno de emulación alojado, no al catálogo ni a una aserción de prueba.

## Decisión requerida

Elegir un entorno capaz de ejecutar `connectedDebugAndroidTest` de forma fiable:

### Opción A — Validación local en Android Studio

Ejecutar en el equipo de desarrollo:

```powershell
.\gradlew connectedDebugAndroidTest
```

con un AVD estable ya iniciado.

**Recomendada**, porque reproduce el procedimiento que anteriormente completó 33 pruebas instrumentadas correctamente y evita seguir consumiendo minutos de Actions en arranques defectuosos.

### Opción B — Runner GitHub con aceleración fiable

Configurar un runner propio o revisar la capacidad KVM del runner antes de lanzar el emulador.

### Opción C — Firebase Test Lab u otro dispositivo gestionado

Externalizar la ejecución instrumentada a una infraestructura Android especializada.

## Condición de cierre

El gate queda cerrado cuando:

1. `connectedDebugAndroidTest` ejecuta realmente la batería (no 0 tests);
2. todas las pruebas pasan;
3. el esquema Room no presenta cambios no confirmados;
4. se registra la evidencia de ejecución;
5. el manifiesto puede pasar de `DRAFT` a `RELEASED`.

Hasta entonces v13 no debe integrarse en `master`.


## Evidencia de cierre

Ejecución local realizada el 2026-08-13 sobre `Medium_Phone(AVD) - 16`:

```text
BUILD SUCCESSFUL — compileDebugAndroidTestKotlin
Starting 84 tests on Medium_Phone(AVD) - 16
Finished 84 tests on Medium_Phone(AVD) - 16
BUILD SUCCESSFUL in 1m 27s
0 skipped, 0 failed
```

Resultado:

- 84/84 pruebas instrumentadas superadas;
- ninguna prueba omitida;
- ninguna prueba fallida;
- rutas históricas de migración 1/2/4/5 → 7 validadas;
- catálogo v13 importado y consultado;
- composición explícita Room v7 validada;
- manifiesto autorizado para pasar de `DRAFT` a `RELEASED`.

El Gate 55 queda cerrado.
