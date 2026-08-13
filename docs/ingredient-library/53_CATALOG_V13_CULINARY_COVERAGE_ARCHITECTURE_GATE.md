# Gate 53 — Catálogo v13: cobertura culinaria y arquitectura de ingredientes compuestos

**Estado:** RESUELTO — OPCIÓN B IMPLEMENTADA

**Fecha:** 2026-08-11

## Contexto

La revisión visual de la aplicación confirmó que la arquitectura de biblioteca y seguridad funcionaba, pero también reveló que el catálogo v12 no cubría todavía suficientemente los ingredientes culinarios cotidianos. v12 contenía 272 ingredientes y estaba fuertemente orientado a validar la arquitectura de seguridad, linaje y regulación.

La rama `program/culinary-catalog-v13` amplía el catálogo sin modificar v12 y sin cambiar Room v6.

## Estado actual de v13

v13 utiliza `schemaVersion = 5`, incorpora composición explícita y permanece en estado `DRAFT` hasta cerrar la validación Android.

Conteos actuales:

- categorías: 20
- ingredientes: 808
- alias: 335\n- componentes explícitos: 48
- relaciones de linaje culinario: 143
- grupos de seguridad: 14
- fuentes de seguridad: 3
- relaciones de seguridad: 133
- exenciones regulatorias: 20

La fotografía v12 permanece inmutable.

## Expansión realizada

Se han incorporado 282 ingredientes adicionales respecto a v12, concentrados en:

- arroces, harinas, almidones y legumbres;
- aceites, grasas, azúcares y endulzantes;
- ingredientes básicos de despensa, bebidas, sales y vinagres;
- conservas vegetales y legumbres cocidas;
- cortes adicionales de carnes y aves;
- lácteos y quesos cotidianos;
- huevos y presentaciones de huevo;
- pescados de uso habitual;
- crustáceos y moluscos de uso habitual.

Los ingredientes culinarios simples o sin relación directa registrada con un grupo regulado no reciben una relación de seguridad inventada.

Los 100 nuevos ingredientes de las familias reguladas de leche, huevo, pescado, crustáceos y moluscos tienen relaciones de seguridad explícitas y separadas de su linaje culinario. El linaje no propaga seguridad ni exenciones.

## Herramienta reproducible

`tools/catalog/generate_v13_regulated_relations.py` genera de forma determinista las 100 relaciones de seguridad y las 100 relaciones de linaje correspondientes a los nuevos ingredientes regulados, y recalcula los contadores del manifiesto.

Se utilizó un workflow temporal de una sola ejecución para materializar los JSON generados. El workflow temporal se eliminó después; el script permanece como herramienta de mantenimiento.

## Activación y pruebas

`IngredientCatalogAssetReader.DEFAULT_VERSION_DIRECTORY` apunta ya a `ingredient-catalog/v13` en esta rama.

Se ha añadido `CatalogV13CulinaryCoverageTest`, que valida:

- integridad estructural del catálogo;
- conteos de v13;
- búsqueda mediante alias representativos;
- preservación del límite `CULINARY` / `REGULATORY_TECHNICAL`;
- ausencia de relaciones de seguridad inventadas en alimentos simples;
- existencia de relaciones explícitas en leche, huevo, pescado, crustáceos y moluscos;
- transición de importación v12 → v13;
- conservación del historial regulatorio por versión.

El gate arquitectónico quedó resuelto mediante la opción B. La ejecución Android CI completa permanece como gate de liberación independiente.

## Límite alcanzado

La mayor parte de los huecos restantes ya no son alimentos simples o especies concretas. Son ingredientes preparados o de composición variable, por ejemplo:

- pan y productos de panadería;
- pasta y fideos;
- mayonesa y otras emulsiones;
- chocolate y coberturas;
- caldos, fondos y cubitos;
- embutidos y carnes procesadas;
- salsas preparadas;
- bebidas vegetales;
- conservas y preparados de pescado;
- masas y preparados de repostería;
- otros alimentos compuestos o dependientes de formulación/marca.

Añadir estos productos como simples nombres al catálogo es técnicamente posible, pero el modelo actual no representa su composición de forma estructurada. Esto afecta directamente a la utilidad y fiabilidad de la información de seguridad.

## Decisión adoptada

### Opción A — Identidades compuestas genéricas sin composición estructurada

Añadir los productos preparados como `CULINARY`, normalmente con `VARIABLE_BY_BRAND` o `VARIABLE_BY_PREPARATION`.

- No cambia Room.
- No se infieren alérgenos por composición típica.
- Solo se registran relaciones de seguridad cuando sean universales y estén justificadas.
- En muchos productos la ficha indicará que la información disponible puede ser incompleta.
- Amplía rápidamente la biblioteca, pero limita el valor del análisis de seguridad para alimentos compuestos.

### Opción B — Composición explícita para ingredientes compuestos

Introducir una relación estructurada de componentes para los ingredientes compuestos del catálogo.

Ejemplo conceptual:

`CatalogIngredientComponent(parentIngredientId, componentIngredientId, ...)`

La composición tendría una semántica distinta al linaje culinario. El motor podría usar únicamente componentes explícitos para agregar seguridad, sin inferir nada por similitud o familia.

Consecuencias previsibles:

- evolución del contrato de catálogo;
- nueva entidad/relación persistida;
- migración Room v6 → v7;
- reglas explícitas para composición fija frente a formulación variable;
- pruebas de agregación y de no propagación por linaje;
- mayor utilidad y trazabilidad para pan, pasta, mayonesa, chocolate, salsas, preparados, etc.

**Recomendación:** Opción B, porque evita llenar la biblioteca de productos nominalmente disponibles pero poco útiles desde el punto de vista de seguridad.

### Opción C — Mantener los compuestos fuera del catálogo maestro

El catálogo maestro se limita a alimentos simples, especies, cortes y productos de composición suficientemente predecible.

Los productos compuestos o comerciales se crean mediante el formulario de ingrediente personalizado/comercial y se documentan a partir de su etiqueta.

- No cambia Room.
- Es la opción más conservadora para seguridad.
- Mantiene el catálogo maestro conceptualmente limpio.
- Obliga a más creación manual y entra en tensión con el objetivo de una biblioteca amplia que reduzca al mínimo el formulario.

## Criterio de decisión

La pregunta ya no es cuántos ingredientes añadir, sino qué debe significar una identidad compuesta dentro del catálogo maestro y qué evidencia puede utilizar el motor de seguridad.

Hasta resolver este gate no deben añadirse masivamente alimentos preparados o de formulación variable.
\n## Resolución posterior\n\nSe adoptó la opción B y se implementaron `schemaVersion = 5`, Room v7 y componentes explícitos. La expansión editorial independiente documentada en el Gate 54 elevó el catálogo deduplicado a 808 ingredientes.\n