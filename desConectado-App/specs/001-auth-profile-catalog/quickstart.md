# Quickstart: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

Guía para preparar el entorno y validar la entrega de punta a punta. No contiene código de la
solución; el detalle de implementación va en `tasks.md`.

## 1. Prerrequisitos (estado al 2026-09-19: todos cumplidos)

| Requisito                                   | Estado          | Notas                                             |
|---------------------------------------------|-----------------|---------------------------------------------------|
| SDK de Android y emulador con Google Play   | Instalado       | Plataforma 37; AVD `desconectado` (imagen Google Play, API 36) |
| Firebase CLI                                | Disponible      | Como dependencia local de `firebase/` (`npx firebase`); no hace falta instalarla global |
| JDK 17 (para Gradle)                        | Instalado       | Es el primero en PATH                             |
| JDK 21 (para los emuladores de Firebase)    | Instalado       | Apuntar `JAVA_HOME` al 21 al correr emuladores    |
| Node.js                                     | Instalado (24)  | Scripts de siembra y pruebas de reglas            |
| Proyecto de Firebase                        | Creado          | `des-conectado`; plan gratuito (Spark)            |

## 2. Proyecto de Firebase (una sola vez, por el equipo)

1. Crear el proyecto en la consola de Firebase.
2. Autenticación: habilitar **Correo y contraseña** y **Google**. En Plantillas, personalizar el
   correo de **restablecimiento de contraseña** en español con voseo (FR-023, FR-025). No se
   configura confirmación de correo (no se exige).
3. Agregar una app Android con el nombre de paquete de la app y la huella SHA-1 de la keystore de
   depuración de **cada integrante** (ver 2.1).
4. Descargar `google-services.json` y colocarlo en `app/` (no se versiona).
5. Crear la base Firestore y desplegar las reglas y el índice de movimientos:
   `firebase deploy --only firestore:rules,firestore:indexes` (sin el índice, la lista de últimos
   desafíos hechos falla en el proyecto real; riesgo R-7).
6. Sembrar los catálogos: `node firebase/seed/seed.mjs --project <id>`. Al volver a sembrar tras el
   ajuste 2026-09-23 los 6 desafíos se actualizan con sus títulos de actividad (mismos `id`).

### 2.1 Huella SHA-1 de depuración

Se obtiene con la tarea `signingReport` de Gradle o con `keytool` sobre `~/.android/debug.keystore`.
Sin este paso, el ingreso con Google falla en ese equipo (riesgo R-3).

El APK que se distribuya (sin Google Play) se firma con una keystore propia del equipo; su huella
SHA-1 también debe registrarse en Firebase, o el ingreso con Google fallará en ese APK. Quien lo
instale debe permitir la instalación desde orígenes desconocidos.

## 3. Validación automatizada

| Qué                          | Comando                                                                 | Resultado esperado      |
|------------------------------|-------------------------------------------------------------------------|-------------------------|
| Pruebas unitarias (JVM)      | `./gradlew testDebugUnitTest`                                           | Todas en verde          |
| Datos sembrados              | `cd firebase && node --test tests/seed`                                 | Invariantes cumplidas   |
| Reglas de seguridad          | `cd firebase && npm test` (levanta el emulador de Firestore)            | Todas en verde          |
| UI e integración             | Con emuladores activos y sembrados: `./gradlew connectedDebugAndroidTest -PuseEmulator=true` | Todas en verde |

Emuladores para las pruebas de UI: `npx firebase emulators:start --only auth,firestore --project des-conectado`
con `JAVA_HOME` en el JDK 21; la app de depuración los alcanza en `10.0.2.2`. Sembrarlos antes con
`EMULATOR_PROJECT_ID=des-conectado node firebase/seed/seed.mjs --emulator`. Es importante usar el mismo id de
proyecto que la app (`des-conectado`): con `npm run emulators` (proyecto `demo-desconectado`) la limpieza de
cuentas entre pruebas no llega al proyecto correcto y el registro falla con "correo en uso". Las pruebas de restablecimiento de contraseña leen los
códigos desde la API REST del emulador de Auth, que no envía correos reales.

## 4. Validación manual guiada

