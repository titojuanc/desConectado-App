# Evidencia funcional: entrega 3

## Matriz por requisito

| Requisito | Evidencia automatizada |
|---|---|
| FR-001 Rating obligatorio/inmutable | `ChallengeRatingTest.kt`, `challenge-rating.test.mjs`, `DesafioActivoScreenTest.kt` (Compose compilado). |
| FR-002 Historial completo de desafíos | `PerfilViewModelTest.kt`, tests de consulta del repositorio de desafíos. |
| FR-003 Historial de canjes | `PerfilViewModelTest.kt`, `pending-redemption.test.mjs`, tests del repositorio de puntos. |
| FR-004 Lotes, FIFO y vencimiento | `PointLotTest.kt`, `points-expiration.test.mjs`, `pending-redemption.test.mjs`. |
| FR-005 Próximo vencimiento | `UpcomingPointExpiryTest.kt`, `PerfilScreenTest.kt` (Compose compilado). |
| FR-006 Categorías/Todos | tests de `DesafiosViewModel`, `challenges.test.mjs`, `DesafiosScreenTest.kt` (Compose compilado). |
| FR-007 Catálogo y puntos | `challenges.test.mjs`: 28 desafíos, distribución, fórmula e IDs históricos. |
| FR-008 Métricas | `ProgressCalculatorTest.kt`: estados, zona por resultado, racha y semana. |
| FR-009 Diez logros | `AchievementCalculatorTest.kt`, `achievements.test.mjs`, Rules de achievements. |
| FR-010 Progreso parcial/idempotencia | `AchievementCalculatorTest.kt`, Rules de achievements, `PerfilViewModelTest.kt`. |
| FR-011 Catálogo cosmético | `rewards.test.mjs`: 15 cosméticos, costos/config, cupones preservados y badges fuera de venta. |
| FR-012 Canjes y caja sorpresa | `pending-redemption.test.mjs`, `rewards-cosmetics.test.mjs`, `CosmeticBoxSelectionTest.kt`, `CosmeticOwnershipTest.kt`. |
| FR-013 Aplicación persistente | Rules de `cosmeticOwnership/preferences`, `RecompensasCanjeViewModelTest.kt`, `RecompensasScreenTest.kt` (Compose compilado). |
| FR-014 Resumen del perfil | `ProgressCalculatorTest.kt`, `PerfilViewModelTest.kt`, `PerfilScreenTest.kt` (Compose compilado). |
| FR-015 Registro/meta/nombre | `RegistroViewModelTest.kt`, `IngresoViewModelTest.kt`, `UserPreferencesTest.kt`, `profile-preferences.test.mjs`; emulator tests actualizados y Compose compilado. |
| FR-016 Privacidad/permisos | `PerfilScreenTest.kt` verifica explicación/enlace (Compose compilado); no se solicitaron permisos nuevos. |
| FR-017 Cupones internos existentes | `rewards.test.mjs` verifica los dos IDs/costos existentes y descripciones app-only. |

## Comandos de validación

- `./gradlew.bat testDebugUnitTest --rerun-tasks compileDebugAndroidTestKotlin`: 183 tests JVM, 0 fallos; compilación instrumentada correcta.
- `npm test` desde `firebase/` con JDK 21: 77 Rules, 0 fallos.
- `npm run test:seed` desde `firebase/`: 28 tests, 0 fallos.
- `node seed/seed.mjs --dry-run` desde `firebase/`: valida 28 desafíos, 17 recompensas y 10 logros sin escribir.
- `npm run test:maintenance` desde `firebase/`: 5 tests de guardias del reset, 0 fallos y sin conexión a Firebase.

## Evidencia manual

La instrumentación Compose compila, pero no se ejecutó en dispositivo: `adb` no está instalado/conectado en este entorno. Por eso no se generaron capturas. Antes de iniciar la adaptación visual, completar T045 en un dispositivo real siguiendo los nueve escenarios de `../quickstart.md`.

El seed productivo de cosméticos/logros y el reset productivo no se ejecutaron. El reset preparado exige `--project`, `--execute` y `--confirm-reset-delivery-3 <mismo-project-id>`; conserva Auth/perfil/preferencias y deja un marcador para limpiar sesiones locales antiguas.
