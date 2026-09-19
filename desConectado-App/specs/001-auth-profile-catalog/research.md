# Research: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Fecha**: 2026-09-19 | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

Resuelve el `TODO(STACK)` de la constitución y todo lo que el Technical Context dejaba abierto.
Ninguna decisión se verificó contra documentación en línea en esta sesión; las que dependen del
comportamiento de un servicio externo están marcadas con **[Verificar]** y tienen una tarea de
comprobación en `quickstart.md`.

## Estado del entorno de desarrollo (verificado 2026-09-19)

| Herramienta        | Estado                                                         |
|--------------------|----------------------------------------------------------------|
| JDK                | 17 (Temurin, primero en PATH) y 21 (Temurin) instalados        |
| Node / npm         | 24.15 / 11.12 instalados                                       |
| Android Studio/SDK | **No instalado** (sin `ANDROID_HOME`, sin `adb`)               |
| Firebase CLI       | **No instalado**                                               |
| Flutter            | No instalado                                                   |

Consecuencia: antes de escribir código hay que instalar Android Studio (SDK + imagen de emulador
con Google Play) y Firebase CLI. Es el mayor riesgo de calendario de esta entrega (ver R-1).

## D-1. Framework: Kotlin + Jetpack Compose

- **Decisión**: app Android nativa en Kotlin con Jetpack Compose y Material 3.
- **Razón**:
  - El Principio II exige app Android nativa, no PWA.
  - La entrega del 01/10 necesita medir el uso de otras apps (API nativa de estadísticas de uso de
    Android); en Kotlin es una llamada directa, en Flutter o React Native exige escribir un módulo
    nativo igualmente.
  - Los SDK de Firebase y de Google Sign-In son de primera mano en Kotlin.
  - Compose reduce la cantidad de código de UI frente a vistas XML.
- **Alternativas**:
  - *Flutter*: buen rendimiento de UI, pero no está instalado y la medición de uso requiere código
    nativo de todos modos.
  - *React Native / Expo*: Node ya está instalado, pero Expo Go no soporta la medición de uso y un
    build de desarrollo exige el SDK de Android igual.
  - *Vistas XML + Kotlin*: nativo, pero más código y menos ergonomía.
- **Riesgo**: se asume que el equipo puede trabajar en Kotlin. Si no, esta decisión es la primera a
  revisar.

## D-2. Versiones y nivel de API

- **Decisión**: `minSdk` 26 (Android 8.0); `targetSdk` y `compileSdk` a la versión estable más
  reciente al crear el proyecto. Versiones de AGP, Kotlin, Compose BOM y Firebase BoM fijadas en
  `gradle/libs.versions.toml` con las últimas estables disponibles al inicializar.
- **Razón**: API 26 introduce los canales de notificación, necesarios en la entrega del 08/10, y
  cubre la gran mayoría de los dispositivos activos. Compose y el gestor de credenciales de Google
  funcionan con margen a partir de ahí.
- **Alternativa**: `minSdk` 24 o 21: más dispositivos, pero obliga a ramas de compatibilidad para
  notificaciones sin beneficio para el MVP.

## D-3. Backend: Firebase, plan gratuito (Spark)

- **Decisión**: Firebase Authentication (correo y contraseña, y Google) + Cloud Firestore. Sin Cloud
  Functions en esta entrega.
- **Razón**: lo definió el equipo; cubre autenticación, persistencia de sesión y base de datos sin
  servidor propio; el plan gratuito alcanza para esta entrega.
- **Alternativas**: backend propio (más trabajo sin beneficio); Supabase (equivalente, pero el
  equipo ya eligió Firebase).
- **Advertencia para la entrega del 01/10 (R-2)**: el Principio VI exige que acreditar y canjear
  puntos sea autoritativo en el backend. Eso normalmente se resuelve con Cloud Functions, que exigen
  el plan de pago (Blaze, con tarjeta). La alternativa sin costo es imponer las reglas con
  transacciones y reglas de seguridad de Firestore, con garantías más débiles. Hay que decidirlo
  antes de planificar la entrega 2.

## D-4. Ingreso con Google: gestor de credenciales de Android

- **Decisión**: obtener el token de identidad de Google con la API Credential Manager
  (`GetGoogleIdOption` / `GetSignInWithGoogleOption`) y canjearlo en Firebase Auth con
  `GoogleAuthProvider.getCredential`.
