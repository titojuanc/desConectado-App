# Revisión contra la constitución v2.1.0 (T113) — PRELIMINAR

Fecha: 2026-09-19. Estado: **preliminar**; se cierra al terminar la evidencia en dispositivo (T111).

## Restricciones de T113

| Restricción | Resultado | Cómo se verificó |
|-------------|-----------|------------------|
| Permisos: solo `INTERNET` y `ACCESS_NETWORK_STATE` | **DESVIACIÓN (ver abajo)** | Manifiesto fusionado de `assembleRelease` |
| Sin cámara ni bloqueo (Principios I y III) | Cumple | Sin `CAMERA`, accesibilidad, `SYSTEM_ALERT_WINDOW` ni administrador de dispositivo en el manifiesto fusionado |
| Sin Cloud Functions | Cumple | Ninguna dependencia de Functions, App Check ni cámara en `libs.versions.toml` ni `app/build.gradle.kts` |
| Sin publicación en Google Play | Cumple | Distribución por APK; sin configuración de publicación |
| Sin canje de puntos ni iniciar desafíos | Cumple | Las pantallas de Desafíos y Recompensas no tienen acciones; probado en `DesafiosScreenTest` y `RecompensasScreenTest` (`hasClickAction()` = 0), pendientes de ejecutar en emulador |
| Sin recuperación por teléfono o SMS | Cumple | Solo `sendPasswordResetEmail` |
| Contraseñas sin almacenar ni mostrar | Cumple | `Perfil` no tiene campo de contraseña; la custodia Firebase Auth; probado en `PerfilScreenTest` |
| Reglas con denegación por defecto | Cumple | `firestore.rules`; 30 pruebas de reglas en verde (`evidencia/us1/firebase-reglas-y-siembra.txt`) |

## Desviación: permisos que agregan las bibliotecas

El `AndroidManifest.xml` de la app declara únicamente `INTERNET` y `ACCESS_NETWORK_STATE`, pero el
manifiesto **fusionado** del release incluye además:

- `android.permission.USE_BIOMETRIC` y `android.permission.USE_FINGERPRINT`
- `com.google.android.providers.gsf.permission.READ_GSERVICES`
- `com.desconectado.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (permiso propio que AndroidX agrega
  por defecto a toda app; no es un acceso a datos del dispositivo)

Los tres primeros los aportan las bibliotecas de Google (Credential Manager y servicios de Google
Play). Son permisos "normales": se conceden al instalar y **no muestran ningún diálogo** al usuario, y
la app no los usa. Aun así incumplen la lectura literal de "permisos mínimos indispensables".

**Decisión pendiente (de la persona):**

1. Aceptarlos y anotar la excepción en la entrega, o
2. quitarlos con `tools:node="remove"` en `app/src/main/AndroidManifest.xml`. Es una opción de bajo
   riesgo para el flujo actual (Google ID token, sin passkeys), pero no se aplicó porque no se pudo
   probar el ingreso con Google en un dispositivo. Si se aplica, verificarlo en T082.

## Principio IV (evidencia)

Cada requisito tiene pruebas escritas y ejecutadas en verde (detalle en `automatizadas/resumen.md`):
JVM 92/92, instrumentadas 59/59 (AVD API 36 contra los emuladores de Firebase), reglas 30/30 y
siembra 22/22. **Pendientes**: los procedimientos manuales en emulador (M-1 a M-12) y todo lo que
requiere el proyecto Firebase real y el dispositivo (T082, T111).
