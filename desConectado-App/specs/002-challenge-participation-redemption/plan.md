# Implementation Plan: Participación en Desafíos y Canje de Puntos

**Branch**: `002-challenge-participation-redemption` | **Date**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-challenge-participation-redemption/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

La app permitirá iniciar un único desafío activo, medir en el dispositivo el uso de Instagram,
TikTok, Facebook, X, Snapchat y YouTube durante su duración, tolerar hasta cinco minutos
acumulados sin conexión y registrar el resultado. Un desafío cumplido acreditará sus puntos una
sola vez. La persona también podrá canjear recompensas digitales con saldo suficiente. La
implementación reutilizará Kotlin/Compose/Firebase y agregará un adaptador pequeño de estadísticas
de uso, estados persistidos del desafío y transacciones Firestore protegidas por reglas.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin, JDK 17 para Gradle

**Primary Dependencies**: Jetpack Compose/Material 3, ViewModel/StateFlow, Firebase Auth/Firestore,
`UsageStatsManager` de Android y `kotlinx-coroutines`

**Storage**: Firestore como fuente persistente; estado de un desafío activo y acumulado offline en
almacenamiento local de la app; no se guardan nombres ni datos de uso de otras apps más allá de los
totales necesarios para decidir el resultado

**Testing**: JUnit 4 y coroutines-test para lógica, Compose UI Test, pruebas instrumentadas en
Android, Firebase Emulator Suite y pruebas de reglas con `@firebase/rules-unit-testing`

**Target Platform**: Android API 26+, APK directo; el acceso de uso se concede desde Ajustes del
dispositivo y no es un permiso runtime convencional

**Project Type**: Aplicación móvil Android nativa con Firebase administrado

**Performance Goals**: iniciar un desafío en hasta tres acciones; mostrar el estado activo en menos
de 1 segundo tras abrir la app; completar la decisión de un resultado o canje en menos de 3 segundos
con conexión normal

**Constraints**: sin bloqueo de apps, cámara, IA, iOS ni Cloud Functions; cero minutos permitidos
en las apps objetivo; tolerancia offline acumulada de 5 minutos; no saldo negativo ni movimientos
editables; conexión para sincronizar el resultado

**Scale/Scope**: seis desafíos, cinco recompensas, un desafío activo por cuenta, decenas de usuarios
de demostración; esta entrega no incluye vencimiento, calificaciones, notificaciones ni historial
ampliado

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Resultado | Evidencia |
|------|-----------|-----------|
| I. Desafío, no bloqueo | PASA | Solo se consulta uso de apps; no se bloquean ni superponen otras apps. |
| II. Android nativo | PASA | Se conserva Kotlin/Compose y distribución por APK directo. |
| III. Sin IA ni cámara | PASA | La validación usa estadísticas de uso; no hay fotos ni IA. |
| IV. Evidencia verificable | PASA CONDICIONADO | El plan exige pruebas primero para medición, cumplimiento, acreditación y canje. |
| V. Entrega incremental | PASA | Se limita a participación, cancelación y canje del 2026-10-01. |
| VI. Economía íntegra | PENDIENTE DE FASE 0 | Las reglas y transacciones se prueban antes de implementar. |

No hay violaciones constitucionales. El gate VI no se considera cerrado hasta que la investigación y
las pruebas del emulador demuestren que las reglas rechazan duplicados, importes incorrectos, saldo
negativo y escrituras ajenas.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
app/src/main/java/com/desconectado/app/
├── data/challenges/       # estado activo y resultados
├── data/points/            # movimientos, saldo y recompensas canjeadas
├── data/usage/             # adaptador UsageStatsManager
├── domain/                 # reglas puras de uso, tiempo, puntos y canje
├── ui/challenges/          # catálogo, confirmación y desafío activo
├── ui/rewards/             # catálogo, confirmación y mis recompensas
└── ui/points/              # saldo compartido
app/src/test/java/com/desconectado/app/
├── domain/                 # pruebas test-first de reglas
└── ui/                     # pruebas de ViewModel
app/src/androidTest/java/com/desconectado/app/
├── data/                   # UsageStatsManager y Firestore emulator
└── ui/                     # flujos Compose
firebase/
├── firestore.rules
└── tests/rules/             # invariantes de puntos y propiedad
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| No hay violaciones | N/A | La separación de repositorios ya forma parte de la arquitectura aprobada. |
