# Modelo de datos: Progreso, Recompensas y Perfil

## Desafío de catálogo

Extiende el catálogo existente con `category` (`move`, `focus`, `social`, `rest`) y `active`. Conserva ID, nombre, descripción, duración, dificultad, puntos y orden. La semilla mantiene IDs históricos; los desafíos retirados quedan inactivos, no se borran.

## Resultado y valoración

El resultado existente conserva `challengeRunId`, `challengeId`, título, inicio/fin, duración, estado, tiempo medido, offline y puntos. El historial incluye todos los estados y lee por fecha de finalización descendente.

`ChallengeRating`: `challengeRunId`, `stars` (1..5), `tags` (IDs permitidos), `createdAt`, `updatedAt`. Solo resultados `COMPLETED`; un documento por `challengeRunId`. El resultado económico permanece inmutable; la valoración tiene su propia regla de escritura limitada.

## Lote de puntos

`PointLot`: `lotId` determinista desde el movimiento de crédito, fecha de sesión diaria, `issuedPoints > 0`, `remainingPoints` entre 0 e `issuedPoints`, `earnedAt` y `expiresAt` 30 días después. Los créditos del mismo día se agrupan en ese lote. Canjes consumen FIFO por antigüedad y reducen el remanente del lote original; al vencer, solo se debita su remanente. El corte/huso horario diario y el disparador del vencimiento quedan pendientes de confirmación. El saldo del usuario se actualiza junto a movimientos/lotes.

`PointMovement`: mantiene `credit` y `redeem`; agrega `expire` con `sourceId` del lote, importe positivo, timestamp de servidor e ID determinista `expire-{lotId}`. Ningún movimiento confirmado se edita ni se borra. Los consumos y vencimientos no pueden retirar más que el remanente.

## Logro y progreso

`AchievementDefinition`: ID, nombre, descripción, criterio, categoría opcional y umbral. Catálogo inicial: Primer paso; En marcha (5 desafíos); Modo presente (5 h); Sin apuro (desafío >=3 h); Aire libre, Foco total, Más cerca, Tiempo para mí (5 por categoría); Explorador (1 por categoría); 100 horas presentes.

`UserAchievement`: ID de logro, progreso actual, umbral, `unlockedAt` opcional. El ID del documento es el ID de definición; la concesión es idempotente. Métricas derivables: desafíos completados, tiempo desconectado válido, racha consecutiva y progreso semanal. La condición exacta para considerar cumplido un día y el huso horario quedan por preguntar antes de implementar.

## Recompensa y propiedad

`RewardDefinition`: catálogo actual extendido con `kind` para coupon/theme/focus-background/icon-pack/profile-frame/point-icon/completion-animation/completion-sound/surprise-box; `costPoints`, `active`, `order`, `config` y categoría. Las insignias no son `RewardDefinition` comprables.

`RedeemedReward`: conserva los campos existentes y agrega snapshot del tipo/configuración y, para caja sorpresa, el `grantedRewardId`. La operación crea movimiento, descuenta saldo y registra propiedad en una sola transacción; `redemptionId` idempotente evita repetir caja o compra.

## Preferencias

`UserPreferences`: `activeThemeId`, `activeFocusBackgroundId`, `activeIconPackId`, `activeProfileFrameId`, `activePointIconId`, `activeCompletionAnimationId`, `activeCompletionSoundId`, `weeklyGoalMinutes` y toggles de notificación. La meta semanal se configura obligatoriamente al registrarse y puede editarse luego; valor/rango deben preguntarse antes de implementar. Cada ID activo debe pertenecer a la persona. El avatar usa ID de recurso integrado o iniciales; no una fotografía.

`Profile`: extiende el username editable y los datos calculados/mostrados sin convertir métricas en fuente independiente de verdad. Correo e identidad siguen vinculados a Firebase Auth.

## Relaciones y seguridad

- Un usuario tiene múltiples resultados, valoraciones, movimientos, lotes, canjes, logros y preferencias.
- Un crédito origina un lote; un canje o vencimiento consume uno o más lotes; cada débito referencia sus lotes consumidos.
- Catálogos son de solo lectura para clientes; historial, preferencias y propiedad son accesibles solo por la cuenta dueña.
- Puntos, logros concedidos, canjes y resultados confirmados no pueden borrarse ni alterarse retroactivamente.