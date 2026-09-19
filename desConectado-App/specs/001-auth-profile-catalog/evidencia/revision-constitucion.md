# Revisión contra la constitución v2.1.0 (T113) — PRELIMINAR

Fecha: 2026-09-19. Estado: **preliminar**; se cierra al terminar la evidencia en dispositivo (T111).

## Restricciones de T113

| Restricción | Resultado | Cómo se verificó |
|-------------|-----------|------------------|
| Permisos mínimos indispensables (`INTERNET` y `ACCESS_NETWORK_STATE`) | Cumple, con una excepción documentada abajo | Manifiesto fusionado de `assembleRelease` y `dumpsys package` en el emulador |
| Sin cámara ni bloqueo (Principios I y III) | Cumple | Sin `CAMERA`, accesibilidad, `SYSTEM_ALERT_WINDOW` ni administrador de dispositivo en el manifiesto fusionado |
| Sin Cloud Functions | Cumple | Ninguna dependencia de Functions, App Check ni cámara en `libs.versions.toml` ni `app/build.gradle.kts` |
| Sin publicación en Google Play | Cumple | Distribución por APK; sin configuración de publicación |
| Sin canje de puntos ni iniciar desafíos | Cumple | Las pantallas de Desafíos y Recompensas no tienen acciones; probado en `DesafiosScreenTest` y `RecompensasScreenTest` (`hasClickAction()` = 0), en verde en el emulador |
| Sin recuperación por teléfono o SMS | Cumple | Solo `sendPasswordResetEmail` |
| Contraseñas sin almacenar ni mostrar | Cumple | `Perfil` no tiene campo de contraseña; la custodia Firebase Auth; probado en `PerfilScreenTest` |
| Reglas con denegación por defecto | Cumple | `firestore.rules`; 30 pruebas de reglas en verde (`automatizadas/firebase-reglas.txt`) |

## Permisos: decisión tomada (2026-09-19)

El `AndroidManifest.xml` de la app declara únicamente `INTERNET` y `ACCESS_NETWORK_STATE`. Las
bibliotecas agregaban además, en el manifiesto fusionado:

| Permiso | Lo agregaba | Decisión |
|---------|-------------|----------|
| `USE_BIOMETRIC` y `USE_FINGERPRINT` | `androidx.biometric:biometric:1.1.0` (llega con Credential Manager; sirve para passkeys y biometría) | **Quitados** con `tools:node="remove"` en `app/src/main/AndroidManifest.xml`: la app solo pide un token de Google |
| `com.google.android.providers.gsf.permission.READ_GSERVICES` | `com.google.android.recaptcha:recaptcha:18.6.1` (llega con Firebase Auth) | **Se mantiene**: lo lee la protección reCAPTCHA de Auth, que interviene en el registro, el ingreso y el restablecimiento; quitarlo podría hacerlos fallar |
| `<app>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | `androidx.core` | **Se mantiene**: lo agrega AndroidX a toda app; no da acceso a datos del dispositivo |

`READ_GSERVICES` es un permiso "normal": se concede al instalar y no muestra ningún diálogo. La app
no solicita ningún permiso en ejecución.

**Verificación tras el cambio**: `assembleRelease` compila; el manifiesto fusionado ya no contiene
`USE_BIOMETRIC` ni `USE_FINGERPRINT`; `dumpsys package` en el emulador lista solo `INTERNET`,
`ACCESS_NETWORK_STATE` y `READ_GSERVICES`; JVM 92/92 e instrumentadas 59/59 en verde (incluido el
ingreso con Google con tokens falsos). **Pendiente**: confirmar el ingreso con Google real en T082.

## Principio IV (evidencia)

Cada requisito tiene pruebas escritas y ejecutadas en verde (detalle en `automatizadas/resumen.md`):
JVM 92/92, instrumentadas 59/59 (AVD API 36 contra los emuladores de Firebase), reglas 30/30 y
siembra 22/22. **Pendientes**: los procedimientos manuales en emulador (M-1 a M-12) y todo lo que
requiere el proyecto Firebase real y el dispositivo (T082, T111).