Ejecutar en un dispositivo o emulador Android con Google Play. Registrar resultado y captura de cada
paso en la carpeta de evidencia de la entrega.

| ID    | Historia | Pasos                                                                                          | Resultado esperado                                          |
|-------|----------|------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| M-1   | 1        | Instalar, registrar cuenta nueva con datos válidos                                              | Pantalla principal; SC-001 (menos de 2 min)                 |
| M-2   | 1        | Repetir el registro con el mismo correo                                                         | Mensaje de correo en uso; sin cuenta nueva                  |
| M-3   | 1        | Cerrar y reabrir la app                                                                        | Entra directo a la pantalla principal                       |
| M-4   | 1        | Cerrar sesión; ingresar con contraseña incorrecta y luego correcta                             | Error genérico; luego acceso                                |
| M-5   | 1        | Activar modo avión e intentar ingresar                                                         | Aviso de que se requiere conexión                           |
| M-6   | 4        | En cuenta nueva, "Continuar con Google" con una cuenta real                                    | Acceso sin escribir datos; SC-003                           |
| M-7   | 4        | Cerrar sesión y repetir con la misma cuenta de Google                                          | Misma cuenta, mismos datos                                  |
| M-8   | 4        | Cancelar la pantalla de Google                                                                 | Vuelve al formulario sin error ni cuenta                    |
| M-9   | 2, 5     | Abrir Desafíos y Recompensas                                                                   | 6 desafíos y 5 recompensas con todos sus datos; sin canje    |
| M-10  | 3        | Abrir Perfil en cuenta de correo y en cuenta de Google                                          | Datos del registro o de Google; nunca contraseña            |
| M-11  | 6        | Con el proyecto real: en Ingreso elegir "Olvidé mi contraseña", enviar el correo de una cuenta existente, abrir el enlace del mail, elegir contraseña nueva e ingresar con ella; probar la anterior; repetir con un correo sin cuenta y con una cuenta creada solo con Google | Mensaje de confirmación siempre igual; correo en español con voseo; SC-009 (correo en menos de 2 min, ingreso en menos de 5); solo sirve la contraseña nueva; sin cuenta no llega nada; con la cuenta de Google se puede seguir ingresando con Google |
| M-12  | 1        | Con sesión iniciada, activar modo avión, cerrar la app y abrirla                                | Pantalla principal con aviso de conexión requerida; Desafíos, Recompensas y Perfil sin datos y con Reintentar; la sesión no se pierde; al volver la conexión y reintentar cargan |
| M-13  | 7        | Registrar una cuenta nueva; recorrer Desafíos, Recompensas y Perfil                             | Ícono de puntos con 0 arriba a la izquierda en las tres pestañas; Perfil con saldo 0 y el texto explicativo; sin ninguna acción de puntos; al tocar el ícono desde Desafíos o Recompensas se abre Perfil |
| M-14  | 7        | Con el proyecto real, cargar con administrador un saldo y 6 movimientos `credit` para una cuenta de prueba y abrir su Perfil | Saldo en el ícono y en el Perfil; solo los 5 desafíos más recientes, del más nuevo al más antiguo; confirma que el índice compuesto está desplegado |
| M-15  | 7        | Con la sesión iniciada, activar modo avión y abrir las tres pestañas y el Perfil                | Ícono con guion (no 0); sección de puntos con mensaje y Reintentar; al volver la conexión y reintentar cargan |
| M-16  | 2        | Abrir Desafíos                                                                                  | 6 títulos de actividad ("Salir a trotar", "Andar en bici", "Juntarse con amigos"…); cada descripción aclara que no se usan redes |
| T-VERIF-1 | 4    | Registrar `X@gmail.com` con contraseña, cerrar sesión y entrar con Google usando ese correo    | Misma cuenta, sin duplicar (o se activa el camino de colisión de `research.md` D-5) |

## 5. Matriz de evidencia (Principio IV, FR-024)

Resultados al 2026-09-19. Detalle y salidas: [`evidencia/automatizadas/resumen.md`](evidencia/automatizadas/resumen.md)
(JVM 92/92, instrumentadas 59/59 en AVD API 36 contra los emuladores de Firebase, reglas 30/30, siembra 22/22),
[`evidencia/dispositivo/resultados-manuales.md`](evidencia/dispositivo/resultados-manuales.md) (teléfono real, proyecto
real, APK de depuración y de entrega) y [`evidencia/us4/resultados-manuales.md`](evidencia/us4/resultados-manuales.md).
Los resultados manuales son **informados por la persona que probó** (sin capturas).

