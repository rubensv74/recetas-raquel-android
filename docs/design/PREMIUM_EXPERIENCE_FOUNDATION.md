# Premium Experience Foundation

> La dirección cromática de este documento queda sustituida por
> `docs/brand/RECETORIA_GOLDEN_EDITORIAL_SYSTEM.md`. Se conserva como registro
> histórico de las decisiones de estructura y experiencia.

**Estado:** base de diseño y calidad para implementación incremental  
**Fecha:** 2026-08-11  
**Ámbito:** experiencia completa de Recetoria, con la pantalla de detalle de receta como primer patrón de referencia.

## 1. Objetivo

Recetoria no debe limitarse a una aplicación funcional con una capa visual agradable. El objetivo es que se perciba premium de forma consistente en diseño, interacción, accesibilidad, rendimiento, robustez, seguridad alimentaria y calidad del dato.

La referencia visual aprobada define una dirección editorial cálida y limpia: fotografía protagonista, superficies marfil, acentos cobre/dorado, títulos con carácter editorial, textos de interfaz sobrios, tarjetas discretas, iconografía propia y una jerarquía muy clara entre contenido culinario, acciones y seguridad alimentaria.

La referencia es una guía de lenguaje visual, no una fuente de datos ni un diseño que deba copiarse literalmente.

## 2. Principios obligatorios

1. **Premium no significa decorativo.** Cada elemento visual debe mejorar lectura, comprensión, navegación o confianza.
2. **La receta es la protagonista.** La fotografía y el contenido culinario dominan; la interfaz acompaña.
3. **Seguridad alimentaria visible sin alarmismo.** El usuario debe distinguir grupo, estado, evidencia y recomendación sin que un color o icono sustituya al texto.
4. **La UI nunca inventa seguridad.** Todos los estados deben proceder del dominio (`RecipeSafetySummary` y modelos relacionados). No se hardcodean alérgenos para mejorar una demo o reproducir un mockup.
5. **Categoría no equivale a severidad.** Un color propio de un grupo puede ayudar a identificarlo, pero nunca debe codificar por sí solo riesgo, certeza o gravedad.
6. **Offline de verdad.** El acabado premium no puede introducir dependencia de red para fuentes, iconos, imágenes de interfaz o funciones esenciales.
7. **Accesibilidad por diseño.** No se añade al final como corrección.
8. **Consistencia antes que variedad.** Un conjunto reducido de componentes bien resueltos debe reutilizarse en toda la app.
9. **Movimiento con intención.** Las animaciones deben explicar cambios de estado y dar continuidad, no llamar la atención sobre sí mismas.
10. **Calidad verificable.** Cada incremento visual debe conservar pruebas funcionales y añadir cobertura cuando cambie comportamiento o semántica.

## 3. Auditoría del punto de partida

El proyecto ya dispone de una base técnica adecuada para evolucionar sin reescribir la aplicación:

- Kotlin + Jetpack Compose + Material 3.
- Arquitectura offline con Room y repositorios locales.
- Pantallas separadas por contexto (`home`, `detail`, `editor`, `cooking`, `ingredientlibrary`, etc.).
- La pantalla `RecipeDetailScreen` ya concentra fotografía, metadatos, acción de cocinar, seguridad alimentaria, ingredientes, pasos y notas.
- El modelo de seguridad ya distingue estados de presentación, tipo de relación, evidencia, avisos de revisión y exenciones regulatorias.

La capa visual actual es deliberadamente básica. `Color.kt` define únicamente una paleta mínima y `Theme.kt` utiliza tipografía y shapes estándar de Material 3. Esto permite introducir un design system propio sin tocar persistencia ni reglas de negocio.

## 4. Lenguaje visual aprobado

### 4.1 Carácter

La aplicación debe sentirse como un cuaderno culinario editorial contemporáneo: cálido, doméstico, preciso y sofisticado. Debe evitar tanto el aspecto clínico de una app sanitaria como el aspecto genérico de una plantilla Material sin personalización.

### 4.2 Paleta base candidata

Los valores siguientes son tokens iniciales y deberán validarse en dispositivo y con contraste antes de congelarse:

| Token | Valor inicial | Uso |
|---|---:|---|
| `CanvasIvory` | `#F7F3EC` | Fondo principal claro |
| `SurfaceWarm` | `#FFFDF9` | Tarjetas y superficies elevadas |
| `InkPrimary` | `#292621` | Texto principal |
| `InkSecondary` | `#6D675F` | Texto secundario |
| `CopperPrimary` | `#A96F29` | CTA, selección y acento de marca |
| `CopperPressed` | `#83531D` | Estado presionado / énfasis fuerte |
| `BorderSoft` | `#E5DDD2` | Divisores y contornos suaves |
| `SafetyWarning` | `#A95F2B` | Advertencia semántica |
| `SafetyInfo` | `#4F7888` | Información / estado informativo |

