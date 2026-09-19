---

description: "Lista de tareas de la entrega del 24/09: cuenta, perfil y catálogos, hasta obtener un APK instalable"
---

# Tasks: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Input**: Documentos de diseño en `/specs/001-auth-profile-catalog/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUIDAS. La constitución (Principio IV) exige pruebas de cada requisito y desarrollo
test-first de la lógica de negocio, y el spec exige evidencia (FR-024, SC-008).

**Objetivo final**: al terminar, existe un APK firmado listo para instalar en un dispositivo
Android real (`dist/desConectado-entrega1.apk`) que cumple los requisitos de la entrega del 24/09.

**Organization**: tareas agrupadas por historia de usuario del spec, para implementar y probar cada
una por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencia de tareas incompletas)
- **[Story]**: historia de usuario a la que pertenece (US1 a US5)
- **(MANUAL)**: la tarea la debe hacer la persona, no el asistente (instalaciones, consola de
  Firebase, dispositivo físico). El asistente prepara todo lo demás y espera la confirmación.

## Path Conventions

Estructura fijada en `plan.md`. Rutas abreviadas en las descripciones:

- Kotlin principal: `app/src/main/java/com/desconectado/app/`
- Pruebas JVM: `app/src/test/java/com/desconectado/app/`
- Pruebas instrumentadas: `app/src/androidTest/java/com/desconectado/app/`
- Recursos: `app/src/main/res/`
- Firebase (reglas, siembra, pruebas Node): `firebase/`

Las pantallas Compose son sin estado (reciben el estado y funciones como parámetros) para poder
probarlas sin Firebase. Cada elemento clave lleva una etiqueta de prueba estable (`testTag`):
`campo_username`, `campo_correo`, `campo_password`, `boton_registrar`, `boton_ingresar`,
`boton_google`, `enlace_registro`, `enlace_ingreso`, `lista_desafios`, `lista_recompensas`,
`texto_username`, `texto_email`, `boton_cerrar_sesion`, `tab_desafios`, `tab_recompensas`,
`tab_perfil`, `aviso_sin_conexion`, `boton_reintentar`, `enlace_olvide_password`,
`campo_correo_restablecer`, `boton_enviar_restablecimiento`, `mensaje_restablecimiento_enviado`.

**Convención de textos (FR-023)**: todos los textos visibles se escriben en español rioplatense,
tratando a la persona de "vos" ("Ingresá", "Creá tu cuenta", "Ya tenés una cuenta", "Olvidé mi
contraseña"), en archivos `app/src/main/res/values/strings_*.xml` por sección, nunca literales en el
código Kotlin. El nombre siempre se escribe "(des)Conectado".

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: entorno de desarrollo, proyecto Firebase y esqueleto Gradle que compila.

- [ ] T001 (MANUAL) Instalar el entorno de Android: Android Studio con SDK (plataforma estable más reciente, build-tools y platform-tools), aceptar licencias (`sdkmanager --licenses`), definir `ANDROID_HOME`, agregar `platform-tools` al PATH, crear un emulador (AVD) con imagen **Google Play** y de API 34 o superior; instalar Firebase CLI con `npm install -g firebase-tools`. Verificar `adb --version` y `firebase --version`. Gradle usa el JDK 17 (ya instalado); los emuladores de Firebase necesitan `JAVA_HOME` apuntando al JDK 21
- [ ] T002 (MANUAL) Crear el proyecto Firebase y registrar la app. Primero obtener el SHA-1 de depuración: si falta `%USERPROFILE%\.android\debug.keystore`, crearlo con `keytool -genkey -v -keystore %USERPROFILE%\.android\debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"` y leerlo con `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android`. En la consola de Firebase: habilitar Authentication con **Correo y contraseña** y **Google**, crear Firestore en modo producción, agregar una app Android con el paquete `com.desconectado.app` y ese SHA-1, y **recién entonces** descargar `google-services.json` a `app/google-services.json` (verificar que incluya un `oauth_client` de tipo 3, necesario para el ingreso con Google)
- [X] T003 Crear `.gitignore` en la raíz ignorando: `google-services.json`, `keystore/`, `keystore.properties`, `*.jks`, `local.properties`, `build/`, `.gradle/`, `.idea/`, `node_modules/`, `dist/`, `firebase/service-account*.json`, `evidencia/**/*.tmp`
- [X] T004 Crear los archivos Gradle raíz `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` y el catálogo de versiones `gradle/libs.versions.toml` con las últimas versiones estables de: Android Gradle Plugin, Kotlin (y su plugin de Compose), Compose BOM, Navigation Compose, Lifecycle ViewModel/Runtime Compose, kotlinx-coroutines (y `-test`), Firebase BoM (`firebase-auth-ktx`/`firebase-auth`, `firebase-firestore`), plugin `google-services`, `androidx.credentials`, `androidx.credentials:credentials-play-services-auth`, `googleid`, JUnit 4, Turbine, `androidx.test.ext:junit`, `compose ui-test-junit4` y `ui-test-manifest`
- [X] T005 Generar el wrapper de Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) descargando la distribución de Gradle compatible con el plugin de Android elegido en T004 y ejecutando `gradle wrapper`; verificar con `./gradlew --version` (debe usar JDK 17)
- [X] T006 Crear `app/build.gradle.kts`: `namespace` y `applicationId` = `com.desconectado.app`; `minSdk` 26; `compileSdk`/`targetSdk` = versión estable más reciente; Compose habilitado; plugin `google-services`; `buildConfig` habilitado con el campo booleano `USE_FIREBASE_EMULATOR` (en `debug` toma el valor de la propiedad Gradle `useEmulator`, por defecto `false`; en `release` siempre `false`); `signingConfigs.release` que lee `keystore.properties` de la raíz si existe; `release` con `isMinifyEnabled = false`; dependencias de T004, `testInstrumentationRunner` de AndroidX
- [X] T007 [P] Crear `app/src/main/AndroidManifest.xml` (permisos únicamente `INTERNET` y `ACCESS_NETWORK_STATE`; `Application` = `.DesConectadoApp`; `MainActivity` como launcher; etiqueta de app "(des)Conectado"), `app/src/debug/AndroidManifest.xml` con `android:networkSecurityConfig` apuntando a `app/src/debug/res/xml/network_security_config.xml`, que permite tráfico sin cifrar únicamente hacia `10.0.2.2` (emuladores de Firebase; solo en depuración)
- [X] T008 [P] Crear los recursos base: `app/src/main/res/values/strings.xml` (solo `app_name` = "(des)Conectado"), `app/src/main/res/values/themes.xml` (tema base sin ActionBar), e ícono de launcher adaptable (vectorial, verde/azul) en `app/src/main/res/mipmap-anydpi-v26/`
- [X] T009 [P] Crear `firebase/firebase.json` (emuladores: Auth en 9099, Firestore en 8080, reglas en `firestore.rules`) y `firebase/package.json` (tipo módulo; dependencias de desarrollo `firebase-admin`, `firebase` y `@firebase/rules-unit-testing`; scripts `test` = `firebase emulators:exec --only firestore "node --test tests/rules"`, `test:seed` = `node --test tests/seed`)
- [X] T010 Ejecutar `npm install` en `firebase/` y comprobar que `firebase emulators:start --only auth,firestore` arranca con `JAVA_HOME` en el JDK 21; detenerlo después

