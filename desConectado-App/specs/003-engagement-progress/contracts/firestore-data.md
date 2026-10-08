# Contrato Firestore: Progreso, Recompensas y Perfil

## Lecturas de catálogo

- `challenges/{challengeId}` agrega `category` y `active`. La app muestra solo activos; conservar documentos antiguos/inactivos para movimientos previos.
- `rewards/{rewardId}` agrega `kind`, `category`, `active` y `config`. No borrar elementos poseídos ni cambiar costos usados por movimientos anteriores; preferir nuevas versiones/IDs.
- `achievements/{achievementId}` contiene definiciones de logros, solo lectura autenticada.

## Datos por usuario

- `users/{uid}/challengeResults/{runId}`: lectura del dueño; documento terminal inmutable.
- `users/{uid}/challengeRatings/{runId}`: lectura del dueño; creación única solo si existe resultado completado propio; campos permitidos `challengeRunId`, `stars` (1..5), `createdAt == request.time`. Sin etiquetas ni actualización/borrado.
- `users/{uid}/pointLots/{lotId}`: lectura del dueño; el lote nace junto con crédito válido. Cambios de remanente solo como parte de transacción autorizada de canje o vencimiento; fechas e importe originales inmutables.
- `users/{uid}/movements/{movementId}`: lectura del dueño y append-only. Tipos permitidos `credit`, `redeem`, `expire`; importe positivo, ID determinista y prueba de transacción con usuario/lote/recompensa relacionados.
- `users/{uid}/pendingRedemptions/current`: lectura del dueño; como máximo un canje pendiente. Cada paso actualiza `pointsDebited` solo junto con un movimiento `redeem`, el lote FIFO y el balance. Crear la recompensa final y borrar el pending solo cuando `pointsDebited == costPoints`.
- `users/{uid}/redeemedRewards/{redemptionId}`: lectura del dueño, creación inmutable atómica con movimiento. Un reintento usa el mismo ID; una caja sorpresa guarda el premio seleccionado en el mismo registro.
- `users/{uid}/achievements/{achievementId}`: lectura del dueño; concesión única y ligada al criterio cumplido.
- `users/{uid}/preferences/current`: lectura/escritura del dueño con campos permitidos; todo cosmético activo debe estar en propiedad. `weeklyGoalMinutes` se define durante el registro y se puede actualizar; no fijar rango/default sin confirmación.

## Operaciones atómicas

1. **Crédito**: validar resultado completado, catálogo y tiempo de servidor; crear movimiento `credit-*`, lote y saldo actualizado.
2. **Canje**: validar costo vigente, saldo suficiente y unicidad; crear un pending que bloquea otros débitos. Android elige el siguiente lote FIFO (Rules no puede demostrar que no se omitió uno anterior; confianza aceptada por producto). Por cada lote elegido, una transacción determinista reduce lote/saldo y agrega movimiento `redeem`, actualizando `pointsDebited`. Al alcanzar costo, registrar `redeemedRewards/{redemptionId}` y borrar pending. Reintentos reanudan pasos faltantes; el perfil solo lista canjes completados.
3. **Vencimiento**: lote diario con fecha/zona capturadas en Android; débito diferido al abrir/consultar/canjear. Validar con `request.time` que `expiresAt` ya pasó, limitar débito al remanente, reducir lote y crear `expire-{lotId}` junto con saldo. FIFO reduce el remanente al consumir; reintentos son idempotentes. No procesar vencimientos mientras exista `pendingRedemptions/current`. El cliente aporta fecha/zona y Firestore no valida que sean auténticas; esta limitación fue aceptada.
4. **Caja sorpresa**: seleccionar una recompensa elegible aún no poseída y registrar caja y premio como una operación idempotente.
5. **Logro**: conceder un ID estable una sola vez; el progreso no puede conceder puntos salvo que haya movimiento de crédito definido por catálogo.

## Límites y migración de lanzamiento

Usar Firestore dentro de la cuota gratuita y procesar expiraciones cuando la app vuelva a consultar saldo/canje. TTL requiere billing y no es apto porque no actualiza atómicamente balance/ledger. No usar Functions. Rules valida el tiempo mínimo y las relaciones, no la zona/reloj enviados por Android. Si se agotan cuotas, dejar expiraciones pendientes para reintento y no permitir canjes con lotes ya vencidos.

Preparar un script Admin de reinicio, sin ejecutarlo durante desarrollo: conservar `users/{uid}` y Auth, dejar `pointsBalance` en cero y borrar `movements`, `redeemedRewards`, `challengeResults`, ratings y `activeChallenge`. Las sesiones activas se eliminan sin crear resultado. Requerir dry-run por defecto, `--project` explícito, chunks y confirmación inequívoca para producción. El cliente debe limpiar la sesión local en el próximo arranque y no poder acreditar un run iniciado antes del reset.