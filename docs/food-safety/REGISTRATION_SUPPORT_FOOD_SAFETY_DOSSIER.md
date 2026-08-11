# DOSSIER TÉCNICO DE SEGURIDAD ALIMENTARIA PARA APOYO AL REGISTRO

**Estado:** BASE DOCUMENTAL — requiere revisión regulatoria fresca antes de cualquier presentación formal  
**Ámbito principal:** España / Unión Europea  
**Fecha de consolidación:** 2026-08-08

## 1. Propósito

Este documento reúne las principales decisiones técnicas y regulatorias adoptadas para la biblioteca de ingredientes y el sistema de alertas de seguridad alimentaria de Recetas de Raquel.

Su finalidad es servir como **evidencia técnica de apoyo** para futuros procesos de registro, publicación, auditoría o revisión de la aplicación. No sustituye una evaluación jurídica, sanitaria o regulatoria específica ni afirma por sí solo que la aplicación esté sujeta a un régimen concreto de registro.

Antes de utilizar este dossier fuera del proyecto debe hacerse una revisión actualizada de la legislación, guías oficiales y requisitos aplicables en la fecha de presentación.

## 2. Fuentes de referencia adoptadas

El proyecto mantiene un registro trazable de fuentes bajo `SOURCE_REGISTER.md`.

La jerarquía de referencia distingue entre:

- legislación de la Unión Europea aplicable al ámbito ES/EU;
- normativa española y documentación institucional de AESAN;
- evaluación científica oficial de EFSA;
- referencias FAO/WHO y Codex;
- nomenclatura WHO/IUIS;
- etiquetas de fabricantes y declaraciones de usuario como clases de evidencia diferentes.

Entre las referencias actualmente registradas se encuentran:

- Reglamento (UE) 1169/2011 y su Anexo II;
- Comunicación de la Comisión 2017/C 428/01;
- Reglamento Delegado (UE) 2024/2512;
- Reglamento de Ejecución (UE) 828/2014;
- Reglamento (UE) 2021/382;
- Real Decreto 126/2015;
- AESAN — alergias e intolerancias alimentarias;
- AESAN PNCOCA — alérgenos y sustancias que causan intolerancias;
- EFSA — documentación científica y procedimientos de evaluación de exenciones;
- FAO/WHO — evaluaciones de riesgo de alérgenos alimentarios;
- Codex CXC 80-2020 y trabajo PAL;
- WHO/IUIS Allergen Nomenclature;
- NIDDK como referencia complementaria para diferenciación de celiaquía, sensibilidad y alergia al trigo.

## 3. Decisión sobre los 14 grupos regulados en la UE

El proyecto modela los **14 grupos del Anexo II del Reglamento (UE) 1169/2011** como grupos regulatorios explícitos. Esta lista no se interpreta como una lista exhaustiva de todas las alergias alimentarias posibles.

Los grupos son:

1. cereales que contienen gluten;
2. crustáceos;
3. huevos;
4. pescado;
5. cacahuetes;
6. soja;
7. leche;
8. frutos de cáscara;
9. apio;
10. mostaza;
11. sésamo;
12. dióxido de azufre y sulfitos;
13. altramuces;
14. moluscos.

Decisiones de modelado relevantes:

- `cereales que contienen gluten` no se reduce a un único booleano `gluten`;
- se conservan las especies reguladas de cereales y frutos de cáscara;
- los sulfitos requieren contexto de concentración cuando sea aplicable;
- las exenciones regulatorias deben ser datos explícitos, versionados y respaldados por fuente;
- derivados y componentes de ingredientes compuestos no se eliminan del análisis por simplificación del nombre comercial;
- la lista de 14 grupos no impide registrar otras relaciones alimentarias cuando exista evidencia suficiente.

## 4. Papel de AESAN en las decisiones del proyecto

AESAN se utiliza como autoridad sanitaria española de referencia para:

- diferenciar alergia alimentaria e intolerancia alimentaria;
- complementar la interpretación española del marco de alérgenos e intolerancias;
- documentar casos reales en los que la identidad nominal de un alimento no permite concluir ausencia de riesgo;
- documentar escenarios de composición desconocida o alérgenos no declarados.

El proyecto ha utilizado ejemplos institucionales de alertas AESAN para reforzar dos principios:

1. una composición desconocida debe producir un estado `UNKNOWN / REQUIRES_REVIEW`, no una conclusión de seguridad;
2. la identidad canónica de un ingrediente no demuestra por sí sola ausencia de contacto cruzado, contaminación o declaración incorrecta.

Las alertas concretas no se convierten automáticamente en reglas universales de producto.

## 5. Separación entre condiciones médicas y preferencias

El modelo diferencia explícitamente:

```text
FOOD_ALLERGY
FOOD_INTOLERANCE
CELIAC_DISEASE
NON_CELIAC_SENSITIVITY
OTHER_MEDICALLY_INDICATED_RESTRICTION
```

No se mezclan en esta taxonomía las preferencias personales, vegetarianismo/veganismo, restricciones religiosas u otras elecciones dietéticas no médicas.

La alergia a la leche no se equipara a intolerancia a la lactosa. La celiaquía tampoco se equipara a alergia al trigo ni a sensibilidad no celíaca.