**Checkpoint**: `./gradlew help` funciona y los emuladores de Firebase arrancan.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: modelos, contratos de repositorio, inicialización de Firebase, tema y estructura de
navegación que todas las historias necesitan.

**⚠️ CRITICAL**: ninguna historia puede empezar hasta terminar esta fase.

- [X] T011 Crear `app/src/main/java/com/desconectado/app/domain/model/Resultado.kt`: `sealed interface Resultado<out T>` con `Exito(valor)` y `Fallo(error: ErrorApp)`, y `sealed interface ErrorApp` con los objetos `SinConexion`, `CorreoEnUso`, `CredencialesInvalidas`, `Cancelado`, `CuentaExistenteConOtroProveedor`, `Desconocido` (contracts/repositories.md)
- [X] T012 [P] Crear `app/src/main/java/com/desconectado/app/domain/model/Perfil.kt`: `Perfil(username: String, email: String)`, sin ningún campo de contraseña. Restricciones (data-model.md): `username` "Registro: 3 a 30 caracteres, recortado. Servidor: 1 a 30"; `email` "Minúsculas y sin espacios; igual al correo de la cuenta autenticada"
- [X] T013 [P] Crear `app/src/main/java/com/desconectado/app/domain/model/Desafio.kt`: `Desafio(id, title, description, durationMinutes: Int, difficulty: Dificultad, points: Int, order: Int)` y `enum class Dificultad { FACIL, NORMAL, DIFICIL }` con valor de almacén `easy`, `normal`, `hard` (data-model.md: `difficulty` "Uno de `easy`, `normal`, `hard`"; `durationMinutes` y `points` "Mayor que 0")
- [X] T014 [P] Crear `app/src/main/java/com/desconectado/app/domain/model/Recompensa.kt`: `Recompensa(id, name, description, costPoints: Int, kind: TipoRecompensa, order: Int)` y `enum class TipoRecompensa { INSIGNIA, TEMA, CUPON }` con valor de almacén `badge`, `theme`, `coupon` (data-model.md: `kind` "Uno de `badge`, `theme`, `coupon`"; `costPoints` "Mayor que 0")
- [X] T015 [P] Crear `app/src/main/java/com/desconectado/app/domain/model/Estados.kt`: `sealed interface EstadoSesion` (`Cargando`, `SinSesion`, `ConSesion(uid: String)`) y `enum class Conectividad { CONECTADO, SIN_CONEXION }`
- [X] T016 [P] Crear `app/src/main/java/com/desconectado/app/domain/repository/AuthRepository.kt` con la interfaz de contracts/repositories.md: `authState: Flow<EstadoSesion>`, `registrar(username, email, password)`, `ingresar(email, password)`, `ingresarConGoogle(idToken)`, `vincularGoogle(idToken)`, `restablecerPassword(email)`, `cerrarSesion()`, `verificarCuenta()`; las operaciones de red devuelven `Resultado<Unit>`
- [X] T017 [P] Crear `app/src/main/java/com/desconectado/app/domain/repository/ProfileRepository.kt`: `perfil(uid): Resultado<Perfil>` y `asegurarPerfil(uid, nombre: String?, email: String): Resultado<Unit>`
- [X] T018 [P] Crear `app/src/main/java/com/desconectado/app/domain/repository/CatalogRepository.kt`: `desafios(): Resultado<List<Desafio>>` y `recompensas(): Resultado<List<Recompensa>>`
- [X] T019 [P] Crear `app/src/main/java/com/desconectado/app/domain/repository/ConnectivityMonitor.kt`: `estado: Flow<Conectividad>`
- [X] T020 Implementar `app/src/main/java/com/desconectado/app/data/connectivity/AndroidConnectivityMonitor.kt` con `ConnectivityManager` y un `NetworkCallback` que emite `Conectividad` (valor inicial según la red activa), sin filtrar duplicados manualmente
- [X] T021 Crear `app/src/main/java/com/desconectado/app/DesConectadoApp.kt` (clase `Application`) y `app/src/main/java/com/desconectado/app/AppContainer.kt`: si `BuildConfig.USE_FIREBASE_EMULATOR` es verdadero, llamar `useEmulator("10.0.2.2", 9099)` en Auth y `useEmulator("10.0.2.2", 8080)` en Firestore **antes** de cualquier uso; después configurar Firestore con caché **solo en memoria** (sin persistencia en disco, research.md D-7); el contenedor expone `connectivityMonitor` y las instancias de Firebase (los repositorios se agregan en las historias)
- [X] T022 [P] Crear el tema en `app/src/main/java/com/desconectado/app/ui/theme/Color.kt`, `Type.kt` y `Theme.kt`: Material 3, paleta de verdes y azules con neutros, esquema claro y oscuro según el sistema
- [X] T023 [P] Crear los componentes compartidos `app/src/main/java/com/desconectado/app/ui/components/AvisoSinConexion.kt` (aviso con `testTag("aviso_sin_conexion")`) y `app/src/main/java/com/desconectado/app/ui/components/EstadoPantalla.kt` (composables de cargando, error con botón `testTag("boton_reintentar")`) y `app/src/main/res/values/strings_comunes.xml` con sus textos en español
- [X] T024 Crear la estructura de navegación principal: `app/src/main/java/com/desconectado/app/ui/navigation/MainShell.kt` (barra inferior con tres destinos Desafíos, Recompensas y Perfil, con `testTag` `tab_desafios`, `tab_recompensas`, `tab_perfil`; muestra `AvisoSinConexion` en la parte superior mientras `Conectividad` sea `SIN_CONEXION`), pantallas provisorias `ui/challenges/DesafiosScreen.kt`, `ui/rewards/RecompensasScreen.kt` y `ui/profile/PerfilScreen.kt` (solo un texto "Próximamente"; se reemplazan en sus historias) y `MainActivity.kt` que muestra el tema y `MainShell`
- [X] T025 [P] Crear los auxiliares de prueba JVM `app/src/test/java/com/desconectado/app/testutil/MainDispatcherRule.kt` (regla que reemplaza el dispatcher principal) y `app/src/test/java/com/desconectado/app/fakes/FakeConnectivityMonitor.kt` (estado controlable desde la prueba)
- [X] T026 [P] Crear `firebase/firestore.rules` con `rules_version = '2'` y denegación por defecto de toda lectura y escritura, y `firebase/tests/rules/helpers.mjs` con la función que crea el entorno de prueba (`initializeTestEnvironment`) cargando `firestore.rules` y devuelve contextos autenticado y no autenticado
- [X] T027 Verificar la base: `./gradlew assembleDebug` compila y la app se instala y abre en el emulador mostrando la barra inferior con las tres secciones provisorias

**Checkpoint**: la base compila y se ve la estructura; las historias pueden empezar.

---

## Phase 3: User Story 1 - Registrarse e ingresar con correo y contraseña (Priority: P1) 🎯 MVP

