# 34 — FASE 5: REGLA DE IDENTIDAD EN EL EDITOR

**Estado:** REGLA FUNCIONAL CERRADA  
**Fecha:** 2026-08-09

Una fila seleccionada desde la biblioteca representa una identidad concreta del catálogo y debe persistir `catalogIngredientId`.

Por seguridad e integridad de datos, el editor no permitirá que el usuario cambie únicamente el texto visible del nombre y mantenga detrás el mismo `catalogIngredientId`. Esa combinación produciría una discrepancia entre la identidad evaluada por el sistema y el ingrediente que ve la persona usuaria.

Para una fila de catálogo:

```text
nombre canónico   solo lectura
cantidad          editable
unidad            editable
notas             editable
orden             editable
eliminación       permitida
```

Para sustituir la identidad se elimina la fila y se selecciona otra desde la biblioteca. La introducción manual permanece separada y no crea un `catalogIngredientId` por inferencia.

Esta regla no cambia el modelo de datos acordado; aplica el invariante ya definido entre identidad de catálogo e ingrediente personalizado.
