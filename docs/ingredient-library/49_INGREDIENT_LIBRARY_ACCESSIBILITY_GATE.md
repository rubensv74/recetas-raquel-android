# Gate 49 — Accesibilidad de estados en la biblioteca de ingredientes

Estado: **VALIDADO**  
Fecha: 2026-08-10  
Rama: `program/ingredient-library-food-safety`

## 1. Objetivo

Cerrar el requisito de accesibilidad del diseño funcional para que los estados de seguridad/regulación de la biblioteca no dependan únicamente del texto corrido o del color y puedan ser interpretados por lectores de pantalla.

## 2. Implementación

Cada fila de ingrediente mantiene el texto descriptivo existente y añade una señal visual independiente:

- `ⓘ` para información de seguridad registrada;
- `ⓘ` para información regulatoria específica;
- `⚠` para información potencialmente incompleta.

El símbolo incorpora una descripción semántica específica para TalkBack:

- `Información de seguridad disponible`;
- `Información regulatoria disponible`;
- `Aviso: información posiblemente incompleta`.

La semántica no sustituye el texto visible. Ambos mecanismos coexisten.

## 3. Invariantes preservados

- consultar información continúa siendo una acción distinta de seleccionar el ingrediente;
- una exención regulatoria no se presenta como seguridad ni aptitud;
- ausencia de relación directa de seguridad continúa usando lenguaje neutral y precautorio;
- no se modifica la agregación de seguridad de recetas;
- no se modifica el catálogo;
- no se modifica la persistencia;
- Room permanece en v6.

## 4. Cobertura automática

Las pruebas de UI protegen los tres estados:

1. ingrediente con relación de seguridad registrada;
2. ingrediente con información regulatoria específica;
3. ingrediente sin relación directa de seguridad registrada.

Para cada caso se mantiene la comprobación del texto visible y se añade la comprobación de la descripción semántica. Para el estado de información incompleta se comprueba además la presencia visible de `⚠`.

## 5. Gate técnico

Implementación validada sobre:

- commit: `4cb425ec31a39335a11718f10411f1804ab867f9`;
- workflow: `Android CI`;
- run: **31408112185**;
- run number: **145**.

Resultado:

- `assembleDebug`: PASS;
- unit tests: PASS;
- `lint`: PASS;
- compilación de tests instrumentados: PASS;
- `assembleRelease`: PASS;
- guard Room previo: PASS;
- `connectedDebugAndroidTest`: PASS;
- **76/76 tests instrumentados**;
- `BUILD SUCCESSFUL`;
- guard Room posterior: PASS;
- conjunto de esquemas preservado: `1.json` a `6.json`.

## 6. Cierre

El requisito de la biblioteca queda cubierto mediante **texto + señal visual + semántica accesible**, sin introducir inferencias clínicas, nuevas reglas de seguridad ni cambios de esquema.
