# 42 — AUDITORÍA DE COBERTURA REGULATORIA — ANEXO II

**Estado:** COMPLETADA — BASE PARA CATÁLOGO v11  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha de revisión:** 2026-08-10

## Objetivo

Contrastar el registro regulatorio del catálogo v10 con el Anexo II vigente del Reglamento (UE) n.º 1169/2011 y detectar excepciones legales expresas todavía no modeladas.

Fuente de verdad jurídica revisada:

- Reglamento (UE) n.º 1169/2011, versión consolidada 2025-04-01, CELEX `02011R1169-20250401`.
- EUR-Lex: `https://eur-lex.europa.eu/eli/reg/2011/1169/2025-04-01/spa`

## Resultado de la auditoría

El catálogo v10 contiene 14 exenciones regulatorias y cubre:

- cereales: jarabes de glucosa de trigo incluida dextrosa, maltodextrinas de trigo y jarabes de glucosa de cebada;
- soja: aceite y grasa totalmente refinados, tocoferoles naturales derivados de soja, fitosteroles/ésteres de fitosterol y ésteres de fitostanol;
- mostaza: ácido behénico bajo las condiciones regulatorias ya documentadas.

Quedaban sin representación explícita seis cláusulas del Anexo II:

1. cereales con gluten utilizados para hacer destilados alcohólicos;
2. gelatina de pescado utilizada como soporte de vitaminas o preparados de carotenoides;
3. gelatina de pescado o ictiocola utilizada como clarificante en cerveza y vino;
4. lactosuero utilizado para hacer destilados alcohólicos;
5. lactitol;
6. frutos de cáscara utilizados para hacer destilados alcohólicos.

## Decisión de modelado

El motor actual no interpreta semánticamente `conditions`; la aplicabilidad operativa se determina por identidad exacta, jurisdicción, estado y fecha. Por ello una excepción condicionada no puede asociarse a un ingrediente genérico como `ing-fish`, `ing-milk` o `ing-wheat`.

v11 crea una identidad técnica específica para cada cláusula que falta. Cuando la excepción depende del proceso o finalidad, esa condición forma parte de la identidad canónica.

Ejemplo:

```text
NO:  Lactosuero -> exento si se usa para destilados
SÍ:  Lactosuero utilizado para hacer destilados alcohólicos -> exención exacta
```

Este patrón evita falsos positivos sin añadir un motor de reglas condicionales y mantiene la separación de ADR-026:

```text
linaje culinario != evidencia de seguridad != exención regulatoria
```

## Cobertura objetivo de v11

v11 conserva sin modificar las 14 exenciones de v10 y añade seis. El total pasa a 20.

Las nuevas identidades son:

- `ing-gluten-cereals-alcoholic-distillates` -> cereales con gluten;
- `ing-fish-gelatine-vitamin-carotenoid-carrier` -> pescado;
- `ing-fish-gelatine-isinglass-beer-wine-fining` -> pescado;
- `ing-whey-alcoholic-distillates` -> leche;
- `ing-lactitol` -> leche;
- `ing-nuts-alcoholic-distillates` -> frutos de cáscara.

No se crean relaciones de seguridad para estas identidades. La excepción legal permanece en el registro regulatorio independiente.

## Sulfitos

El punto 12 del Anexo II establece declaración a concentraciones superiores a 10 mg/kg o 10 mg/l de SO2 total en el producto listo para consumo o reconstituido. No se trata como una fila de `RegulatoryExemption`: es un umbral regulatorio y permanece modelado en la capa de seguridad/regulación correspondiente.

## Nota sobre productos derivados

La nota asociada a determinadas excepciones del Anexo II extiende el tratamiento a productos derivados únicamente cuando sea improbable que el procesamiento aumente la alergenicidad determinada por la autoridad competente para el producto de origen.

No se implementa propagación automática por linaje. Cualquier derivado adicional necesitará evidencia y revisión explícitas antes de adquirir semántica regulatoria propia.

## Invariantes

1. Una exención de etiquetado nunca significa alimento seguro o apto clínicamente.
2. Las excepciones condicionadas se asocian solo a identidades que incorporan la condición relevante.
3. Los ingredientes genéricos de cereal, pescado, leche y frutos de cáscara no reciben estas exenciones.
4. El linaje no propaga exenciones.
5. La fuente `EU_FIC_1169_2011` se reutiliza sin modificar sus metadatos, conforme a ADR-028.
6. v10 permanece inmutable; la ampliación se publica como catálogo v11.

## Gate previsto

La validación de v11 debe demostrar:

- catálogo válido contra `CatalogValidator`;
- 272 ingredientes, 43 relaciones de linaje y 20 exenciones;
- cero relaciones de seguridad creadas para las seis identidades nuevas;
- cero exenciones nuevas sobre ingredientes genéricos;
- importación v10 -> v11 conservando 14 snapshots históricos de v10 y 20 de v11;
- 34 snapshots regulatorios totales tras la actualización;
- 20 exenciones operativas con v11 activa;
- CI Android completo en verde.
