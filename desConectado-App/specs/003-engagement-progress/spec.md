# Especificación: Progreso, Recompensas y Perfil

**Feature Branch**: `003-engagement-progress`
**Created**: 2026-10-05
**Status**: Draft
**Input**: Tercera entrega ampliada con las propuestas de desafíos y recompensas y las referencias visuales del 4/10.

## Objetivo y alcance

Completar las funciones que necesitan las pantallas objetivo antes de adaptar su presentación visual. Incluye calificar desafíos, vencer puntos, consultar historiales completos, explorar desafíos por categoría, obtener logros, personalizar la app con recompensas y consultar/editar el progreso personal. La cancelación de desafíos y el canje básico ya pertenecen a la entrega 2 aprobada y se reutilizan.

Las referencias de alcance son `documentos de referencia/Desafios_desconectado.docx`, `documentos de referencia/Recompensas_desconectado.docx` y las cuatro imágenes de `reference-images/`. La especificación describe comportamientos, no la réplica visual de esas imágenes.

## User Scenarios & Testing

### User Story 1 - Calificar un desafío (Priority: P1)

Después de completar un desafío, la persona debe calificar la experiencia con estrellas de 1 a 5 antes de volver al catálogo. La calificación queda asociada al resultado y no se puede editar.

**Why this priority**: La calificación figura en la tercera entrega y el flujo de feedback está descrito en la propuesta funcional.
**Independent Test**: Completar un desafío, enviar estrellas y verificar la valoración asociada al resultado; cancelar o fallar un desafío no debe solicitarla.

**Acceptance Scenarios**:
1. **Given** un desafío completado sin calificación, **When** la persona envía una valoración de 1 a 5 estrellas, **Then** queda guardada una sola valoración para ese resultado y se habilita volver al catálogo.
2. **Given** un desafío cancelado, fallido o invalidado, **When** termina, **Then** no se solicita una valoración post-desafío.
3. **Given** una valoración ya guardada, **When** se reintenta el envío, **Then** no se duplica ni se modifica.

### User Story 2 - Consultar historiales completos (Priority: P1)

La persona consulta todos sus desafíos y canjes, con sus fechas, estados y movimientos de puntos, del más reciente al más antiguo.

**Why this priority**: El historial forma parte explícita de la tercera entrega y la tienda necesita hacer visibles las recompensas ya obtenidas.
**Independent Test**: Generar resultados de varios estados y varios canjes; verificar orden, datos asociados y aislamiento entre cuentas.

**Acceptance Scenarios**:
1. **Given** desafíos completados, fallidos, cancelados o invalidados, **When** se abre el historial, **Then** aparecen todos con estado, fecha y puntos otorgados (cero si no correspondieron).
2. **Given** canjes anteriores, **When** se abre el historial de canjes, **Then** cada recompensa, costo, fecha y código disponible aparecen una sola vez.
3. **Given** un historial vacío o sin conexión, **When** se carga, **Then** se distingue el estado vacío del error y no se presenta una lista parcial como completa.

### User Story 3 - Entender y usar el vencimiento de puntos (Priority: P1)

La persona puede conocer qué puntos vencen y cuándo; los vencimientos se reflejan en el saldo y en un movimiento auditable.

**Why this priority**: El vencimiento está en el alcance de la entrega y modifica directamente el saldo disponible para canjear.
**Independent Test**: Acreditar lotes con fechas distintas, avanzar el reloj más allá de su vencimiento y comprobar saldo, orden de consumo y movimientos.

**Acceptance Scenarios**:
1. **Given** puntos acreditados con vencimiento futuro, **When** se consulta el saldo, **Then** la persona puede distinguir puntos disponibles y próximos a vencer.
2. **Given** que un lote alcanza su fecha de vencimiento, **When** el vencimiento se procesa, **Then** se registra una única salida por los puntos no usados y el saldo nunca es negativo.
3. **Given** un canje que consume puntos de más de un lote, **When** se confirma, **Then** se consumen primero los puntos que vencen antes y el historial permite reconstruir el saldo.

### User Story 4 - Explorar el catálogo por categoría (Priority: P2)