**Goal**: crear cuenta, ingresar, mantener la sesión y cerrarla (FR-001 a FR-006, FR-009 a FR-012, FR-023).

**Independent Test**: registrar una cuenta nueva, cerrar y reabrir la app, cerrar sesión e ingresar
de nuevo (quickstart M-1 a M-5).

### Tests for User Story 1 ⚠️

> **Escribir estas pruebas PRIMERO y comprobar que FALLAN antes de implementar.**

- [X] T028 [P] [US1] Prueba `app/src/test/java/com/desconectado/app/domain/ValidacionesTest.kt`: `normalizarCorreo("  Ana@Mail.COM ")` da `ana@mail.com`; `validarCorreo` acepta `a@b.co` y rechaza `sin-arroba`, `a@`, `@b.co`; `validarPassword` rechaza 7 caracteres, acepta 8, acepta espacios y caracteres especiales tal cual; `validarUsername` rechaza 2 caracteres, acepta 3 y 30, rechaza 31, y recorta espacios antes de medir; `validarRegistro` devuelve todos los campos inválidos a la vez con su motivo
- [X] T029 [P] [US1] Prueba `app/src/test/java/com/desconectado/app/domain/NombrePerfilTest.kt` para la función `nombreParaPerfil(nombre: String?, email: String)`: nombre de Google válido se usa recortado; nombre nulo o en blanco usa la parte local del correo; resultado recortado a 30 caracteres (data-model.md: servidor acepta "1 a 30")
- [X] T030 [P] [US1] Crear los dobles `app/src/test/java/com/desconectado/app/fakes/FakeAuthRepository.kt` (implementa todas las operaciones de `AuthRepository`, incluida `restablecerPassword`; resultados y estado de sesión programables, registra las llamadas y permite simular una petición lenta) y `app/src/test/java/com/desconectado/app/fakes/FakeProfileRepository.kt`
- [X] T031 [P] [US1] Prueba `app/src/test/java/com/desconectado/app/ui/auth/RegistroViewModelTest.kt`: con datos inválidos no llama al repositorio y expone el error de cada campo; con datos válidos llama a `registrar` con correo normalizado; `CorreoEnUso` produce el mensaje de correo en uso; sin conexión no llama y expone el aviso; mientras hay una petición en curso ignora un segundo envío (doble toque, un solo `registrar`)
- [X] T032 [P] [US1] Prueba `app/src/test/java/com/desconectado/app/ui/auth/IngresoViewModelTest.kt`: `CredencialesInvalidas` produce un único mensaje genérico que no revela si falló el correo o la contraseña; sin conexión no llama; doble toque ignorado; correo normalizado antes de llamar
- [X] T033 [P] [US1] Prueba `app/src/test/java/com/desconectado/app/ui/auth/SesionViewModelTest.kt`: el estado del repositorio se refleja (`Cargando`, `SinSesion`, `ConSesión`); al iniciar con conexión llama a `verificarCuenta`; sin conexión no la llama y, con la sesión iniciada, el estado sigue siendo `ConSesión` (la sesión no se cierra, FR-011)
- [X] T034 [P] [US1] Prueba de reglas `firebase/tests/rules/users.test.mjs`: el dueño puede crear `users/{uid}` con exactamente `username`, `email`, `createdAt`; se rechaza con un campo extra, con `username` vacío o de 31 caracteres, con `email` distinto (en minúsculas) del correo del token, con `createdAt` distinto de la hora del servidor, con `uid` ajeno y sin autenticar; el dueño lee su documento y no el de otro; nadie modifica ni borra; una ruta no declarada se deniega
- [X] T035 [P] [US1] Prueba instrumentada `app/src/androidTest/java/com/desconectado/app/data/AuthRepositoryEmulatorTest.kt` contra los emuladores: registrar crea cuenta y perfil y deja sesión iniciada; correo repetido da `CorreoEnUso`; contraseña incorrecta y correo inexistente dan `CredencialesInvalidas`; mayúsculas y espacios en el correo se ignoran; `cerrarSesion` deja `SinSesion`; `verificarCuenta` cierra sesión si la cuenta se eliminó en el emulador (usar la API REST del emulador de Auth para borrar el usuario)
- [X] T036 [P] [US1] Prueba de UI `app/src/androidTest/java/com/desconectado/app/ui/AuthScreensTest.kt` con composables sin Firebase: Ingreso y Registro muestran el nombre "(des)Conectado" (FR-023); el campo de contraseña está oculto; un campo inválido muestra su mensaje junto al campo; los botones se deshabilitan sin conexión y aparece `aviso_sin_conexion`; con sesión no son alcanzables y sin sesión solo Ingreso y Registro (FR-012)

### Implementation for User Story 1

