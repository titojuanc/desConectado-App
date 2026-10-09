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

La suite Compose completa sigue sin ejecutarse. Antes de iniciar la adaptación visual, completar T045 en un dispositivo real siguiendo los nueve escenarios de `../quickstart.md` y el checklist `../quickstart-produccion.md`.

## Firebase real desde Android

Validación del 2026-10-09, rama `entrega-3`, AVD `desconectado` Android 16, Firebase SDK de la variante debug con `USE_FIREBASE_EMULATOR=false`. El commit inicial `1366d33` fue pusheado antes del despliegue.

- Proyecto confirmado: `des-conectado`, coincidente en Android, CLI y cuenta Admin local ignorada por Git.
- Rules compiladas/desplegadas e índices declarados desplegados. Seed: 28 desafíos, 17 recompensas y 10 logros; 4 desafíos y 4 recompensas sobrantes archivados, sin borrar usuarios.
- `ProductionConnectionsTest`: reporte final con 1 test, 0 fallos, 0 errores y 0 omitidos, 23.240 s. Ejecutado con banderas explícitas `productionSmoke=des-conectado` y `syntheticQa=true`, solo en cuentas QA nuevas; datos sintéticos autorizados por el usuario.
- SDK/repositorios verificados: registro, ingreso/revalidación de sesión, lectura/edición de username, meta y notificaciones persistentes; 28/17 elementos de catálogo; historiales, saldo, vencimiento y pending; definiciones/progreso de 10 logros; inicio/cancelación de desafío, resultado e idempotencia.
- Caminos positivos con fixtures sintéticos: resultado completado, acreditación inicial y segunda al mismo lote, rating inmutable/idempotente, escrituras de logros, canje de tema, propiedad y Aplicar/Quitar persistentes; caja sorpresa con premio único y activación; débito de lote vencido/idempotencia. No prueban una finalización real con estadísticas de uso.
- Rechazos comprobados: tema no poseído, canje sin saldo y rating sobre desafío cancelado. El saldo no cambió en esas operaciones.
- Fallos encontrados/corregidos: índice `pointLots` no desplegado (`FAILED_PRECONDITION`), resuelto desplegando índices; pérdida de precisión de `earnedAt` al convertir Timestamp a Date, resuelta conservando segundos/nanosegundos al leer lotes. La segunda acreditación que fallaba pasó sin relajar Rules.
- Regresión posterior al fix: 183 tests JVM, 0 fallos; APK debug construido; diagnósticos del repositorio y smoke sin errores. Rules 77/77, seed 28/28 y maintenance 5/5 pasaron antes del despliegue; sus archivos no cambiaron durante el fix.
- Limpieza: cinco cuentas QA creadas en estas ejecuciones, verificadas por marcador de perfil y correo QA generado, eliminadas exclusivamente por UID en Auth y Firestore; ausencia comprobada después. No se ejecutó el reset general ni se canjearon cupones.

Pendiente en teléfono: proveedor Google/OAuth con cuenta interactiva, recepción/restablecimiento de correo, estadísticas de uso/notificaciones del SO, persistencia visible tras reinicio y efectos visuales/sonoros de cada cosmético. No se declaran probados esos casos ni la suite completa de UI. T045 y feedback del usuario siguen bloqueando la adaptación visual final.

## Feedback: barra, secciones y medallas

- Recompensas: Canjeables y Logros en pestanas; medallas desbloqueadas en Perfil. Progreso compartido y recargado al cambiar de destino o terminar un desafio.
- Desafio: barra encima de navegacion inferior, contador `mm:ss` de ancho estable y cruz; sin botones de actualizar/comprobar/finalizar. Ticker en ViewModel con vida por sesion; operaciones serializadas y valoracion conservada al terminar.
- Seed adicional en `des-conectado`: 4 desafios activos de 10 segundos/1 punto, 3 recompensas de 1 punto y 3 logros inmediatos, todos `debug-`/`[TEST]`. Batch solo sobre esos diez documentos, sin borrar/archivar datos normales.
- AVD Android 16: 5 tests de DesafioActivoScreen pasados, incluyendo barra persistente entre pestanas y cancelacion; 1 test aislado Canjeables/Logros pasado; 1 test de Perfil con medalla desbloqueada visible y pendiente oculto pasado. Ejecuciones separadas, 0 fallos/omisiones en reportes finales.
- Recorrido real, no sintetico: 1 test opt-in de `tenSecondChallengeCompletesAutomatically`, con acceso real de uso en AVD; espera de diez segundos, finalizacion automatica, resultado COMPLETED de 10 segundos, saldo +1, tres logros debug desbloqueados, rating, canje de tema de 1 punto, saldo 0 y preferencia guardada. Reporte final 1/1, sin omisiones.
- Cuenta QA de este recorrido eliminada exclusivamente por UID despues de corroborar perfil/Auth y marcador; catalogo test conservado para el telefono.
- Seed 31/31 y JVM 183/183 pasados; release firmado construido. La suite completa de UI no se ejecuto. No se desplegaron cambios de Rules ni se ejecuto reset.