La persona explora desafíos de Moverme, Enfocarme, Socializar y Descansar, además de ver Todos; cada propuesta comunica duración, dificultad, puntos y una descripción motivadora.

**Why this priority**: Las categorías y el catálogo ampliado son la pantalla principal de la referencia; los seis desafíos actuales no tienen categoría.
**Independent Test**: Cargar los desafíos propuestos, filtrar cada categoría y comprobar que se muestran los elementos correctos sin alterar sus reglas de inicio.

**Acceptance Scenarios**:
1. **Given** el catálogo, **When** se elige una categoría, **Then** solo aparecen sus desafíos y Todos restaura el catálogo completo.
2. **Given** un desafío del documento de referencia, **When** se muestra, **Then** conserva nombre, descripción, duración, dificultad, categoría y puntos configurados.
3. **Given** una propuesta que no está disponible, **When** se abre el catálogo, **Then** no aparece como seleccionable.

### User Story 5 - Ver progreso y obtener logros (Priority: P2)

La persona consulta el tiempo desconectado, desafíos completados, racha, avance de una meta semanal y los logros obtenidos o pendientes. Los logros se conceden automáticamente según el progreso, no se compran con puntos.

**Why this priority**: El perfil y la pestaña Logros de las referencias se basan en esas métricas y desbloqueos.
**Independent Test**: Preparar historiales que alcancen y no alcancen cada umbral y verificar las métricas y el desbloqueo correspondiente.

**Acceptance Scenarios**:
1. **Given** desafíos completados, **When** se consulta el progreso, **Then** el tiempo total y los contadores coinciden con los resultados válidos.
2. **Given** que una persona alcanza un criterio de logro, **When** se registra el resultado que lo cumple, **Then** se concede una sola vez y se actualiza su progreso.
3. **Given** el objetivo semanal, **When** se agrega tiempo válido, **Then** el avance y el tiempo restante para la meta se actualizan sin contar desafíos no cumplidos.

### User Story 6 - Canjear y aplicar personalizaciones (Priority: P2)

La persona explora cosméticos internos, compra los que puede pagar, ve los que ya posee y aplica los elementos compatibles a la apariencia de la app.

**Why this priority**: Las imágenes muestran tienda, temas y fondos en uso; el documento propone temas, fondos de enfoque, packs de íconos, marcos, estrella, animación, sonido y caja sorpresa.
**Independent Test**: Canjear un elemento de cada tipo, verificar saldo/propiedad, aplicarlo, cerrar y reabrir la app y comprobar que la selección persiste.

**Acceptance Scenarios**:
1. **Given** una recompensa cosmética con saldo suficiente, **When** se confirma, **Then** se descuenta su costo una sola vez y pasa a ser propiedad de la persona.
2. **Given** un elemento de apariencia poseído, **When** se selecciona como activo, **Then** queda aplicado y persiste entre sesiones; no poseerlo impide activarlo.
3. **Given** una caja sorpresa, **When** se abre, **Then** entrega de forma persistente un cosmético aún no poseído, sin posibilidad de repetir el premio mediante reintentos.
4. **Given** insignias de progreso, **When** se consulta la tienda, **Then** no se ofrecen como compra: se obtienen automáticamente mediante logros.

### User Story 7 - Gestionar perfil y preferencias (Priority: P2)

La persona edita los datos de perfil permitidos, consulta privacidad y configura notificaciones y apariencia; el perfil resume sus métricas y logros.

**Why this priority**: La captura incluye editar perfil, notificaciones, privacidad y apariencia, además del resumen de progreso.
**Independent Test**: Cambiar cada preferencia, cerrar y volver a abrir la app y comprobar persistencia y respeto por permisos del dispositivo.

**Acceptance Scenarios**:
1. **Given** un perfil autenticado, **When** se modifica el nombre visible, **Then** se valida, persiste y se refleja en el perfil sin cambiar la identidad de autenticación.
2. **Given** preferencias de notificación o apariencia, **When** se actualizan, **Then** se conservan y controlan el comportamiento correspondiente.
3. **Given** la sección de privacidad, **When** se consulta, **Then** se explica el uso de estadísticas y se puede volver a los ajustes del sistema para revisar el permiso.

## Edge Cases

