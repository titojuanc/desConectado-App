# Data Model: Participacion en Desafios y Canje de Puntos

## ChallengeCatalogItem

Reutiliza `challenges/{id}` de la entrega 1.

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | texto | Identificador estable del catalogo. |
| `title` | texto | No vacio. |
| `durationMinutes` | entero | Mayor que 0. |
| `points` | entero | Mayor que 0; valor acreditable exacto. |
| `difficulty`, `order` | enum, entero | Reglas del catalogo existente. |

## ActiveChallenge

Documento unico `users/{uid}/activeChallenge/current`.

| Campo | Tipo | Reglas |
|---|---|---|
| `challengeId`, `challengeTitle` | texto | Copiados del catalogo al iniciar. |
| `durationMinutes`, `points` | entero | Coinciden con el catalogo al iniciar. |
| `startedAt` | timestamp | Hora del servidor o referencia sincronizada de inicio. |
| `offlineSeconds` | entero | 0 o mayor; maximo permitido antes de invalidar: 300. |
| `status` | enum | `active`, `cancelled`, `failed`, `completed`, `invalidated`. |
| `updatedAt` | timestamp | Hora del ultimo cambio. |

Solo puede existir un documento `current`. Un estado terminal se transforma en un `ChallengeResult` inmutable y el documento activo se elimina o queda marcado terminal de forma idempotente.

## ChallengeResult

Documento inmutable `users/{uid}/challengeResults/{challengeRunId}`.

| Campo | Tipo | Reglas |
|---|---|---|
| `challengeId`, `challengeTitle` | texto | Snapshot del catalogo. |
| `startedAt`, `finishedAt` | timestamp | `finishedAt` no anterior a `startedAt`. |
| `status` | enum | `completed`, `failed`, `cancelled`, `invalidated`. |
| `measuredSocialSeconds`, `offlineSeconds` | entero | No negativos; la app no almacena detalle por app. |
| `pointsAwarded` | entero | Igual a catalogo si `completed`; 0 en los demas estados. |

El id `challengeRunId` es estable para reintentos del cierre y evita duplicar el resultado.

## PointsMovement

Documento inmutable `users/{uid}/movements/{movementId}`.

| Campo | Tipo | Reglas |
|---|---|---|
| `type` | enum | `credit` o `redeem`. |
| `amount` | entero positivo | Credito suma; canje resta. |
| `challengeId` / `rewardId` | texto | Exactamente el origen correspondiente al tipo. |
| `sourceId` | texto | Para `credit`, `challengeRunId`; para `redeem`, `redemptionId`. |
| `createdAt` | timestamp | Hora del servidor. |
| `code` | texto opcional | Obligatorio y unico para cupones. |

El saldo `users/{uid}.pointsBalance` cambia en la misma transaccion y nunca puede ser negativo.

## RedeemedReward

Documento inmutable `users/{uid}/redeemedRewards/{redemptionId}`.

| Campo | Tipo | Reglas |
|---|---|---|
| `rewardId`, `name` | texto | Snapshot de la recompensa. |
| `costPoints` | entero positivo | Coincide con el catalogo al confirmar. |
| `redemptionId` | texto | `unique-{rewardId}` para insignia/tema; ID nuevo para cada cupon. |
| `movementId` | texto | Referencia al debito unico. |
| `code` | texto opcional | Obligatorio para `coupon`; fórmula exacta `DC-` + `redemptionId` codificado en Base64 URL-safe sin padding. |
| `createdAt` | timestamp | Hora del servidor. |

## State transitions

`active -> completed` si termina el tiempo y el uso social es 0; `active -> failed` si el uso es mayor que 0; `active -> invalidated` si el offline acumulado supera 300 segundos; `active -> cancelled` por accion explicita. Solo `completed` crea un credito.

## Validation invariants

- La cuenta autenticada solo puede leer y escribir su propia subcoleccion.
- `credit` usa el documento `credit-{challengeRunId}`, solo puede existir una vez, su monto debe coincidir con el catalogo y solo se crea cuando la hora del servidor supera `startedAt + durationMinutes`.
- Una insignia o tema usa el documento `unique-{rewardId}` y solo puede existir una vez; un cupon puede usar varios `redemptionId`.
- Todo `redeem` debe coincidir con el catalogo y dejar un saldo posterior no negativo.
- Movimiento, saldo, resultado y recompensa canjeada se confirman atomicamente.
- Ningun movimiento, resultado ni recompensa canjeada puede editarse o borrarse desde el cliente.
