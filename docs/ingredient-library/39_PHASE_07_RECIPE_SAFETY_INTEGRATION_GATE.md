# 39 — FASE 7: INTEGRACIÓN DE SEGURIDAD EN RECETA — GATE

**Estado:** IMPLEMENTADA — VALIDACIÓN LOCAL PENDIENTE  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-10

## Objetivo

Cerrar la integración entre la evidencia explícita de seguridad almacenada para ingredientes y la pantalla final de receta, respetando ADR-027 y sin introducir inferencias clínicas.

## Alcance implementado

```text
Room / catálogo actual               reutilizado, sin migración
Lectura evidencia catálogo           implementada con grupo + fuente
Lectura evidencia personalizada      reutiliza declaraciones persistidas
Agregación por receta                implementada
Avisos de identidad/composición      implementados
Exenciones regulatorias              conservadas en canal separado
RecipeDetailViewModel                integrado
RecipeDetailScreen                   panel de seguridad integrado
Pruebas unitarias                    añadidas
Pruebas Compose UI                   añadidas
```

## Reglas verificables

1. Solo se agregan relaciones de seguridad explícitamente almacenadas.
2. El linaje culinario no se consulta para crear alertas.
3. Una identidad no resoluble genera revisión; no se intenta adivinar el ingrediente.
4. `compositionKnown = false` en un ingrediente personalizado genera aviso global de revisión.
5. Las exenciones regulatorias no eliminan observaciones de seguridad.
6. El mismo grupo se presenta una sola vez, conservando todas las observaciones.
7. La UI muestra texto, no solo color, y dispone de descripción semántica para el panel.
8. Si no existen coincidencias se usa: `No se han detectado coincidencias en los datos registrados.`
9. La UI recuerda que la información disponible puede ser incompleta.
10. No se emiten afirmaciones de receta segura, apta para alérgicos, libre de alérgenos o sin riesgo.

## Archivos principales

```text
app/src/main/java/com/rmm/recetasraquel/domain/ingredient/IngredientCatalogEntry.kt
app/src/main/java/com/rmm/recetasraquel/domain/repository/IngredientCatalogRepository.kt
app/src/main/java/com/rmm/recetasraquel/data/local/dao/IngredientCatalogDao.kt
app/src/main/java/com/rmm/recetasraquel/data/repository/LocalIngredientCatalogRepository.kt
app/src/main/java/com/rmm/recetasraquel/domain/usecase/BuildRecipeSafetySummaryUseCase.kt
app/src/main/java/com/rmm/recetasraquel/app/AppContainer.kt
app/src/main/java/com/rmm/recetasraquel/MainActivity.kt
app/src/main/java/com/rmm/recetasraquel/app/RecetasRaquelApp.kt
app/src/main/java/com/rmm/recetasraquel/ui/detail/RecipeDetailViewModel.kt
app/src/main/java/com/rmm/recetasraquel/ui/detail/RecipeDetailScreen.kt
app/src/test/java/com/rmm/recetasraquel/domain/usecase/BuildRecipeSafetySummaryUseCaseTest.kt
app/src/androidTest/java/com/rmm/recetasraquel/ui/detail/RecipeSafetyPanelUiTest.kt
```

## Gate local requerido

Ejecutar desde la raíz del repositorio:

```powershell
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" clean assembleDebug
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" assembleRelease
```

## Criterios de cierre

```text
assembleDebug                  PASS
testDebugUnitTest              PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
connectedDebugAndroidTest      PASS
assembleRelease                PASS
Room schemas                   1.json, 2.json, 3.json, 4.json solamente
5.json                         NO debe existir
working tree                   limpio después de sincronizar
```

No se ha modificado el esquema Room. La aparición de `5.json` sería inesperada y debe investigarse antes de continuar.

## Inspección manual mínima

Abrir una receta con evidencia conocida y comprobar que el panel muestra grupo, estado, ingrediente, evidencia y fuente sin afirmar seguridad de consumo. Abrir una receta con un ingrediente personalizado de composición desconocida y confirmar `Requiere revisión`. Abrir una receta sin coincidencias registradas y confirmar el lenguaje neutral.

## Siguiente paso

Si el gate queda verde, la Fase 7 puede marcarse como validada. El siguiente trabajo podrá continuar con el cierre del programa y la revisión de cobertura/UX pendiente, salvo que aparezca una nueva decisión arquitectónica.