| Requisito | Prueba automatizada (todas en verde) | Prueba manual | Estado |
|-----------|--------------------------------------|---------------|--------|
| FR-001, FR-002, FR-009 | `ValidacionesTest`, `RegistroViewModelTest`, `AuthScreensTest`, `AuthRepositoryEmulatorTest` (registro y correo sin distinguir mayúsculas) | M-1 aprobado | Cumple |
| FR-003 | `AuthRepositoryEmulatorTest` (correo repetido), `RegistroViewModelTest` | M-2 aprobado | Cumple |
| FR-004 | `IngresoViewModelTest`, `AuthRepositoryEmulatorTest`, `AuthScreensTest` | M-4 aprobado | Cumple |
| FR-005, FR-006 | `SesionViewModelTest`, `AuthRepositoryEmulatorTest` (`cerrarSesion`, `verificarCuenta`), `PerfilFlowEmulatorTest` | M-3, M-4 aprobados | Cumple |
| FR-007, FR-008 | `AuthGoogleEmulatorTest` (token falso), `GoogleFlowTest`, `GoogleScreensTest` | M-6, M-7, M-8, T-VERIF-1 aprobados; Google real en teléfono, con la firma de depuración y con la de release | Cumple (Firebase no unifica solo: se usa la vinculación, que funciona) |
| FR-010, FR-021, FR-022 | `PerfilScreenTest`, `PerfilViewModelTest`, `AuthScreensTest` (contraseña oculta), reglas de `users` | M-10 aprobado (correo y Google) | Cumple |
| FR-011 | `SesionViewModelTest`, `DesafiosViewModelTest`, `PerfilViewModelTest`, `RecompensasViewModelTest`, `ErroresRedTest`, `AuthScreensTest` | M-5, M-12 aprobados | Cumple |
| FR-012 | `AuthScreensTest` (sin sesión / con sesión / sin conexión) | M-1, M-4 aprobados | Cumple |
| FR-013 a FR-016 | `challenges.test.mjs` (siembra y reglas), `FormatoTest`, `DesafiosScreenTest`, `CatalogDesafiosEmulatorTest` | M-9 aprobado | Cumple |
| FR-017, FR-020 | `DesafiosScreenTest` y `RecompensasScreenTest` (ninguna acción accionable) | M-9 aprobado | Cumple |
| FR-018, FR-019 | `rewards.test.mjs` (siembra y reglas), `RecompensasScreenTest`, `CatalogDesafiosEmulatorTest` | M-9 aprobado | Cumple |
| FR-023 | `AuthScreensTest` (marca "(des)Conectado"), revisión de textos en voseo (T104) | M-1 aprobado | **Parcial**: la app está en voseo; el correo de restablecimiento sale en español con la traducción de fábrica de Firebase, en tuteo (ver `evidencia/revision-constitucion.md`) |
| FR-024 | Este documento | — | Cumple |
| FR-025, FR-026 | `RestablecerPasswordViewModelTest`, `RestablecerPasswordScreenTest`, `PasswordResetEmulatorTest` (códigos por REST) | M-11 aprobado (correo recibido en español) | Cumple; el texto del correo no está en voseo (misma excepción que FR-023) |

### Pruebas previstas para el ajuste 2026-09-23 (Principio IV; se escriben antes que el código)

Estado al 2026-09-23: **implementadas y en verde** (JVM 117/117, instrumentadas 79/79, reglas 48/48, siembra
25/25); detalle en [`evidencia/ajuste-0923/resumen.md`](evidencia/ajuste-0923/resumen.md). Falta la pasada manual
en teléfono real (M-13 a M-16, T135 a T137).

