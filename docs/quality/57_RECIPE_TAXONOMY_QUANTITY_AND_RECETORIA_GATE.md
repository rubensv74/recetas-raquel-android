# Gate 57 — Taxonomía de recetas, cantidades obligatorias y nombre Recetoria

**Estado:** SUPERADO
**Fecha:** 2026-08-15
**Rama de origen:** `program/premium-experience-foundation`

## Objetivo

Cerrar tres incoherencias funcionales detectadas tras el Gate 56:

1. ampliar la clasificación de recetas habituales sin modificar las categorías ya guardadas;
2. impedir el guardado cuando cualquier fila de ingrediente carece de cantidad;
3. adoptar `Recetoria` como nombre visible de la aplicación, manteniendo el logo actual de forma provisional.

## Decisiones

- Se conserva la taxonomía existente para no dejar recetas históricas fuera de los filtros.
- Se añaden familias que cubren huecos claros: guisos, patatas, bocadillos y wraps, pizzas y tartas saladas, y helados.
- El selector explica que la lista se puede desplazar, porque `Postres` ya existía pero quedaba fuera del área inicialmente visible.
- La cantidad continúa siendo texto libre porque debe admitir valores culinarios como `1/2`, `una pizca` o `al gusto`.
- Una fila de ingrediente añadida no se descarta silenciosamente: nombre y cantidad deben completarse o la fila debe eliminarse.
- La validación se aplica en el editor y en la normalización de `RecipeDraft`, evitando que otra entrada de guardado eluda la regla.
- El namespace, application ID, clases y esquema de Room no se renombran: son identificadores técnicos y cambiarlos no aporta valor al nombre mostrado.
- El diseño del logo queda fuera de este gate.

## Evidencia técnica

```powershell
.\gradlew -g "C:\Temp\gradle_home_gate57" assembleDebug testDebugUnitTest lintDebug compileDebugAndroidTestKotlin connectedDebugAndroidTest
```

- `BUILD SUCCESSFUL` en 1 min 53 s;
- compilación debug, pruebas unitarias, lint y compilación de pruebas Android: PASS;
- 86 de 86 pruebas instrumentadas ejecutadas;
- 0 omitidas y 0 fallidas;
- emulador `Medium_Phone(AVD) - 16` conectado hasta finalizar.

## Aceptación manual

- El desplegable permite seleccionar `Postres` y las nuevas familias añadidas.
- Una receta con un ingrediente nombrado pero sin cantidad no se guarda y muestra el error correspondiente.
- Una fila de ingrediente completamente vacía tampoco se ignora al guardar.
- Tras completar la cantidad, el guardado funciona.
- Launcher, cabecera de inicio y detalle muestran `Recetoria`.
- El icono permanece provisional y no ha sido rediseñado.

Todos los puntos anteriores fueron validados manualmente el 15 de agosto de 2026.

## Resultado

Gate 57 superado. El incremento puede publicarse en la rama de trabajo. La integración en `master` requiere una decisión posterior explícita.
