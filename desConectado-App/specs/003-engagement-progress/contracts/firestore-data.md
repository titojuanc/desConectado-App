# Contrato Firestore: Progreso, Recompensas y Perfil

## Lecturas de catálogo

- `challenges/{challengeId}` agrega `category` y `active`. La app muestra solo activos; conservar documentos antiguos/inactivos para movimientos previos.
- `rewards/{rewardId}` agrega `kind`, `category`, `active` y `config`. El catálogo incluye 15 cosméticos aprobados y conserva los dos cupones; badges quedan archivadas. No borrar propiedad ni cambiar snapshots/costos usados por canjes anteriores.
- `achievements/{achievementId}` contiene `name`, `description`, `criterion`, `threshold`, `order`, `category` opcional y `active`; solo lectura autenticada.

## Datos por usuario

- `users/{uid}/challengeResults/{runId}`: lectura del dueño; documento terminal inmutable. `category` es un snapshot opcional (`move`, `focus`, `social`, `rest`) para compatibilidad con resultados históricos.
- `users/{uid}/challengeRatings/{runId}`: lectura del dueño; creación única solo si existe resultado completado propio; campos permitidos `challengeRunId`, `stars` (1..5), `createdAt == request.time`. Sin etiquetas ni actualización/borrado.
- `users/{uid}/pointLots/{lotId}`: lectura del dueño; el lote nace junto con crédito válido. Cambios de remanente solo como parte de transacción autorizada de canje o vencimiento; fechas e importe originales inmutables.
- `users/{uid}/movements/{movementId}`: lectura del dueño y append-only. Tipos permitidos `credit`, `redeem`, `expire`; importe positivo, ID determinista y prueba de transacción con usuario/lote/recompensa relacionados.
- `users/{uid}/pendingRedemptions/current`: lectura del dueño; como máximo un canje pendiente. Conserva snapshot `kind`/`config` y, para caja, `grantedRewardId`/nombre/tipo/config. Cada paso actualiza `pointsDebited` solo junto con movimiento `redeem`, lote FIFO y balance. Finalizar y borrar pending solo cuando `pointsDebited == costPoints`.
- `users/{uid}/redeemedRewards/{redemptionId}`: lectura del dueño, creación inmutable atómica con movimiento y snapshot de tipo/config. Una caja registra el premio asignado en el mismo documento.
- `users/{uid}/cosmeticOwnership/{rewardId}`: lectura del dueño; creación atómica solo con el canje final asociado. No se edita ni borra. La caja crea propiedad para el artículo grant, nunca para la caja.
- `users/{uid}/achievements/{achievementId}`: lectura del dueño; `achievementId`, `progress`, `threshold`, `updatedAt` y `unlockedAt` opcional. El umbral coincide con la definición activa; el progreso no disminuye y el desbloqueo usa hora servidor y queda inmutable. La app calcula el criterio; Rules no puede contar el historial completo.
- `users/{uid}/preferences/current`: lectura/escritura del dueño con `activeCosmetics`, `weeklyGoalMinutes` opcional y `notificationsEnabled` booleano (ausente equivale a false). La meta es obligatoria en Registro y se actualiza en 30..840 minutos, múltiplos de 30. Los slots conocidos solo apuntan a ownership del mismo tipo.
- `users/{uid}` permite cambiar `username` validado (1..30 caracteres) sin alterar email/Auth, saldo u otros campos; la rama económica conserva sus reglas atómicas actuales.

## Operaciones atómicas

1. **Crédito**: validar resultado completado, catálogo y tiempo de servidor; crear movimiento `credit-*`, lote y saldo actualizado.
2. **Canje**: validar costo vigente, saldo suficiente y unicidad; crear un pending que bloquea otros débitos. Android elige el siguiente lote FIFO (Rules no puede demostrar que no se omitió uno anterior; confianza aceptada por producto). Por cada lote elegido, una transacción determinista reduce lote/saldo y agrega movimiento `redeem`, actualizando `pointsDebited`. Al alcanzar costo, registrar `redeemedRewards/{redemptionId}` y borrar pending. Reintentos reanudan pasos faltantes; el perfil solo lista canjes completados.
3. **Vencimiento**: lote diario con fecha/zona capturadas en Android; débito diferido al abrir/consultar/canjear. Validar con `request.time` que `expiresAt` ya pasó, limitar débito al remanente, reducir lote y crear `expire-{lotId}` junto con saldo. FIFO reduce el remanente al consumir; reintentos son idempotentes. No procesar vencimientos mientras exista `pendingRedemptions/current`. El cliente aporta fecha/zona y Firestore no valida que sean auténticas; esta limitación fue aceptada.
4. **Caja sorpresa**: seleccionar un cosmético activo no poseído y fijarlo en pending antes del débito. Si no hay elegibles, no se inicia ni cobra. El canje final, el grant y ownership se registran atómicamente; reintentos usan el premio fijado.
5. **Logro**: conceder un ID estable una sola vez; el progreso no puede conceder puntos salvo que haya movimiento de crédito definido por catálogo.
6. **Aplicación**: activar/quitar solo IDs presentes en `cosmeticOwnership`; reglas verifican propietario y tipo de slot.

	Tras un resultado completado, el cliente recarga resultados y recalcula definiciones/progreso; al entrar al perfil vuelve a sincronizar idempotentemente. Resultados fallidos/cancelados no actualizan logros.

## Límites y migración de lanzamiento

Usar Firestore dentro de la cuota gratuita y procesar expiraciones cuando la app vuelva a consultar saldo/canje. TTL requiere billing y no es apto porque no actualiza atómicamente balance/ledger. No usar Functions. Rules valida el tiempo mínimo y las relaciones, no la zona/reloj enviados por Android. Si se agotan cuotas, dejar expiraciones pendientes para reintento y no permitir canjes con lotes ya vencidos.

Los logros no tienen valor económico: un cliente modificado puede falsificar progreso/desbloqueos. Rules limita propietario, definición, umbral declarado, monotonicidad e inmutabilidad, pero no verifica los conteos desde todo el historial.

Preparar un script Admin de reinicio, sin ejecutarlo durante desarrollo: conservar `users/{uid}` y Auth, dejar `pointsBalance` en cero y borrar `movements`, `redeemedRewards`, `challengeResults`, ratings y `activeChallenge`. Las sesiones activas se eliminan sin crear resultado. Requerir dry-run por defecto, `--project` explícito, chunks y confirmación inequívoca para producción. El cliente debe limpiar la sesión local en el próximo arranque y no poder acreditar un run iniciado antes del reset.