- **Razón**: es el flujo vigente; las bibliotecas anteriores de Google Sign-In están en desuso.
  Requiere el ID de cliente web que Firebase crea al habilitar Google como proveedor, y registrar la
  huella SHA-1 de cada keystore de depuración en el proyecto de Firebase.
- **Alternativa**: `GoogleSignInClient` clásico: en desuso.
- **Prueba**: el flujo con la pantalla real de Google solo se puede probar a mano en un dispositivo
  o emulador con Google Play. La capa que recibe el token se prueba automatizada con el emulador de
  Auth, que acepta tokens de Google falsos.

## D-5. Una cuenta por correo (FR-008) [Verificar]

- **Decisión**: mantener activada la opción de Firebase "una cuenta por dirección de correo".
  Con ella es imposible duplicar cuentas: si el correo ya existe con contraseña, el ingreso con
  Google o bien se enlaza a esa cuenta, o bien falla con una colisión.
- **Manejo**: si Firebase devuelve una colisión de credenciales, la app muestra "Ya tenés una cuenta
  con este correo. Ingresá con tu contraseña y vinculamos Google" y, tras el ingreso con
  contraseña, llama a `linkWithCredential` con la credencial de Google pendiente.
- **[Verificar]**: comportamiento exacto de Firebase para un correo con contraseña ya verificado o
  no verificado. Tarea de comprobación T-VERIF-1 en `quickstart.md` contra el proyecto real. El
  resultado decide si el camino de colisión es necesario o si Firebase unifica solo.

## D-6. Datos: perfil y catálogos en Firestore

- **Decisión**:
  - `users/{uid}` guarda `username`, `email` y `createdAt`; lo crea el cliente tras autenticarse, y
    si falta al iniciar sesión (registro interrumpido, cuenta de Google nueva) se crea entonces.
  - `challenges/{id}` y `rewards/{id}` son de solo lectura para usuarios autenticados y se cargan
    con un script de siembra que usa credenciales de administrador.
- **Razón**: la constitución fija el backend como fuente de verdad y las entregas siguientes
  necesitan `users/{uid}` para puntos e historial. Los catálogos en la nube evitan republicar la app
  para ajustar puntos o duraciones (los valores del spec son iniciales y ajustables).
- **Alternativas**:
  - *Solo `displayName` de Firebase Auth*: evita Firestore hoy, pero no sirve de base para puntos.
  - *Catálogos empaquetados en la app*: más simple, pero contradice "contenido provisto por la
    plataforma" y obliga a publicar la app para cada ajuste.
- **Nombre de usuario de una cuenta de Google**: el nombre que entrega Google; si viene vacío, la
  parte local del correo (recortada a 30 caracteres). Las reglas del servidor aceptan de 1 a 30
  caracteres; el mínimo de 3 solo se impone en el formulario de registro.

## D-7. Política sin conexión (FR-011)

- **Decisión**: Firestore con caché solo en memoria; los catálogos se leen forzando el servidor; un
  observador de conectividad publica el estado de red y la UI bloquea registro, ingreso y carga
  cuando no hay conexión.
- **Razón**: por defecto Firestore en Android guarda una caché en disco y serviría catálogos sin
  conexión, lo que contradice FR-011 y la constitución. Firebase Auth conserva la sesión sin red, así
  que con la app abierta sin conexión el usuario ve la pantalla principal con un aviso y las
  secciones bloqueadas, en vez de una pantalla rota.
- **Alternativa**: permitir catálogos en caché: más amable, pero incumple el requisito.

## D-8. Sesión persistente y cuenta eliminada (FR-005)

- **Decisión**: Firebase Auth persiste la sesión entre reinicios. Al iniciar con conexión se ejecuta
  una recarga del usuario; si Firebase responde que la cuenta ya no existe o fue deshabilitada, se
  cierra la sesión y se vuelve al ingreso.
- **Razón**: cubre el caso límite "cuenta inexistente con sesión guardada" del spec.

## D-9. Arquitectura de la app

- **Decisión**: un solo módulo Gradle (`app`), MVVM con `ViewModel` + `StateFlow`, Navigation
  Compose, y una capa de datos tras interfaces (`AuthRepository`, `ProfileRepository`,
  `CatalogRepository`, `ConnectivityMonitor`). Inyección de dependencias manual con un contenedor
  creado en la clase `Application`. Sin Hilt ni multi-módulo.
- **Razón**: Principio V (YAGNI). Las interfaces permiten probar los `ViewModel` con dobles de
  prueba, sin Firebase (Principio IV). La inyección manual evita configuración y tiempo de
  compilación que no aportan en 5 días.
