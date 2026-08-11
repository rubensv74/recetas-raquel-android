# Gate 52 — Readiness final de biblioteca y seguridad alimentaria

**Estado:** READY FOR PR  
**Fecha:** 2026-08-11  
**Rama:** `program/ingredient-library-food-safety`

## Objetivo

Consolidar la evidencia final antes de integrar en `master` la evolución de biblioteca de ingredientes, seguridad alimentaria y soporte regulatorio.

## Estado funcional consolidado

Quedan validados, entre otros, los siguientes bloques:

- biblioteca amplia de ingredientes por categorías;
- búsqueda y alias;
- distinción `CULINARY` / `REGULATORY_TECHNICAL`;
- ocultación de identidades regulatorias técnicas en la biblioteca normal;
- detalle de identidad, alias y relaciones culinarias;
- detalle de seguridad y fuentes;
- información regulatoria separada de las advertencias de seguridad;
- creación de ingredientes personalizados;
- declaraciones de seguridad personalizadas;
- tratamiento explícito de composición desconocida;
- metadatos de producto comercial;
- sección `Frecuentes` derivada de recetas guardadas;
- decisión expresa de no implementar `Recientes` sin señal temporal fiable;
- selección de ingrediente y transferencia al editor de receta;
- preservación de `catalogIngredientId` / `customIngredientId` al guardar;
- resumen de seguridad de receta;
- etiquetado regulatorio separado en detalle de receta;
- accesibilidad de los estados de seguridad.

## Contratos arquitectónicos preservados

1. Identidad/linaje culinario, evidencia de seguridad y exenciones regulatorias son conceptos independientes.
2. Una exención regulatoria es un efecto jurídico de etiquetado y no implica ausencia de alérgeno ni seguridad clínica.
3. Las exenciones nunca eliminan ni reducen observaciones de seguridad de una receta.
4. No existe propagación automática de seguridad por linaje culinario.
5. Las identidades `REGULATORY_TECHNICAL` permanecen disponibles internamente pero no son seleccionables desde la biblioteca culinaria normal.
6. La app continúa siendo offline-first, sin backend, autenticación ni permisos de red nuevos.

## Persistencia

- Room actual: **v6**.
- Esquemas versionados: `1.json` a `6.json`.
- Catálogo activo: **v12**.
- Historial regulatorio versionado preservado.

## Evidencia remota definitiva

Android CI #153:

- Run: `31461553912`
- SHA validado: `fd93f58945b6e1862fdbdb24555f8b43f3350711`
- Evento: `workflow_dispatch`
- Resultado global: `success`

Resultados:

```text
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
Room schema guard                PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS
Room schema guard post-emulator  PASS
```

Pruebas instrumentadas:

```text
82 tests
0 skipped
0 failed
BUILD SUCCESSFUL
```

Room se mantuvo sin diferencias respecto al contrato versionado antes y después del emulador.

## Sincronización con master

En la revisión previa a PR, la rama aparece históricamente por detrás de `master` en cuatro commits. Los únicos archivos afectados por esos commits son:

- `.github/workflows/android-ci.yml`;
- `AGENTS.md`.

Ambos archivos tienen en `master` y en esta rama **exactamente el mismo blob SHA**, por lo que no existe diferencia material que requiera un merge técnico previo ni una nueva ejecución manual.

No se repite Android CI completo porque #153 ya ejecutó la regresión de release sobre el último código funcional; los commits posteriores son exclusivamente documentales.

## Criterio de integración

La feature está preparada para Pull Request hacia `master`.

La PR deberá ejecutar el gate ordinario configurado para cambios Android. La validación instrumentada completa ya queda cubierta por Android CI #153.

La fusión a `master` requiere autorización expresa y queda fuera de este gate.
