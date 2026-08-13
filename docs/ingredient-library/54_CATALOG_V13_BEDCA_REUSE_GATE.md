# Gate 54 — Reutilización de BEDCA para la expansión culinaria v13

**Estado:** RESUELTO — OPCIÓN A  
**Fecha:** 2026-08-13  
**Rama:** `program/culinary-catalog-v13`

## Motivo

El objetivo de v13 exige ampliar la biblioteca hasta 800–1.200 ingredientes y establece BEDCA como referencia principal de nomenclatura.

La revisión realizada antes de continuar la carga masiva no ha localizado una licencia abierta ni condiciones expresas que autoricen la extracción, reproducción o redistribución sistemática de la nomenclatura de BEDCA dentro de una aplicación.

La interfaz pública de BEDCA muestra:

> Copyright © 2007 Consorcio BEDCA y Agencia Española de Seguridad Alimentaria y Nutrición. Todos los derechos reservados.

AESAN identifica BEDCA como la base española de composición de alimentos y la relaciona con EuroFIR, pero la información pública revisada no resuelve los derechos de reutilización del conjunto.

## Estado técnico previo al gate

- v12 activo en `master`: 272 ingredientes.
- v13 en rama: 584 registros brutos.
- Duplicados confirmados: 4 identidades de harina.
- Base v13 efectiva después de deduplicar: 580 ingredientes.
- Objetivo mínimo pendiente: 220 ingredientes adicionales.
- Manifiesto: `DRAFT`.
- Gate Android conectado: no cerrado.
- PR: no creada.

## Riesgo

Los nombres individuales de alimentos suelen ser términos descriptivos. El problema no es utilizar expresiones comunes como «manzana» o «arroz», sino extraer o reconstruir sistemáticamente una parte sustancial de una base de datos protegida y redistribuirla como catálogo propio.

No debe inferirse permiso de reutilización a partir del acceso público o del respaldo institucional.

## Opciones

### Opción A — Curación culinaria independiente

Construir los 220+ ingredientes restantes mediante una lista editorial propia basada en práctica culinaria española, comercio minorista habitual y fuentes abiertas compatibles.

BEDCA se mantiene únicamente como referencia conceptual y de contraste puntual, sin copiar ni extraer sistemáticamente su catálogo, códigos o datos nutricionales.

**Ventajas:**

- Permite continuar inmediatamente.
- Reduce el riesgo de derechos sobre bases de datos.
- Mantiene el catálogo centrado en la experiencia de búsqueda de recetas.

**Condiciones:**

- Registrar procedencia editorial propia.
- No declarar equivalencia oficial con BEDCA.
- No incorporar datos nutricionales de BEDCA.
- Conservar revisión humana antes de `RELEASED`.

### Opción B — Solicitar autorización a BEDCA/AESAN

Solicitar por escrito las condiciones de reutilización, atribución, modificación y redistribución dentro de la aplicación.

**Ventajas:**

- Permitiría una alineación formal con BEDCA si se concede autorización.

**Costes:**

- Bloquea el avance hasta recibir respuesta.
- Puede imponer atribución, restricciones de uso o límites de redistribución.

### Opción C — Utilizar otra fuente con licencia abierta

Adoptar una fuente alimentaria pública con licencia explícita y realizar una adaptación editorial al español, manteniendo AESAN y la normativa UE únicamente para seguridad alimentaria.

**Ventajas:**

- Procedencia reproducible y auditable.

**Costes:**

- Mayor trabajo de normalización.
- La nomenclatura puede no reflejar bien el uso doméstico español.
- Hay que verificar compatibilidad de licencia antes de importar.

## Recomendación

**Opción A — Curación culinaria independiente.**

El objetivo de v13 es que Raquel encuentre ingredientes habituales, no reproducir una base nutricional. Una lista propia, revisada y trazable cubre mejor esa necesidad y evita convertir BEDCA en una dependencia jurídica y operativa innecesaria.

## Decisión requerida

Elegir A, B o C antes de añadir masivamente los 220+ ingredientes restantes.

Hasta que se resuelva este gate:

- no se realizará extracción automatizada de BEDCA;
- no se presentará v13 como catálogo oficial o derivado de BEDCA;
- v13 permanecerá en `DRAFT`;
- no se integrará la rama en `master`.
\n## Resolución\n\nEl 2026-08-13 se adoptó la opción A. La ampliación se realizó mediante curación editorial propia, sin extracción masiva de BEDCA, sin datos nutricionales externos y sin declarar equivalencia oficial con BEDCA. El primer lote independiente añade 228 identidades; después de retirar cuatro duplicados previos, v13 alcanza 808 ingredientes.\n