- Un desafío terminado se sincroniza después de que ya venció un lote de puntos o se reintenta la acreditación.
- El vencimiento y un canje concurrente afectan los mismos puntos; el saldo y los movimientos deben quedar consistentes.
- La app permanece cerrada cuando vence un lote y vuelve a abrirse sin conexión.
- Se repite un envío de valoración, se intenta valorar un resultado no exitoso o el catálogo de etiquetas cambia.
- Una recompensa cosmética poseída deja de estar en el catálogo; la persona no debe perder su propiedad ni quedar con una selección inválida.
- La caja sorpresa no tiene cosméticos elegibles restantes.
- Cambian la zona horaria, el inicio de semana o la fecha del dispositivo al calcular rachas y metas.
- Se cierra sesión durante una carga; no se deben mostrar datos de la cuenta anterior.
- La persona no concede o revoca el permiso de estadísticas; la pantalla de privacidad no debe solicitar permisos ajenos al alcance.

## Requirements

### Functional Requirements

- **FR-001**: La app MUST exigir una puntuación inmutable de 1 a 5 estrellas para cada desafío completado antes de volver al catálogo; no incluye etiquetas de feedback. Cada resultado solo admite una valoración.
- **FR-002**: La app MUST conservar los resultados de todos los estados del desafío y permitir consultar el historial completo ordenado por fecha descendente.
- **FR-003**: La app MUST mostrar el historial completo de canjes, incluyendo recompensa, costo, fecha y código cuando corresponda.
- **FR-004**: La app MUST agrupar los puntos ganados durante una sesión diaria en un lote que vence a la medianoche local del día 30 posterior a la fecha local de finalización; conservará la zona del lote al crearlo, consumirá puntos en orden FIFO y restará cada consumo del remanente de su lote.
- **FR-005**: La app MUST mostrar bajo el saldo del perfil los puntos que vencen en el siguiente vencimiento; agrupar lotes con el mismo `expiresAt` y calcular los días calendario en la zona horaria capturada por el lote.
- **FR-006**: La app MUST asignar cada desafío a una de cuatro categorías: Moverme, Enfocarme, Socializar o Descansar, además de permitir la vista Todos; Todos MUST ser el filtro inicial al abrir el catálogo.
- **FR-007**: El catálogo MUST incluir las 28 propuestas de `Desafios_desconectado.docx`, conservando categorías, títulos, descripciones, duraciones y dificultades. Los puntos MUST calcularse como `10 × duraciónMinutos / 30 × multiplicadorDificultad`, usando fácil `1`, normal `1.25` y difícil `1.5`, redondeando al múltiplo de 5 más cercano y los empates hacia arriba. La fórmula se aplica también a los seis desafíos actuales.
- **FR-008**: La app MUST derivar tiempo desconectado, cantidad de desafíos completados, racha y avance semanal únicamente de resultados completados válidos.
- **FR-009**: La app MUST conceder logros automáticamente, de forma idempotente, para: Primer paso; En marcha (5 desafíos); Modo presente (5 horas); Sin apuro (un desafío de 3 horas o más); Aire libre, Foco total, Más cerca y Tiempo para mí (5 desafíos de cada categoría); Explorador (al menos uno por categoría); 100 horas presentes.
- **FR-010**: La app MUST mostrar progreso parcial de los logros cuantificables y distinguir desbloqueados de pendientes.
- **FR-011**: La tienda MUST soportar cosméticos propios de la app: cuatro temas, fondos de enfoque, packs de íconos, marcos de perfil, estrella de puntos, animación, sonido y caja sorpresa.
- **FR-012**: Un canje MUST descontar exactamente el costo, una sola vez. Si abarca varios lotes, puede procesarse mediante débitos FIFO idempotentes por etapa; mientras queda pendiente se bloquean otros débitos y vencimientos, se reanuda automáticamente al volver a la tienda y se informa allí sin mostrarlo como canje completado. La propiedad de un elemento no se pierde al desaparecer del catálogo y la caja sorpresa no puede otorgar un elemento ya poseído.
- **FR-013**: La persona MUST poder activar únicamente personalizaciones que posee; la selección activa debe persistir y la insignia de logro MUST ser independiente de las compras.
- **FR-014**: La app MUST mostrar en el perfil tiempo desconectado total, desafíos completados, racha, meta semanal y resumen de logros.
- **FR-015**: La app MUST solicitar que la persona configure su meta semanal al registrarse y permitir ajustarla después; MUST permitir editar el nombre visible y preferencias de notificaciones/apariencia, sin modificar la identidad de autenticación desde el perfil.
- **FR-016**: La app MUST explicar la medición de uso y enlazar a los ajustes del sistema para administrar su permiso; MUST NOT pedir cámara, capturar fotos, bloquear aplicaciones ni publicar el uso individual de otras aplicaciones.
- **FR-017**: La app MUST conservar y seguir permitiendo consultar las recompensas digitales y los movimientos de canje que ya existen en la entrega 2; no se introducen beneficios externos a la app.