| Requisito | Prueba prevista | Manual |
|-----------|-----------------|--------|
| FR-013, FR-014 | `catalog.json` y sus invariantes en `tests/seed` (6 desafíos, 2 por dificultad, títulos únicos, descripción que menciona redes); `DesafiosScreenTest`, `CatalogDesafiosEmulatorTest` | M-16 |
| FR-027, FR-030 | Reglas: crear perfil con `pointsBalance` 0 y rechazar otro valor o campos extra (`users.test.mjs`); `FirestoreProfileRepository` con emulador (saldo 0 y cuenta anterior sin campo) | M-13 |
| FR-028 | `MainShellTest` (indicador presente en las tres pestañas); `formatearPuntos` en `FormatoTest` | M-13 |
| FR-029 | `PointsRepository` con emulador (orden, límite de 5, solo `credit`); `PerfilViewModelTest` y `PerfilScreenTest` (con datos, vacío, error) | M-13, M-14 |
| FR-031 | `normalizarSaldo` en `ValidacionesTest`/`FormatoTest`; reglas: saldo negativo rechazado | — |
| FR-032 | Reglas (`points.test.mjs`): rechazar crear, editar y borrar movimientos, cambiar `pointsBalance`, y escribir o leer los de otra cuenta; `PerfilScreenTest` (sin acciones de puntos) | M-13 |
| FR-033 | `PerfilViewModelTest` y `MainShellTest` (guion sin conexión, Reintentar) | M-15 |

### Matriz del ajuste 2026-09-23

| Requisito | Prueba automatizada (todas en verde) | Prueba manual | Estado |
|-----------|--------------------------------------|---------------|--------|
| FR-013, FR-014 | `challenges.test.mjs` (siembra), `DesafiosScreenTest`, `CatalogDesafiosEmulatorTest` | M-16 en emulador con captura; falta teléfono real | Cumple (automatizado); manual pendiente |
| FR-027, FR-030 | `users.test.mjs`, `PointsRepositoryEmulatorTest`, `PerfilFlowEmulatorTest` | M-13 en emulador; falta teléfono real | Cumple (automatizado); manual pendiente |
| FR-028 | `PuntosUiTest`, `SaldoViewModelTest`, `PuntosTest`, `PerfilFlowEmulatorTest` | M-13 en emulador con captura | Cumple (automatizado); manual pendiente |
| FR-029 | `PointsRepositoryEmulatorTest`, `PerfilViewModelTest`, `PerfilScreenTest` | M-14 en emulador con captura; falta el proyecto real | Cumple (automatizado); manual pendiente |
| FR-031 | `PuntosTest`, `PointsRepositoryEmulatorTest` (saldo = suma), `points.test.mjs` | — | Cumple |
| FR-032 | `points.test.mjs`, `users.test.mjs`, `PerfilScreenTest` | M-13 | Cumple |
| FR-033 | `SaldoViewModelTest`, `PerfilViewModelTest`, `PerfilScreenTest`, `PuntosUiTest` | M-15 pendiente | Cumple (automatizado); manual pendiente |

Los criterios SC-006 (6 desafíos con título de actividad), SC-010 (indicador visible en las tres pestañas y
coincidente con el Perfil) y SC-011 (100% de las escrituras de puntos rechazadas) quedan cubiertos por las
pruebas de la tabla anterior.

## Criterios de éxito

| Criterio | Cómo se cubre | Estado |
|----------|---------------|--------|
| SC-001, SC-002, SC-003 (tiempos de registro, ingreso y Google) | M-1, M-4 y M-6 aprobados en teléfono real | Cumplidos funcionalmente; **tiempos no cronometrados** |
| SC-004 | `ValidacionesTest`, `RegistroViewModelTest`, `AuthScreensTest` | Cumple |
| SC-005 | Estructura de navegación con tres destinos (`AuthScreensTest`, `PerfilFlowEmulatorTest`) | Cumple |
| SC-006 | Prueba de siembra (6 desafíos, 5 recompensas) y M-9; datos reales verificados en `des-conectado` | Cumple |
| SC-007 | `PerfilScreenTest` (ninguna contraseña visible) | Cumple |
| SC-008 | Esta matriz | Cumple |
| SC-009 | M-11 aprobado | Cumplido funcionalmente; **tiempos no cronometrados** |

Los tiempos de SC-001, SC-002, SC-003 y SC-009 no se midieron con cronómetro; los guiones se ejecutaron y se
informaron como aprobados. Quedan como "no medidos" a propósito, sin inventar cifras.
