# Feature Specification: Participación en Desafíos y Canje de Puntos

**Feature Branch**: `002-challenge-participation-redemption` (pendiente de crear)

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description: "Necesito empezar a desarrollar las features necesarias para la segunda entrega. Empieza a pensar cómo podríamos seguir"

## Clarifications

### Session 2026-09-28

- Q: ¿Qué apps y qué límite de uso cuentan para cumplir cada desafío? → A: Instagram, TikTok, Facebook, X, Snapchat y YouTube; WhatsApp queda excluido y el límite permitido es cero minutos.
- Q: Si se pierde Internet mientras corre un desafío, ¿cuándo debe invalidarse? → A: Se toleran hasta cinco minutos acumulados sin conexión por desafío; la medición local continúa durante la interrupción y, si se supera el plazo, el desafío se invalida sin puntos.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Aceptar y completar un desafío (Priority: P1)

Una persona con sesión iniciada elige un desafío del catálogo, autoriza la medición de uso y lo inicia. Durante el tiempo indicado, la app mide el uso de las redes sociales definidas para el desafío. Al finalizar, la app informa si se cumplió y, si corresponde, acredita los puntos indicados en el catálogo.

**Why this priority**: Es el flujo principal que convierte el catálogo de la primera entrega en una actividad verificable y permite ganar puntos.

**Independent Test**: Con una cuenta y un desafío disponibles, iniciar una sesión, simular un uso por debajo del límite y comprobar el resultado y el saldo; repetir superando el límite y comprobar que no se acreditan puntos.

**Acceptance Scenarios**:

1. **Given** una persona autenticada, conectada y con permiso de medición, **When** elige e inicia un desafío, **Then** la app registra el desafío en curso y muestra su progreso.
2. **Given** un desafío en curso, **When** termina la duración y el uso medido queda dentro del límite, **Then** la app lo marca cumplido y acredita una sola vez los puntos del catálogo.
3. **Given** un desafío en curso, **When** el uso medido supera el límite permitido, **Then** la app lo marca no cumplido y no acredita puntos.
4. **Given** un desafío activo, **When** la persona cierra y vuelve a abrir la app, **Then** la app recupera el estado y el progreso del desafío sin reiniciar su duración.

### User Story 2 - Cancelar un desafío (Priority: P1)

La persona puede cancelar un desafío activo en cualquier momento. La cancelación termina la sesión sin otorgar puntos y permite elegir otro desafío inmediatamente.

**Why this priority**: La cancelación sin sanciones es un requisito obligatorio del proyecto y permite responder a una emergencia sin bloquear el dispositivo.

**Independent Test**: Iniciar un desafío, cancelarlo antes de completarlo y verificar que no se acreditan puntos y que se puede iniciar otro.

**Acceptance Scenarios**:

1. **Given** un desafío activo, **When** la persona confirma que quiere cancelarlo, **Then** deja de estar activo, queda registrado como cancelado y no otorga puntos.
2. **Given** un desafío cancelado, **When** la persona elige otro desafío, **Then** puede iniciarlo sin esperar a que termine la duración anterior.

### User Story 3 - Canjear una recompensa (Priority: P1)

La persona consulta las recompensas disponibles, ve su costo y confirma el canje de una recompensa que puede pagar. La app descuenta el costo y muestra la recompensa obtenida en su perfil.

**Why this priority**: El canje es parte explícita del objetivo de la entrega del 1/10 y completa el uso de los puntos que ya se muestran en el perfil.

**Independent Test**: Con saldo suficiente, canjear una recompensa y comprobar el nuevo saldo y el registro de la recompensa; repetir con saldo insuficiente y verificar que no cambia ningún dato.

**Acceptance Scenarios**:

1. **Given** saldo suficiente y una recompensa disponible, **When** la persona confirma el canje, **Then** se descuenta exactamente el costo y la recompensa aparece en sus recompensas.
2. **Given** saldo insuficiente, **When** la persona intenta confirmar un canje, **Then** el canje se rechaza, el saldo no cambia y se informa cuánto cuesta la recompensa.
3. **Given** un canje confirmado, **When** la persona vuelve a abrir la app, **Then** el saldo y la recompensa obtenida siguen visibles.

### Edge Cases

