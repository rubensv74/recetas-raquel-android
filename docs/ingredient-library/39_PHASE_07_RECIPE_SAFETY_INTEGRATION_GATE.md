# 39 — FASE 7: INTEGRACIÓN DE SEGURIDAD EN RECETA — GATE

**Estado:** VALIDADA — ADR-027 + ADR-028 / ROOM v5  
**Rama:** `program/ingredient-library-food-safety`  
**Fecha:** 2026-08-10

## Objetivo

Cerrar la integración entre la evidencia explícita de seguridad almacenada para ingredientes y la pantalla final de receta, respetando ADR-027 y sin introducir inferencias clínicas.

La Fase 7 se implementó inicialmente sobre Room v4. Posteriormente, durante su cierre, la auditoría del canal regulatorio detectó que ADR-026 exigía preservar revisiones históricas. Esa necesidad dio lugar a ADR-028 y a la evolución controlada Room v4 -> v5. La agregación clínica/visual de Fase 7 no cambia; evoluciona únicamente la persistencia y auditoría de exenciones regulatorias.

## Alcance implementado

```text
Room                              v5 — validado por ADR-028
Lectura evidencia catálogo        implementada con grupo + fuente
Lectura evidencia personalizada   reutiliza declaraciones persistidas
Agregación por receta             implementada
Avisos identidad/composición      implementados
Exenciones regulatorias           canal separado + snapshot + vigencia contextual
Historial regulatorio             versionado por catalogVersion
RecipeDetailViewModel             integrado
RecipeDetailScreen                panel de seguridad integrado
Pruebas unitarias                 PASS
Pruebas instrumentadas / UI       PASS
```

## Reglas verificables

1. Solo se agregan relaciones de seguridad explícitamente almacenadas.
2. El linaje culinario no se consulta para crear alertas.
3. Una identidad no resoluble genera revisión; no se intenta adivinar el ingrediente.
4. `compositionKnown = false` en un ingrediente personalizado genera aviso global de revisión.
5. Las exenciones regulatorias no eliminan observaciones de seguridad.
6. Solo llegan al resumen las exenciones del snapshot activo aplicables a la identidad exacta, jurisdicción configurada y fecha regulatoria actual.
7. Los snapshots regulatorios anteriores permanecen para auditoría, pero no participan en la evaluación actual.
8. La fecha regulatoria se obtiene mediante `TimeProvider`; la configuración por defecto es `EU-ES` con zona `Europe/Madrid`, por lo que la lógica es determinista y testeable.
9. El mismo grupo se presenta una sola vez, conservando todas las observaciones.
10. La UI muestra texto, no solo color, y dispone de descripción semántica para el panel.
11. Si no existen coincidencias se usa: `No se han detectado coincidencias en los datos registrados.`
12. La UI recuerda que la información disponible puede ser incompleta.
13. No se emiten afirmaciones de receta segura, apta para alérgicos, libre de alérgenos o sin riesgo.
14. Un `sourceId` regulatorio publicado no puede reescribirse con metadatos distintos; una fuente materialmente revisada exige un identificador nuevo.

## Archivos principales

