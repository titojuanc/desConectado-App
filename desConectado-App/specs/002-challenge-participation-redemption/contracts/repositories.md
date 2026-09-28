# Contrato: Repositorios de Participacion y Canje

**Spec**: [../spec.md](../spec.md) | **Modelo**: [../data-model.md](../data-model.md)

Todas las operaciones remotas devuelven el `Resultado` existente y errores tipados; la UI no muestra errores crudos de Firebase ni Android.

## UsageStatsRepository

```text
hasUsageAccess(): Boolean
openUsageAccessSettings(): Unit
socialUsageSeconds(start, end, packageNames): Resultado<Long>
```

Devuelve solo el total de segundos de los paquetes objetivo. Un acceso revocado o una medicion incompleta impide iniciar o finalizar el desafio y no se interpreta como cero.

## ChallengeRepository

```text
active(uid): Resultado<ActiveChallenge?>
start(uid, challengeId): Resultado<ActiveChallenge>
updateOffline(uid, runId, seconds): Resultado<Unit>
finish(uid, runId, result): Resultado<ChallengeResult>
cancel(uid, runId): Resultado<ChallengeResult>
```

`start` rechaza una segunda sesion activa. `finish` es idempotente por `runId` y solo acredita si la regla de dominio produce `completed`.

## PointsRepository

```text
redeem(uid, rewardId, redemptionId): Resultado<RedeemedReward>
saldo(uid): Resultado<Int>
recompensasCanjeadas(uid): Resultado<List<RedeemedReward>>
```

`redeem` realiza una unica operacion atomica y puede reintentarse con el mismo id. No existe una operacion generica para modificar saldo o movimientos.

## Domain services

```text
evaluarCumplimiento(usoSocialSeconds, offlineSeconds, now, desafio): ResultadoDesafio
calcularOfflineAcumulado(previous, connectionEvents): Long
validarCanje(saldo, costo): Resultado<Unit>
```

Son funciones puras, sin Android ni Firebase, y se prueban primero con limites exactos, cero uso, uso positivo, 300/301 segundos offline y saldo suficiente/insuficiente.