- La persona no concede el permiso de medición o lo revoca durante un desafío.
- La conexión se pierde durante un desafío o durante la confirmación de un canje.
- La persona intenta iniciar otro desafío mientras ya tiene uno activo.
- La app se cierra, el dispositivo se reinicia o cambia la hora mientras hay un desafío en curso.
- El registro de un desafío cumplido o de un canje se reintenta después de un tiempo de espera, y no debe duplicar el movimiento ni descontar dos veces.
- El catálogo de desafíos o recompensas cambia mientras la persona tiene una pantalla abierta.
- La medición no está disponible para una aplicación o devuelve un valor incompleto.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST permitir que una persona autenticada seleccione e inicie un desafío disponible del catálogo.
- **FR-002**: Antes de iniciar el primer desafío, la app MUST explicar qué datos de uso necesita y para qué; MUST permitir continuar solo después de que la persona otorgue el permiso requerido.
- **FR-003**: La app MUST mantener como máximo un desafío activo por persona y MUST mostrar el desafío, el tiempo restante y el estado de cumplimiento.
- **FR-004**: La medición MUST contar únicamente el uso de Instagram, TikTok, Facebook, X, Snapchat y YouTube; MUST NOT contar WhatsApp ni otras aplicaciones. El límite permitido para las aplicaciones incluidas MUST ser cero minutos durante el desafío.
- **FR-005**: La app MUST comparar el uso medido durante la duración del desafío con el límite permitido y determinar el resultado sin depender de una declaración de la persona.
- **FR-006**: La app MUST conservar el estado y el progreso de un desafío activo al navegar fuera de la pantalla y al volver a abrir la app.
- **FR-007**: Un desafío solo MUST otorgar los puntos indicados en el catálogo si termina dentro del límite de uso; el mismo desafío MUST NOT acreditarse más de una vez.
- **FR-008**: La app MUST permitir cancelar un desafío activo en cualquier momento; la cancelación MUST NOT otorgar puntos y MUST permitir iniciar otro desafío de inmediato.
- **FR-009**: La app MUST requerir conexión a Internet para iniciar y completar la sincronización de un desafío. Durante un desafío, MUST tolerar hasta cinco minutos acumulados sin conexión, continuar la medición local y sincronizar al recuperar la conexión; al superar cinco minutos acumulados, MUST invalidar el desafío y no acreditar puntos.
- **FR-010**: La app MUST mostrar el saldo actualizado y el resultado del desafío después de completarlo o cancelarlo.
- **FR-011**: La app MUST permitir consultar las recompensas disponibles, sus descripciones y sus costos en puntos.
- **FR-012**: La app MUST permitir confirmar un canje únicamente si el saldo alcanza para cubrir su costo; un canje rechazado MUST dejar intactos el saldo y el historial.
- **FR-013**: Un canje aceptado MUST descontar exactamente el costo de la recompensa y registrar la recompensa obtenida junto con el movimiento correspondiente, sin permitir saldos negativos ni duplicar un canje ante reintentos.
- **FR-014**: La app MUST mostrar las recompensas canjeadas en una sección personal. Los cupones MUST ser digitales, tener un código único y no representar beneficios fuera de la app.
- **FR-015**: La app MUST registrar por persona los resultados de desafíos y canjes de forma auditable, sin permitir editar ni borrar movimientos ya confirmados.
- **FR-016**: La app MUST NOT bloquear, ocultar, cerrar ni superponer contenido de otras aplicaciones. MUST NOT solicitar cámara, capturar fotos ni usar inteligencia artificial.
- **FR-017**: La medición MUST compartir solo los datos necesarios para determinar el resultado del desafío; MUST NOT publicar el uso individual de otras aplicaciones.

### Key Entities *(include if feature involves data)*

- **Desafío en curso**: desafío elegido por una persona, con inicio, duración, progreso medido y estado (activo, cumplido, no cumplido o cancelado).
- **Resultado de desafío**: resultado final asociado a la persona y al desafío, con puntos acreditados solo si se cumplió.
- **Movimiento de puntos**: cambio inmutable del saldo asociado a una persona, originado por completar un desafío o canjear una recompensa.
- **Recompensa canjeada**: recompensa del catálogo obtenida por una persona, con su costo y, si corresponde, un código digital único.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona con permisos otorgados puede iniciar un desafío en no más de tres acciones desde el catálogo.
- **SC-002**: En todos los casos de prueba de límite, el resultado coincide con el uso medido y el límite configurado para el desafío.
- **SC-003**: El 100% de los desafíos cumplidos acredita exactamente una vez los puntos del catálogo; los desafíos no cumplidos o cancelados acreditan cero puntos.
- **SC-004**: El 100% de los canjes aceptados descuenta exactamente el costo y deja un registro visible; el 100% de los canjes sin saldo suficiente deja saldo e historial sin cambios.
- **SC-005**: Después de cerrar y volver a abrir la app durante una sesión activa, la persona ve el mismo desafío y un progreso coherente, sin que el tiempo de la sesión se reinicie.
- **SC-006**: Cada requisito funcional de esta entrega tiene una prueba automatizada o un procedimiento manual documentado con resultado y evidencia referenciada.

## Assumptions

- Esta feature corresponde a la entrega del 2026-10-01 y reutiliza las cuentas, el catálogo de desafíos, el catálogo de recompensas y el saldo de la primera entrega.
- Se conservan inicialmente los seis desafíos y sus duraciones y puntos actuales, y las cinco recompensas digitales y sus costos actuales del catálogo sembrado; cualquier cambio de contenido se documentará como decisión de alcance.
- El acceso a medición de uso requiere autorización explícita del usuario; si no la concede, puede seguir usando el resto de la app, pero no iniciar desafíos.
- La cancelación se incluye en esta entrega porque la constitución la exige, aunque la tabla de entregas también mencione interrupción en la fecha del 2026-10-08.
- La entrega no incluye vencimiento de puntos, calificación, notificaciones ni el historial ampliado previsto para el 2026-10-08.
- El uso se mide en el dispositivo y se sincronizan solo los datos mínimos para guardar el resultado y los puntos; las recompensas no tienen valor fuera de la app.
- La aplicación sigue siendo nativa Android, requiere conexión para iniciar y completar desafíos y no incluye iOS, bloqueo de aplicaciones, cámara ni IA.