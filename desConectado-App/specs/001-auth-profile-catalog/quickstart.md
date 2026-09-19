# Quickstart: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

Guía para preparar el entorno y validar la entrega de punta a punta. No contiene código de la
solución; el detalle de implementación va en `tasks.md`.

## 1. Prerrequisitos (estado al 2026-09-19: faltan los dos primeros)

| Requisito                                   | Estado hoy      | Notas                                             |
|---------------------------------------------|-----------------|---------------------------------------------------|
| Android Studio con SDK de Android           | No instalado    | Incluir una imagen de emulador **con Google Play** |
| Firebase CLI                                | No instalado    | `npm install -g firebase-tools`                   |
| JDK 17 (para Gradle)                        | Instalado       | Es el primero en PATH                             |
| JDK 21 (para los emuladores de Firebase)    | Instalado       | Apuntar `JAVA_HOME` al 21 al correr emuladores    |
| Node.js                                     | Instalado (24)  | Scripts de siembra y pruebas de reglas            |
| Cuenta de Google con acceso a Firebase      | A confirmar     | Plan gratuito (Spark) alcanza                     |

## 2. Proyecto de Firebase (una sola vez, por el equipo)

1. Crear el proyecto en la consola de Firebase.
2. Autenticación: habilitar **Correo y contraseña** y **Google**. En Plantillas, personalizar el
   correo de **restablecimiento de contraseña** en español con voseo (FR-023, FR-025). No se
   configura confirmación de correo (no se exige).
3. Agregar una app Android con el nombre de paquete de la app y la huella SHA-1 de la keystore de
   depuración de **cada integrante** (ver 2.1).
4. Descargar `google-services.json` y colocarlo en `app/` (no se versiona).
5. Crear la base Firestore y desplegar las reglas: `firebase deploy --only firestore:rules`.
6. Sembrar los catálogos: `node firebase/seed/seed.mjs --project <id>`.

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

Emuladores para las pruebas de UI: `firebase emulators:start --only auth,firestore` con
`JAVA_HOME` en el JDK 21; la app de depuración los alcanza en `10.0.2.2`. Sembrarlos antes con
`node firebase/seed/seed.mjs --emulator`. Las pruebas de restablecimiento de contraseña leen los
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
| T-VERIF-1 | 4    | Registrar `X@gmail.com` con contraseña, cerrar sesión y entrar con Google usando ese correo    | Misma cuenta, sin duplicar (o se activa el camino de colisión de `research.md` D-5) |

## 5. Matriz de evidencia (Principio IV, FR-024)

| Requisito                  | Prueba automatizada                          | Prueba manual |
|----------------------------|----------------------------------------------|---------------|
| FR-001, FR-002, FR-009     | Unitarias de validación; UI de registro; integración (cuenta operativa sin confirmar correo) | M-1 |
| FR-003                     | Integración con emulador de Auth             | M-2           |
| FR-004                     | Integración con emulador de Auth; UI de ingreso | M-4        |
| FR-005, FR-006             | Unitaria de `AuthRepository`; UI             | M-3, M-4      |
| FR-007, FR-008             | Integración con token falso en emulador de Auth | M-6, M-7, T-VERIF-1 |
| FR-010, FR-021, FR-022     | UI del perfil (sin contraseña); reglas       | M-10          |
| FR-011                     | Unitarias del bloqueo sin conexión y de sesión que no se cierra sin red; UI | M-5, M-12 |
| FR-012                     | UI de navegación (sin sesión / con sesión)   | M-1, M-4      |
| FR-013 a FR-016            | Prueba de siembra; UI de Desafíos            | M-9           |
| FR-017, FR-020             | UI (ausencia de acciones de inicio y canje)  | M-9           |
| FR-018, FR-019             | Prueba de siembra; UI de Recompensas         | M-9           |
| FR-023                     | UI (marca "(des)Conectado" y revisión de textos en voseo) | M-1, M-11 (correo) |
| FR-024                     | Este documento completado con resultados     | —             |
| FR-025, FR-026             | Unitarias de `RestablecerPasswordViewModel`; UI; integración con emulador de Auth (códigos por REST) | M-11 |

Los criterios de éxito SC-001 a SC-003 se miden manualmente (M-1, M-4, M-6); SC-004 a SC-007 con las
pruebas de la matriz; SC-008 con esta matriz completa; SC-009 manualmente con M-11.
