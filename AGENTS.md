# Instrucciones para agentes

## Alcance

Estas instrucciones se aplican a todo el repositorio.

## Restricciones técnicas

- Mantener un único módulo Android `app` y una sola `MainActivity`.
- Usar Kotlin, Jetpack Compose y Material 3; no crear pantallas XML.
- Preservar el funcionamiento completamente offline. No añadir permisos ni clientes de red.
- No añadir autenticación, backend, GitHub Sync, Hilt u otro contenedor de inyección sin una decisión explícita posterior.
- Mantener `com.rmm.recetasraquel` como namespace y application ID, y `minSdk` 26.
- Añadir dependencias solo cuando un requisito vigente no pueda resolverse razonablemente con las existentes.
- No introducir secretos, credenciales ni datos personales reales.

## Organización

- `app`: raíz de composición y coordinación de la aplicación.
- `ui`: pantallas, navegación, componentes y tema Compose.
- `domain`: modelos y reglas independientes de Android cuando se incorporen.
- `data`: persistencia local, repositorios y mappers cuando se incorpore Room.
- `backup`: importación y exportación futuras.

No crear archivos o capas vacías para anticipar trabajo futuro.

## Calidad y entrega

- Mantener flujo de datos unidireccional y estado observable e inmutable desde la UI.
- Añadir o actualizar pruebas relevantes para cada cambio.
- Antes de entregar, ejecutar `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` y `./gradlew lintDebug`.
- No hacer commit, push, merge ni publicar APK sin autorización expresa.
- Actualizar la documentación y los ADR cuando cambie una decisión estructural.