- [X] T037 [US1] Implementar `app/src/main/java/com/desconectado/app/domain/Validaciones.kt`: `normalizarCorreo`, `validarCorreo`, `validarPassword` (mínimo 8 caracteres, sin recortar), `validarUsername` (3 a 30 tras recortar), `validarRegistro` y `nombreParaPerfil`; hace pasar T028 y T029
- [X] T038 [US1] Implementar `app/src/main/java/com/desconectado/app/data/profile/FirestoreProfileRepository.kt`: `perfil(uid)` lee `users/{uid}` forzando el servidor; `asegurarPerfil` crea el documento si falta con exactamente los campos `username` (data-model.md: "Registro: 3 a 30 caracteres, recortado. Servidor: 1 a 30"), `email` ("Minúsculas y sin espacios; igual al correo de la cuenta autenticada") y `createdAt` ("Lo fija el servidor al crear; no se modifica", con `FieldValue.serverTimestamp()`); usa `nombreParaPerfil` cuando no hay nombre; mapea errores de red a `SinConexion`
- [X] T039 [US1] Implementar `app/src/main/java/com/desconectado/app/data/auth/FirebaseAuthRepository.kt`: `authState` con `AuthStateListener` (emite `Cargando` hasta el primer resultado); `registrar` (correo normalizado, crea la cuenta y llama a `asegurarPerfil` con el nombre de usuario recortado), `ingresar` (también llama a `asegurarPerfil` por si el registro anterior quedó a medias), `cerrarSesion`, `verificarCuenta` (recarga el usuario y cierra sesión solo si Firebase indica que no existe o está deshabilitado; un fallo de red NO cierra la sesión); mapea `FirebaseAuthUserCollisionException` a `CorreoEnUso`, credenciales inválidas a `CredencialesInvalidas`, `FirebaseNetworkException` a `SinConexion`; dejar `ingresarConGoogle` y `vincularGoogle` devolviendo `Fallo(Desconocido)` hasta US4, y `restablecerPassword` hasta US6
- [X] T040 [US1] Agregar a `firebase/firestore.rules` la regla de `users/{uid}` de contracts/firestore-data.md: leer solo el dueño; crear solo el dueño con exactamente `username`, `email`, `createdAt`, `username` de 1 a 30 caracteres, `email` igual en minúsculas al del token y `createdAt == request.time`; sin actualizar ni borrar; hace pasar T034
- [X] T041 [P] [US1] Crear `app/src/main/res/values/strings_auth.xml` con todos los textos en español rioplatense (voseo, ver Convención de textos) de Ingreso, Registro y Espera: etiquetas de campos, botones, enlaces, mensajes de validación por campo, "Este correo ya está registrado.", "Correo o contraseña incorrectos.", aviso de conexión requerida y error genérico
- [X] T042 [P] [US1] Implementar `app/src/main/java/com/desconectado/app/ui/auth/RegistroViewModel.kt` (estado con campos, errores por campo, `enviando`, mensaje de error; valida con `validarRegistro`; ignora envíos mientras `enviando`; bloquea sin conexión) y hacer pasar T031
- [X] T043 [P] [US1] Implementar `app/src/main/java/com/desconectado/app/ui/auth/IngresoViewModel.kt` con las mismas garantías y el mensaje único de credenciales inválidas; hace pasar T032
- [X] T044 [P] [US1] Implementar `app/src/main/java/com/desconectado/app/ui/auth/SesionViewModel.kt`: expone `EstadoSesion`, llama a `verificarCuenta` al iniciar con conexión; hace pasar T033
- [X] T045 [P] [US1] Crear `app/src/main/java/com/desconectado/app/ui/auth/EsperaScreen.kt` con la marca "(des)Conectado" y un indicador de progreso
- [X] T046 [P] [US1] Crear `app/src/main/java/com/desconectado/app/ui/auth/IngresoScreen.kt` sin estado: marca "(des)Conectado", campos `campo_correo` y `campo_password` (contraseña oculta), botón `boton_ingresar`, enlace `enlace_registro`, errores por campo, `AvisoSinConexion`, botones deshabilitados mientras `enviando` o sin conexión
- [X] T047 [P] [US1] Crear `app/src/main/java/com/desconectado/app/ui/auth/RegistroScreen.kt` sin estado: marca, campos `campo_username`, `campo_correo`, `campo_password` (oculta), botón `boton_registrar`, enlace `enlace_ingreso`, errores por campo, aviso sin conexión
- [X] T048 [US1] Actualizar `app/src/main/java/com/desconectado/app/AppContainer.kt` para construir `FirebaseAuthRepository` y `FirestoreProfileRepository`
- [X] T049 [US1] Crear `app/src/main/java/com/desconectado/app/ui/navigation/AppNavigation.kt` y conectar `MainActivity.kt`: `Cargando` muestra Espera, `SinSesión` muestra el grafo Ingreso/Registro y `ConSesión` muestra `MainShell`, también cuando no hay conexión (con el `aviso_sin_conexion` visible y sin cerrar la sesión); sin sesión no es alcanzable el shell y con sesión no lo son Ingreso ni Registro (FR-012); hace pasar T036
- [ ] T050 [US1] Verificar la historia: correr `./gradlew testDebugUnitTest`, `npm test` en `firebase/` y, con los emuladores activos (`JAVA_HOME` en JDK 21) y el AVD abierto, `./gradlew connectedDebugAndroidTest -PuseEmulator=true`; ejecutar a mano M-1 a M-5 y M-12 de quickstart.md en el emulador y guardar salidas y capturas en `specs/001-auth-profile-catalog/evidencia/us1/`

**Checkpoint**: la Historia 1 funciona y se prueba sola.

---

## Phase 4: User Story 2 - Explorar el catálogo de desafíos (Priority: P2)

**Goal**: mostrar los 6 desafíos "No uses redes sociales por X tiempo" en tres dificultades
(FR-013 a FR-017).

**Independent Test**: iniciar sesión y abrir Desafíos; ver 6 desafíos con todos sus datos y sin
opción de iniciarlos (quickstart M-9, parte de desafíos).

### Tests for User Story 2 ⚠️

- [X] T051 [P] [US2] Prueba de datos `firebase/tests/seed/challenges.test.mjs` sobre `firebase/seed/catalog.json`: al menos 2 desafíos por dificultad; cualquier desafío de dificultad mayor tiene estrictamente más `durationMinutes` y más `points` que cualquiera de dificultad menor (`easy` < `normal` < `hard`); `title` no vacío y que empieza con "No uses redes sociales por"; `description` no vacía; `durationMinutes` y `points` enteros "Mayor que 0"; `difficulty` "Uno de `easy`, `normal`, `hard`"
- [X] T052 [P] [US2] Prueba de reglas `firebase/tests/rules/challenges.test.mjs`: una persona autenticada lee `challenges` y `challenges/{id}`; sin autenticar no lee; ninguna escritura, actualización ni borrado desde la app
- [X] T053 [P] [US2] Prueba `app/src/test/java/com/desconectado/app/domain/FormatoTest.kt` para `formatearDuracion(minutos)`: 30 da "30 minutos", 60 da "1 hora", 120 da "2 horas", 90 da "1 hora 30 minutos"; y `etiquetaDificultad` da "Fácil", "Normal", "Difícil"
- [X] T054 [P] [US2] Crear `app/src/test/java/com/desconectado/app/fakes/FakeCatalogRepository.kt` y la prueba `app/src/test/java/com/desconectado/app/ui/challenges/DesafiosViewModelTest.kt`: pasa de cargando a lista; un fallo produce estado de error con reintento; `SinConexion` produce el estado sin conexión y no carga; reintentar vuelve a pedir
- [X] T055 [P] [US2] Prueba de UI `app/src/androidTest/java/com/desconectado/app/ui/DesafiosScreenTest.kt`: la lista `lista_desafios` muestra título, descripción, duración, puntos y etiqueta de dificultad (texto) de cada tarjeta; existen las tres dificultades; no existe ningún botón de iniciar (FR-017); estado de error con `boton_reintentar`; aviso sin conexión
- [X] T056 [P] [US2] Prueba instrumentada `app/src/androidTest/java/com/desconectado/app/data/CatalogDesafiosEmulatorTest.kt` con el emulador de Firestore sembrado: `desafios()` devuelve 6 desafíos ordenados por `order`; sin conexión (emulador detenido) devuelve `SinConexion` y no datos de caché

### Implementation for User Story 2

