# Implementation Plan: Participación en Desafíos y Canje de Puntos

**Branch**: `002-challenge-participation-redemption` | **Date**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-challenge-participation-redemption/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

La app permitirá iniciar un único desafío activo, medir en el dispositivo el uso de Instagram,
TikTok, Facebook, X, Snapchat y YouTube durante su duración, tolerar hasta cinco minutos
acumulados sin conexión y registrar el resultado. Un desafío cumplido acreditará sus puntos una
sola vez. La persona podrá repetir cupones con saldo suficiente, pero cada insignia o tema solo
una vez. La implementación reutilizará Kotlin/Compose/Firebase y agregará un adaptador pequeño de
estadísticas de uso, estados persistidos del desafío y transacciones Firestore protegidas por reglas.

## Technical Context

**Language/Version**: Kotlin, JDK 17 para Gradle

**Primary Dependencies**: Jetpack Compose/Material 3, ViewModel/StateFlow, Firebase Auth/Firestore,
`UsageStatsManager` de Android y `kotlinx-coroutines`; la lista de paquetes objetivo queda fijada
en `research.md`

**Storage**: Firestore como fuente persistente; estado de un desafío activo y acumulado offline en
DataStore local; no se guardan nombres ni datos de uso de otras apps más allá de los totales
necesarios para decidir el resultado

**Testing**: JUnit 4 y coroutines-test para lógica, Compose UI Test, pruebas instrumentadas en
Android, Firebase Emulator Suite y pruebas de reglas con `@firebase/rules-unit-testing`

**Target Platform**: Android API 26+, APK directo; el acceso de uso se concede desde Ajustes del
dispositivo y no es un permiso runtime convencional

**Project Type**: Aplicación móvil Android nativa con Firebase administrado

**Performance Goals**: iniciar un desafío en hasta tres acciones; mostrar el estado activo en menos
de 1 segundo tras abrir la app; completar la decisión de un resultado o canje en menos de 3 segundos
con conexión normal; estos tres objetivos se medirán en los guiones manuales de `quickstart.md`

**Constraints**: sin bloqueo de apps, cámara, IA, iOS ni Cloud Functions; cero minutos permitidos
en las apps objetivo; una app objetivo ausente cuenta como cero; revocar el acceso invalida de
inmediato; tolerancia offline acumulada de 5 minutos; no saldo negativo ni movimientos editables;
conexión para sincronizar el resultado

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
| VI. Economía íntegra | PASA CONDICIONADO | Las reglas y transacciones se prueban antes de implementar, incluida la duración server-side. |

No hay violaciones constitucionales. El gate VI no se considera cerrado hasta que la investigación y
las pruebas del emulador demuestren que las reglas rechazan duplicados, importes incorrectos, saldo
negativo y escrituras ajenas.

### Post-Design Re-evaluation

| Gate | Resultado | Evidencia posterior al diseño |
|------|-----------|-------------------------------|
| I-III | PASA | `research.md` rechaza accesibilidad, bloqueo, cámara e IA; `data-model.md` solo guarda totales. |
| IV | PASA CONDICIONADO | `quickstart.md` exige evidencia para FR-001 a FR-017 y las tareas deberán escribir pruebas antes del código. |
| V | PASA | `data-model.md` limita la entrega a un desafío activo, resultados, puntos y canje. |
| VI | PASA CONDICIONADO | `contracts/firestore-data.md` define transacciones, IDs deterministas y duración server-side; queda validarlo en el emulador antes de implementar. |

No se agregan permisos de bloqueo, cámara o geolocalización, ni servidor propio. La dependencia de
`UsageStatsManager` queda aislada detrás de un contrato y con validación manual en dispositivo real.

## Project Structure

### Documentation (this feature)

```text
specs/002-challenge-participation-redemption/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
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

**Structure Decision**: Un único módulo `app`, siguiendo las capas y convenciones de la entrega 1.
La medición se encapsula detrás de un repositorio para probar la lógica sin depender del dispositivo;
Firestore se accede mediante repositorios y las operaciones de saldo se agrupan en transacciones.
No se agrega servidor propio ni Cloud Functions.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| No hay violaciones | N/A | La separación de repositorios ya forma parte de la arquitectura aprobada. |
