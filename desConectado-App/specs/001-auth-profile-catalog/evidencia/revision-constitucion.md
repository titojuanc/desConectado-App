# Revisión final contra la constitución v2.1.0 (T113)

Fecha: 2026-09-19. Estado: **cerrada**, con las excepciones aceptadas que se detallan abajo.

## Restricciones de T113

| Restricción | Resultado | Cómo se verificó |
|-------------|-----------|------------------|
| Permisos mínimos indispensables (`INTERNET` y `ACCESS_NETWORK_STATE`) | Cumple, con una excepción aceptada (1) | Manifiesto del APK de entrega (`aapt2 dump permissions`) y `dumpsys package` en el emulador |
| Sin cámara ni bloqueo (Principios I y III) | Cumple | Sin `CAMERA`, accesibilidad, `SYSTEM_ALERT_WINDOW` ni administrador de dispositivo |
| Sin Cloud Functions ni App Check | Cumple | Ninguna dependencia en `libs.versions.toml` ni `app/build.gradle.kts` |
| Sin publicación en Google Play | Cumple | Distribución por APK (`dist/desConectado-entrega1.apk`) |
| Sin canje de puntos ni iniciar desafíos | Cumple | `DesafiosScreenTest` y `RecompensasScreenTest` (ninguna acción accionable); M-9 |
| Sin recuperación por teléfono o SMS | Cumple | Solo `sendPasswordResetEmail` |
| Contraseñas sin almacenar ni mostrar | Cumple | `Perfil` sin campo de contraseña; custodia de Firebase Auth; `PerfilScreenTest` |
| Reglas con denegación por defecto | Cumple | `firestore.rules` publicadas; 30 pruebas de reglas; una lectura sin sesión al proyecto real da HTTP 403 |

## Excepciones aceptadas

**1. Permisos que agregan las bibliotecas (decisión 2026-09-19).** El manifiesto de la app declara solo
`INTERNET` y `ACCESS_NETWORK_STATE`. Se quitaron `USE_BIOMETRIC` y `USE_FINGERPRINT` (los agregaba
`androidx.biometric`, para passkeys, que la app no usa) con `tools:node="remove"`. Se mantienen dos, ambos
"normales" (se conceden al instalar, sin diálogo, y la app no solicita ningún permiso en ejecución):
`READ_GSERVICES`, que lee la protección reCAPTCHA de Firebase Auth (`com.google.android.recaptcha`), y
`<app>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, que AndroidX agrega a toda app y no da acceso a datos del
dispositivo. Verificado: el ingreso con Google funciona en teléfono real con el APK de entrega.

**2. Correo de restablecimiento en tuteo (FR-023 y FR-025, decisión 2026-09-19).** FR-023 pide voseo en todos
los textos y FR-025 un mensaje en español. El correo sale en español, pero con la **traducción de fábrica de
Firebase**, que trata de "tú" ("Haz clic… ignora este correo"), y no en voseo. La tarea T102 pedía
personalizar la plantilla en la consola; la persona decidió cerrarla sin hacerlo. La API de Firebase no permite
editar plantillas de correo (`EMAIL_TEMPLATE_UPDATE_NOT_ALLOWED`), solo la consola. No hay que volver a
publicar la app para corregirlo: basta editar la plantilla en *Authentication → Plantillas* y, para que el
nombre diga "(des)Conectado", el "Nombre público" del proyecto.

**3. Evidencia manual informada, sin capturas ni cronometraje.** Los guiones M-1 a M-12 y T-VERIF-1 se
ejecutaron en un teléfono real contra el proyecto real (APK de depuración y de entrega) y se registraron
como informados por la persona que probó. SC-001, SC-002, SC-003 y SC-009 no se cronometraron: figuran como
"no medidos" en la matriz de `quickstart.md` §5.

## Principio IV (evidencia)

Cada requisito tiene pruebas escritas y ejecutadas en verde (detalle en `automatizadas/resumen.md`): JVM
92/92, instrumentadas 59/59 (AVD API 36 contra los emuladores de Firebase), reglas 30/30 y siembra 22/22, más
la pasada manual en teléfono real. La lógica de negocio (validaciones, ViewModels, mapeo de errores) se
desarrolló con la prueba escrita y vista fallar antes de la implementación. La matriz completa está en
`quickstart.md` §5.

## Decisiones de diseño confirmadas en el proyecto real

- Firebase **no unifica** solo una cuenta de correo con una de Google con el mismo correo: se activa el camino
  de vinculación (T-VERIF-1), que funciona.
- La protección contra enumeración de correos está activa, lo que da la respuesta uniforme que exige FR-026.

---

# Revisión del ajuste 2026-09-23 contra la constitución v2.2.1 (T140)

Fecha: 2026-09-23. Estado: **cerrada en lo automatizado**; falta la pasada en teléfono real (ver
`ajuste-0923/resumen.md`).

| Restricción | Resultado | Cómo se verificó |
|-------------|-----------|------------------|
| Principio I: el cumplimiento sigue siendo por uso de redes; sin geolocalización ni sensores | Cumple | Solo cambian títulos y descripciones (`catalog.json`); sin permisos de ubicación ni de sensores |
| Permisos mínimos | Cumple (igual que la entrega 1) | `aapt dump permissions` del APK de ajuste: `INTERNET`, `ACCESS_NETWORK_STATE` y los dos de las bibliotecas |
| Principio VI: puntos y su registro en Firestore, sin escrituras desde la app | Cumple | `points.test.mjs` y `users.test.mjs` (48/48); `PointsRepository` no tiene operaciones de escritura |
| Cuenta nueva con saldo 0 y sin historial | Cumple | `PointsRepositoryEmulatorTest`, `PerfilFlowEmulatorTest` |
| Sin acreditación, canje, vencimiento ni inicio de desafíos | Cumple | `PerfilScreenTest` (solo "Cerrar sesión" es accionable); ningún código de esas funciones |
| Sin Cloud Functions ni plan de pago | Cumple | Sin dependencias nuevas; el índice y las reglas se despliegan en el plan gratuito |
| Sin funciones de "Fuera de alcance" | Cumple | Sin rankings, mapas, cámara, IA ni bloqueo |
| Voseo (FR-023) | Cumple | Textos nuevos en `strings_puntos.xml` ("Tenés", "Tocá", "Probá", "completés") |
| Principio IV: pruebas escritas y vistas fallar antes de implementar | Cumple | Ver `ajuste-0923/resumen.md` |

**Excepción aceptada en la regla de creación del perfil.** El contrato admite `pointsBalance` ausente o igual a 0
al crear el perfil para que el APK de la entrega 1, ya instalado, siga registrando cuentas cuando se publiquen
las reglas. La ausencia equivale a 0 y nadie puede crear una cuenta con puntos. Se cierra cuando todos los
teléfonos tengan la app nueva (entrega del 01/10).

