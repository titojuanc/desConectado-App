---

description: "Tareas de la entrega 2026-10-01: participación en desafíos y canje de puntos"
---

# Tasks: Participación en Desafíos y Canje de Puntos

**Input**: Design documents from `/specs/002-challenge-participation-redemption/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluidas. La constitución exige pruebas concretas por requisito y desarrollo test-first de medición, cumplimiento, acreditación y canje.

**Organization**: Las tareas están agrupadas por historia para permitir incrementos independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo cuando trabaja archivos distintos y no depende de una tarea incompleta.
- **[Story]**: historia de usuario a la que pertenece la tarea.
- Todas las descripciones incluyen rutas exactas.

## Path Conventions

- Kotlin principal: `app/src/main/java/com/desconectado/app/`
- Pruebas JVM: `app/src/test/java/com/desconectado/app/`
- Pruebas instrumentadas: `app/src/androidTest/java/com/desconectado/app/`
- Firebase: `firebase/`
- Evidencia: `specs/002-challenge-participation-redemption/evidencia/`

## Dependencies and Execution Order

```text
Setup T001-T006
  -> Foundational T007-T014
      -> US1 T015-T030
          -> US2 T031-T037
          -> US3 T038-T047
US2 + US3 -> Polish T048-T053
```

US2 y US3 requieren los contratos y modelos fundacionales, pero pueden implementarse en paralelo
una vez terminado US1 si no modifican los mismos archivos de navegación compartida. El MVP mínimo
para demostrar la entrega es US1 completa más US2; US3 puede entregarse inmediatamente después con
saldo y catálogos ya disponibles.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: preparar dependencias, permisos mínimos y emuladores sin agregar funciones fuera de alcance.

- [X] T001 Revisar `gradle/libs.versions.toml` y `app/build.gradle.kts` para confirmar las dependencias existentes de Firestore, coroutines y Compose; agregar DataStore Preferences para persistir la sesión activa en `app/build.gradle.kts`.
- [X] T002 [P] Actualizar `app/src/main/AndroidManifest.xml` únicamente con la declaración de acceso de uso requerida por `UsageStatsManager`, documentando que el permiso se concede desde Ajustes y no agregar cámara, accesibilidad, overlay, ubicación ni administración de dispositivo.
- [X] T003 [P] Crear `app/src/main/res/values/strings_desafios_activos.xml` con textos en español rioplatense para permiso de uso, estado activo, cumplido, no cumplido, cancelado, invalidado, conexión y confirmaciones.
- [X] T004 [P] Crear `app/src/main/res/values/strings_canjes.xml` con textos en español rioplatense para costo, saldo insuficiente, confirmación, éxito, código de cupón y recompensas obtenidas.
- [X] T005 [P] Crear `firebase/firestore.indexes.json` o actualizarlo únicamente para las consultas concretas definidas en `specs/002-challenge-participation-redemption/contracts/firestore-data.md`; documentar cada campo y ordenamiento usado.
- [X] T006 Actualizar `firebase/firebase.json` para registrar los índices de T005 y verificar que los emuladores Auth/Firestore siguen usando el proyecto `des-conectado`.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: contratos, errores, reloj, persistencia y helpers de prueba compartidos por las tres historias.

- [X] T007 [P] Crear `app/src/main/java/com/desconectado/app/domain/model/ActiveChallenge.kt` con los campos `challengeId`, `challengeTitle`, `durationMinutes`, `points`, `startedAt`, `offlineSeconds`, `status` y `updatedAt`; restringir `offlineSeconds` a valores no negativos y `status` a `active`, `cancelled`, `failed`, `completed`, `invalidated`.
- [X] T008 [P] Crear `app/src/main/java/com/desconectado/app/domain/model/ChallengeResult.kt` con `challengeRunId`, instantes de inicio/fin, estado `completed`, `failed`, `cancelled` o `invalidated`, `measuredSocialSeconds`, `offlineSeconds` y `pointsAwarded`; exigir `pointsAwarded` igual al catálogo solo en `completed` y 0 en los demás estados.
- [X] T009 [P] Crear `app/src/main/java/com/desconectado/app/domain/model/RedeemedReward.kt` y `PointMovement.kt` en `app/src/main/java/com/desconectado/app/domain/model/`, incluyendo tipos `credit`/`redeem`, `sourceId`, `costPoints`, `movementId`, `redemptionId` (`unique-{rewardId}` para insignia/tema y nuevo para cupón), `code` opcional con fórmula `DC-` + Base64 URL-safe sin padding y fecha; no exponer operaciones mutables.
- [X] T010 [P] Crear `app/src/main/java/com/desconectado/app/domain/repository/UsageStatsRepository.kt`, `ChallengeRepository.kt` y `PointsRepository.kt` con las firmas de `contracts/repositories.md`, usando `Resultado` y errores tipados existentes.
- [X] T011 [P] Crear `app/src/main/java/com/desconectado/app/domain/TimeSource.kt` con reloj inyectable para instantes y duración monotónica; crear `app/src/test/java/com/desconectado/app/fakes/FakeTimeSource.kt` para pruebas deterministas de 300/301 segundos offline.
- [X] T012 [P] Crear `app/src/test/java/com/desconectado/app/fakes/FakeUsageStatsRepository.kt`, `FakeChallengeRepository.kt` y `FakePointsRepository.kt` con resultados programables, registro de llamadas y simulación de errores de conexión, permiso y duplicado.
- [X] T013 [P] Crear `app/src/test/java/com/desconectado/app/domain/DesafioCumplimientoTest.kt` con pruebas escritas primero para uso social 0, uso social positivo, duración terminada, offline de 300 segundos y offline de 301 segundos; comprobar que el resultado esperado es `completed`, `failed` o `invalidated` y que solo `completed` entrega puntos.
- [X] T014 Actualizar `app/src/main/java/com/desconectado/app/AppContainer.kt` para registrar los nuevos repositorios y fuentes de tiempo sin romper Auth, Profile, Catalog ni Points de la entrega 1; verificar que el emulador siga pudiendo sustituirse en debug.

## Phase 3: User Story 1 - Aceptar y completar un desafío (Priority: P1)

**Goal**: iniciar, restaurar, medir y cerrar un desafío, acreditando puntos exactamente una vez.

**Independent Test**: con una cuenta autenticada, conceder acceso de uso, iniciar un desafío, completar un caso con 0 segundos sociales, repetir con uso positivo y cerrar/reabrir la app durante una sesión activa.

### Tests for User Story 1

- [X] T015 [P] [US1] Crear `app/src/test/java/com/desconectado/app/data/UsageStatsRepositoryTest.kt` con los package names exactos de `research.md`, exclusión de WhatsApp, paquete ausente como cero uso, suma de intervalos de foreground y rechazo cuando el acceso no está concedido.
- [ ] T016 [P] [US1] Crear `app/src/test/java/com/desconectado/app/ui/challenges/DesafioActivoViewModelTest.kt` con pruebas de inicio en no más de tres acciones, estado activo, restauración, revocación que invalida inmediatamente, finalización cumplida, finalización no cumplida, medición incompleta y doble cierre idempotente.
- [X] T017 [P] [US1] Crear `firebase/tests/rules/challenge-participation.test.mjs` antes de implementar reglas, verificando que solo la persona dueña lee/escribe su desafío activo y que una escritura ajena, ruta extra o estado inválido es rechazada.
- [X] T018 [P] [US1] Crear `firebase/tests/rules/points-credit.test.mjs` antes de implementar acreditación, intentando crédito duplicado por `challengeRunId`, acreditación antes de `startedAt + durationMinutes` según hora del servidor, importe distinto del catálogo, crédito con estado no cumplido, saldo negativo, lote incompleto, edición y borrado; si la comparación server-side no es expresable, comprobar que la regla rechaza la acreditación.
- [ ] T019 [P] [US1] Crear `app/src/androidTest/java/com/desconectado/app/data/UsageStatsDeviceTest.kt` que compruebe en un dispositivo/emulador que el acceso de uso se detecta, que usa los seis package names de `research.md`, que un paquete ausente cuenta como cero y que no incluye paquetes fuera de la lista.
- [ ] T020 [P] [US1] Crear `app/src/androidTest/java/com/desconectado/app/data/ChallengeRepositoryEmulatorTest.kt` para iniciar una sesión, restaurarla, cerrar un resultado cumplido y repetir el cierre con el mismo `challengeRunId` sin duplicar movimiento.

### Implementation for User Story 1

- [X] T021 [US1] Implementar `app/src/main/java/com/desconectado/app/data/usage/AndroidUsageStatsRepository.kt` con `UsageStatsManager`, los seis package names exactos de `research.md`, detección de acceso concedido, consulta de eventos/intervalos y omisión de paquetes no instalados; devolver error explícito cuando la medición no esté disponible.
- [X] T022 [US1] Implementar `app/src/main/java/com/desconectado/app/domain/DesafioCumplimiento.kt` para comparar uso social y offline acumulado; usar `TimeSource`, tratar 300 segundos como tolerados y 301 como invalidación, y otorgar puntos únicamente al estado `completed`.
- [X] T023 [US1] Implementar persistencia local con DataStore Preferences en `app/src/main/java/com/desconectado/app/data/challenges/ActiveChallengeStore.kt`, guardando solo id, título, duración, puntos, inicio, offline acumulado, estado y actualización; restaurar después de cierre/reinicio.
- [X] T024 [US1] Implementar `app/src/main/java/com/desconectado/app/data/challenges/FirestoreChallengeRepository.kt` para `active`, `start`, `updateOffline` y `finish`, manteniendo un único `activeChallenge/current` y haciendo idempotente el cierre por `challengeRunId`.
- [X] T025 [US1] Extender `firebase/firestore.rules` con rutas `activeChallenge/current` y `challengeResults/{runId}`; validar propietario, campos permitidos, estados, timestamps, un único activo y que un resultado terminal no pueda editarse ni borrarse.
- [X] T026 [US1] Implementar la transacción de acreditación en `app/src/main/java/com/desconectado/app/data/points/FirestorePointsRepository.kt`, creando el movimiento determinista `credit-{challengeRunId}` y actualizando `users/{uid}.pointsBalance` atómicamente; validar catálogo, saldo no negativo, duración server-side y reintentos idempotentes según `contracts/firestore-data.md`, rechazando la acreditación si la regla temporal no puede validarse.
- [X] T027 [US1] Crear `app/src/main/java/com/desconectado/app/ui/challenges/DesafioActivoViewModel.kt` para coordinar permiso, inicio, refresco de uso, conexión, restauración, invalidación inmediata por revocación y cierre; no mostrar uso de apps individuales ni acreditar ante medición incompleta.
- [X] T028 [P] [US1] Crear `app/src/main/java/com/desconectado/app/ui/challenges/DesafioActivoScreen.kt` con estado activo, tiempo restante, resultado, error y acceso a Ajustes; no incluir controles de bloqueo, overlay, cámara ni IA.
- [X] T029 [US1] Actualizar `app/src/main/java/com/desconectado/app/ui/navigation/AppNavigation.kt` y `app/src/main/java/com/desconectado/app/ui/navigation/MainShell.kt` para abrir el detalle desde el catálogo, restaurar el desafío activo al iniciar sesión y mostrar saldo/resultado actualizado.
- [ ] T030 [US1] Ejecutar las pruebas de T013 y T015-T020, `./gradlew testDebugUnitTest`, `npm test` en `firebase/` y las instrumentadas con emuladores; guardar resultados de US1 en `specs/002-challenge-participation-redemption/evidencia/us1/`.

## Phase 4: User Story 2 - Cancelar un desafío (Priority: P1)

**Goal**: cancelar sin puntos y permitir iniciar otro desafío inmediatamente.

**Independent Test**: iniciar un desafío, confirmar cancelación, comprobar estado cancelado y saldo intacto, e iniciar un segundo desafío.

### Tests for User Story 2

- [ ] T031 [P] [US2] Ampliar `app/src/test/java/com/desconectado/app/ui/challenges/DesafioActivoViewModelTest.kt` con confirmación de cancelación, doble toque, cancelación sin conexión y posibilidad de iniciar otro desafío tras cancelar.
- [ ] T032 [P] [US2] Ampliar `firebase/tests/rules/challenge-participation.test.mjs` con transición `active` a `cancelled`, rechazo de puntos en cancelación y rechazo de cancelar un resultado ya terminal.
- [ ] T033 [P] [US2] Crear `app/src/androidTest/java/com/desconectado/app/ui/DesafioActivoScreenTest.kt` con botón de cancelar, diálogo de confirmación, mensaje de cero puntos y ausencia de controles de bloqueo.

### Implementation for User Story 2

- [X] T034 [US2] Implementar `cancel(uid, runId)` en `app/src/main/java/com/desconectado/app/data/challenges/FirestoreChallengeRepository.kt`, guardando resultado `cancelled` con `pointsAwarded = 0` y liberando el documento activo de forma idempotente; mantener `invalidate(uid, runId, reason)` para revocación.
- [X] T035 [US2] Agregar confirmación y acción de cancelación en `app/src/main/java/com/desconectado/app/ui/challenges/DesafioActivoScreen.kt` y `DesafioActivoViewModel.kt`; deshabilitar doble envío y permitir volver al catálogo.
- [X] T036 [US2] Verificar que `firebase/firestore.rules` rechaza cualquier crédito asociado a resultado `cancelled` y que el segundo desafío puede iniciarse solo después de liberar la sesión activa.
- [ ] T037 [US2] Ejecutar pruebas JVM, reglas e instrumentadas de US2 y guardar capturas/outputs en `specs/002-challenge-participation-redemption/evidencia/us2/`.

## Phase 5: User Story 3 - Canjear una recompensa (Priority: P1)

**Goal**: canjear recompensas digitales con saldo suficiente, sin saldo negativo ni duplicados.

**Independent Test**: con saldo suficiente, confirmar un canje y comprobar saldo, movimiento, recompensa y código; repetir con saldo insuficiente y reintento.

### Tests for User Story 3

- [X] T038 [P] [US3] Crear `app/src/test/java/com/desconectado/app/domain/CanjeTest.kt` con saldo suficiente/insuficiente, costo exacto, costo cero o negativo inválido, código `DC-` + Base64 URL-safe sin padding derivado de `redemptionId` sin datos personales, cupones repetibles, insignia/tema repetidos rechazados y reintento con el mismo id.
- [X] T039 [P] [US3] Crear `app/src/test/java/com/desconectado/app/ui/rewards/RecompensasCanjeViewModelTest.kt` con confirmación, saldo insuficiente, doble toque, error de red, éxito persistido, cupón repetido permitido, insignia/tema repetidos rechazados y carga de recompensas canjeadas.
- [ ] T040 [P] [US3] Crear `firebase/tests/rules/rewards-redemption.test.mjs` antes de implementar reglas, probando costo distinto, saldo negativo, recompensa inexistente, ID determinista `unique-{rewardId}` duplicado, cupón repetido con IDs distintos, código `DC-` + Base64 URL-safe sin padding derivado de `redemptionId`, cuenta ajena, edición, borrado y código vacío/reutilizado.
- [ ] T041 [P] [US3] Crear `app/src/androidTest/java/com/desconectado/app/data/PointsRedemptionEmulatorTest.kt` para canje válido, saldo insuficiente, reintento idempotente y persistencia de la recompensa.
- [ ] T042 [P] [US3] Crear `app/src/androidTest/java/com/desconectado/app/ui/RecompensasCanjeScreenTest.kt` para costo, diálogo de confirmación, error de saldo, código de cupón y lista personal de recompensas.

### Implementation for User Story 3

- [X] T043 [US3] Implementar `app/src/main/java/com/desconectado/app/domain/Canje.kt` con validaciones puras de saldo, costo, tipo de recompensa y generación de código `DC-` + Base64 URL-safe sin padding desde un `redemptionId` aleatorio, sin datos personales.
- [X] T044 [US3] Extender `app/src/main/java/com/desconectado/app/data/points/FirestorePointsRepository.kt` con la transacción `redeem`, usando `unique-{rewardId}` para insignias/temas y un `redemptionId` nuevo para cada cupón, derivando `code = DC- + Base64 URL-safe sin padding(redemptionId)`, creando `redeemedRewards/{redemptionId}`, movimiento `redeem` y saldo actualizado exactamente una vez.
- [ ] T045 [US3] Extender `firebase/firestore.rules` para `redeemedRewards/{redemptionId}` y movimientos `redeem`, validando catálogo, costo, propietario, saldo posterior no negativo, `code == "DC-" + base64UrlNoPadding(redemptionId)`, unicidad determinista de insignias/temas y documentos inmutables.
- [X] T046 [US3] Crear `app/src/main/java/com/desconectado/app/ui/rewards/RecompensasCanjeViewModel.kt` y actualizar `app/src/main/java/com/desconectado/app/AppContainer.kt` para cargar catálogo, saldo y recompensas canjeadas sin exponer operaciones genéricas de escritura.
- [X] T047 [US3] Actualizar `app/src/main/java/com/desconectado/app/ui/rewards/RecompensasScreen.kt`, `app/src/main/res/values/strings_canjes.xml` y navegación para mostrar confirmación, saldo insuficiente, éxito, código y sección personal; mantener recompensas digitales sin valor fuera de la app.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: cerrar evidencia, seguridad, regresiones y APK de la segunda entrega.

- [ ] T048 [P] Revisar `app/src/main/AndroidManifest.xml` y `firebase/firestore.rules` para confirmar ausencia de permisos/capacidades de cámara, accesibilidad, overlay, ubicación, bloqueo e IA; después de la prueba server-side de T018, actualizar o cerrar `TODO(PUNTOS_SEGURIDAD)` en `.specify/memory/constitution.md` según el resultado y guardar la revisión en `specs/002-challenge-participation-redemption/evidencia/revision-constitucion.md`.
- [ ] T049 [P] Revisar todos los textos nuevos en `app/src/main/res/values/strings_desafios_activos.xml` y `app/src/main/res/values/strings_canjes.xml` para voseo, marca `(des)Conectado` y estados accesibles; cubrir cambios en `app/src/androidTest/java/com/desconectado/app/ui/`.
- [ ] T050 Ejecutar `./gradlew testDebugUnitTest`, `cd firebase; npm test; npm run test:seed`, y `./gradlew connectedDebugAndroidTest -PuseEmulator=true`; guardar salidas con sufijo `-entrega2` en `specs/002-challenge-participation-redemption/evidencia/automatizadas/`.
- [ ] T051 [P] Ejecutar los guiones manuales de `quickstart.md` en emulador y dispositivo real, especialmente permiso de uso, apps ausentes, revocación, restauración, offline de 5 minutos, invalidación, cancelación, cupones repetidos, insignias/temas únicos, fórmula `DC-` + Base64 URL-safe sin padding, rechazo server-side de acreditación temprana y tiempos de rendimiento: inicio/restauración en menos de 1 segundo y resultado/canje en menos de 3 segundos; guardar capturas y resultados en `specs/002-challenge-participation-redemption/evidencia/dispositivo/`.
- [ ] T052 Actualizar `specs/002-challenge-participation-redemption/quickstart.md` con resultados, enlaces a evidencia y matriz FR-001 a FR-017; no marcar una historia como completa sin prueba automatizada o procedimiento manual.
- [ ] T053 Generar el APK de entrega con `./gradlew assembleRelease`, copiarlo a `dist/desConectado-entrega2.apk`, verificar firma/permisos e instalarlo para repetir los flujos principales sin emuladores Firebase.

## Parallel Opportunities

- Setup: T002-T005 pueden ejecutarse en paralelo.
- Foundational: T007-T013 pueden dividirse por archivos antes de integrar T014.
- US1: T015-T020 son pruebas independientes; T021, T023 y T028 pueden avanzar en paralelo después de T015-T018.
- US2: T031-T033 pueden escribirse en paralelo; T034 y T035 se integran después.
- US3: T038-T042 son pruebas independientes; T043 y T047 pueden avanzar en paralelo antes de la integración de T044-T046.
- Polish: T048, T049 y T051 son independientes; T050 requiere que las historias estén integradas.

## Implementation Strategy

1. Completar Setup y Foundational sin alterar el flujo existente de autenticación, perfil y catálogos.
2. Entregar US1 como MVP técnico: permiso de uso, desafío activo, restauración, medición, resultado y acreditación segura.
3. Añadir US2 inmediatamente para cumplir la cancelación obligatoria.
4. Añadir US3 y verificar el ciclo completo de puntos, canje y recompensa digital.
5. Ejecutar Polish solo con las tres historias verdes y conservar toda la evidencia.
