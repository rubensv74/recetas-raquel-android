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
- Antes de entregar, ejecutar localmente `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` y `./gradlew lintDebug`.
- Cuando un incremento afecte UI, persistencia, navegación, integración Android o comportamiento dependiente de dispositivo/emulador, ejecutar también localmente las pruebas instrumentadas relevantes antes de considerar el incremento validado.
- Crear un commit local de checkpoint siempre que exista un estado coherente, compilable y razonablemente validado; no acumular varios gates o incrementos cerrados sin commit.
- La autorización permanente del proyecto permite crear esos commits locales de checkpoint sin pedir confirmación en cada ocasión.
- Los problemas de infraestructura externa (por ejemplo, AVD offline o dispositivo desconectado) no bloquean un checkpoint si el código compila y las validaciones locales razonablemente ejecutables pasan; la validación pendiente debe quedar documentada y ejecutarse en cuanto la infraestructura vuelva a estar disponible.
- `push`, `merge`, `tag`, publicación de APK/release y operaciones Git destructivas siguen requiriendo autorización expresa.
- Actualizar la documentación y los ADR cuando cambie una decisión estructural.

## GitHub Actions — Local First / Remote Gate

GitHub Actions es un recurso de validación remota y debe utilizarse solo cuando aporte un gate que no sea razonable repetir en cada cambio local.

- No crear ni ampliar workflows que se ejecuten automáticamente en cada `push` de ramas de trabajo salvo necesidad técnica documentada.
- Compilación debug, unit tests y lint se validan localmente durante el desarrollo.
- El gate remoto ordinario se reserva al Pull Request hacia `master` o a una ejecución manual deliberada.
- Tests instrumentados con emulador remoto, `assembleRelease` y validaciones costosas se reservan para `workflow_dispatch`, release candidate o un gate que las necesite expresamente.
- Todo workflow debe usar filtros por rutas cuando sea posible y `concurrency` con cancelación de ejecuciones obsoletas cuando puedan solaparse.
- No usar `clean` en CI salvo que la limpieza sea parte explícita de la prueba.
- No generar artifacts remotos si no van a consumirse.
- Antes de añadir un job o trigger remoto, justificar: qué riesgo detecta, por qué no basta la validación local y cuál es el momento mínimo en el que debe ejecutarse.

Principio obligatorio: **validar localmente primero; ejecutar GitHub Actions solo como gate remoto necesario.**
