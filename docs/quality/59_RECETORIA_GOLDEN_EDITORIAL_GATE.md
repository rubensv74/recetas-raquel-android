# Gate 59 — Identidad Golden Editorial de Recetoria

**Estado:** LISTO PARA VALIDACIÓN LOCAL
**Fecha:** 2026-08-16
**Rama:** `agent/recetoria-golden-editorial`

## Alcance

- sistema Material 3 claro y oscuro centralizado;
- tipografía editorial para contenido culinario y sans-serif para UI y seguridad;
- icono adaptativo, monocromo, legacy y splash oficiales;
- marca integrada en el catálogo;
- favoritos y selecciones en Antique Gold;
- acción principal en Toasted Bronze;
- detalle con fotografía protagonista, cabecera editorial, metadatos y cocinar;
- tarjetas diferenciadas para presencia, puede contener y revisión crítica;
- documentación y pruebas de tokens y contraste.

## Invariantes conservadas

- navegación, ViewModels y estados;
- modelo Room, migraciones y recetas guardadas;
- almacenamiento y orientación de fotografías;
- catálogo, alias y relaciones de ingredientes;
- agregación, evidencia, fuentes y excepciones regulatorias;
- textos de seguridad y pruebas de negocio.

## Gate automático

```powershell
.\gradlew -g "C:\Temp\gradle_home_gate59_gold" assembleDebug testDebugUnitTest lintDebug compileDebugAndroidTestKotlin connectedDebugAndroidTest
```

## Gate visual

- catálogo, fotografía, búsqueda, categorías, favoritos y estados vacío/error;
- creación y edición, campos, unidades, desplegables y diálogos;
- detalle, cocinar, ingredientes, pasos y notas;
- presencia confirmada en ámbar, puede contener en azul y revisión en rojo;
- biblioteca y detalle de ingrediente, regulación y avisos del fabricante;
- modo claro y oscuro, fuente grande y navegación TalkBack básica;
- icono circular, redondeado y squircle; splash sin recortes.

El gate se marca `SUPERADO` únicamente tras el comando completo, la revisión en
emulador y la incorporación de capturas reales.