```text
app/src/main/java/com/rmm/recetasraquel/domain/ingredient/IngredientCatalogEntry.kt
app/src/main/java/com/rmm/recetasraquel/domain/repository/IngredientCatalogRepository.kt
app/src/main/java/com/rmm/recetasraquel/data/local/dao/IngredientCatalogDao.kt
app/src/main/java/com/rmm/recetasraquel/data/local/entity/RegulatoryExemptionEntity.kt
app/src/main/java/com/rmm/recetasraquel/data/local/IngredientLibraryMigrations.kt
app/src/main/java/com/rmm/recetasraquel/data/local/RecipeDatabase.kt
app/src/main/java/com/rmm/recetasraquel/data/catalog/CatalogImporter.kt
app/src/main/java/com/rmm/recetasraquel/data/repository/LocalIngredientCatalogRepository.kt
app/src/main/java/com/rmm/recetasraquel/domain/usecase/BuildRecipeSafetySummaryUseCase.kt
app/src/main/java/com/rmm/recetasraquel/app/AppContainer.kt
app/src/main/java/com/rmm/recetasraquel/ui/detail/RecipeDetailViewModel.kt
app/src/main/java/com/rmm/recetasraquel/ui/detail/RecipeDetailScreen.kt
app/src/test/java/com/rmm/recetasraquel/domain/usecase/BuildRecipeSafetySummaryUseCaseTest.kt
app/src/androidTest/java/com/rmm/recetasraquel/data/local/RegulatoryExemptionApplicabilityDaoTest.kt
app/src/androidTest/java/com/rmm/recetasraquel/data/local/RegulatoryExemptionMigration45Test.kt
app/src/androidTest/java/com/rmm/recetasraquel/data/catalog/CatalogSafetySourceHistoryGuardTest.kt
app/src/androidTest/java/com/rmm/recetasraquel/ui/detail/RecipeSafetyPanelUiTest.kt
```

## Cobertura añadida

La batería comprueba que una exención de un snapshot histórico, fuera de jurisdicción, futura, caducada o inactiva no llega al resumen actual de receta.

También comprueba que una exención aplicable no crea por sí misma una relación de seguridad ni modifica las observaciones explícitas.

ADR-028 añade además cobertura para:

- migración completa de instalaciones antiguas hasta Room v5;
- migración v4 -> v5 conservando una exención y todos sus campos;
- coexistencia de snapshots regulatorios;
- consulta histórica explícita;
- integridad de claves foráneas;
- inmutabilidad de las fuentes históricas.

## Gate automatizado vigente

GitHub Actions ejecuta:

```text
assembleDebug
unit tests
lintDebug
compileDebugAndroidTestKotlin
assembleRelease
Room schema guard
connectedDebugAndroidTest
Room schema guard posterior al emulador
```

El contrato Room vigente es:

```text
1.json
2.json
3.json
4.json
5.json
```

`5.json` es obligatorio y corresponde a ADR-028. El CI también exige que la generación de Room no produzca diferencias respecto al esquema versionado.

## Gate local equivalente

Desde la raíz del repositorio:

```powershell
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" clean assembleDebug
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" testDebugUnitTest
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" lintDebug
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" compileDebugAndroidTestKotlin
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" connectedDebugAndroidTest
.\gradlew.bat -g "C:\Temp\gradle_home_ingredient_library" assembleRelease
```

## Gate de cierre validado

Ejecución final:

```text
Android CI run 31376760612
Code SHA 71ee4d38b3dba77831ae850a7c496af68942af4c
```

Resultado:

```text
assembleDebug                  PASS
unit tests                     PASS
lintDebug                      PASS
compileDebugAndroidTestKotlin  PASS
assembleRelease                PASS
Room schemas 1..5              PASS
connectedDebugAndroidTest      PASS — 63/63
Room guard post-emulador       PASS
```

Durante la validación se corrigió únicamente una prueba de navegación que asumía visibilidad inmediata de elementos contenidos en `LazyColumn`. La UI y el flujo de producto no necesitaron cambios para superar el gate.

## Inspección manual mínima

Abrir una receta con evidencia conocida y comprobar que el panel muestra grupo, estado, ingrediente, evidencia y fuente sin afirmar seguridad de consumo. Abrir una receta con un ingrediente personalizado de composición desconocida y confirmar `Requiere revisión`. Abrir una receta sin coincidencias registradas y confirmar el lenguaje neutral.

La presentación detallada de exenciones regulatorias, si se incorpora posteriormente, deberá permanecer visualmente separada de las advertencias de seguridad y no podrá traducir una exención legal a una afirmación clínica.

## Cierre

La Fase 7 queda validada sobre Room v5. ADR-027 mantiene la semántica de agregación de seguridad y ADR-028 garantiza la trazabilidad histórica de la capa regulatoria. No queda abierto ningún gate técnico de esta fase.