Debe existir una paleta oscura equivalente, diseñada deliberadamente. No se invertirán colores de forma automática ni se confiará únicamente en defaults de Material 3.

### 4.3 Tipografía

Se utilizará una combinación editorial:

- **Display / títulos de receta:** serif con personalidad y excelente legibilidad.
- **UI / cuerpo / metadatos:** sans serif neutra y muy legible.
- **Números y tiempos:** jerarquía estable para lectura rápida.

Si se incorporan fuentes externas, deberán empaquetarse localmente, incluir licencia compatible en el repositorio y no requerir descarga en ejecución. La selección exacta se congelará tras probar al menos títulos largos, caracteres españoles, cifras y escalado de fuente.

### 4.4 Forma, elevación y densidad

- Radios coherentes y contenidos: pequeños para chips, medios para tarjetas, mayores para hero/CTA cuando corresponda.
- Sombras suaves; nunca usar elevación fuerte para compensar una jerarquía deficiente.
- Divisores tenues y suficientes espacios negativos.
- Altura táctil mínima de 48 dp en acciones interactivas.
- Densidad compacta en metadatos, más generosa en lectura de pasos y seguridad.

### 4.5 Fotografía

La fotografía de portada es un elemento de producto, no un adorno:

- Hero ancho y estable, con `ContentScale.Crop` controlado.
- Evitar saltos de layout durante carga.
- Placeholder coherente con la marca cuando no exista foto.
- Mantener el pipeline local y las garantías actuales de orientación y almacenamiento.
- No aplicar filtros visuales que alteren de forma engañosa el aspecto del alimento.

## 5. Componentes premium a construir

La pantalla de detalle será el banco de pruebas del sistema. Los componentes se extraerán únicamente cuando exista uso real; no se crearán capas vacías.

### Núcleo de detalle

- `RecipeHero`: fotografía, tratamiento de ausencia y transición visual.
- `RecipeTopBar`: volver, editar y favorito con iconos coherentes y semántica accesible.
- `RecipeEditorialHeader`: categoría, título, descripción y jerarquía tipográfica.
- `RecipeMetaStrip`: raciones, preparación, cocción y total en una sola lectura visual.
- `PrimaryCookAction`: CTA principal ancho, claro y con estados pressed/disabled.
- `AllergenChipRow`: resumen compacto de grupos presentes o declarados, con pictograma, nombre y semántica.
- `FoodSafetyCard`: detalle de seguridad con jerarquía por grupo, estado, evidencia y revisión.
- `IngredientRow`: cantidad, unidad, ingrediente y notas con alineación consistente.
- `RecipeStepCard`: número, instrucción, temporizador y foto del paso.
- `SectionHeader`: patrón común para secciones largas.

### Estados de producto

También deben diseñarse como parte del sistema: loading, contenido vacío, error recuperable, dato no disponible, acción deshabilitada y contenido que requiere revisión.

## 6. Iconografía de alérgenos y seguridad

Los 14 grupos del Anexo II deben contar con pictograma propio y reconocible. El pictograma identifica el grupo; el texto identifica el significado. Nunca se dependerá exclusivamente del color.

El trabajo ya iniciado en `SafetyGroupPictogram` debe convertirse en el componente base. En la evolución premium se podrán definir fondos o acentos categóricos por grupo, pero deberán cumplir estas reglas:

- el mismo grupo conserva siempre el mismo lenguaje visual;
- el color de grupo no representa gravedad;
- los estados `PRESENCIA_IDENTIFICADA`, `PUEDE_CONTENER_DECLARADO`, `REQUIERE_REVISION`, etc. se comunican de forma separada;
- la exención regulatoria sigue siendo un concepto distinto de «seguro» y no elimina la trazabilidad de la relación original;
- todos los pictogramas deben tener `contentDescription` útil y acompañamiento textual visible.

## 7. Regla crítica de integridad del dato

La referencia visual utilizada para fijar la estética contiene un ejemplo ilustrativo en el que la descripción de una Tarta de Santiago indica que no lleva harina de trigo, mientras el panel muestra gluten por harina de trigo. Esa incoherencia **no debe reproducirse**.

Un acabado premium pierde todo su valor si la información de seguridad es contradictoria. Por tanto:

- la UI no añadirá grupos por inferencia visual, categoría de receta o texto de demostración;
- el resumen compacto y el panel detallado deberán derivar del mismo `RecipeSafetySummary`;
- una afirmación debe conservar la procedencia/evidencia que la sustenta;
- cualquier dato incompleto debe expresarse como incompleto o «requiere revisión», nunca elevarse artificialmente a presencia confirmada;
- las exenciones regulatorias deben mostrarse separadas de la evidencia culinaria y del estado de seguridad.

## 8. Accesibilidad premium

Objetivo: que la interfaz siga siendo de alta calidad con TalkBack, escalado de fuente y usuarios con baja discriminación cromática.

