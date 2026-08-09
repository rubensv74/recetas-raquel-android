# 27 — REGULATORY REFRESH: TOCOFEROLES, FITOSTEROLES Y FITOSTANOLES DE SOJA

**Estado:** REVISADO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha de revisión:** 2026-08-09

## 1. Objetivo

Revisar las excepciones restantes del punto 6 del Anexo II del Reglamento (UE) 1169/2011 antes de introducirlas en el catálogo estructurado.

La revisión se limita a la obligación de declaración regulatoria. No evalúa seguridad clínica individual ni convierte una excepción legal en una afirmación de ausencia de alérgeno.

## 2. Fuente normativa primaria

Fuente oficial revisada:

```text
Reglamento (UE) 1169/2011
versión consolidada aplicable desde 2025-04-01
EUR-Lex / CELEX 02011R1169-20250401
https://eur-lex.europa.eu/eli/reg/2011/1169/2025-04-01/spa
```

Fuente interna ya registrada:

```text
sourceId = EU_FIC_1169_2011
```

No se crea una fuente nueva porque se trata del mismo instrumento jurídico ya incorporado al registro de fuentes.

## 3. Excepciones de soja confirmadas

El punto 6 del Anexo II excluye de la obligación general de declaración de soja, además del aceite y grasa totalmente refinados ya incorporados en v8, las siguientes categorías:

### 6(b) Tocoferoles de origen soja

```text
Tocoferoles naturales mezclados (E306)
D-alfa tocoferol natural
Acetato de D-alfa tocoferol natural
Succinato de D-alfa tocoferol natural
```

La excepción exige que sean derivados de soja. La identidad genérica de un tocoferol sin ese origen no debe recibir esta exención.

### 6(c) Fitosteroles de origen soja

```text
Fitosteroles derivados de aceites vegetales de soja
Ésteres de fitosterol derivados de aceites vegetales de soja
```

La identidad regulatoria conserva expresamente el origen soja.

### 6(d) Éster de fitostanol de origen soja

```text
Éster de fitostanol derivado de fitosteroles de aceite de soja
```

El linaje regulatorio puede representarse como derivado del registro de fitosteroles de soja, manteniendo separadas las relaciones de seguridad.

## 4. Decisiones de modelado

Se aplican las reglas ya aprobadas por ADR-025 y ADR-026:

```text
linaje culinario/regulatorio != relación de seguridad
exención regulatoria         != ausencia de alérgeno
exención regulatoria         != aptitud clínica
```

Cada identidad tendrá:

```text
verificationStatus     VERIFIED
compositionVariability VARIABLE_BY_PREPARATION
sourceUpdatedAt         null
```

`sourceUpdatedAt` permanece nulo porque la fecha de revisión documental no debe convertirse en una marca temporal de actualización del ingrediente.

## 5. Fechas de vigencia

Para este lote se mantienen:

```text
effectiveFrom = null
effectiveTo   = null
```

La revisión confirma que las excepciones figuran en el Anexo II consolidado vigente, pero no introduce una fecha material nueva que debamos atribuir al registro actual. No se inventa una fecha a partir de la fecha de consolidación.

## 6. Separación de seguridad

No se crearán relaciones `IngredientSafetyRelation` para estas identidades.

El ingrediente padre:

```text
ing-soybean
```

mantiene su relación existente con:

```text
sg-eu-soybeans
```

La ausencia de una relación de seguridad en los derivados exentos no debe mostrarse al usuario como `sin soja`, `sin riesgo`, `apto para alérgicos` ni formulaciones equivalentes.

## 7. Alcance del lote siguiente

La siguiente versión de catálogo podrá incorporar siete identidades técnicas:

```text
ing-soy-natural-mixed-tocopherols-e306
ing-soy-natural-d-alpha-tocopherol
ing-soy-natural-d-alpha-tocopherol-acetate
ing-soy-natural-d-alpha-tocopherol-succinate
ing-soy-phytosterols
ing-soy-phytosterol-esters
ing-soy-plant-stanol-ester
```

con siete relaciones de linaje y siete exenciones regulatorias independientes.

## 8. Resultado

**Gate documental superado.**

La fuente primaria soporta la creación controlada del lote. No se ha identificado una decisión arquitectónica nueva ni una razón para modificar ADR-026.