- [X] T057 [US2] Crear `firebase/seed/catalog.json` con la clave `challenges` con los 6 valores iniciales del spec: 30 minutos (Fácil, 10), 1 hora (Fácil, 20), 2 horas (Normal, 50), 4 horas (Normal, 100), 8 horas (Difícil, 200), 12 horas (Difícil, 320); cada uno con `id`, `title` ("No uses redes sociales por {tiempo}", p. ej. "No uses redes sociales por 30 minutos"), `description` en español, `durationMinutes`, `difficulty`, `points`, `order`; y `firebase/seed/validate.mjs` con la función que valida las invariantes de T051 (la usan el script y las pruebas); hace pasar T051
- [X] T058 [US2] Crear `firebase/seed/seed.mjs`: valida con `validate.mjs` y aborta si hay errores; con `--emulator` apunta a `FIRESTORE_EMULATOR_HOST=127.0.0.1:8080`; con `--project <id>` usa `firebase-admin` con la credencial indicada en la variable `GOOGLE_APPLICATION_CREDENTIALS`; escribe `challenges/{id}` (y `rewards/{id}` si existe la clave en el JSON) de forma idempotente
- [X] T059 [US2] Agregar a `firebase/firestore.rules` la lectura de `challenges/{id}` para cualquier persona autenticada y ninguna escritura; hace pasar T052
- [X] T060 [P] [US2] Implementar `app/src/main/java/com/desconectado/app/domain/Formato.kt` con `formatearDuracion` y `etiquetaDificultad`; hace pasar T053
- [X] T061 [US2] Implementar `desafios()` en `app/src/main/java/com/desconectado/app/data/catalog/FirestoreCatalogRepository.kt`: consulta `challenges` ordenada por `order` ascendente forzando la lectura desde el servidor; mapea los valores de almacén de `difficulty` a `Dificultad`; error de red a `SinConexion`; `recompensas()` devuelve `Fallo(Desconocido)` hasta US5; registrarlo en `AppContainer.kt`
- [X] T062 [P] [US2] Crear `app/src/main/res/values/strings_desafios.xml` con el título de la sección, etiquetas Fácil/Normal/Difícil, formato de puntos y de duración
- [X] T063 [US2] Implementar `app/src/main/java/com/desconectado/app/ui/challenges/DesafiosViewModel.kt` (estados cargando, lista, error y sin conexión; reintento); hace pasar T054
- [X] T064 [US2] Reemplazar la pantalla provisoria `app/src/main/java/com/desconectado/app/ui/challenges/DesafiosScreen.kt`: `lista_desafios` con una tarjeta por desafío (título, descripción, duración con `formatearDuracion`, puntos y etiqueta de dificultad con texto **y** color), sin botón de iniciar; conectarla en `ui/navigation/MainShell.kt`; hace pasar T055
- [ ] T065 [US2] Verificar la historia: `npm run test:seed` y `npm test` en `firebase/`, sembrar el emulador con `node firebase/seed/seed.mjs --emulator`, correr pruebas JVM e instrumentadas, ejecutar a mano la parte de desafíos de M-9 y guardar evidencia en `specs/001-auth-profile-catalog/evidencia/us2/`

**Checkpoint**: las Historias 1 y 2 funcionan de forma independiente.

---

## Phase 5: User Story 3 - Ver mi perfil (Priority: P2)

**Goal**: mostrar los datos del registro sin contraseña y permitir cerrar sesión (FR-006, FR-010,
FR-021, FR-022).

**Independent Test**: registrar una cuenta con datos conocidos, abrir Perfil y comprobar que
coinciden y que la contraseña no aparece (quickstart M-10, cuenta de correo).

### Tests for User Story 3 ⚠️

- [X] T066 [P] [US3] Prueba `app/src/test/java/com/desconectado/app/ui/profile/PerfilViewModelTest.kt`: carga el perfil de la sesión actual y expone nombre de usuario y correo; error con reintento; sin conexión; `cerrarSesion` llama al repositorio
- [X] T067 [P] [US3] Prueba de UI `app/src/androidTest/java/com/desconectado/app/ui/PerfilScreenTest.kt`: `texto_username` y `texto_email` muestran los datos recibidos; existe `boton_cerrar_sesion`; no aparece ningún campo ni texto de contraseña
- [X] T068 [P] [US3] Prueba instrumentada de flujo `app/src/androidTest/java/com/desconectado/app/ui/PerfilFlowEmulatorTest.kt` contra los emuladores: registrar con nombre "Ana Prueba", correo "ana@mail.com" y contraseña "Secreto123"; abrir Perfil; verificar nombre y correo exactos y que `onNodeWithText("Secreto123")` no existe; cerrar sesión lleva a Ingreso

### Implementation for User Story 3

- [X] T069 [P] [US3] Crear `app/src/main/res/values/strings_perfil.xml` con título, etiquetas de nombre y correo, "Cerrar sesión" y mensajes de error
- [X] T070 [US3] Implementar `app/src/main/java/com/desconectado/app/ui/profile/PerfilViewModel.kt` usando `AuthRepository.authState` para el `uid` y `ProfileRepository.perfil`; hace pasar T066
- [X] T071 [US3] Reemplazar la pantalla provisoria `app/src/main/java/com/desconectado/app/ui/profile/PerfilScreen.kt`: `texto_username`, `texto_email`, `boton_cerrar_sesion`, estados de carga, error y sin conexión; conectarla en `MainShell.kt`; hace pasar T067 y T068
- [ ] T072 [US3] Verificar la historia: correr las pruebas de US3, ejecutar a mano M-10 (cuenta de correo) y guardar evidencia en `specs/001-auth-profile-catalog/evidencia/us3/`

**Checkpoint**: Historias 1, 2 y 3 funcionan de forma independiente.

---

## Phase 6: User Story 4 - Ingresar con Google (Priority: P2)

**Goal**: registrarse e ingresar con una cuenta de Google en un paso (FR-007, FR-008, FR-022).

**Independent Test**: en un dispositivo o emulador con Google Play, "Continuar con Google" sin
cuenta previa, luego repetirlo con la sesión cerrada (quickstart M-6 a M-8 y T-VERIF-1).

### Tests for User Story 4 ⚠️

- [X] T073 [P] [US4] Prueba instrumentada `app/src/androidTest/java/com/desconectado/app/data/AuthGoogleEmulatorTest.kt` con el emulador de Auth y tokens de Google falsos: un token nuevo crea cuenta y perfil con el nombre del token; repetirlo devuelve el mismo `uid`; un token sin nombre crea el perfil con la parte local del correo; un token con el mismo correo que una cuenta de contraseña existente o devuelve el mismo `uid` o `CuentaExistenteConOtroProveedor`, y nunca crea una cuenta duplicada (aproximación automatizada de T-VERIF-1; el comportamiento real se confirma en T082)
- [X] T074 [P] [US4] Prueba `app/src/test/java/com/desconectado/app/ui/auth/GoogleFlowTest.kt` sobre `IngresoViewModel` y `RegistroViewModel`: `Cancelado` no muestra error ni cambia el estado; token obtenido llama a `ingresarConGoogle`; `CuentaExistenteConOtroProveedor` abre el estado de "vincular" pidiendo la contraseña, y tras ingresar con contraseña llama a `vincularGoogle`; sin conexión bloquea
- [X] T075 [P] [US4] Prueba de UI `app/src/androidTest/java/com/desconectado/app/ui/GoogleScreensTest.kt`: `boton_google` existe en Ingreso y en Registro, se deshabilita sin conexión; el diálogo de vinculación muestra el mensaje "Ya tenés una cuenta con este correo…" y pide la contraseña

### Implementation for User Story 4

