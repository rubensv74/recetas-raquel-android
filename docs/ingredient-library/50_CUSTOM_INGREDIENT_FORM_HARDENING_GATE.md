# Gate 50 — Endurecimiento del formulario de ingrediente personalizado

**Estado:** IMPLEMENTADO — GATE MANUAL DE CI PENDIENTE  
**Fecha:** 2026-08-10  
**Rama:** `program/ingredient-library-food-safety`  
**HEAD funcional auditado:** `7f10556c053a4a33acd0b98b273837be5d5ebfc8`

## 1. Objetivo

Auditar el formulario de ingrediente personalizado contra el diseño funcional vigente y reforzar su comportamiento sin ampliar el modelo de datos ni introducir nuevas inferencias de seguridad.

El bloque parte de una conclusión importante: Room v6 ya dispone de todos los campos comprometidos por el diseño actual. No hace falta una migración para completar este alcance.

## 2. Cobertura funcional confirmada

El formulario y el agregado personalizado ya soportan:

- nombre;
- tipo `SIMPLE`, `COMPOUND` o `COMMERCIAL_PRODUCT`;
- categoría;
- unidad habitual;
- alias;
- notas;
- composición conocida o desconocida como estado explícito;
- marca, nombre comercial y fecha de lectura de etiqueta para productos comerciales;
- cero o más declaraciones de seguridad creadas por la persona usuaria.

La información personalizada continúa separada del catálogo maestro.

## 3. Endurecimiento aplicado

### 3.1 Metadatos exclusivos de productos comerciales

El repositorio local protege ahora el contrato de dominio, no solo la pantalla.

`brand`, `tradeName` y `labelReadAt` solo se persisten cuando el tipo es `COMMERCIAL_PRODUCT`.

Si otro punto de entrada intenta enviar esos campos para `SIMPLE` o `COMPOUND`, el repositorio los descarta. Así una llamada directa al repositorio no puede crear un estado que la UI no representa.

La fecha de lectura de etiqueta sigue validándose como fecha ISO real cuando corresponde a un producto comercial.

### 3.2 Alias

Se mantiene la normalización existente:

- se eliminan vacíos;
- se deduplican alias equivalentes tras normalización;
- no se guarda como alias una variante equivalente al propio nombre del ingrediente.

La prueba de persistencia incorpora explícitamente este último caso.

### 3.3 Declaraciones de seguridad duplicadas

El repositorio ya impedía dos relaciones con la misma pareja:

```text
safetyGroupId + relationType
```

El formulario replica ahora esa comprobación antes del guardado y muestra un mensaje comprensible:

> No repitas el mismo grupo y tipo de relación de seguridad.

La protección profunda del repositorio se mantiene; la validación de UI no la sustituye.

### 3.4 Campos condicionales de producto comercial

Las pruebas de UI protegen que:

- `COMMERCIAL_PRODUCT` muestra marca, nombre comercial y fecha de lectura de etiqueta;
- `SIMPLE` no muestra esos campos aunque el estado de UI contenga valores residuales.

El repositorio añade una segunda barrera para que dichos valores residuales tampoco puedan persistirse.

## 4. Invariantes de seguridad preservados

Este bloque no cambia las reglas de seguridad alimentaria:

- la persona usuaria solo puede registrar evidencia `USER_DECLARED` o `UNVERIFIED`;
- no puede autocalificar una declaración como evidencia legal, científica u oficial;
- una composición desconocida continúa significando información incompleta que requiere revisión;
- ausencia de declaraciones no significa ausencia de alérgenos o riesgo;
- no se infieren relaciones por nombre, categoría, similitud o linaje;
- una identidad personalizada no se convierte en identidad del catálogo;
- no se mezclan seguridad y exenciones regulatorias.

## 5. Cobertura automática añadida

### Pruebas unitarias

Se añade `CustomIngredientEditorValidationTest` para comprobar que:

- la misma pareja grupo + tipo de relación se detecta como duplicada;
- el mismo grupo puede tener tipos de relación distintos.

### Pruebas instrumentadas de persistencia

`CustomIngredientRepositoryTest` cubre además:

- preservación de metadatos comerciales válidos;
- eliminación de alias equivalente al nombre;
- imposibilidad de persistir metadatos comerciales en `SIMPLE` y `COMPOUND`;
- rechazo atómico de una relación de seguridad duplicada;
- rechazo atómico de una fecha de etiqueta imposible en un producto comercial.

### Pruebas instrumentadas de UI

`CustomIngredientEditorUiTest` cubre además:

- presencia de los tres campos adicionales en producto comercial;
- ausencia de esos campos en un ingrediente simple.

## 6. Persistencia y arquitectura

No se modifica:

- `RecipeDatabase`;
- versión Room;
- entidades;
- DAO;
- migraciones;
- schemas `1.json` a `6.json`;
- catálogo de ingredientes;
- agregación de seguridad de recetas;
- historial regulatorio.

No aparece un nuevo gate de arquitectura en este bloque.

La composición estructurada por componentes, códigos de barras, OCR de etiquetas u otros datos de producto siguen fuera de alcance y requerirían una decisión arquitectónica independiente.

## 7. Estado del gate técnico

El diff funcional desde `9a8340dc0fc6aa083ad5dc51782016c75c24bd3f` hasta `7f10556c053a4a33acd0b98b273837be5d5ebfc8` está limitado a código y pruebas del formulario personalizado.

No existe una pull request abierta para esta rama, por lo que los commits intermedios no han consumido Android CI. El workflow está configurado para validación automática de PR hacia `master` y para ejecución completa mediante `workflow_dispatch`.

En la sesión actual, el conector de GitHub no expone la acción `workflow_dispatch` y el entorno no dispone de `gh`. Por ese motivo no se fuerza una PR artificial ni se altera el workflow para lanzar CI.

Hasta ejecutar el gate manual, este documento debe permanecer en estado **IMPLEMENTADO — GATE MANUAL DE CI PENDIENTE** y no debe considerarse `VALIDADO`.

## 8. Criterio de cierre

Para cambiar el estado a `VALIDADO`, el gate manual debe confirmar al menos:

```text
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
compileDebugAndroidTestKotlin    PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS
Room schema guard                PASS
```

Tras ese resultado se registrarán el commit exacto, el run de GitHub Actions y el número final de pruebas instrumentadas.
