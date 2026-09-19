# Evidencia de la Historia 6: recuperar contraseña (T103)

Fecha: 2026-09-19. Solo pruebas automatizadas; el correo real y M-11 se ejecutan con el APK (T111).

## JVM: 15/15 pasan

| Clase | Pruebas | Fallan |
|---|---|---|
| RestablecerPasswordViewModelTest | 9 | 0 |
| ErroresRedTest | 6 | 0 |

## Instrumentadas (UI y emulador de Auth): 8/8 pasan

| Clase | Pruebas | Fallan |
|---|---|---|
| RestablecerPasswordScreenTest | 6 | 0 |
| PasswordResetEmulatorTest | 2 | 0 |

La prueba de "sin conexión" de Auth no se hace contra un emulador detenido: `FirebaseAuth.useEmulator` es global al proceso en Android. Se cubre con `ErroresRedTest` (clasificación del "internal error" de reCAPTCHA como `SinConexion`).
