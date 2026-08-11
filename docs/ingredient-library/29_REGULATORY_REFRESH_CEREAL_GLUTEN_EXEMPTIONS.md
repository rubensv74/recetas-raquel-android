# 29 — REGULATORY REFRESH: EXENCIONES DE CEREALES CON GLUTEN

**Estado:** REVISIÓN REGULATORIA CERRADA — preparada para catálogo v10  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha de revisión:** 2026-08-09  
**Jurisdicción objetivo:** `EU-ES`

## 1. Objetivo

Revisar las excepciones del punto 1 del Anexo II del Reglamento (UE) 1169/2011 relativas a cereales que contienen gluten y determinar qué identidades técnicas pueden incorporarse al catálogo sin convertir una exención de etiquetado en una afirmación clínica de seguridad.

## 2. Fuente normativa principal

Reglamento (UE) 1169/2011, texto consolidado vigente consultado con fecha de versión 2025-04-01, Anexo II, punto 1.

Referencias oficiales:

- ELI consolidado EN: `https://eur-lex.europa.eu/eli/reg/2011/1169/2025-04-01/eng`
- ELI consolidado ES: `https://eur-lex.europa.eu/eli/reg/2011/1169/2025-04-01/spa`
- Comunicación de la Comisión 2017/C 428/01 sobre información de sustancias o productos que causan alergias o intolerancias: `https://eur-lex.europa.eu/legal-content/EN/AUTO/?toc=OJ%3AC%3A2017%3A428%3AFULL&uri=uriserv%3AOJ.C_.2017.428.01.0001.01.ENG`

## 3. Resultado normativo

El Anexo II punto 1 enumera como cereales que contienen gluten al trigo —incluidas referencias como espelta y trigo khorasan—, centeno, cebada, avena y sus variedades híbridas, y establece excepciones específicas para:

```text
(a) jarabes de glucosa a base de trigo, incluida la dextrosa;
(b) maltodextrinas a base de trigo;
(c) jarabes de glucosa a base de cebada;
(d) cereales utilizados para hacer destilados alcohólicos,
    incluido el alcohol etílico de origen agrícola.
```

Para los puntos (a) y (b), la nota aplicable del Reglamento extiende el tratamiento a productos derivados únicamente cuando el proceso posterior no sea susceptible de aumentar el nivel de alergenicidad evaluado por la Autoridad para el producto de origen.

## 4. Alcance seleccionado para v10

V10 incorporará únicamente identidades técnicas que pueden representarse sin inferencias sobre recetas, fabricantes o procesos posteriores:

```text
ing-wheat-glucose-syrup       Jarabe de glucosa a base de trigo
ing-wheat-dextrose            Dextrosa a base de trigo
ing-wheat-maltodextrin        Maltodextrina a base de trigo
ing-barley-glucose-syrup      Jarabe de glucosa a base de cebada
```

No se incorporará todavía el punto 1(d) sobre cereales utilizados para destilados alcohólicos.

La razón es de modelado conservador: esa excepción describe un uso/proceso de una familia de materias primas y no una única identidad técnica inequívoca. Incorporarla como un ingrediente genérico podría inducir una aplicación demasiado amplia de la exención. Su modelado se revisará por separado cuando exista una identidad concreta que pueda representarse sin inferencia automática.

Esto no requiere por ahora una nueva decisión arquitectónica: simplemente se mantiene fuera del catálogo hasta disponer de una representación suficientemente precisa.

## 5. Linaje previsto

Las relaciones no clínicas serán:

```text
ing-wheat-glucose-syrup   DERIVED_FROM ing-wheat
ing-wheat-dextrose        DERIVED_FROM ing-wheat
ing-wheat-maltodextrin    DERIVED_FROM ing-wheat
ing-barley-glucose-syrup  DERIVED_FROM ing-barley
```

Todas usarán:

```text
sourceReference = EU_FIC_1169_2011
reviewedAt       = 2026-08-09
```

El grafo de linaje no propagará relaciones de seguridad.

## 6. Exenciones regulatorias previstas

Cada identidad tendrá un registro `RegulatoryExemption` con:

```text
safetyGroupId     sg-eu-cereals-gluten
jurisdiction      EU-ES
regulatoryEffect  EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION
sourceId          EU_FIC_1169_2011
effectiveFrom     null
effectiveTo       null
reviewedAt        2026-08-09
isActive          true
```

No se inventan fechas de entrada en vigor específicas para estas excepciones dentro del modelo de catálogo.

## 7. Condición de la nota legal

Para las identidades de trigo incluidas en los puntos (a) y (b), el campo `conditions` debe conservar expresamente que la excepción de productos derivados no puede extenderse si un proceso posterior pudiera aumentar la alergenicidad evaluada para el producto de origen.

El catálogo no dispone de un motor que determine automáticamente si un proceso industrial concreto satisface esa condición. Por ello:

- la identidad técnica listada puede registrar la exención regulatoria;
- ningún producto derivado adicional hereda automáticamente esa exención;
- no se genera una relación `IngredientSafetyRelation` a partir de la exención;
- no se formula una conclusión clínica a partir de la exención.

## 8. Separación clínica obligatoria

Para las cuatro identidades previstas:

```text
IngredientSafetyRelation = 0
RegulatoryExemption       = 1
```

La existencia de una exención de declaración obligatoria no permite mostrar mensajes como:

```text
sin gluten
seguro para celíacos
sin riesgo
apto para alérgicos
puede consumirlo
```

La aplicación debe continuar diferenciando legislación de etiquetado, enfermedad celíaca, alergia al trigo y otras condiciones médicas.

## 9. Identidades genéricas que no reciben la exención

No deben recibir esta exención por similitud nominal:

```text
jarabe de glucosa genérico
dextrosa genérica
maltodextrina genérica
cualquier producto que solo indique "cereal"
productos derivados posteriores sin trazabilidad suficiente
```

La procedencia `a base de trigo` o `a base de cebada` forma parte de la identidad técnica regulatoria.

## 10. Criterio para v10

V10 podrá prepararse cuando v9 haya superado su gate post-activación.

El delta previsto es:

```text
+4 ingredientes técnicos
+4 relaciones de linaje
+4 exenciones regulatorias
+0 relaciones de seguridad
+0 cambios Room
```

Debe existir una prueba instrumentada específica que demuestre que las cuatro identidades mantienen la separación entre exención regulatoria y relación de seguridad.