## 6. Modelo de evidencia

Toda relación de seguridad debe tener, como mínimo:

```text
sourceId
evidenceLevel
relationType
reviewedAt
```

Niveles de evidencia actualmente definidos:

```text
EU_LEGAL
OFFICIAL_SCIENTIFIC
OFFICIAL_HEALTH_AUTHORITY
MANUFACTURER_LABEL
USER_DECLARED
UNVERIFIED
```

Tipos de relación de seguridad:

```text
INHERENT_SOURCE
CONTAINS
DERIVED_FROM
REGULATED_COMPONENT
DECLARED_MAY_CONTAIN
POSSIBLE_CROSS_REACTIVITY
UNKNOWN
```

Una relación sin evidencia suficiente debe permanecer `UNVERIFIED` y no puede utilizarse para generar afirmaciones absolutas de seguridad.

## 7. Inferencias prohibidas

La aplicación no debe crear relaciones clínicas automáticamente a partir de:

- coincidencias aproximadas de texto;
- similitud botánica o zoológica;
- nombres comerciales parecidos;
- uso culinario habitual;
- parentesco taxonómico;
- conocimiento no documentado del modelo de IA.

La reactividad cruzada documentada tampoco equivale a presencia confirmada de un alérgeno.

## 8. Linaje culinario y seguridad son grafos diferentes

El proyecto ha adoptado un grafo de linaje culinario no clínico para representar relaciones como cortes, formas, variantes y derivados.

Ejemplo:

```text
Harina de trigo DERIVED_FROM Trigo
```

Esta relación **no crea ni hereda automáticamente** una relación de seguridad. La relación alimentaria correspondiente debe existir por separado y disponer de su propia fuente y evidencia.

Esta separación reduce el riesgo de que una relación de identidad culinaria sea interpretada incorrectamente como una conclusión clínica.

## 9. Lenguaje de seguridad adoptado

La aplicación evita expresiones absolutas como:

```text
Receta segura
Apta para alérgicos
Libre de alérgenos
Sin riesgo
Puede consumirla
100 % segura
```

El lenguaje preferido expresa únicamente lo que la evidencia permite afirmar:

```text
Contiene…
Derivado de…
El fabricante declara "puede contener"…
Posible reactividad cruzada documentada…
Requiere revisión
La composición puede variar
La información disponible puede ser incompleta
```

La ausencia de una relación registrada nunca se interpreta como prueba de ausencia de riesgo.

## 10. Control de cambios y reproducibilidad

El catálogo de ingredientes y seguridad se versiona independientemente del esquema Room.

Cada versión publicada del catálogo debe ser inmutable y conservar:

- manifiesto de versión;
- fuentes utilizadas;
- fechas de revisión;
- recuentos de ingredientes, alias, grupos y relaciones;
- cambios respecto a la versión anterior;
- validación estructural;
- evidencias de pruebas;
- historial Git.

La importación se realiza de forma transaccional para evitar estados parciales.

## 11. Evidencias que deberían acompañar una futura revisión o registro

Para cada versión relevante de la aplicación debería poder prepararse un paquete documental con:

1. versión de la aplicación;
2. versión de Room y esquemas exportados;
3. versión del catálogo de ingredientes;
4. manifiesto del catálogo;
5. `SOURCE_REGISTER.md` actualizado;
6. matriz de cobertura de ingredientes y grupos;
7. registro de decisiones regulatorias y arquitectónicas;
8. cambios de relaciones de seguridad desde la versión anterior;
9. lista de registros `UNVERIFIED` o `REVIEW_REQUIRED`;
10. informe del validador del catálogo;
11. resultados de pruebas unitarias e instrumentadas;
12. limitaciones conocidas y huecos de investigación;
13. fecha y responsable de la revisión del contenido sensible.

## 12. Limitaciones que deben declararse

La documentación de soporte debe dejar explícito que:

- los 14 grupos UE no son todas las alergias posibles;
- la información científica cambia con el tiempo;
- productos comerciales pueden cambiar formulación;
- las etiquetas de fabricante son específicas de producto y fecha;
- contacto cruzado y PAL requieren contexto adicional;
- la aplicación no sustituye consejo médico;
- datos incompletos o no verificados deben presentarse como tales;
- ninguna versión histórica debe considerarse indefinidamente actual sin un proceso de revisión.

## 13. Documentos relacionados

```text
docs/food-safety/SOURCE_REGISTER.md
docs/food-safety/EU_REGULATED_ALLERGENS.md
docs/food-safety/FOOD_SAFETY_RESEARCH.md
docs/food-safety/DATA_QUALITY_POLICY.md
docs/food-safety/CROSS_CONTACT_AND_PAL.md
docs/food-safety/RESEARCH_GAPS.md
docs/food-safety/INGREDIENT_COVERAGE_MATRIX.md
docs/food-safety/CATALOG_MAINTENANCE_GUIDE.md
docs/food-safety/SAFETY_KNOWLEDGE_MAINTENANCE_REQUIREMENTS.md
```

## 14. Gate previo a uso externo

Antes de utilizar este dossier para un registro, publicación formal, auditoría o revisión externa se debe ejecutar un **regulatory refresh** con fuentes oficiales actuales y registrar las diferencias respecto a esta versión base.