- Objetivos táctiles de al menos 48 dp.
- Contraste validado para texto, iconos funcionales y estados.
- Ninguna información crítica transmitida solo por color.
- Orden semántico coherente con el orden visual.
- Descripciones accesibles para iconos de acción y grupos de seguridad.
- Soportar escalado de texto sin truncar títulos o información de riesgo.
- Evitar tamaños fijos que rompan el layout con texto grande.
- Mantener etiquetas de test/semántica existentes cuando su contrato siga vigente.

## 9. Interacción y movimiento

La app debe sentirse rápida y física sin exceso de animación:

- feedback inmediato en favoritos, selección y CTA;
- transiciones cortas y discretas entre estados;
- expansión/colapso solo cuando reduzca carga cognitiva;
- scroll estable y sin desplazamientos inesperados;
- haptics únicamente en acciones donde aporten confirmación real;
- respetar preferencias del sistema relacionadas con reducción de movimiento cuando aplique.

No se añadirá una librería de animación salvo necesidad demostrada.

## 10. Rendimiento y robustez

El estándar premium incluye ausencia de fricción técnica:

- evitar trabajo pesado dentro de composables;
- mantener listas con keys estables;
- evitar recalcular ordenaciones por elemento durante composición;
- dimensionar correctamente imágenes locales;
- minimizar recomposiciones evitables;
- no introducir red ni telemetría como requisito para la experiencia;
- preservar funcionamiento completo sin conexión;
- mantener errores de carga y datos corruptos como estados explícitos y recuperables.

Como mejora concreta, la numeración de pasos del detalle no debería buscar repetidamente el índice dentro de una lista ordenada para cada elemento; el rediseño es una oportunidad para preparar una lista ordenada/indexada una sola vez.

## 11. Estrategia de implementación incremental

### Fase A — Design system

Crear tokens de color, tipografía, shapes, spacing y elevación; completar el tema claro/oscuro; introducir los primeros componentes compartidos sin modificar reglas de negocio.

### Fase B — Recipe Detail como pantalla patrón

Reconstruir la jerarquía visual del detalle sobre los datos existentes: hero, header editorial, metadatos, CTA, resumen de alérgenos, tarjeta de seguridad, ingredientes y pasos. Mantener navegación, test tags y contratos del ViewModel salvo necesidad justificada.

### Fase C — Propagación del lenguaje visual

Aplicar el sistema ya probado a Home, editor, biblioteca de ingredientes, modo cocina y ajustes. No rediseñar todas las pantallas en paralelo.

### Fase D — Microinteracciones y estados

Cerrar animaciones, feedback, empty/loading/error states, accesibilidad avanzada y coherencia entre tamaños de pantalla.

### Fase E — Quality gate premium

Validar compilación, tests unitarios, lint, tests instrumentados relevantes, accesibilidad, modo oscuro, escalado de fuente, orientación/tamaños soportados y revisión visual en dispositivo real.

## 12. Estrategia de pruebas

Cada cambio deberá mantener el protocolo local-first del repositorio.

- Unit tests para lógica/presentación derivada cuando exista.
- Compose UI tests para estructura, semántica y acciones críticas.
- Tests de regresión de los estados de seguridad y sus etiquetas.
- Pruebas instrumentadas para detalle, navegación y componentes dependientes de Android.
- Validación manual visual con una matriz pequeña pero fija: contenido completo, receta sin foto, receta sin alérgenos detectados, receta con varios grupos, `may contain`, `requires review`, título largo y fuente ampliada.

No se añadirá CI costoso por el mero rediseño. El gate remoto seguirá reservado al PR o a ejecución manual cuando aporte valor.

## 13. Criterios de aceptación de una pantalla premium

Una pantalla solo se considera cerrada cuando cumple simultáneamente:

- identidad visual coherente con el sistema;
- jerarquía comprensible en pocos segundos;
- acciones principales inequívocas;
- información de seguridad consistente y trazable;
- accesibilidad funcional sin depender del color;
- estados loading/empty/error resueltos;
- funcionamiento offline preservado;
- rendimiento fluido en hardware objetivo;
- pruebas relevantes en verde;
- sin lógica de negocio duplicada dentro de la UI.

## 14. Integración con el trabajo de pictogramas

A fecha de esta definición existe una rama activa `agent/allergen-group-icons` que modifica `RecipeDetailScreen` y añade `SafetyGroupPictogram`. La implementación visual del detalle no debe arrancar desde una versión anterior y luego sobrescribir ese trabajo.

Orden recomendado:

1. cerrar y validar la rama de pictogramas;
2. integrarla en la línea base;
3. crear el incremento de design system sobre esa base actualizada;
4. transformar `RecipeDetailScreen` de forma incremental;
5. propagar los componentes aprobados al resto de la aplicación.

Esto reduce conflictos y evita rehacer la misma zona de UI dos veces.

---

Esta especificación es el quality bar de producto. La imagen de referencia fija el carácter visual; el código, el modelo de seguridad y los gates anteriores fijan el nivel de confianza que debe acompañarlo.
