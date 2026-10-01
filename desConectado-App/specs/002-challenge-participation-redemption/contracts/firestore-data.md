# Contrato: Firestore para Participacion y Canje

**Spec**: [../spec.md](../spec.md) | **Modelo**: [../data-model.md](../data-model.md)

## Rutas

| Ruta | Lectura | Creacion desde la app | Modificacion/borrado |
|---|---|---|---|
| `users/{uid}/activeChallenge/current` | dueno | dueno, una sola activa | solo transiciones validas del dueno |
| `users/{uid}/challengeResults/{runId}` | dueno | dueno, resultado idempotente | nadie |
| `users/{uid}/movements/{movementId}` | dueno | solo dentro de transaccion validada | nadie |
| `users/{uid}/redeemedRewards/{redemptionId}` | dueno | solo dentro de transaccion validada | nadie |
| `users/{uid}` | dueno | saldo inicial 0 | saldo solo en transaccion validada |
| `challenges/{id}` | autenticado | nadie | nadie |
| `rewards/{id}` | autenticado | nadie | nadie |

## Reglas de acreditacion

Una transaccion de cierre cumplido debe crear un resultado `completed`, un movimiento `credit` y actualizar el saldo. Las reglas deben comprobar que:

1. La persona es duena de todas las rutas.
2. El resultado no existe previamente y el movimiento usa el ID determinista `credit-{challengeRunId}`.
3. El desafio existe en el catalogo y el movimiento usa exactamente sus puntos.
4. El estado es `completed`, el uso social es 0 y el tiempo final es posterior al inicio y a `startedAt + durationMinutes` según `request.time`. Si las reglas no pueden expresar esta comparación, la acreditación se rechaza.
5. El saldo nuevo es el saldo anterior mas el credito.

## Reglas de canje

Una transaccion de canje debe crear `redeemedRewards/{redemptionId}`, un movimiento `redeem` y actualizar el saldo. Las reglas deben comprobar que:

1. La recompensa existe y el costo coincide.
2. El saldo anterior alcanza el costo y el nuevo saldo no es negativo.
3. El movimiento y la recompensa usan el mismo `redemptionId`/`movementId`; para insignia o tema el ID es `unique-{rewardId}`.
4. Un reintento con el mismo id no puede crear un segundo debito.
5. Un cupon tiene `redemptionId` aleatorio y `code == "DC-" + base64UrlNoPadding(redemptionId)`, no contiene datos personales y no se reutiliza; insignias y temas no pueden tener una segunda recompensa con la misma clave determinista.

Las pruebas de reglas deben intentar crear importes incorrectos, duplicados, saldo negativo, escrituras cruzadas, ediciones, borrados y lotes incompletos.
