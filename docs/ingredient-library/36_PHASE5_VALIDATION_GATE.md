# 36 — FASE 5: GATE DE VALIDACIÓN DE BIBLIOTECA DE INGREDIENTES

**Estado:** CERRADA — VALIDADA EN GITHUB ACTIONS  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-09  
**Commit funcional validado:** `e3718783223045902dc2b69f704215a989939f21`

## Resultado

La Fase 5 queda cerrada como flujo funcional de biblioteca-first para añadir ingredientes a una receta.

El gate automático ejecutado por GitHub Actions terminó en verde tanto para calidad como para pruebas instrumentadas.

```text
assembleDebug                    PASS
testDebugUnitTest                PASS
lintDebug                        PASS
compileDebugAndroidTestKotlin    PASS
assembleRelease                  PASS
connectedDebugAndroidTest        PASS — 50/50, 0 skipped, 0 failed
Room schema guard                PASS — 1.json, 2.json, 3.json, 4.json
```

No se generó `5.json` ni se modificó el esquema Room.

## Alcance validado

- importación del catálogo activo antes de la consulta;
- biblioteca dedicada accesible desde `Añadir ingrediente`;
- búsqueda determinista por nombre canónico y alias;
- normalización sin distinción de mayúsculas/minúsculas ni acentos;
- ranking exacto → prefijo → contiene;
- ausencia de fuzzy matching para identidad;
- filtro por categoría;
- selección explícita de una identidad de catálogo;
- devolución al editor de identificador, nombre canónico y unidad por defecto;
- persistencia de `catalogIngredientId` en el uso de receta;
- nombre de una identidad de catálogo protegido frente a edición funcional en el ViewModel;
- cantidad, unidad, notas, orden y eliminación continúan editables;
- vía manual mantenida separada y sin asociación implícita al catálogo;
- estados de información diferenciados entre relación de seguridad registrada, exención regulatoria e información directa potencialmente incompleta.

## Lenguaje de seguridad validado

La biblioteca utiliza mensajes descriptivos de los datos disponibles:

```text
Información de seguridad registrada
Información regulatoria específica
La información disponible puede ser incompleta
```

Ninguno de estos estados se interpreta como ausencia de alérgenos, ausencia de riesgo o aptitud clínica.

## Pruebas UI añadidas

`IngredientLibraryUiTest` cubre:

- estado inicial y acceso separado a introducción manual;
- selección explícita de un resultado;
- wording para información de seguridad registrada;
- wording para exención regulatoria;
- advertencia cuando no existe relación directa de seguridad registrada;
- selección de categoría separada del flujo manual.

Las pruebas históricas de catálogo de recetas y modo cocina se adaptaron al nuevo límite de dependencia usando un `FakeIngredientCatalogRepository` de test, sin acoplar esos tests al catálogo real.

## Nota de interfaz

La identidad de catálogo está protegida en la lógica del editor: `updateIngredientName` ignora cambios de nombre cuando existe `catalogIngredientId`. El control visual sigue reutilizando el campo de texto común del editor; convertirlo en una presentación visual inequívocamente de solo lectura se considera una mejora de claridad de UI y no cambia el invariante de identidad validado en esta fase.

## Siguiente fase

Fase 6 — ingredientes personalizados.

El contrato de implementación está definido en `35_PHASE6_CUSTOM_INGREDIENT_IMPLEMENTATION.md`. La infraestructura Room v4 ya contiene las tablas necesarias, por lo que el alcance inicial de Fase 6 no requiere migración de base de datos.