### Key Entities

- **Valoración de desafío**: resultado exitoso, puntuación, etiquetas y fechas de envío/actualización.
- **Lote de puntos**: acreditación, cantidad inicial y restante, vencimiento y origen; los consumos y vencimientos quedan trazables.
- **Movimiento de vencimiento**: débito inmutable asociado a uno o más lotes vencidos.
- **Desafío de catálogo**: propuesta con categoría y contenido editorial además de duración, dificultad y puntos.
- **Logro**: criterio, progreso, estado de desbloqueo y fecha de concesión.
- **Cosmético**: artículo de tienda, tipo, costo y configuración visual/funcional; propiedad separada de selección activa.
- **Preferencias de perfil**: nombre visible, selección estética, meta semanal y preferencias de notificación.

## Success Criteria

- **SC-001**: El 100% de los resultados exitosos puede recibir como máximo una valoración válida; los estados no exitosos no reciben valoración.
- **SC-002**: El saldo puede reconstruirse a partir de acreditaciones, canjes y vencimientos, nunca es negativo y no duplica movimientos ante reintentos.
- **SC-003**: El historial presenta todos los resultados y canjes de una cuenta, correctamente ordenados y aislados de otras cuentas.
- **SC-004**: Los 28 desafíos se encuentran en su categoría correcta y todos los criterios de logro producen el mismo resultado ante reintentos.
- **SC-005**: Todas las personalizaciones poseídas pueden aplicarse, sobreviven al reinicio y las no poseídas no pueden activarse.
- **SC-006**: Las métricas de perfil coinciden con los resultados válidos para total, racha y semana configurada.

## Decisiones confirmadas y pendientes