- [X] T076 [US4] Implementar en `app/src/main/java/com/desconectado/app/data/auth/FirebaseAuthRepository.kt` `ingresarConGoogle(idToken)` (crea la credencial con `GoogleAuthProvider.getCredential`, inicia sesión y llama a `asegurarPerfil` con el nombre y el correo de la cuenta) y `vincularGoogle(idToken)` (`linkWithCredential` sobre el usuario ya autenticado); ante `FirebaseAuthUserCollisionException` conserva la credencial pendiente y devuelve `CuentaExistenteConOtroProveedor`; `Cancelado` no lo produce el repositorio
- [X] T077 [P] [US4] Crear `app/src/main/java/com/desconectado/app/data/auth/GoogleCredentialProvider.kt`: obtiene el token de identidad con `CredentialManager` y `GetGoogleIdOption` usando el recurso `default_web_client_id` (generado por el plugin `google-services`), con `setFilterByAuthorizedAccounts(false)`; requiere un `Context` de actividad; devuelve `Resultado<String>`: cancelación del usuario a `Cancelado`, falta de red a `SinConexion`, sin cuentas de Google en el dispositivo a `Desconocido`
- [X] T078 [P] [US4] Crear `app/src/main/res/values/strings_google.xml`: "Continuar con Google", mensaje y botones del diálogo de vinculación, error si no hay cuenta de Google
- [X] T079 [US4] Agregar `continuarConGoogle` y el estado de vinculación a `app/src/main/java/com/desconectado/app/ui/auth/IngresoViewModel.kt` y `app/src/main/java/com/desconectado/app/ui/auth/RegistroViewModel.kt`; hace pasar T074
- [X] T080 [US4] Agregar el botón `boton_google` y el diálogo de vinculación a `app/src/main/java/com/desconectado/app/ui/auth/IngresoScreen.kt` y `app/src/main/java/com/desconectado/app/ui/auth/RegistroScreen.kt`, obteniendo el token con `GoogleCredentialProvider` desde el `Context` de la actividad; hace pasar T075
- [X] T081 [US4] Registrar `GoogleCredentialProvider` en `app/src/main/java/com/desconectado/app/AppContainer.kt` y verificar que el flujo automatizado de T073 y T074 pasa
- [ ] T082 [US4] (MANUAL) Con el proyecto Firebase real (T002) y un dispositivo o emulador con Google Play: ejecutar M-6, M-7, M-8, M-10 (cuenta de Google) y T-VERIF-1 de quickstart.md; registrar el resultado de T-VERIF-1 en `specs/001-auth-profile-catalog/evidencia/us4/` y, si Firebase no unifica solo, confirmar que el camino de vinculación funciona

**Checkpoint**: las historias 1 a 4 funcionan de forma independiente.

---

## Phase 7: User Story 5 - Explorar el catálogo de recompensas (Priority: P3)

**Goal**: mostrar las 5 recompensas genéricas, sin acción de canje (FR-018 a FR-020).

**Independent Test**: iniciar sesión y abrir Recompensas; ver 5 recompensas con nombre,
descripción y costo, sin botón de canje (quickstart M-9, parte de recompensas).

### Tests for User Story 5 ⚠️

- [X] T083 [P] [US5] Prueba de datos `firebase/tests/seed/rewards.test.mjs` sobre `firebase/seed/catalog.json`: al menos 5 recompensas; `name` y `description` no vacíos; `costPoints` entero "Mayor que 0"; `kind` "Uno de `badge`, `theme`, `coupon`"; ninguna recompensa de tipo `coupon` menciona comercios, locales ni descuentos reales (el texto no coincide con `/comercio|local(es)?|tienda física|descuento real/i`)
- [X] T084 [P] [US5] Prueba de reglas `firebase/tests/rules/rewards.test.mjs`: una persona autenticada lee `rewards`; sin autenticar no lee; ninguna escritura desde la app
- [X] T085 [P] [US5] Prueba `app/src/test/java/com/desconectado/app/ui/rewards/RecompensasViewModelTest.kt`: carga y ordena por `order`; error con reintento; sin conexión no carga
- [X] T086 [P] [US5] Prueba de UI `app/src/androidTest/java/com/desconectado/app/ui/RecompensasScreenTest.kt`: `lista_recompensas` muestra nombre, descripción y costo en puntos de cada tarjeta; no existe ninguna acción de canje (FR-020); textos sin mención de beneficios fuera de la app (FR-019)

### Implementation for User Story 5

- [X] T087 [US5] Agregar la clave `rewards` a `firebase/seed/catalog.json` con las 5 recompensas del spec: Insignia "Primer paso" (`badge`, 50), Tema de color para la app (`theme`, 100), Cupón digital "Descuento de ejemplo" (`coupon`, 200), Insignia "Semana desconectada" (`badge`, 300), Cupón digital "Regalo sorpresa" (`coupon`, 500), con descripciones en español que dejen claro que son elementos digitales dentro de la app y sin valor fuera de ella; extender `firebase/seed/validate.mjs` con las validaciones de T083; hace pasar T083
- [X] T088 [US5] Agregar a `firebase/firestore.rules` la lectura de `rewards/{id}` para cualquier persona autenticada y ninguna escritura; hace pasar T084
- [X] T089 [US5] Implementar `recompensas()` en `app/src/main/java/com/desconectado/app/data/catalog/FirestoreCatalogRepository.kt` (ordenadas por `order`, lectura desde el servidor, error de red a `SinConexion`) y extender `app/src/androidTest/java/com/desconectado/app/data/CatalogDesafiosEmulatorTest.kt` con una comprobación de las 5 recompensas
- [X] T090 [P] [US5] Crear `app/src/main/res/values/strings_recompensas.xml` con título de la sección, formato de costo en puntos y aclaración de que las recompensas son digitales y solo de la app
- [X] T091 [US5] Implementar `app/src/main/java/com/desconectado/app/ui/rewards/RecompensasViewModel.kt` (mismos estados que Desafíos); hace pasar T085
- [X] T092 [US5] Reemplazar la pantalla provisoria `app/src/main/java/com/desconectado/app/ui/rewards/RecompensasScreen.kt`: `lista_recompensas` con una tarjeta por recompensa (nombre, descripción, costo), sin botón de canje; conectarla en `MainShell.kt`; hace pasar T086
- [ ] T093 [US5] Verificar la historia: `npm run test:seed`, `npm test`, sembrar el emulador, correr pruebas JVM e instrumentadas, ejecutar a mano la parte de recompensas de M-9 y guardar evidencia en `specs/001-auth-profile-catalog/evidencia/us5/`

**Checkpoint**: las historias 1 a 5 funcionan de forma independiente.

---

## Phase 8: User Story 6 - Recuperar mi contraseña (Priority: P3)

**Goal**: pedir el restablecimiento desde Ingreso y recibir un correo en español para elegir una
contraseña nueva (FR-025, FR-026; SC-009).

**Independent Test**: con una cuenta de correo existente, solicitar el restablecimiento, abrir el
enlace recibido, elegir una contraseña nueva e ingresar con ella; la anterior deja de funcionar
(quickstart M-11).

### Tests for User Story 6 ⚠️

