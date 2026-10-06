# Implementation Plan: Progreso, Recompensas y Perfil

**Branch**: `003-engagement-progress` | **Date**: 2026-10-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-engagement-progress/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Comenzar por el historial completo de desafíos, que reutiliza resultados ya persistidos y no depende
de decisiones pendientes. Después cerrar la tercera entrega funcional: feedback post-desafío y
vencimiento auditable. Añadir luego las capacidades que aparecen en las referencias: catálogo categorizado,
logros, personalización y métricas/preferencias del perfil. Reutilizar los flujos aprobados de
participación y canje; mantener el estilo actual durante la implementación y hacer la adaptación
visual únicamente cuando todas las funciones estén disponibles.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin; Android Gradle Plugin/Kotlin según configuración existente; JDK 17 para compilar Android.

**Primary Dependencies**: Jetpack Compose/Material 3, ViewModel/StateFlow, Firebase Auth/Firestore, DataStore y coroutines. Firebase Admin SDK/Node.js se conserva para seed. No hay Firebase Functions configurado.

**Storage**: Firestore para resultados, movimientos, lotes, propiedad de cosméticos, logros y preferencias sincronizables; DataStore solo para preferencias locales/no sensibles cuando no deban sincronizarse.

**Testing**: pruebas JVM de reglas de dominio, pruebas de reglas Firestore con emulador y pruebas de flujo Compose solo para las nuevas funciones. No se reabre la aprobación ni se repite como alcance el testeo de entrega 2.

**Target Platform**: Android API 26+ y Firebase existente.

**Project Type**: Aplicación móvil Android nativa con Firebase administrado.

**Performance Goals**: Historiales paginados o consultados sin bloquear la pantalla; actualización de saldo/progreso coherente tras cada operación; catálogo y perfil utilizables en una conexión móvil normal.

**Constraints**: conservar reglas de propiedad/inmutabilidad y saldo no negativo; no cámara, fotos, IA, bloqueo ni beneficios externos. Los logros no se compran. Los puntos se agrupan por día y vencen 30 días después, con consumo FIFO; el corte diario y procesamiento con la app cerrada requieren decisión. La meta semanal se configura al registrarse y es editable, sin valor por defecto asumido. Solo productos Firebase gratuitos. Adaptación visual completa fuera de alcance.

**Scale/Scope**: 28 desafíos en 4 categorías, 10 logros, el conjunto de cosméticos aprobado desde los documentos y las funciones de rating, vencimiento, historiales y preferencias. Una cuenta puede tener historiales extensos; preservar IDs y snapshots ya existentes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Resultado | Evidencia y condición |
|------|-----------|-----------------------|
| I. Desafío, no bloqueo | PASA | Se conserva medición de uso; no se bloquean ni superponen aplicaciones. |
| II. Android nativo | PASA | Kotlin/Compose y Firebase existentes. |
| III. Sin IA ni cámara | PASA | Avatar con recursos integrados o iniciales; no se habilita captura/foto. |
| IV. Evidencia verificable | PASA CONDICIONADO | La constitución requiere evidencia por requisito y test-first para lógica de puntos; se verifican solo las funciones nuevas. |
| V. Entrega incremental | PASA CONDICIONADO | El usuario amplió expresamente el alcance de la entrega 3; separar núcleo de fecha 8/10 y extensiones de referencia. No implementar rediseño visual en esta feature. |
| VI. Economía íntegra | REQUIERE DISEÑO | El vencimiento diario a 30 días y el consumo FIFO están confirmados; corte de día y materialización cuando la app está cerrada siguen por definirse. Cada débito debe validarse con hora del servidor. |

La tarea de diseño debe verificar que el vencimiento pueda ejecutarse con productos Firebase
gratuitos y ser validado por hora del servidor/reglas. No asumir vencimiento perezoso, un disparador
en segundo plano ni una escritura confiable desde cliente. Si no puede cumplirse con el nivel
gratuito, presentar la limitación y pedir una decisión antes de implementar.

## Project Structure

### Documentation (this feature)

```text
specs/003-engagement-progress/
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
├── data/catalog/            # categorías y lectura del catálogo
├── data/points/             # ledger, lotes, vencimiento y canjes
├── data/challenges/         # historial y valoración de resultados
├── data/profile/            # perfil, preferencias y métricas
├── domain/model/            # lotes, valoraciones, logros y cosméticos
├── domain/                   # reglas de vencimiento, logros y agregados
└── ui/                       # estados/acciones funcionales con estilos existentes
app/src/test/java/com/desconectado/app/       # reglas nuevas de dominio/ViewModel
app/src/androidTest/java/com/desconectado/app/ # flujos Android que requieran integración
firebase/
├── firestore.rules
├── seed/catalog.json
└── tests/rules/              # propiedad e invariantes de nuevos movimientos
```

**Structure Decision**: Extender el módulo Android y los repositorios existentes, manteniendo las
reglas de Firestore como barrera de seguridad. Los datos personales y económicos permanecen bajo
`users/{uid}`; los catálogos son administrados por seed. No crear un módulo de UI nuevo ni cambiar
la navegación visual antes de cerrar los contratos de comportamiento.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Sin violaciones aprobadas | N/A | La ampliación del alcance fue solicitada para la tercera entrega; las decisiones de economía permanecen bloqueadas hasta cerrar las invariantes de Firestore. |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
