# 44 — RECIPE DETAIL: INFORMACIÓN REGULATORIA DE ETIQUETADO — GATE

**Estado:** VALIDADO  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-10  
**Catálogo activo:** v11  
**Room:** v5  
**Relacionados:** ADR-026 / ADR-028

## Objetivo

Presentar en la receta final las exenciones regulatorias aplicables sin mezclarlas con las alertas de seguridad alimentaria ni traducir una excepción legal de etiquetado en una afirmación clínica.

## Separación visual y semántica

`RecipeDetail` mantiene dos canales visibles independientes:

```text
Información sobre seguridad alimentaria
!=
Información regulatoria de etiquetado
```

La tarjeta regulatoria solo se muestra cuando `RecipeSafetySummary.regulatoryExemptions` contiene al menos una exención aplicable para el snapshot de catálogo, jurisdicción y fecha evaluados por el resolver.

La tarjeta de seguridad no se elimina, rebaja ni modifica como consecuencia de una exención regulatoria.

## Información mostrada

Para cada exención aplicable se presenta:

- nombre del ingrediente dentro de la receta;
- efecto regulatorio explicado en lenguaje legible;
- condiciones exactas de aplicación;
- ámbito territorial;
- fuente jurídica legible;
- fechas de vigencia cuando existan;
- fecha de revisión;
- notas regulatorias cuando existan.

Los códigos internos de las fuentes conocidas se traducen para presentación. Por ejemplo:

```text
EU_FIC_1169_2011
-> Unión Europea · Reglamento (UE) n.º 1169/2011 · Anexo II
```

## Lenguaje de seguridad obligatorio

La tarjeta incluye expresamente que la información se refiere a obligaciones de etiquetado y que una excepción:

```text
NO significa ausencia del alérgeno
NO significa ausencia de riesgo
NO significa aptitud para una persona alérgica o intolerante
```

También recuerda comprobar el etiquetado actual del producto y seguir las indicaciones sanitarias aplicables a la persona cuando exista alergia o intolerancia.

## Implementación

Archivos principales:

```text
app/src/main/java/com/rmm/recetasraquel/ui/detail/RecipeRegulatoryPanel.kt
app/src/main/java/com/rmm/recetasraquel/ui/detail/RecipeDetailScreen.kt
app/src/androidTest/java/com/rmm/recetasraquel/ui/detail/RecipeSafetyPanelUiTest.kt
```

No se modifica:

- el esquema Room;
- la lógica del resolver regulatorio;
- el cálculo de grupos de seguridad;
- el significado de ADR-026;
- el histórico regulatorio de ADR-028.

## Caso de prueba específico

La prueba instrumentada crea una receta con una exención regulatoria de aceite de soja totalmente refinado y sin relaciones clínicas registradas para ese caso.

Verifica que:

1. existe una tarjeta `recipe_regulatory_panel` separada;
2. aparece el nombre del ingrediente dentro de esa tarjeta;
3. se muestran las condiciones regulatorias;
4. se presenta la fuente oficial con un nombre legible;
5. se muestra el aviso explícito que impide interpretar la exención como ausencia de riesgo o aptitud clínica.

## Incidencias detectadas durante el gate

La primera ejecución de la prueba encontró dos nodos con el texto del ingrediente. Era el comportamiento correcto: el nombre aparecía tanto en la tarjeta regulatoria como en la lista normal de ingredientes.

La prueba fue corregida para exigir que el nombre se encuentre específicamente dentro de `recipe_regulatory_panel`. No fue necesario modificar el producto.

Una ejecución posterior detectó un import incorrecto de `onNode` en el test Compose. Se eliminó ese import y se mantuvo `composeRule.onNode(...)`, que es el contrato correcto en la versión usada por el proyecto.

Estas incidencias afectaron exclusivamente al código de prueba.

## Gate automatizado definitivo

Ejecución de cierre:

```text
Workflow: Android CI
Run:      31383568673
Code SHA: 774d47321d4eb18ac3285eef6c70b061d02c3e55
```

Resultado:

```text
assembleDebug                  PASS
unit tests                     PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
assembleRelease                PASS
Room schema guard              PASS
connectedDebugAndroidTest      PASS — 65/65
Room guard post-emulador       PASS
```

La ejecución instrumentada registró:

```text
Starting 65 tests on emulator-5554 - 16
Finished 65 tests on emulator-5554 - 16
BUILD SUCCESSFUL
```

El guard posterior confirmó que el contrato Room permanece exactamente en:

```text
1.json
2.json
3.json
4.json
5.json
```

## Cierre

La receta final ya distingue de forma explícita y comprobada entre información de seguridad alimentaria e información regulatoria de etiquetado.

El catálogo v11 y Room v5 permanecen alineados con ADR-026 y ADR-028, y la UI no convierte una exención legal en una afirmación de seguridad clínica.
