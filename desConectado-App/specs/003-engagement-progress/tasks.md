---

description: "Plan de implementación funcional para la entrega 3 ampliada"
---

# Tasks: Progreso, Recompensas y Perfil

**Input**: Design documents from `/specs/003-engagement-progress/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/firestore-data.md, quickstart.md

**Tests**: No se repiten pruebas de entrega 2 aprobada. Las validaciones nuevas se limitan a los comportamientos añadidos en esta feature.

**Organization**: Tareas agrupadas por historia, con funciones de la entrega 3 primero y capacidades adicionales de las referencias después. Esta lista no incluye la adaptación visual final.

## Dependencias y orden

```text
US2 Historial de desafíos (puede comenzar ahora, usa resultados persistidos)
US1 Rating (preguntar T002 al comenzar)
US3 Vencimiento (preguntar T001 al comenzar)
US4 Catálogo (preguntar puntos al comenzar) -> US5 Logros/métricas (preguntar T003)
US6 Cosméticos (preguntar catálogo/costos T004)
US7 Perfil/meta semanal (preguntar valor/rango T003)
Historias implementadas -> Integración funcional
Después de esta feature: adaptación visual de las referencias
```

## Phase 1: Decisiones locales (bloquean solo su función)

- [X] T001 Confirmar y registrar en `specs/003-engagement-progress/research.md` lote por sesión local, cierre a medianoche de la zona capturada al finalizar el primer desafío, vencimiento al inicio del día 30, finalización online como fecha de ganancia y procesamiento diferido; Rules valida con `request.time`, sin TTL ni Functions. Registrar el reset productivo autorizado, preparado pero no ejecutado.
- [ ] T002 Antes de implementar rating, confirmar etiquetas y si el envío se puede posponer o editar en `specs/003-engagement-progress/spec.md`; las etiquetas no aparecen enumeradas en la documentación leída.
- [ ] T003 Antes de implementar racha/progreso temporal, preguntar qué cuenta como día cumplido, política de zona horaria y valor/rango inicial de la meta semanal en `specs/003-engagement-progress/spec.md`.
- [ ] T004 Antes de implementar cosméticos, pedir selección de catálogo/IDs/costos al usuario; no inferirlos de las imágenes ni sembrarlos anticipadamente.

## Phase 2: Foundational (bloquea las historias)

- [ ] T005 Extender `app/src/main/java/com/desconectado/app/domain/model/Desafio.kt` con categoría/estado de disponibilidad y `Recompensa.kt` con tipos cosméticos extensibles, manteniendo compatibilidad de lectura con documentos actuales.
- [ ] T006 Extender `firebase/seed/validate.mjs` y `firebase/seed/seed.mjs` para validar los nuevos campos y conservar IDs históricos inactivos en vez de borrar desafíos/recompensas referenciados por movimientos existentes.
- [ ] T007 Actualizar `firebase/firestore.rules` y `specs/003-engagement-progress/contracts/firestore-data.md` con las rutas/campos nuevos y acceso de propietario, sin relajar las reglas existentes de saldo, historial ni inmutabilidad.

## Phase 3: User Story 1 - Calificar un desafío (Priority: P1)

**Goal**: Capturar una valoración idempotente después de un resultado exitoso.

**Independent Test**: Completar un desafío, guardar estrellas/etiquetas y consultarlas luego; verificar que no se solicita rating para otros estados.

- [ ] T008 [P] [US1] Escribir primero pruebas de dominio/repositorio y reglas para límites 1/5, etiquetas inválidas, estados no exitosos, dueño ajeno y reintento en `app/src/test/java/com/desconectado/app/domain/` y `firebase/tests/rules/challenge-rating.test.mjs`.
- [ ] T009 [US1] Crear `app/src/main/java/com/desconectado/app/domain/model/ChallengeRating.kt` y validaciones de puntuación/etiquetas permitidas para un resultado `COMPLETED`.
- [ ] T010 [US1] Añadir lectura/escritura de rating por `challengeRunId` en `app/src/main/java/com/desconectado/app/domain/repository/ChallengeRepository.kt` y `app/src/main/java/com/desconectado/app/data/challenges/FirestoreChallengeRepository.kt`; no modificar el resultado ni sus puntos.
- [ ] T011 [US1] Extender `firebase/firestore.rules` para permitir una única valoración del dueño solo sobre un resultado completado y aceptar únicamente campos/etiquetas permitidos.
- [ ] T012 [US1] Añadir el flujo funcional de valoración post-éxito en `app/src/main/java/com/desconectado/app/ui/challenges/DesafioActivoViewModel.kt` y `DesafioActivoScreen.kt`, con opción posponer según T002 y estados de error/reintento sin duplicar envío.

## Phase 4: User Story 2 - Consultar historiales completos (Priority: P1)

**Goal**: Mostrar todos los resultados de desafíos; el historial de canjes queda como una función separada posterior.

**Independent Test**: Consultar una cuenta con resultados de todos los estados; verificar orden descendente, integridad de campos e aislamiento.

- [X] T013 [US2] Verificar en `app/src/test/java/com/desconectado/app/ui/profile/PerfilViewModelTest.kt` que el estado de perfil conserva resultados completados, fallidos, cancelados e invalidados.
- [X] T014 [US2] Añadir `ChallengeRepository.history` en `app/src/main/java/com/desconectado/app/domain/repository/ChallengeRepository.kt` y consultar `challengeResults` en `app/src/main/java/com/desconectado/app/data/challenges/FirestoreChallengeRepository.kt`, ordenado por finalización y conservando estado/puntos.
- [X] T015 [US2] Integrar la lista completa de resultados en `app/src/main/java/com/desconectado/app/ui/profile/PerfilViewModel.kt` y `PerfilScreen.kt`, diferenciando carga, vacío, sin conexión y error; retirar el límite de cinco de la vista de historial.
- [X] T016 [US2] Reutilizar `PointsRepository.recompensasCanjeadas` en `app/src/main/java/com/desconectado/app/ui/profile/PerfilViewModel.kt` y mostrar nombre, fecha, costo y código opcional en `app/src/main/java/com/desconectado/app/ui/profile/PerfilScreen.kt`, debajo del historial de desafíos.
- [X] T017 [US2] Confirmar en `firebase/firestore.rules` que `challengeResults` conserva lectura exclusiva de la cuenta propietaria; no se requieren cambios de reglas para esta consulta.

## Phase 5: User Story 3 - Vencer puntos (Priority: P1)

**Goal**: Expirar puntos por lote sin saldo negativo ni movimientos duplicados.

**Independent Test**: Con reloj controlado, acreditar dos lotes, canjear parcialmente y ejecutar vencimiento; reconstruir saldo desde movimientos.

- [X] T018 [P] [US3] Escribir primero pruebas de dominio y reglas con reloj/zona inyectados para apertura/cierre de sesión diaria, expiración a medianoche del día 30, cambio de zona dentro de sesión, consumo FIFO parcial, expiración idempotente, canje/vencimiento y saldo reconstruido en `app/src/test/java/com/desconectado/app/domain/` y `firebase/tests/rules/points-expiration.test.mjs`. JVM pasa; el test de reglas queda escrito y `node --check` pasa; falta ejecutarlo con el emulador Java 21.
- [X] T019 [US3] Crear `PointLot` en `app/src/main/java/com/desconectado/app/domain/model/PointLot.kt`, funciones puras en `app/src/main/java/com/desconectado/app/domain/PointLots.kt` y añadir `PointMovement.Type.EXPIRE` en `app/src/main/java/com/desconectado/app/domain/model/PointMovement.kt`.
- [ ] T020 [US3] Persistir `timeZoneId` al cerrar el desafío en `ChallengeResult` y extender la transacción de `app/src/main/java/com/desconectado/app/data/points/FirestorePointsRepository.kt` para reutilizar `pointLotSessions/current` y crear lote/movimiento/saldo atómicamente.
- [ ] T021 [US3] Añadir tests de canje por etapas y luego extender `FirestorePointsRepository.redeem` con `pendingRedemptions/current`, un movimiento idempotente por lote, débitos FIFO y finalización solo al alcanzar el costo; bloquear nuevos débitos mientras pending existe.
- [ ] T022 [US3] Implementar `PointExpirationProcessor` en `app/src/main/java/com/desconectado/app/data/points/PointExpirationProcessor.kt` como procesamiento al abrir/consultar/canjear, compatible con Firestore gratuito; usar ID idempotente, `request.time` y débito limitado al remanente. No agregar TTL ni Cloud Functions; no expirar mientras haya canje pendiente.
- [ ] T023 [US3] Endurecer `firebase/firestore.rules` para validar creación de lotes, consumo y vencimiento por hora de servidor; conservar el saldo atómico y rechazar lote ajeno, exceso o transacción incompleta.
- [ ] T024 [US3] Exponer saldo disponible y próximos vencimientos en `app/src/main/java/com/desconectado/app/domain/repository/PointsRepository.kt`, `app/src/main/java/com/desconectado/app/ui/points/SaldoViewModel.kt` y `app/src/main/java/com/desconectado/app/ui/points/IndicadorPuntos.kt`.

## Phase 6: User Story 4 - Catálogo categorizado (Priority: P2)

**Goal**: Cargar las 28 propuestas y filtrar por las cuatro categorías.

**Independent Test**: Validar el catálogo sembrado, consultar Todos y cada categoría y comparar los campos con el documento de desafíos.

- [ ] T025 [US4] Incorporar las 28 propuestas de `documentos de referencia/Desafios_desconectado.docx` en `firebase/seed/catalog.json` con IDs estables, categoría, duración, dificultad, descripción, puntos aprobados y orden; conservar referencias a IDs anteriores.
- [ ] T026 [US4] Mapear categoría y disponibilidad en `app/src/main/java/com/desconectado/app/data/catalog/FirestoreCatalogRepository.kt` y `app/src/main/java/com/desconectado/app/domain/model/Desafio.kt`.
- [ ] T027 [US4] Implementar selección funcional de Todos/Moverme/Enfocarme/Socializar/Descansar en `app/src/main/java/com/desconectado/app/ui/challenges/DesafiosViewModel.kt` y `DesafiosScreen.kt` con el estilo vigente.
- [ ] T028 [P] [US4] Añadir validación automatizada del seed y pruebas de filtros/cobertura de las 28 propuestas en `firebase/tests/seed/challenges.test.mjs` y `app/src/test/java/com/desconectado/app/ui/challenges/`.

## Phase 7: User Story 5 - Logros y progreso (Priority: P2)

**Goal**: Calcular métricas y desbloquear diez logros desde resultados válidos.

**Independent Test**: Preparar historiales antes/en/después de cada umbral y comprobar conteos, progreso parcial y concesión única.

- [ ] T029 [P] [US5] Escribir primero pruebas de umbrales, semana/racha, cambio de zona horaria e idempotencia en `app/src/test/java/com/desconectado/app/domain/ProgressCalculatorTest.kt` y `firebase/tests/rules/achievements.test.mjs`.
- [ ] T030 [US5] Crear `app/src/main/java/com/desconectado/app/domain/model/Achievement.kt` y `firebase/seed/achievements.json`; extender `firebase/seed/seed.mjs` y `firebase/seed/validate.mjs` para cargar las diez definiciones de FR-009.
- [ ] T031 [US5] Implementar agregados de tiempo total, conteo por categoría, racha y meta semanal en `app/src/main/java/com/desconectado/app/domain/ProgressCalculator.kt`, usando solo desafíos completados y reglas de calendario de T003.
- [ ] T032 [US5] Persistir progreso/concesiones idempotentes por logro en `app/src/main/java/com/desconectado/app/data/profile/` y `users/{uid}/achievements/`; validar los umbrales en `firebase/firestore.rules`.
- [ ] T033 [US5] Integrar el resumen y progreso parcial con `app/src/main/java/com/desconectado/app/ui/profile/PerfilViewModel.kt` y la vista funcional de logros en `app/src/main/java/com/desconectado/app/ui/rewards/` sin rediseñar la UI objetivo.

## Phase 8: User Story 6 - Cosméticos y aplicación (Priority: P2)

**Goal**: Comprar, poseer, aplicar y conservar cosméticos; separar insignias automáticas de la tienda.

**Independent Test**: Canjear un cosmético, aplicar su efecto, reiniciar la app y verificar propiedad y preferencia; abrir una caja sin duplicar premios.

- [ ] T034 [P] [US6] Escribir primero pruebas nuevas para saldo insuficiente, propiedad, no repetición de caja, cambio de selección y persistencia en `app/src/test/java/com/desconectado/app/ui/rewards/` y `firebase/tests/rules/rewards-cosmetics.test.mjs`.
- [ ] T035 [US6] Normalizar tipos, configuración y catálogo aprobado en `app/src/main/java/com/desconectado/app/domain/model/Recompensa.kt` y `firebase/seed/catalog.json`; quitar insignias del canje nuevo sin borrar historial legado.
- [ ] T036 [US6] Extender `FirestorePointsRepository.redeem` en `app/src/main/java/com/desconectado/app/data/points/FirestorePointsRepository.kt` y `firebase/firestore.rules` para propiedad de cosméticos y caja sorpresa idempotente, con selección de artículo no poseído y costo/movimiento atómicos.
- [ ] T037 [US6] Persistir cosmético activo y aplicar paleta, fondo de enfoque, pack de íconos, marco, ícono de puntos, animación y sonido a través de `app/src/main/java/com/desconectado/app/ui/theme/`, preferencias y flujo de desafío, manteniendo el layout actual.
- [ ] T038 [US6] Exponer tienda, propiedad y opción Aplicar/Quitar en `app/src/main/java/com/desconectado/app/ui/rewards/RecompensasCanjeViewModel.kt` y `RecompensasScreen.kt`; mantener cupones existentes según FR-017.

## Phase 9: User Story 7 - Perfil y preferencias (Priority: P2)

**Goal**: Hacer editables los campos aprobados y persistir notificaciones, privacidad y meta/apariencia.

**Independent Test**: Cambiar nombre/preferencias, reiniciar, cambiar cuenta y comprobar persistencia, permisos e aislamiento.

- [ ] T039 [P] [US7] Escribir primero pruebas de validación de nombre, campos inmutables, acceso a Ajustes y persistencia/aislamiento de preferencias en `app/src/test/java/com/desconectado/app/ui/profile/` y `firebase/tests/rules/profile-preferences.test.mjs`.
- [ ] T040 [US7] Ampliar `app/src/main/java/com/desconectado/app/domain/repository/ProfileRepository.kt`, `app/src/main/java/com/desconectado/app/data/profile/FirestoreProfileRepository.kt` y `firebase/firestore.rules` para modificar únicamente el nombre visible permitido; correo e identidad de Auth siguen inmutables.
- [ ] T041 [US7] Crear `app/src/main/java/com/desconectado/app/domain/model/UserPreferences.kt` y persistir meta, apariencia y opciones de notificación en `app/src/main/java/com/desconectado/app/data/profile/` con reglas de propietario.
- [ ] T042 [US7] Añadir estados y acciones funcionales de edición, privacidad, notificaciones y apariencia en `app/src/main/java/com/desconectado/app/ui/profile/`; vincular permiso de uso a Ajustes Android, sin fotos ni permisos nuevos.

## Phase 10: Integración funcional (sin adaptación visual)

- [ ] T043 Actualizar `specs/003-engagement-progress/quickstart.md` y crear `specs/003-engagement-progress/evidencia/` con resultados por requisito FR-001 a FR-017 y referencias a capturas/outputs de los escenarios nuevos.
- [ ] T044 Ejecutar pruebas enfocadas de las historias nuevas, `npm test` y validación de seed en `firebase/`, resolver fallos introducidos por esta feature y dejar sin cambios el alcance aprobado de la entrega 2.
- [ ] T045 Verificar manualmente en dispositivo que ratings, vencimientos, historiales, filtros, logros, cosméticos y preferencias funcionan con los estilos actuales; cerrar cada criterio de `quickstart.md` antes de iniciar la réplica visual de `reference-images/`.
- [ ] T046 Preparar `firebase/maintenance/reset-delivery-3.mjs` para el reset de producción autorizado: dry-run por defecto, `--project` explícito, chunks y guardia de confirmación; conservar Auth/perfiles, dejar saldo en 0 y borrar movimientos, canjes, pending-redemptions, resultados/ratings y desafíos activos sin registrar resultado. Limpiar `lastMovementId` y estado local al siguiente arranque. No ejecutar hasta instrucción explícita de despliegue.
- [ ] T046 Preparar `firebase/maintenance/reset-delivery-3.mjs` para el reset de producción autorizado: dry-run por defecto, `--project` explícito, chunks y guardia de confirmación; conservar Auth/perfiles, dejar balance en 0 y borrar movimientos, canjes, resultados/ratings y desafíos activos sin registrar resultado. Limpiar referencias `lastMovementId` y estado local al siguiente arranque. No ejecutar hasta instrucción explícita de despliegue.

## Oportunidades de paralelismo

- T001-T004 son decisiones independientes; T005-T007 bloquean integración.
- US1 y US2 pueden avanzar en paralelo después de T007 si cada equipo conserva límites de `challengeResults`/ratings.
- US4 puede avanzar en paralelo con US1-US3 después de fijar el seed; US5 depende de categorías y resultados completos.
- US6 puede avanzar en paralelo con US5 tras acordar catálogo, pero ambos integran propiedades y perfil.
- US7 comparte perfil con US5 y debe coordinar cambios en `PerfilViewModel.kt`.
- La adaptación visual no es paralela a este plan: empieza solo después de T045.

## Estrategia de entrega

1. Implementar una función por vez; comenzar por el historial de desafíos, que no depende de las decisiones aún abiertas.
2. Preguntar cada decisión pendiente al llegar a la función que bloquea; no detener funciones independientes.
3. Cerrar el núcleo de la tercera entrega (rating, historial y vencimiento) y después avanzar a catálogo/logros/tienda/perfil.
4. Completar T043-T045 con evidencia funcional. La réplica visual de las imágenes será una feature posterior.

**Alcance deliberadamente excluido**: repetir la validación de entrega 2 aprobada; fotos/avatar capturado; cambios de layout, paleta global final, imágenes decorativas o animaciones visuales de referencia antes de terminar las funciones.