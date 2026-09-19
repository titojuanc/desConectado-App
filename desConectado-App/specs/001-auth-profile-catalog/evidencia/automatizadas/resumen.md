# Resumen de pruebas automatizadas (T110)

Fecha: 2026-09-19 (repetidas tras quitar los permisos biométricos del manifiesto).

## JVM (`./gradlew testDebugUnitTest`): 92/92 pasan

| Clase | Pruebas | Fallan |
|---|---|---|
| ErroresRedTest | 6 | 0 |
| FormatoTest | 6 | 0 |
| NombrePerfilTest | 6 | 0 |
| ValidacionesTest | 18 | 0 |
| GoogleFlowTest | 11 | 0 |
| IngresoViewModelTest | 6 | 0 |
| RegistroViewModelTest | 7 | 0 |
| RestablecerPasswordViewModelTest | 9 | 0 |
| SesionViewModelTest | 4 | 0 |
| DesafiosViewModelTest | 7 | 0 |
| PerfilViewModelTest | 7 | 0 |
| RecompensasViewModelTest | 5 | 0 |

## Instrumentadas (`./gradlew connectedDebugAndroidTest -PuseEmulator=true`, AVD API 36): 59/59 pasan

| Clase | Pruebas | Fallan |
|---|---|---|
| AuthGoogleEmulatorTest | 4 | 0 |
| AuthRepositoryEmulatorTest | 8 | 0 |
| CatalogDesafiosEmulatorTest | 3 | 0 |
| PasswordResetEmulatorTest | 2 | 0 |
| AuthScreensTest | 13 | 0 |
| DesafiosScreenTest | 6 | 0 |
| GoogleScreensTest | 7 | 0 |
| PerfilFlowEmulatorTest | 1 | 0 |
| PerfilScreenTest | 4 | 0 |
| RecompensasScreenTest | 5 | 0 |
| RestablecerPasswordScreenTest | 6 | 0 |

## Firebase (Node, `firebase/`)

- Reglas de seguridad (`npm test`, emulador de Firestore): 30/30 pasan — `firebase-reglas.txt`
- Datos sembrados (`npm run test:seed`): 22/22 pasan — `firebase-siembra.txt`
