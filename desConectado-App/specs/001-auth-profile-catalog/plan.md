# Implementation Plan: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Branch**: `001-auth-profile-catalog` | **Date**: 2026-09-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-auth-profile-catalog/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

App Android nativa en Kotlin con Jetpack Compose que permite registrarse e ingresar con correo y
contraseña o con Google, mantener la sesión, ver el perfil y consultar dos catálogos de solo
lectura (desafíos "No uses redes sociales por X tiempo" en tres dificultades, y recompensas
genéricas). El backend es Firebase en plan gratuito: Authentication para las cuentas y Firestore
para el perfil y los catálogos, protegidos con reglas de seguridad y sembrados con un script.
La lógica de validación es pura y se desarrolla test-first; las pruebas cubren JVM, reglas de
seguridad, datos sembrados y UI contra los emuladores de Firebase, más un guion manual para el
ingreso real con Google. Decisiones y alternativas en [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (última estable al inicializar), JDK 17 para Gradle

**Primary Dependencies**: Jetpack Compose + Material 3, Navigation Compose, ViewModel/StateFlow,
Firebase Authentication y Cloud Firestore (vía Firebase BoM), Credential Manager con Google ID

**Storage**: Cloud Firestore (perfil `users/{uid}`, catálogos `challenges` y `rewards`); la
contraseña la custodia Firebase Authentication. Caché de Firestore solo en memoria.

**Testing**: JUnit 4, kotlinx-coroutines-test y Turbine (JVM); Compose UI Test + emuladores de
Firebase (instrumentadas); `@firebase/rules-unit-testing` y `node --test` (reglas y siembra);
guion manual para Google real

**Target Platform**: Android 8.0 (API 26) o superior, con Google Play Services

**Project Type**: mobile-app + backend administrado (Firebase), sin servidor propio

**Performance Goals**: pantallas de catálogo con datos en menos de 2 s con conexión normal;
registro e ingreso completados en menos de 3 s tras enviar el formulario (los objetivos de
extremo a extremo son SC-001 a SC-003)

**Constraints**: conexión a Internet obligatoria (sin caché en disco de catálogos); permisos solo
`INTERNET` y `ACCESS_NETWORK_STATE`; interfaz solo en español; sin plan de pago de Firebase ni
Cloud Functions en esta entrega

**Scale/Scope**: decenas de usuarios de demostración; 5 pantallas de contenido + 2 de acceso;
6 desafíos y 5 recompensas

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluación contra `.specify/memory/constitution.md` v1.2.0.

| Principio / restricción                     | Resultado | Verificación                                                                                   |
|---------------------------------------------|-----------|------------------------------------------------------------------------------------------------|
| I. Desafío, no Bloqueo                      | PASA      | No hay medición ni bloqueo en esta entrega; ningún permiso de accesibilidad, superposición ni administración de dispositivo |
| II. Android Nativo                          | PASA      | Kotlin + Compose, APK/AAB instalable; sin código de iOS ni PWA                                  |
| III. Sin IA ni Cámara                       | PASA      | Sin permiso de cámara, fotos ni servicios de IA                                                 |
| IV. Evidencia Verificable por Entrega       | PASA      | Matriz FR → prueba en `quickstart.md`; validaciones puras test-first; guion manual documentado |
| V. Alcance Mínimo, Entrega Incremental      | PASA      | Un módulo, DI manual, sin Hilt ni Cloud Functions; sin recuperación de contraseña ni canje       |
| VI. Integridad de la Economía de Puntos     | N/A       | No hay puntos ni saldo; las reglas impiden a la app escribir en catálogos y perfil tras crearlo |
| Seguridad y privacidad                      | PASA      | Contraseña delegada a Firebase Auth; permisos mínimos; sin datos de uso                          |
| Identidad visual y experiencia              | PASA      | Español, marca "(des)Conectado", paleta verde/azul, modo claro y oscuro, sin patrones compulsivos |
| Arquitectura de datos (backend fuente de verdad, conexión obligatoria) | PASA | Firebase como fuente de verdad; lectura forzada del servidor; bloqueo sin conexión |

Sin violaciones; **Complexity Tracking** queda vacío.

### Re-evaluación tras el diseño (Fase 1)

Sin cambios: el modelo de datos, los contratos y el quickstart no agregan permisos, componentes ni
alcance. Un punto queda **abierto a nivel de constitución, no de esta entrega**: la constitución
sigue listando `TODO(STACK)`; este plan lo resuelve, por lo que corresponde una enmienda (ver el
informe de finalización). El riesgo R-2 (puntos autoritativos en el backend) no afecta a esta
entrega pero debe resolverse antes de planificar la del 01/10.

## Project Structure

### Documentation (this feature)

```text
specs/001-auth-profile-catalog/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── firestore-data.md
│   ├── repositories.md
│   └── ui-screens.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
├── google-services.json            # no versionado
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/desconectado/app/
    │   │   ├── DesConectadoApp.kt      # Application; crea el contenedor de dependencias
    │   │   ├── MainActivity.kt
    │   │   ├── domain/                 # modelos y validaciones puras (sin Android ni Firebase)
    │   │   ├── data/
    │   │   │   ├── auth/               # AuthRepository + implementación Firebase + Google
    │   │   │   ├── profile/            # ProfileRepository
    │   │   │   ├── catalog/            # CatalogRepository (desafíos y recompensas)
    │   │   │   └── connectivity/       # ConnectivityMonitor
    │   │   └── ui/
    │   │       ├── theme/              # paleta, tipografía, claro/oscuro
    │   │       ├── navigation/         # grafo y barra inferior
    │   │       ├── auth/               # Ingreso, Registro, Espera
    │   │       ├── challenges/
    │   │       ├── rewards/
    │   │       └── profile/
    │   └── res/values/strings.xml      # textos en español
    ├── test/                           # unitarias (JVM)
    └── androidTest/                    # UI e integración con emuladores

firebase/
├── firebase.json                       # emuladores de Auth y Firestore
├── firestore.rules
├── package.json
├── seed/
│   ├── catalog.json                    # 6 desafíos y 5 recompensas
│   └── seed.mjs                        # valida invariantes y siembra
└── tests/
    ├── rules/                          # pruebas de reglas de seguridad
    └── seed/                           # pruebas de invariantes del catálogo

build.gradle.kts
settings.gradle.kts
gradle/libs.versions.toml               # versiones fijadas
```

**Structure Decision**: un único módulo Android `app` más una carpeta `firebase/` para las reglas,
los emuladores y la siembra, ambos en la raíz del repositorio. Se descarta multi-módulo y una
carpeta `android/` intermedia por Principio V: no hay otro cliente que la justifique. `domain/`
no importa Android ni Firebase, lo que permite probarla en JVM sin dispositivo.

## Complexity Tracking

Sin violaciones de la constitución; no hay complejidad que justificar.
