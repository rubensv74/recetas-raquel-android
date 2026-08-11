# Cierre de producto — Recetas de Raquel v1

**Estado:** EN CURSO  
**Fecha:** 2026-08-11

## Objetivo

Pasar de una aplicación funcionalmente completa a una primera versión estable que pueda utilizarse de forma continuada sin que la evolución técnica ponga en riesgo los datos personales de la usuaria.

## Base ya cerrada

La PR #9 integró en `master` la biblioteca de ingredientes y la base de seguridad alimentaria/regulatoria.

Evidencia de regresión funcional previa a integración:

```text
Android CI #153
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
Room schema guard                PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS — 82/82
Room schema guard post-emulator  PASS
```

El gate ordinario de Pull Request #154 también quedó verde antes del merge.

## Qué significa "v1 terminada"

La versión 1 se considera cerrada cuando se cumplen todos estos puntos:

- [x] CRUD de recetas y consulta local.
- [x] Fotografías de portada y pasos.
- [x] Modo cocina.
- [x] Biblioteca maestra de ingredientes.
- [x] Ingredientes personalizados.
- [x] Seguridad alimentaria y alertas de receta.
- [x] Separación regulatoria.
- [x] Persistencia Room con migraciones no destructivas.
- [x] Documentación raíz alineada con Room v6 / catálogo v12.
- [ ] Backup completo exportable por la usuaria.
- [ ] Restore completo y seguro.
- [ ] Prueba de aceptación en un teléfono real.
- [ ] Corrección de defectos bloqueantes encontrados en aceptación.
- [ ] Regresión/release final.
- [ ] Etiquetado/congelación de la primera versión estable.

## Orden de trabajo

### 1. Documentación de estado

Actualizar README, arquitectura, modelo de datos y especificación de producto para que describan la implementación que existe realmente en `master`.

### 2. Arquitectura Backup/Restore

Decidir:

- formato de la copia;
- datos incluidos;
- tratamiento de fotografías;
- estrategia de restauración;
- compatibilidad entre versiones;
- validación e integridad;
- comportamiento ante copia corrupta/incompatible.

No implementar restore destructivo sin cerrar esta decisión.

### 3. Implementación Backup/Restore

Criterio mínimo:

- export manual iniciado por la usuaria;
- archivo transportable fuera del sandbox de la app;
- inclusión de datos personales y fotografías;
- restore explícito con confirmación fuerte;
- validación previa antes de modificar la instalación actual;
- fallo seguro sin pérdida de la instalación vigente;
- pruebas de round-trip.

### 4. Aceptación en dispositivo real

Recorrido mínimo:

1. instalar versión candidata;
2. crear receta nueva;
3. seleccionar ingredientes de biblioteca;
4. crear ingrediente personalizado;
5. añadir cantidades/unidades/notas;
6. añadir portada y foto de paso;
7. guardar y reabrir;
8. comprobar alertas de seguridad;
9. usar favoritos/búsqueda;
10. ejecutar modo cocina;
11. exportar backup;
12. restaurar sobre una instalación controlada y comprobar igualdad funcional.

### 5. Freeze v1

Después de aceptación no se incorporan nuevas features a la candidata. Solo defectos bloqueantes o de integridad de datos.

## Mejoras que no bloquean v1

Quedan para iteraciones posteriores si la aceptación no las convierte en problema real:

- rediseño visual premium;
- selector premium de unidades;
- pickers/steppers de tiempos;
- temporizadores ejecutables;
- sincronización cloud/GitHub;
- distribución remota de catálogos sensibles.

## Principio de cierre

La v1 no necesita contener todas las mejoras imaginables. Necesita ser **útil, comprensible, recuperable y segura frente a pérdida de datos**.