# Gate 50 — Endurecimiento del formulario de ingrediente personalizado

**Estado:** VALIDADO  
**Fecha:** 2026-08-11  
**Rama:** `program/ingredient-library-food-safety`  
**Commit validado por CI:** `bea450a1149e28b0511302a6590f4b3a6bc13598`

## 1. Objetivo

Auditar el formulario de ingrediente personalizado contra el diseño funcional vigente y reforzar su comportamiento sin ampliar el modelo de datos ni introducir nuevas inferencias de seguridad.

Room v6 ya dispone de todos los campos comprometidos por el diseño actual. No ha sido necesaria una migración.

## 2. Cobertura funcional confirmada

El formulario y el agregado personalizado soportan:

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

`brand`, `tradeName` y `labelReadAt` solo se persisten cuando el tipo es `COMMERCIAL_PRODUCT`.

Si otro punto de entrada intenta enviar esos campos para `SIMPLE` o `COMPOUND`, el repositorio los descarta. La fecha de lectura de etiqueta sigue validándose como fecha ISO real cuando corresponde a un producto comercial.

### 3.2 Alias

Se mantiene la normalización existente:

- se eliminan vacíos;
- se deduplican alias equivalentes tras normalización;
- no se guarda como alias una variante equivalente al propio nombre del ingrediente.

### 3.3 Declaraciones de seguridad duplicadas

La pareja:

```text
safetyGroupId + relationType
```

continúa protegida en el repositorio y ahora también se valida antes del guardado en la UI, mostrando:

> No repitas el mismo grupo y tipo de relación de seguridad.

La validación de UI no sustituye la protección profunda del repositorio.

### 3.4 Campos condicionales de producto comercial

Las pruebas protegen que:

- `COMMERCIAL_PRODUCT` muestra marca, nombre comercial y fecha de lectura de etiqueta;
- `SIMPLE` no muestra esos campos aunque el estado de UI contenga valores residuales;
- esos valores residuales tampoco pueden persistirse.

## 4. Invariantes de seguridad preservados

Este bloque no cambia las reglas de seguridad alimentaria:

- la persona usuaria solo puede registrar evidencia `USER_DECLARED` o `UNVERIFIED`;
- no puede autocalificar una declaración como evidencia legal, científica u oficial;
- una composición desconocida sigue significando información incompleta que requiere revisión;
- ausencia de declaraciones no significa ausencia de alérgenos o riesgo;
- no se infieren relaciones por nombre, categoría, similitud o linaje;
- una identidad personalizada no se convierte en identidad del catálogo;
- no se mezclan seguridad y exenciones regulatorias.

## 5. Cobertura automática

### Pruebas unitarias

`CustomIngredientEditorValidationTest` comprueba que:

- la misma pareja grupo + tipo de relación se detecta como duplicada;
- el mismo grupo puede tener tipos de relación distintos.

### Pruebas instrumentadas de persistencia

`CustomIngredientRepositoryTest` cubre:

- preservación de metadatos comerciales válidos;
- eliminación de alias equivalente al nombre;
- imposibilidad de persistir metadatos comerciales en `SIMPLE` y `COMPOUND`;
- rechazo atómico de una relación de seguridad duplicada;
- rechazo atómico de una fecha de etiqueta imposible en un producto comercial.

### Pruebas instrumentadas de UI

`CustomIngredientEditorUiTest` cubre:

- presencia de los tres campos adicionales en producto comercial;
- ausencia de esos campos en un ingrediente simple.

La incompatibilidad detectada en el run #150 con `assertDoesNotExist()` se corrigió sustituyéndola por una comprobación compatible basada en `fetchSemanticsNode()`. No se modificó código de producción para resolver ese fallo de prueba.

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

## 7. Evidencia final — Android CI #151

GitHub Actions:

- workflow: `Android CI`;
- run: `31442693700`;
- run number: `151`;
- evento: `workflow_dispatch`;
- commit validado: `bea450a1149e28b0511302a6590f4b3a6bc13598`;
- resultado global: **SUCCESS**.

### Gate de calidad

- debug build: PASS;
- unit tests: PASS;
- lint: PASS;
- guard Room: PASS.

### Gate manual completo

- `assembleRelease`: PASS;
- emulador Android API 36: PASS;
- `connectedDebugAndroidTest`: PASS;
- **80 tests ejecutados**;
- **0 omitidos**;
- **0 fallidos**;
- `BUILD SUCCESSFUL`;
- guard Room posterior: PASS.

El contrato Room permanece sin cambios y continúa exactamente en v6.

## 8. Estado

**GATE SUPERADO.**

El formulario de ingrediente personalizado queda endurecido y validado sin ampliar la arquitectura ni introducir nuevas inferencias de seguridad.