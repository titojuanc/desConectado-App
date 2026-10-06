# Contrato Firestore: Progreso, Recompensas y Perfil

## Lecturas de catálogo

- `challenges/{challengeId}` agrega `category` y `active`. La app muestra solo activos; conservar documentos antiguos/inactivos para movimientos previos.
- `rewards/{rewardId}` agrega `kind`, `category`, `active` y `config`. No borrar elementos poseídos ni cambiar costos usados por movimientos anteriores; preferir nuevas versiones/IDs.
- `achievements/{achievementId}` contiene definiciones de logros, solo lectura autenticada.

## Datos por usuario

- `users/{uid}/challengeResults/{runId}`: lectura del dueño; documento terminal inmutable.
- `users/{uid}/challengeRatings/{runId}`: lectura del dueño; creación solo si existe resultado completado propio, stars 1..5 y etiquetas permitidas. Actualización, si se habilita, limitada al documento de valoración y a una ventana/acción definida.
- `users/{uid}/pointLots/{lotId}`: lectura del dueño; el lote nace junto con crédito válido. Cambios de remanente solo como parte de transacción autorizada de canje o vencimiento; fechas e importe originales inmutables.
- `users/{uid}/movements/{movementId}`: lectura del dueño y append-only. Tipos permitidos `credit`, `redeem`, `expire`; importe positivo, ID determinista y prueba de transacción con usuario/lote/recompensa relacionados.
- `users/{uid}/redeemedRewards/{redemptionId}`: lectura del dueño, creación inmutable atómica con movimiento. Un reintento usa el mismo ID; una caja sorpresa guarda el premio seleccionado en el mismo registro.
- `users/{uid}/achievements/{achievementId}`: lectura del dueño; concesión única y ligada al criterio cumplido.
- `users/{uid}/preferences/current`: lectura/escritura del dueño con campos permitidos; todo cosmético activo debe estar en propiedad. `weeklyGoalMinutes` se define durante el registro y se puede actualizar; no fijar rango/default sin confirmación.

## Operaciones atómicas

1. **Crédito**: validar resultado completado, catálogo y tiempo de servidor; crear movimiento `credit-*`, lote y saldo actualizado.
2. **Canje**: validar costo vigente, saldo suficiente y unicidad; reducir lotes en orden de vencimiento, registrar propiedad/movimiento y saldo en una sola transacción.
3. **Vencimiento**: lote diario confirmado, con expiración 30 días después de haber ganado los puntos; validar con hora del servidor que el lote ya venció, limitar débito al remanente, reducir lote y crear `expire-{lotId}` junto con el saldo. FIFO reduce el remanente en el momento del consumo. Reintentos son idempotentes. El corte diario/huso y el momento de procesamiento deben confirmarse antes del contrato final.
4. **Caja sorpresa**: seleccionar una recompensa elegible aún no poseída y registrar caja y premio como una operación idempotente.
5. **Logro**: conceder un ID estable una sola vez; el progreso no puede conceder puntos salvo que haya movimiento de crédito definido por catálogo.

## Decisión técnica pendiente

No usar productos que requieran un plan pago. Comprobar si la app puede iniciar el vencimiento y validar cada débito con hora de servidor/reglas gratuitas; si el vencimiento exacto con la app cerrada no es posible sin un producto pago, explicar la limitación y pedir decisión. No asumir procesamiento perezoso ni usar Functions. El cliente nunca decide libremente el saldo, la fecha efectiva o el premio de caja.