- [X] T094 [P] [US6] Prueba `app/src/test/java/com/desconectado/app/ui/auth/RestablecerPasswordViewModelTest.kt`: un correo con formato inválido no llama a `restablecerPassword` y expone el error junto al campo; un correo válido llama con el correo normalizado (recortado y en minúsculas); un éxito muestra el mensaje de confirmación, y el **mismo** mensaje aparece cuando el repositorio devuelve éxito para una cuenta inexistente (FR-026); sin conexión no llama y expone el aviso; un segundo envío mientras el primero está en curso se ignora (un solo `restablecerPassword`); `SinConexion` y `Desconocido` muestran su mensaje de error
- [X] T095 [P] [US6] Prueba de UI `app/src/androidTest/java/com/desconectado/app/ui/RestablecerPasswordScreenTest.kt` con composables sin Firebase: Ingreso muestra `enlace_olvide_password`; la pantalla de restablecer muestra `campo_correo_restablecer` y `boton_enviar_restablecimiento`; tras enviar aparece `mensaje_restablecimiento_enviado` con el texto en voseo; el error de formato aparece junto al campo; el botón se deshabilita sin conexión y con envío en curso
- [X] T096 [P] [US6] Prueba instrumentada `app/src/androidTest/java/com/desconectado/app/data/PasswordResetEmulatorTest.kt` contra el emulador de Auth: crear una cuenta con contraseña "Original123"; `restablecerPassword` devuelve éxito; leer el código desde la API REST del emulador (`GET /emulator/v1/projects/<proyecto>/oobCodes`) y completar el restablecimiento con `POST /identitytoolkit.googleapis.com/v1/accounts:resetPassword` a la contraseña "Nueva12345"; `ingresar` con "Nueva12345" funciona y con "Original123" da `CredencialesInvalidas`; un correo sin cuenta devuelve éxito y no genera ningún código nuevo (FR-026); con el emulador detenido devuelve `SinConexion`

### Implementation for User Story 6

- [X] T097 [US6] Implementar `restablecerPassword(email)`: agregarlo si falta a `app/src/main/java/com/desconectado/app/domain/repository/AuthRepository.kt` y a `app/src/test/java/com/desconectado/app/fakes/FakeAuthRepository.kt`, e implementarlo en `app/src/main/java/com/desconectado/app/data/auth/FirebaseAuthRepository.kt`: normaliza el correo, fija el código de idioma de Auth en español antes de enviar, llama a `sendPasswordResetEmail`, convierte el error de usuario inexistente en éxito (FR-026), mapea el fallo de red a `SinConexion` y cualquier otro a `Desconocido`; hace pasar T096
- [X] T098 [P] [US6] Crear `app/src/main/res/values/strings_restablecer.xml` con los textos en voseo: enlace "Olvidé mi contraseña", título y campo de correo, botón de envío, mensaje de confirmación ("Si el correo está registrado, te enviamos un enlace para elegir una contraseña nueva."), error de formato de correo, aviso de conexión requerida y enlace para volver a Ingreso
- [X] T099 [US6] Implementar `app/src/main/java/com/desconectado/app/ui/auth/RestablecerPasswordViewModel.kt` (validación con `validarCorreo`, estado `enviando`, mensaje de confirmación, bloqueo sin conexión); hace pasar T094
- [X] T100 [P] [US6] Crear `app/src/main/java/com/desconectado/app/ui/auth/RestablecerPasswordScreen.kt` sin estado: `campo_correo_restablecer`, `boton_enviar_restablecimiento`, `mensaje_restablecimiento_enviado`, enlace para volver a Ingreso, `AvisoSinConexion`
- [X] T101 [US6] Agregar el enlace `enlace_olvide_password` a `app/src/main/java/com/desconectado/app/ui/auth/IngresoScreen.kt`, registrar `RestablecerPasswordViewModel` en `app/src/main/java/com/desconectado/app/AppContainer.kt` y agregar la ruta Ingreso a Restablecer y de vuelta en `app/src/main/java/com/desconectado/app/ui/navigation/AppNavigation.kt` (alcanzable solo sin sesión, FR-012); hace pasar T095
- [ ] T102 [US6] (MANUAL) En la consola de Firebase, Authentication, Plantillas, personalizar el correo de **restablecimiento de contraseña** en español con voseo y con el nombre "(des)Conectado" (FR-023, FR-025); confirmar el remitente y que la protección contra enumeración de correos coincide con lo esperado (research.md D-17)
- [X] T103 [US6] Verificar la historia: correr las pruebas de US6 (JVM, UI y emulador de Auth) y guardar sus salidas en `specs/001-auth-profile-catalog/evidencia/us6/`; el correo real y M-11 se ejecutan con el APK en T111

**Checkpoint**: las seis historias funcionan de forma independiente.

---

## Phase 9: Polish, Evidencia y APK (Cross-Cutting)

**Purpose**: dejar la entrega lista y generar el APK para instalar en el dispositivo.

