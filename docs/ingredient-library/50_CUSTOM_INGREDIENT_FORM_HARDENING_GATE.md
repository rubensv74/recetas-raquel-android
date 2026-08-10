# Gate 50 — Endurecimiento del formulario de ingrediente personalizado

**Estado:** IMPLEMENTADO — REPETICIÓN DE GATE MANUAL PENDIENTE  
**Fecha:** 2026-08-11  
**Rama:** `program/ingredient-library-food-safety`  
**HEAD funcional corregido:** `53a2a24fe1578ccb1abf43fd77b0801b8d007757`

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

## 7. Resultado de Android CI #150

El gate manual se ejecutó sobre el commit documental `abc155a2240c37e5badca650ed0878b932e0418a`.

### Fase de calidad

Resultado: **PASS**.

- debug build: PASS;
- unit tests: PASS;
- lint: PASS;
- guard Room: PASS.

### Fase manual completa

- `assembleRelease`: PASS;
- arranque del emulador: PASS;
- compilación de tests instrumentados: **FAIL**.

El fallo no correspondía a código de producción ni a Room. La causa fue una incompatibilidad de la prueba `CustomIngredientEditorUiTest` con la versión actual de Compose Test:

```text
Unresolved reference 'assertDoesNotExist'
```

La prueba utilizaba una API que no está disponible en la versión de Compose Test del proyecto.

## 8. Corrección aplicada

Commit de corrección:

`53a2a24fe1578ccb1abf43fd77b0801b8d007757`

Se eliminaron las llamadas a `assertDoesNotExist()` y se sustituyeron por una comprobación negativa basada en `fetchSemanticsNode()`, API que ya ha compilado correctamente en gates anteriores del proyecto.

No se ha modificado código de producción.

## 9. Criterio de cierre

El Gate 50 continúa pendiente de una nueva ejecución manual sobre el HEAD corregido. Para cambiar el estado a `VALIDADO`, el nuevo run debe confirmar:

```text
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
Room schema guard                PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS
Room schema guard posterior      PASS
```

Tras ese resultado se registrarán el commit exacto, el run de GitHub Actions y el número final de pruebas instrumentadas.
