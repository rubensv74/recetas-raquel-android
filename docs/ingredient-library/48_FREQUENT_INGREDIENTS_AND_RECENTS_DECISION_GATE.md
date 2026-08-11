# Gate 48 — Ingredientes frecuentes y decisión sobre «Recientes»

Estado: **VALIDADO — OPCIÓN A**  
Fecha: 2026-08-10  
Rama: `program/ingredient-library-food-safety`

## 1. Decisión adoptada

Se mantiene la sección **Frecuentes** y **no se implementa «Recientes»** en esta fase.

La decisión corresponde a la **Opción A**: no introducir una nueva persistencia destinada a registrar cuándo se selecciona cada ingrediente.

## 2. Motivo

El modelo actual permite calcular «Frecuentes» de forma verificable porque cada ingrediente de una receta puede conservar `catalogIngredientId`.

En cambio, el modelo actual no contiene un timestamp de selección del ingrediente. `Recipe.updatedAt` no es un sustituto válido porque cambia ante modificaciones de la receta que no significan que un ingrediente haya sido seleccionado recientemente.

Por tanto, construir «Recientes» a partir de `Recipe.updatedAt` produciría información potencialmente falsa.

La aplicación no debe presentar una inferencia como si fuera un historial real de uso.

## 3. Regla funcional de «Frecuentes»

La sección se deriva exclusivamente de las recetas ya almacenadas.

Reglas:

- se cuentan **recetas distintas** que contienen cada `catalogIngredientId`;
- varias apariciones del mismo ingrediente dentro de una misma receta cuentan como una sola receta;
- un ingrediente debe aparecer en **al menos 2 recetas distintas** para considerarse frecuente;
- se muestran como máximo **6 ingredientes**;
- el orden principal es por número de recetas, de mayor a menor;
- solo se admiten identidades de catálogo con rol `CULINARY`;
- quedan excluidas identidades `REGULATORY_TECHNICAL`;
- ingredientes personalizados o legacy sin identidad de catálogo no se mezclan en esta clasificación.

La sección solo aparece en el estado inicial de la biblioteca cuando existe historial suficiente. Una búsqueda o filtro activo continúa mostrando los resultados ordinarios de la biblioteca.

## 4. Qué NO se añade

Esta decisión no incorpora:

- tabla de historial de selección;
- timestamp `lastSelectedAt`;
- contador mutable de clics o selecciones;
- telemetría de comportamiento;
- migración Room adicional;
- heurísticas basadas en `Recipe.updatedAt`;
- inferencia de «reciente» a partir de la fecha de modificación de una receta.

Room permanece en **v6**.

## 5. Gate técnico de «Frecuentes»

Implementación validada sobre:

- commit: `03a9183776fb7b0e213a97b01846946fbb35ba3b`;
- workflow: `Android CI`;
- run: **31403926138**;
- run number: **143**.

Resultado:

- `assembleDebug`: PASS;
- unit tests: PASS;
- `lint`: PASS;
- compilación de tests instrumentados: PASS;
- `assembleRelease`: PASS;
- guard Room previo: PASS;
- `connectedDebugAndroidTest`: PASS;
- **76/76 tests instrumentados**;
- guard Room posterior: PASS;
- conjunto de esquemas esperado y preservado: `1.json` a `6.json`.

## 6. Cobertura protegida

Las pruebas añadidas protegen especialmente que:

1. la frecuencia se calcule por **recetas distintas**;
2. duplicar un ingrediente dentro de una receta no infle el contador;
3. el orden responda al número real de recetas;
4. una identidad `REGULATORY_TECHNICAL` quede fuera aunque aparezca en recetas históricas;
5. la sección de frecuentes mantenga separadas las acciones de seleccionar ingrediente y consultar su información.

## 7. Consecuencia arquitectónica

No queda abierta ninguna necesidad de cambiar el esquema para esta funcionalidad.

Si en una evolución futura se considera imprescindible una sección «Recientes», deberá abrirse una nueva decisión arquitectónica para definir un historial explícito y auditable de selección. Hasta entonces, **«Recientes» no forma parte del comportamiento implementado y no debe reconstruirse mediante aproximaciones**.

## 8. Cierre

La Opción A queda aceptada y cerrada:

- **Frecuentes: implementado y validado**.
- **Recientes: deliberadamente no implementado**.
- **Persistencia adicional de comportamiento: no autorizada ni necesaria**.
- **Room: v6, sin cambios**.