- [X] T104 [P] Revisar que todos los textos visibles estén en español rioplatense con "vos" (sin "tú" ni "usted": buscar "Ingresa", "Crea", "tienes", "puedes" y similares) y que el nombre se escriba "(des)Conectado" en Ingreso y Registro (FR-023): buscar cadenas en inglés o literales fuera de `app/src/main/res/values/strings*.xml` en `app/src/main/java/com/desconectado/app/ui/`
- [X] T105 [P] Crear `README.md` en la raíz con los pasos para compilar, correr pruebas (JVM, reglas, emuladores) y generar el APK, referenciando `specs/001-auth-profile-catalog/quickstart.md`
- [ ] T106 (MANUAL) Crear la keystore de firma del APK: `keytool -genkeypair -v -keystore keystore/desconectado-release.jks -alias desconectado -keyalg RSA -keysize 2048 -validity 10000`, crear `keystore.properties` en la raíz (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`; ambos archivos están ignorados por git), copiar el SHA-1 de esa keystore (`keytool -list -v`) a la app Android en la consola de Firebase y **volver a descargar** `app/google-services.json`. Guardar una copia de la keystore fuera del repositorio: sin ella no se puede actualizar el APK con la misma firma
- [ ] T107 (MANUAL) Publicar reglas y catálogos en el proyecto real: descargar una clave de cuenta de servicio (consola de Firebase, Configuración del proyecto, Cuentas de servicio) a `firebase/service-account.json` (ignorada por git); ejecutar `firebase deploy --only firestore:rules --project <id>` y, con `GOOGLE_APPLICATION_CREDENTIALS=firebase/service-account.json`, `node firebase/seed/seed.mjs --project <id>`; comprobar en la consola que existen 6 desafíos y 5 recompensas
- [ ] T108 Generar el APK: `./gradlew assembleRelease` (sin `-PuseEmulator`); verificar que existe `app/build/outputs/apk/release/app-release.apk`, comprobar la firma con `apksigner verify --print-certs` (de las build-tools del SDK) y que el SHA-1 coincide con el de T106; copiarlo a `dist/desConectado-entrega1.apk` (carpeta ignorada por git)
- [ ] T109 (MANUAL) Instalar el APK en el dispositivo real: activar las opciones de desarrollador y la depuración USB, ejecutar `adb install -r dist/desConectado-entrega1.apk` (o copiar el archivo al teléfono y permitir la instalación desde orígenes desconocidos); confirmar que la app abre y muestra Ingreso
- [X] T110 Ejecutar todas las suites y guardar sus salidas en `specs/001-auth-profile-catalog/evidencia/automatizadas/`: `./gradlew testDebugUnitTest`, `npm test` y `npm run test:seed` en `firebase/`, y `./gradlew connectedDebugAndroidTest -PuseEmulator=true` con los emuladores activos; todas deben quedar en verde
- [ ] T111 (MANUAL) Con el APK de T109 y el proyecto real, ejecutar en el dispositivo M-1 a M-12 y T-VERIF-1 de quickstart.md, midiendo SC-001 (registro en menos de 2 minutos), SC-002 (ingreso en menos de 30 segundos), SC-003 (Google en menos de 30 segundos y sin escribir datos) y SC-009 (correo de restablecimiento en menos de 2 minutos e ingreso con la contraseña nueva en menos de 5); guardar capturas y resultados en `specs/001-auth-profile-catalog/evidencia/dispositivo/`
- [ ] T112 Completar la matriz de evidencia de `specs/001-auth-profile-catalog/quickstart.md` (sección 5) con el resultado y el enlace a la evidencia de cada requisito, incluidos FR-025 y FR-026 (FR-024, SC-008), y registrar los tiempos medidos de SC-001, SC-002, SC-003 y SC-009
- [ ] T113 Revisión final contra `.specify/memory/constitution.md` v2.1.0: sin permisos distintos de `INTERNET` y `ACCESS_NETWORK_STATE`, sin cámara ni bloqueo, sin Cloud Functions, sin publicación en Google Play, sin funciones de "Fuera de alcance" (canje, iniciar desafíos, recuperación por teléfono o SMS); anotar el resultado en `specs/001-auth-profile-catalog/evidencia/revision-constitucion.md`

**Checkpoint final**: `dist/desConectado-entrega1.apk` instalado y validado en el dispositivo, con la
evidencia de cada requisito referenciada.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)**: sin dependencias. T001 y T002 son manuales y bloquean la compilación real
  (T002 aporta `google-services.json`, sin el cual falla el plugin de Firebase).
- **Foundational (Fase 2)**: depende de la Fase 1 y BLOQUEA todas las historias. Dentro de la fase:
  T011 y T015 antes de T016 a T019; T021 antes de T024.
- **Historias (Fase 3 a 8)**: dependen de la Fase 2.
- **Polish (Fase 9)**: depende de las historias que se quieran entregar; T106 y T107 antes de T108
  y de T111; T102 (plantilla del correo) antes de T111 para que M-11 pruebe el texto final.

### User Story Dependencies

- **US1 (P1)**: tras Fase 2; sin dependencias de otras historias.
- **US2 (P2)**: tras Fase 2; necesita una sesión iniciada para verse, por lo que en la práctica se
  prueba con US1 hecha, pero sus datos y su lógica son independientes.
- **US3 (P2)**: tras Fase 2; usa `AuthRepository` y `ProfileRepository` de US1.
- **US4 (P2)**: tras US1 (extiende `FirebaseAuthRepository`, `IngresoScreen` y `RegistroScreen`).
- **US5 (P3)**: tras Fase 2; extiende `FirestoreCatalogRepository`, `catalog.json` y
  `firestore.rules` creados en US2, por lo que va después de US2.
- **US6 (P3)**: tras US1 (extiende `AuthRepository`, `FirebaseAuthRepository`, `IngresoScreen` y
  `AppNavigation`); comparte archivos con US4, por lo que si se hacen en paralelo hay que coordinar
  `FirebaseAuthRepository.kt`, `IngresoScreen.kt` y `AppContainer.kt`.

### Within Each User Story

- Las pruebas se escriben y deben FALLAR antes de implementar (Principio IV).
- Modelos y validaciones, luego repositorios, luego ViewModels, luego pantallas, luego navegación.
- Las tareas que editan el mismo archivo (`AppContainer.kt`, `firestore.rules`,
  `FirebaseAuthRepository.kt`, `FirestoreCatalogRepository.kt`, `catalog.json`, `MainShell.kt`) no
  llevan `[P]` y van en el orden de sus historias.

### Parallel Opportunities

- Setup: T007, T008 y T009 en paralelo.
- Fase 2: T012 a T015 en paralelo; T016 a T019 en paralelo (tras T011 y T015); T022, T023, T025 y
  T026 en paralelo.
- Cada historia: todas sus pruebas `[P]` en paralelo; los ViewModels y pantallas `[P]` en paralelo.
- Con más de una persona: tras la Fase 2, una persona en US1 (y luego US4 y US6, que extienden sus
  archivos) y otra en US2 y US5 (datos y catálogos), reuniéndose en US3.

---

## Parallel Example: User Story 1

```bash
# Pruebas de US1 en paralelo (archivos distintos):
Task: "T028 ValidacionesTest en app/src/test/java/com/desconectado/app/domain/ValidacionesTest.kt"
Task: "T031 RegistroViewModelTest en app/src/test/java/com/desconectado/app/ui/auth/RegistroViewModelTest.kt"
Task: "T034 users.test.mjs en firebase/tests/rules/users.test.mjs"

# ViewModels y pantallas de US1 en paralelo:
Task: "T042 RegistroViewModel en app/src/main/java/com/desconectado/app/ui/auth/RegistroViewModel.kt"
Task: "T046 IngresoScreen en app/src/main/java/com/desconectado/app/ui/auth/IngresoScreen.kt"
Task: "T047 RegistroScreen en app/src/main/java/com/desconectado/app/ui/auth/RegistroScreen.kt"
```

---

## Implementation Strategy

### MVP First (solo User Story 1)

1. Fase 1 (Setup) y Fase 2 (Foundational).
2. Fase 3 (US1).
3. **DETENERSE Y VALIDAR**: cuenta, sesión persistente y cierre de sesión en el emulador.

### Entrega completa del 24/09 (lo que se pidió)

1. Setup, Foundational y US1.
2. US2, US3 y US5 (catálogos y perfil): todas de lectura, rápidas de sumar.
3. US4 (Google) y US6 (recuperación de contraseña): las de mayor riesgo por depender de
   configuración externa (Google, correo real); hacerlas con la consola de Firebase ya lista (T002).
4. Fase 9: keystore, publicación de reglas y catálogos, APK, instalación en el dispositivo y
   evidencia.

### Riesgos de calendario (quedan 5 días hasta el 24/09)

- T001 y T002 son manuales y bloquean todo lo demás: hacerlas primero.
- Si el tiempo apremia, el orden de recorte es: US4 (Google), US6 (recuperación) y US5
  (recompensas), en ese orden, antes que US3 (perfil). US1 y US2 son imprescindibles para la
  entrega. Recortar una historia exige una enmienda al spec, no un olvido silencioso.
- US6 depende de que el correo real llegue: probarlo pronto con el proyecto real (research.md R-6).

---

## Notes

- `[P]` = archivos distintos, sin dependencias.
- `[Story]` liga cada tarea a una historia del spec.
- Verificar que las pruebas fallan antes de implementar.
- Hacer un commit tras cada tarea o grupo lógico.
- Detenerse en cada checkpoint para validar la historia por separado.
- Evitar: tareas vagas, conflictos de archivo y dependencias entre historias que rompan su
  independencia.