- **Alternativa**: Hilt: estándar en proyectos grandes, pero es sobrecarga con 4 dependencias.

## D-10. Validaciones como lógica pura

- **Decisión**: las reglas de registro (correo, contraseña de 8 o más caracteres, usuario de 3 a 30,
  normalización de correo) viven en funciones Kotlin puras en `domain/`, sin dependencia de Android
  ni Firebase, desarrolladas test-first.
- **Razón**: Principio IV exige test-first para lógica de negocio; las funciones puras se prueban en
  milisegundos y sin dispositivo.

## D-11. Estrategia de pruebas y evidencia (Principio IV)

| Capa                              | Herramienta                                              | Cubre                            |
|-----------------------------------|----------------------------------------------------------|----------------------------------|
| Unitarias (JVM)                   | JUnit 4, kotlinx-coroutines-test, Turbine, dobles propios | Validaciones, ViewModels, estado |
| Reglas de seguridad de Firestore  | `@firebase/rules-unit-testing` + emulador (Node)         | FR-003 de datos, FR-021, catálogos de solo lectura |
| Datos sembrados                   | Prueba Node sobre `catalog.json`                         | FR-013, FR-014, FR-015, FR-018   |
| UI instrumentada                  | Compose UI Test + emuladores de Auth y Firestore         | Flujos de las historias 1, 2, 3, 5 |
| Ingreso con Google                | Emulador de Auth con token falso (automatizada) + manual con Google real | Historia 4    |
| Manual documentado                | Guion en `quickstart.md` con captura y resultado         | Lo no automatizable              |

La matriz FR → prueba está en `quickstart.md`. Cada entrega guarda su evidencia (salidas de prueba
y capturas) referenciada desde ese documento.

## D-12. Emuladores de Firebase en depuración

- **Decisión**: las compilaciones de depuración pueden apuntar a los emuladores locales de Auth y
  Firestore (host `10.0.2.2` desde el emulador de Android) mediante un campo de compilación; las de
  publicación siempre usan el proyecto real.
- **Razón**: pruebas repetibles, sin ensuciar el proyecto real con cuentas de prueba y sin depender
  de Internet.
- **Requisito**: el emulador de Firestore necesita Java; las versiones recientes de la Firebase CLI
  piden JDK 21 o superior. Están instalados JDK 17 y 21, pero el 17 va primero en PATH: para los
  emuladores hay que apuntar `JAVA_HOME` al 21. Gradle sigue con el 17.

## D-13. Configuración y secretos

- **Decisión**: `google-services.json` no se versiona (`.gitignore`); cada integrante lo descarga de
  la consola de Firebase. Se versiona `firebase.json`, `firestore.rules` y el script de siembra.
- **Razón**: la clave no es un secreto fuerte, pero acoplar el repositorio a un proyecto concreto
  dificulta cambiarlo y mezcla datos de prueba con reales.
- **Riesgo**: cada integrante debe registrar la huella SHA-1 de su keystore de depuración para que
  funcione Google (R-3).

## D-14. Interfaz

- **Decisión**: Material 3 con paleta propia de verdes y azules con neutros, modo claro y oscuro
  siguiendo el sistema; textos solo en español (`res/values/strings.xml`); barra de navegación
  inferior con tres destinos (Desafíos, Recompensas, Perfil), lo que cumple SC-005 con un toque.
- **Razón**: identidad visual de la constitución; sin patrones de uso compulsivo.

## D-15. Permisos de Android

- **Decisión**: solo `INTERNET` y `ACCESS_NETWORK_STATE`.
- **Razón**: mínimos indispensables (constitución); ningún permiso de accesibilidad, superposición,
  cámara ni uso de apps en esta entrega.

## Riesgos abiertos

| ID  | Riesgo                                                            | Mitigación                                       |
|-----|-------------------------------------------------------------------|--------------------------------------------------|
| R-1 | Falta Android Studio/SDK/Firebase CLI; quedan 5 días              | Instalarlos primero (día 1); el resto se paraleliza |
| R-2 | Puntos autoritativos en el backend pueden exigir plan Blaze       | Decidir antes de planificar la entrega del 01/10 |
| R-3 | Cada integrante necesita registrar su SHA-1 para Google           | Documentado en `quickstart.md`                   |
| R-4 | Comportamiento de unificación de cuentas por correo sin verificar | Tarea T-VERIF-1 contra el proyecto real          |
| R-5 | El equipo podría no dominar Kotlin                                | Revisar D-1 de inmediato si es el caso           |
