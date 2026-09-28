# Implementation Plan: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Branch**: `001-auth-profile-catalog` | **Date**: 2026-09-19 (ajuste 2026-09-23) | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-auth-profile-catalog/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

App Android nativa en Kotlin con Jetpack Compose que permite registrarse e ingresar con correo y
contraseña o con Google, restablecer la contraseña por correo, mantener la sesión (también al abrir
la app sin conexión, con aviso), ver el perfil y consultar dos catálogos de solo lectura (seis
desafíos con nombres de actividades al aire libre o sociales, que se cumplen sin usar redes
sociales, en tres dificultades; y recompensas genéricas). **Ajuste 2026-09-23**: además muestra,
solo lectura, el saldo de puntos de la persona (ícono arriba a la izquierda en cada pestaña y en el
Perfil) y sus últimos desafíos hechos, guardados por usuario en Firestore; el modelo deja preparado
el camino para acreditar puntos, registrar el desafío en curso y vencer puntos en las entregas
siguientes, sin implementarlos. Todos los
textos van en español rioplatense con "vos". El backend es Firebase en plan gratuito: Authentication para las cuentas y Firestore
para el perfil y los catálogos, protegidos con reglas de seguridad y sembrados con un script.
La lógica de validación es pura y se desarrolla test-first; las pruebas cubren JVM, reglas de
seguridad, datos sembrados y UI contra los emuladores de Firebase, más un guion manual para el
ingreso real con Google y otro para el correo real de restablecimiento. Decisiones y alternativas en
[research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (última estable al inicializar), JDK 17 para Gradle

**Primary Dependencies**: Jetpack Compose + Material 3, Navigation Compose, ViewModel/StateFlow,
Firebase Authentication y Cloud Firestore (vía Firebase BoM), Credential Manager con Google ID

**Storage**: Cloud Firestore (perfil `users/{uid}` con `pointsBalance`, movimientos de puntos
`users/{uid}/movements/{id}`, catálogos `challenges` y `rewards`); la contraseña la custodia
Firebase Authentication. Caché de Firestore solo en memoria. Un índice compuesto para los últimos
desafíos hechos (`firebase/firestore.indexes.json`).

**Testing**: JUnit 4, kotlinx-coroutines-test y Turbine (JVM); Compose UI Test + emuladores de
Firebase (instrumentadas); `@firebase/rules-unit-testing` y `node --test` (reglas y siembra);
guion manual para Google real y para el correo de restablecimiento real

**Target Platform**: Android 8.0 (API 26) o superior, con Google Play Services

**Project Type**: mobile-app + backend administrado (Firebase), sin servidor propio

**Performance Goals**: pantallas de catálogo con datos en menos de 2 s con conexión normal;
registro e ingreso completados en menos de 3 s tras enviar el formulario (los objetivos de
extremo a extremo son SC-001 a SC-003)

**Constraints**: conexión a Internet obligatoria (sin caché en disco de catálogos); permisos solo
`INTERNET` y `ACCESS_NETWORK_STATE`; interfaz solo en español rioplatense (voseo); sin plan de
pago de Firebase ni Cloud Functions; distribución por APK directo, sin Google Play; sin
confirmación de correo

**Scale/Scope**: decenas de usuarios de demostración; 3 secciones de contenido (Desafíos,
Recompensas, Perfil) + 4 pantallas de acceso (Espera, Ingreso, Registro, Restablecer contraseña);
6 desafíos y 5 recompensas; 1 saldo y hasta 5 movimientos leídos por persona

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluación contra `.specify/memory/constitution.md` v2.2.0 (ajuste 2026-09-23; la evaluación
original era contra v2.1.0).

| Principio / restricción                     | Resultado | Verificación                                                                                   |
|---------------------------------------------|-----------|------------------------------------------------------------------------------------------------|
| I. Desafío, no Bloqueo                      | PASA      | No hay medición ni bloqueo en esta entrega; ningún permiso de accesibilidad, superposición ni administración de dispositivo. Los títulos con actividades solo cambian el nombre: la condición sigue siendo no usar redes sociales y las descripciones lo dicen (sin geolocalización ni sensores) |
| II. Android Nativo                          | PASA      | Kotlin + Compose, APK distribuido directamente, sin publicar en Google Play; sin código de iOS ni PWA |
| III. Sin IA ni Cámara                       | PASA      | Sin permiso de cámara, fotos ni servicios de IA                                                 |
| IV. Evidencia Verificable por Entrega       | PASA      | Matriz FR → prueba en `quickstart.md`; validaciones puras test-first; guion manual documentado |
| V. Alcance Mínimo, Entrega Incremental      | PASA      | Un módulo, DI manual, sin Hilt ni Cloud Functions; sin canje, acreditación ni inicio de desafíos; la recuperación de contraseña por correo y la lectura de puntos entran por decisión del equipo y por la enmienda v2.2.0. No se crean el desafío en curso ni el historial de intentos: solo se documenta su camino (research D-22) |
| VI. Integridad de la Economía de Puntos     | PASA (lectura) | Saldo en `users/{uid}.pointsBalance` y movimientos en una subcolección solo de agregado, ambos en Firestore; cuenta nueva con saldo 0; las reglas deniegan toda escritura de puntos desde la app, con pruebas contra el emulador que intentan modificarlos. Acreditación, canje, vencimiento y las invariantes 3 a 5 se diseñan en la entrega del 01/10 (`TODO(PUNTOS_SEGURIDAD)`) |
| Seguridad y privacidad                      | PASA      | Contraseña delegada a Firebase Auth; permisos mínimos; sin datos de uso; reglas con denegación por defecto y probadas |
| Identidad visual y experiencia              | PASA      | Español (voseo), marca "(des)Conectado", paleta verde/azul, modo claro y oscuro, sin patrones compulsivos |
| Arquitectura de datos (backend fuente de verdad, conexión obligatoria) | PASA | Firebase como fuente de verdad; lectura forzada del servidor; bloqueo sin conexión |

Sin violaciones; **Complexity Tracking** queda vacío.

### Re-evaluación tras el diseño (Fase 1)

Sin cambios de permisos ni de stack: no se agregan permisos de Android ni dependencias. Se agregan
una subcolección, un campo, un índice y una pieza de interfaz (indicador de puntos), todos de solo
lectura, y nuevas pruebas de reglas para el Principio VI. El stack de este plan quedó registrado en la constitución (sección "Stack tecnológico",
v2.0.0), por lo que no queda ninguna decisión de esta entrega pendiente de enmienda. Los puntos
autoritativos en el backend dejaron de ser un requisito (Principio VI redefinido). Con el ajuste
2026-09-23 esta entrega ya guarda y muestra puntos, pero sin acreditarlos: el diseño de las reglas
que validan acreditación, canje y saldo sigue en el plan del 01/10 (`TODO(PUNTOS_SEGURIDAD)`). El
modelo elegido aquí (saldo más movimientos solo de agregado) es el que ese diseño necesita, para no
migrar datos después.

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
    │   │   │   ├── profile/            # ProfileRepository (perfil y saldo)
    │   │   │   ├── points/             # PointsRepository (últimos desafíos hechos)
    │   │   │   ├── catalog/            # CatalogRepository (desafíos y recompensas)
    │   │   │   └── connectivity/       # ConnectivityMonitor
    │   │   └── ui/
    │   │       ├── theme/              # paleta, tipografía, claro/oscuro
    │   │       ├── navigation/         # grafo y barra inferior
    │   │       ├── auth/               # Ingreso, Registro, Restablecer contraseña, Espera
    │   │       ├── challenges/
    │   │       ├── rewards/
    │   │       └── profile/
    │   └── res/values/strings.xml      # textos en español
    ├── test/                           # unitarias (JVM)
    └── androidTest/                    # UI e integración con emuladores

firebase/
├── firebase.json                       # emuladores de Auth y Firestore
├── firestore.rules
├── firestore.indexes.json              # índice de movimientos (últimos desafíos hechos)
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
