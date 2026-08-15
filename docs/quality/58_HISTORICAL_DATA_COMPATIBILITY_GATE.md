# Gate 58 — Compatibilidad de datos históricos

**Estado:** SUPERADO
**Fecha:** 2026-08-15
**Rama de origen:** `master`

## Objetivo

Demostrar que la obligatoriedad de la cantidad introducida en el Gate 57 no oculta, elimina ni corrompe ingredientes históricos que todavía tengan una cantidad vacía.

## Hallazgos

1. Las recetas históricas con cantidad nula se cargan en el editor con el campo vacío y conservan el resto de sus datos.
2. El editor impide guardarlas hasta completar cada cantidad y señala la fila correspondiente.
3. Favoritos, lectura del detalle y modo cocina no requieren reescribir la receta y continúan admitiendo los datos históricos.
4. `RecipeMapper.normalizeDraft()` filtraba por nombre antes de validar. Una entrada incompleta podía desaparecer silenciosamente si alcanzaba directamente el repositorio, aunque la UI ya la bloqueaba.
5. La aplicación ya contenía recetas clasificadas como `Platos principales`, pero esa opción no aparecía en la lista controlada para recetas nuevas.

## Correcciones

- La normalización ya no elimina ingredientes antes de validarlos.
- Toda fila recibida por persistencia debe superar las reglas de nombre y cantidad.
- Se añade `Platos principales` sin renombrar ni eliminar categorías existentes.
- Se actualizan dos referencias activas del nombre anterior a `Recetoria`.

## Compatibilidad deliberada

- La columna `quantity` continúa siendo nullable en Room para poder leer bases históricas sin una migración destructiva.
- No se inventan cantidades para ingredientes antiguos.
- No se modifica automáticamente ninguna receta del usuario.
- La corrección ocurre únicamente cuando la persona edita y guarda la receta.
- Valores culinarios no numéricos como `al gusto`, `una pizca` o `1/2` siguen siendo válidos.

## Cobertura añadida

- una fila incompleta que llegue al mapper produce error y no desaparece;
- una receta histórica sin cantidad puede abrirse;
- esa receta no puede guardarse hasta completar la cantidad;
- tras introducir `al gusto`, el borrador actualizado conserva exactamente ese valor;
- la taxonomía controlada incluye `Platos principales` y no contiene duplicados.

## Gate local superado

```powershell
.\gradlew -g "C:\Temp\gradle_home_gate58" assembleDebug testDebugUnitTest lintDebug compileDebugAndroidTestKotlin connectedDebugAndroidTest
```

## Aceptación manual superada

- abrir una receta histórica con cantidad vacía no bloquea ni cierra la pantalla;
- guardar sin corregir muestra `La cantidad es obligatoria`;
- completar la cantidad permite guardar sin perder unidad, ingrediente, notas, pasos ni fotografías;
- marcar o desmarcar como favorita una receta histórica sigue funcionando;
- `Platos principales` aparece en el selector y en el filtro cuando existen recetas con esa categoría.
