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
US4 Catálogo (fórmula de puntos aprobada) -> US5 Logros/métricas (preguntar T003)
US6 Cosméticos (preguntar catálogo/costos T004)
US7 Perfil/meta semanal (preguntar valor/rango T003)
Historias implementadas -> Integración funcional
Después de esta feature: adaptación visual de las referencias
```

## Phase 1: Decisiones locales (bloquean solo su función)

- [X] T001 Confirmar y registrar en `specs/003-engagement-progress/research.md` lote por sesión local, cierre a medianoche de la zona capturada al finalizar el primer desafío, vencimiento al inicio del día 30, finalización online como fecha de ganancia y procesamiento diferido; Rules valida con `request.time`, sin TTL ni Functions. Registrar el reset productivo autorizado, preparado pero no ejecutado.
- [X] T002 Confirmar que rating usa solo 1-5 estrellas, es obligatorio antes de volver al catálogo, no incluye etiquetas y es inmutable; registrar en `specs/003-engagement-progress/spec.md`.
- [X] T003 Confirmar y registrar el día válido (un desafío completado), zona al completar, semana lunes-domingo y meta sin default de 30..840 minutos en pasos de 30; conservar la racha durante el día actual si ayer se cumplió.
- [X] T004 Confirmar catálogo/costos de `Recompensas_desconectado.docx`; IDs slug aprobados, cupones existentes preservados, insignias retiradas de compra y caja sorpresa sin duplicados ni cobro si no hay elegibles.

## Phase 2: Foundational (bloquea las historias)

- [X] T005 Extender `app/src/main/java/com/desconectado/app/domain/model/Desafio.kt` con categoría/estado de disponibilidad, manteniendo compatibilidad de lectura con documentos actuales. La extensión de `Recompensa.kt` queda para cosméticos.
- [X] T006 Extender `firebase/seed/validate.mjs` y `firebase/seed/seed.mjs` para validar los 28 campos nuevos y archivar desafíos históricos inactivos en vez de borrarlos.
- [X] T007 Actualizar `firebase/firestore.rules` y `specs/003-engagement-progress/contracts/firestore-data.md` con las rutas/campos nuevos y acceso de propietario, sin relajar las reglas existentes de saldo, historial ni inmutabilidad. Suite completa de Rules pasa.

## Phase 3: User Story 1 - Calificar un desafío (Priority: P1)

**Goal**: Exigir una valoración de 1..5 estrellas al completar con éxito, sin etiquetas ni edición.

**Independent Test**: Completar un desafío, guardar estrellas y verificar que no se puede volver al catálogo antes; confirmar que otros estados no solicitan rating.

- [X] T008 [P] [US1] Añadir pruebas de dominio en `app/src/test/java/com/desconectado/app/domain/ChallengeRatingTest.kt`, de Compose en `app/src/androidTest/java/com/desconectado/app/ui/DesafioActivoScreenTest.kt` y de Rules en `firebase/tests/rules/challenge-rating.test.mjs` para límites, obligatoriedad, estados, propietario e inmutabilidad. JVM/Rules pasan; Compose compila, sin dispositivo para ejecutarlo.
- [X] T009 [US1] Crear `ChallengeRating` en `app/src/main/java/com/desconectado/app/domain/model/ChallengeRating.kt` con validación de estrellas 1..5 para `COMPLETED`, sin etiquetas.
- [X] T010 [US1] Añadir lectura/escritura de rating por `challengeRunId` en `ChallengeRepository.kt` y `FirestoreChallengeRepository.kt`; no modificar resultado ni puntos.
- [X] T011 [US1] Extender `firebase/firestore.rules` para permitir una sola valoración del dueño únicamente sobre resultado completado; estrellas 1..5; sin actualización ni borrado. Rules test pasa.
- [X] T012 [US1] Añadir rating obligatorio tras éxito en `DesafioActivoViewModel.kt` y `DesafioActivoScreen.kt`; impedir retorno hasta guardar y permitir reintentar sin duplicar.

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

- [X] T018 [P] [US3] Escribir primero pruebas de dominio y reglas con reloj/zona inyectados para apertura/cierre de sesión diaria, expiración a medianoche del día 30, cambio de zona dentro de sesión, consumo FIFO parcial, expiración idempotente, canje/vencimiento y saldo reconstruido en `app/src/test/java/com/desconectado/app/domain/` y `firebase/tests/rules/points-expiration.test.mjs`; JVM y Firestore Emulator pasan.
- [X] T019 [US3] Crear `PointLot` en `app/src/main/java/com/desconectado/app/domain/model/PointLot.kt`, funciones puras en `app/src/main/java/com/desconectado/app/domain/PointLots.kt` y añadir `PointMovement.Type.EXPIRE` en `app/src/main/java/com/desconectado/app/domain/model/PointMovement.kt`.
- [X] T020 [US3] Persistir `timeZoneId` en `ChallengeResult` y extender la transacción de `FirestorePointsRepository.acreditar` para reutilizar `pointLotSessions/current`, creando lote/movimiento/saldo atómicamente.
- [X] T021 [US3] Extender `FirestorePointsRepository.redeem` con `pendingRedemptions/current`, débitos FIFO idempotentes por lote y finalización solo al alcanzar el costo; bloquear otros débitos mientras pending existe. Tests JVM de modelo/ViewModel y Rules de emulador pasan.
- [X] T022 [US3] Implementar `PointExpirationProcessor` como procesamiento al abrir/consultar/canjear, compatible con Firestore gratuito; usar ID idempotente, `request.time` y débito limitado al remanente. No usar TTL ni Cloud Functions; suspender expiraciones mientras hay pending.
- [X] T023 [US3] Endurecer `firebase/firestore.rules` para lotes, pending, canjes por etapas y vencimiento; suite completa `npm test` pasa con JDK 21 (64 tests).
- [X] T024 [US3] Consultar/procesar saldo desde `PointsRepository` en `SaldoViewModel` al abrir/recargar; exponer `proximoVencimiento` desde `FirestorePointsRepository` y mostrar bajo el saldo del perfil la suma de lotes del siguiente `expiresAt` en días calendario de la zona guardada.

## Phase 6: User Story 4 - Catálogo categorizado (Priority: P2)

**Goal**: Cargar las 28 propuestas y filtrar por las cuatro categorías.

**Independent Test**: Validar el catálogo sembrado, consultar Todos y cada categoría y comparar los campos con el documento de desafíos.

- [X] T025 [US4] Incorporar las 28 propuestas en `firebase/seed/catalog.json` con categorías/datos editoriales y puntos FR-007; preservar tres IDs coincidentes y archivar los retirados con `active=false`.
- [X] T026 [US4] Mapear categoría y disponibilidad en `FirestoreCatalogRepository.kt` y `Desafio.kt`; excluir archivados de la lista activa.
- [X] T027 [US4] Implementar selección funcional de Todos/Moverme/Enfocarme/Socializar/Descansar, con Todos inicial, en `DesafiosViewModel.kt` y `DesafiosScreen.kt` usando el estilo vigente.
- [X] T028 [P] [US4] Validar catálogo y fórmula en `firebase/tests/seed/challenges.test.mjs` (24/24); el test JVM de filtros pasa y la prueba Compose se compila correctamente. No se ejecutó instrumentación por no haber dispositivo conectado.

## Phase 7: User Story 5 - Logros y progreso (Priority: P2)

**Goal**: Calcular métricas y desbloquear diez logros desde resultados válidos.

**Independent Test**: Preparar historiales antes/en/después de cada umbral y comprobar conteos, progreso parcial y concesión única.

- [X] T029 [P] [US5] Tests first de umbrales, semana/racha, cambio de zona, refresco tras completar e idempotencia/propiedad en `ProgressCalculatorTest.kt`, `AchievementCalculatorTest.kt`, `AchievementRefreshTest.kt` y `firebase/tests/rules/achievements.test.mjs`.
- [X] T030 [US5] Crear `Achievement.kt` y `firebase/seed/achievements.json`; extender `seed.mjs`/`validate.mjs` para validar y sincronizar las diez definiciones FR-009, archivando sobrantes.
- [X] T031 [US5] Implementar agregados de tiempo total, conteo por categoría, racha y semana en `ProgressCalculator.kt`, solo desde resultados completados y calendario T003; la meta es opcional hasta configuración en US7.
- [X] T032 [US5] Persistir progreso/concesiones idempotentes en `FirestoreAchievementRepository` y `users/{uid}/achievements/`; Rules valida propietario, definición activa, umbral declarado, progreso monotónico y concesión inmutable. El umbral contra todo el historial se calcula en cliente por limitación de Rules aprobada.
- [X] T033 [US5] Integrar métricas y progreso parcial al perfil con estados de carga/error/sin conexión; el test Compose compila, pero no se ejecutó en dispositivo. No se rediseñó la UI.

## Phase 8: User Story 6 - Cosméticos y aplicación (Priority: P2)

**Goal**: Comprar, poseer, aplicar y conservar cosméticos; separar insignias automáticas de la tienda.

**Independent Test**: Canjear un cosmético, aplicar su efecto, reiniciar la app y verificar propiedad y preferencia; abrir una caja sin duplicar premios.

- [X] T034 [P] [US6] Tests first de propiedad, caja sin duplicados/agotada, selección/persistencia, reanudación y canje insuficiente en tests de dominio/ViewModel y `firebase/tests/rules/rewards-cosmetics.test.mjs`.
- [X] T035 [US6] Extender tipos/configuración y cargar catálogo aprobado de 15 cosméticos más dos cupones; quitar badges de compra, archivarlas sin borrar historial y validar IDs/costos/config.
- [X] T036 [US6] Extender `FirestorePointsRepository.redeem` y Rules: snapshot en pending, grant de caja fijado, ownership en cierre atómico, protección de duplicados y no cobrar si la caja está agotada.
- [X] T037 [US6] Persistir selecciones y aplicar tema, fondo, pack de íconos, marco, estrella, animación y sonido manteniendo layouts existentes.
- [X] T038 [US6] Mostrar propiedad y controles Aplicar/Quitar; ocultar recompra de cosméticos poseídos y preservar cupones existentes. Test Compose compila, no ejecutado en dispositivo.

## Phase 9: User Story 7 - Perfil y preferencias (Priority: P2)

**Goal**: Hacer editables los campos aprobados y persistir notificaciones, privacidad y meta/apariencia.

**Independent Test**: Cambiar nombre/preferencias, reiniciar, cambiar cuenta y comprobar persistencia, permisos e aislamiento.

- [X] T039 [P] [US7] Tests first de validación de nombre/meta, campos inmutables, acceso a Ajustes y persistencia/aislamiento en tests ProfileViewModel/UserPreferences y `firebase/tests/rules/profile-preferences.test.mjs`.
- [X] T040 [US7] Actualizar `ProfileRepository`/`FirestoreProfileRepository` para permitir solo username validado; Firestore conserva email/Auth/economía inmutables.
- [X] T041 [US7] Crear `UserPreferences.kt` y repositorio Firestore para meta opcional, cosméticos y toggle general apagado por defecto con Rules de propietario/rango.
- [X] T042 [US7] Integrar edición de nombre/meta, privacidad/Ajustes, notificaciones y apariencia. Compose compila, pero no se ejecutó en dispositivo; no se agregan fotos ni permisos.

## Phase 10: Integración funcional (sin adaptación visual)

- [X] T043 Actualizar `quickstart.md` y crear `evidencia/README.md` con matriz FR-001..FR-017, resultados de suites y limitación de capturas/manual.
- [X] T044 Ejecutar tests JVM, `npm test`, validación de seed y tests de maintenance; suites finales verdes, sin alterar flujos aprobados de entrega 2.
- [ ] T045 Verificación manual en dispositivo pendiente: `adb` no está instalado/conectado en este entorno. No iniciar réplica visual hasta completar ratings, vencimientos, historiales, filtros, logros, cosméticos y preferencias en dispositivo.
- [X] T046 Preparar `firebase/maintenance/reset-delivery-3.mjs`: dry-run por defecto, `--project`, chunks y confirmación exacta; preserva Auth/perfil/preferencias, saldo 0, borra ledger/lotes/canjes/pending/resultados/ratings/logros/ownership/desafíos activos sin crear resultados, elimina `lastMovementId` y escribe marcador `delivery3ResetAt` para limpieza local al siguiente inicio. No ejecutado.

## Phase 11: Validación productiva y handoff

- [X] T047 Actualizar plan y `quickstart-produccion.md` con orden de despliegue, cuenta descartable, pruebas de conexión Android y límites de no ejecutar reset.
- [X] T048 Validar, commitear y pushear el estado local de `entrega-3`, sin incluir credenciales ni artefactos ignorados.
- [ ] T049 Revisar dry-run; desplegar solo Rules y sembrar desafíos/recompensas/logros en `des-conectado` con project explícito. No ejecutar reset.
- [ ] T050 Arrancar el AVD `desconectado`, compilar con `USE_FIREBASE_EMULATOR=false` y probar conexiones Auth/Firestore desde Android con cuenta desechable aislada.
- [ ] T051 Corregir conexiones/rules que fallen en producción y repetir tests focalizados y smoke afectados.
- [ ] T052 Entregar checklist `quickstart-produccion.md` para el teléfono real y recoger feedback del usuario antes del frontend final.

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