- Se reutilizan autenticación, medición, cancelación, canje, movimientos y navegación existentes; no se vuelve a construir la entrega 2.
- **Confirmado**: los puntos ganados en un mismo día forman una sesión/lote diario que vence 30 días después de haber sido ganado. El consumo es FIFO por antigüedad; lo consumido se resta del lote y solo vence su remanente.
- **Confirmado**: el lote cierra a la medianoche de la zona local capturada al ganar los puntos. Si la zona cambia durante ese mismo día, el lote original permanece abierto hasta su medianoche original. La expiración ocurre a las 00:00 local del día 30 posterior.
- **Confirmado**: si la finalización se sincroniza después, se conserva la fecha local de finalización comunicada por Android; se acepta que Firestore no pueda verificar el reloj del dispositivo. Las reglas sí deben impedir débitos antes del `expiresAt` enviado.
- **Confirmado**: no se implementa una cola offline para cierres; el resultado se guarda cuando hay conexión. El día/zona de finalización se captura en ese cierre.
- **Confirmado**: los canjes que requieren varias deducciones pueden quedar en estado pendiente entre transacciones; un solo canje pendiente por cuenta bloquea otros canjes y vencimientos, se reanuda al volver a la tienda y solo se muestra como completado después de registrar la recompensa. El estado pendiente se informa en la tienda, no en el historial del perfil.
- **Confirmado**: se procesa el vencimiento al volver a usar la app (al abrir/consultar/canjear); no se requiere expiración en segundo plano. No usar Firestore TTL ni Cloud Functions.
- **Confirmado**: el perfil muestra siempre el próximo vencimiento debajo del saldo, como “X puntos están por vencer en Y días”; agrupar lotes que vencen en el mismo instante y calcular Y con días calendario de la zona original del lote.
- **Confirmado**: en el reinicio de lanzamiento se conserva cuentas/perfiles, pero se borran saldos acumulados, movimientos, canjes e historiales de desafíos de todas las cuentas productivas; las sesiones activas se cancelan y eliminan sin dejar resultado. El reinicio se prepara con dry-run y no se ejecuta durante el desarrollo.
- **Confirmado**: la meta semanal se configura durante el registro y puede ajustarse desde la app. No definir un valor predeterminado ni un rango hasta preguntarlo.
- **Confirmado**: la racha se pierde cuando no se cumple; no se acumulan ni compensan días incumplidos. El criterio exacto de día cumplido y el huso horario se preguntarán antes de implementar esa función.
- **Confirmado**: no se usarán productos Firebase que requieran pasar a un plan pago; no planificar Cloud Functions pagas. La estrategia de ejecución del vencimiento debe verificarse dentro del nivel gratuito y no puede confiar el cálculo del saldo al cliente.
- **Confirmado**: el rating usa solo estrellas de 1 a 5, es obligatorio al completar, no ofrece etiquetas y no puede editarse.
- **Confirmado**: asignación de puntos para las 28 propuestas y los seis desafíos actuales: `10 × duraciónMinutos / 30 × multiplicadorDificultad`; multiplicadores fácil `1`, normal `1.25`, difícil `1.5`; redondeo al múltiplo de 5 más cercano, empates hacia arriba.
- **Confirmado**: el catálogo final tiene las 28 propuestas; preservar los tres IDs actuales de caminar 30 min, bici 1 h y juntarse con amigos 4 h. Los desafíos retirados se archivan y no aparecen en el catálogo activo.
- **Confirmado**: al abrir el catálogo se muestra Todos; seleccionar una categoría solo filtra durante la sesión actual.
- **Confirmado**: un día de racha cuenta con al menos un desafío completado válido, cualquiera sea su duración. La racha de ayer se conserva durante el día local actual y se rompe al terminar un día sin completar desafíos.
- **Confirmado**: cada resultado se asigna al día local usando la zona del dispositivo al completarlo; la semana empieza el lunes. La meta semanal se elige obligatoriamente durante el registro, sin valor predeterminado, entre 30 y 840 minutos en incrementos de 30; luego se puede ajustar dentro del mismo rango.
- **Confirmado**: la app calcula progreso/logros desde resultados completados; Firestore protege propiedad, monotonicidad e idempotencia, pero no puede verificar conteos arbitrarios del historial. Se acepta que un cliente modificado pueda falsificar logros, que no tienen valor económico ni otorgan puntos.
- **Confirmado**: preferencias usan un único toggle general de notificaciones, apagado por defecto. La meta semanal debe elegirse durante Registro; el flujo Google desde Registro también pasa la selección. Perfiles legacy mantienen meta sin configurar hasta que su dueña la elija.
- **Confirmado para cosméticos**: usar el catálogo/costos de `Recompensas_desconectado.docx`: Tema Bosque 100, Tema Atardecer 150, Tema Océano 150, Tema Noche 200; Pack de íconos Minimal 80 y Naturaleza 100; Fondo Montañas 60, Noche estrellada 80 y Amanecer 80; Marco de perfil 100 y Marco Naturaleza 150; Estrella especial 200; Animación de logro 120; Sonido de finalización 60; Caja sorpresa 120. Los IDs serán slugs estables de los nombres. La caja entrega un cosmético elegible aún no poseído; si no quedan elegibles, no se cobra ni se registra. Mantener cupones existentes; insignias pasan a logros automáticos y se archivan del catálogo de compra sin borrar su historial.
- Para el avatar se usarán opciones integradas o iniciales, no captura ni selección de fotos, mientras siga vigente la restricción de cámara/fotografías.
- La apariencia puede aplicar paletas, fondos, íconos y marcos. No se infiere que temas cambien la lógica de desafíos o concedan puntos.
- Esta especificación amplía la tercera entrega por decisión del usuario; la fase final de adaptación visual queda fuera de este plan funcional.