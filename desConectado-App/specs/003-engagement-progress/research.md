# Investigación: Progreso, Recompensas y Perfil

**Fecha**: 2026-10-05
**Estado**: decisiones de producto confirmadas; estrategia técnica limitada al plan gratuito.

## Hallazgos del repositorio

- El catálogo actual está en `firebase/seed/catalog.json`: 6 desafíos sin categoría y 5 recompensas. `FirestoreCatalogRepository` descarta campos desconocidos y `Desafio`/`Recompensa` no representan etiquetas de categoría ni aplicación cosmética.
- `firebase/seed/seed.mjs` sincroniza cada colección exactamente y elimina documentos ausentes del JSON. No quitar IDs antiguos: movimientos y reglas de la entrega aprobada los referencian.
- Los resultados se guardan por estado, pero la consulta de perfil actual lee solo movimientos `credit` y limita a cinco. Los canjes sí se consultan en el repositorio, aunque no se presentan como historial en la pantalla actual.
- El documento `users/{uid}` permite cambiar saldo y último movimiento, pero no nombre ni preferencias. Las reglas admiten `credit` y `redeem`; todavía no admiten `expire` ni lotes de puntos.
- No hay Firebase Functions configurado en `firebase/firebase.json`. El Android compila con JDK 17; el emulador Firebase instalado requiere Java 21 para correr las pruebas de reglas.
- El perfil actual no almacena avatar, racha, meta semanal, tiempo acumulado ni logros. No hay tema seleccionable ni aplicación de cosméticos.

## Decisiones confirmadas y enfoque

### Vencimiento y ledger

**Confirmado por el usuario**: un lote por sesión diaria, corte a la medianoche de la zona local capturada al primer crédito. Si cambia de zona durante la sesión, el lote conserva su corte original. Vence a las 00:00 local del día 30 posterior a la fecha local de finalización. FIFO; cada consumo reduce el remanente, y solo se vence lo que quede. Se usa `finishedAt`/zona Android incluso si sincroniza tarde; el usuario acepta esa limitación de confianza en el reloj cliente.

La expiración se procesa al volver a usar la app (abrir/consultar/canjear), no en segundo plano. El cierre del desafío sigue requiriendo conexión; no se agrega cola offline. Firestore Rules compara `expiresAt` con `request.time`; la zona local y la fecha de finalización vienen del cliente y no se pueden demostrar con reglas. El usuario aceptó expresamente esta limitación. Rules limita escrituras a la cuenta propia y rechaza expiración anticipada/saldo inconsistente.

La documentación de precios de Firestore indica que TTL no tiene cuota gratuita. Rules/Firestore sí tienen cuotas gratuitas de operaciones (sujetas a límites diarios); el cliente procesa vencimientos con `request.time`. No usar TTL, Cloud Functions ni producto que requiera billing. Si se agotan cuotas, los vencimientos pendientes se reintentan en otra consulta.

### Logros y métricas

**Confirmado por el usuario**: un día de racha cuenta con al menos un desafío completado válido, sin duración mínima. La racha se conserva durante el día actual si se cumplió ayer y se rompe al cerrar un día local incumplido; no se compensa. Cada resultado se asigna a su fecha con la zona capturada al completarlo. La semana va de lunes a domingo. La meta se elige durante el registro sin valor predeterminado, entre 30 y 840 minutos en incrementos de 30; se puede ajustar después en el mismo rango.

Los conteos y horas se derivan solo de resultados completados válidos. La categoría se guarda como snapshot del resultado para que cambios futuros del catálogo no alteren logros. Firestore puede imponer acceso propietario, progreso monotónico e idempotencia, pero Rules no puede contar una colección arbitraria de resultados; se acepta cálculo cliente porque los logros no tienen valor económico ni otorgan puntos. Un cliente modificado podría falsificarlos.

### Catálogos

Agregar las 28 propuestas en cuatro categorías con Todos como vista inicial. Puntos confirmados: `10 × duraciónMinutos / 30 × multiplicadorDificultad`, multiplicadores fácil `1`, normal `1.25`, difícil `1.5`; redondear al múltiplo de 5 más cercano con empate hacia arriba. Se aplica también a los seis desafíos actuales; preservar tres IDs coincidentes y archivar los retirados sin borrarlos. Mantener separado `achievement` automático de `reward` comprable. No determinar cosméticos/costos todavía; se definen cuando se implemente esa función.

### Feedback y preferencias

Una valoración inmutable por resultado exitoso, con estrellas de 1 a 5, obligatoria antes de volver al catálogo y sin etiquetas. Guardar rating en un recurso separado; no reabrir el resultado financiero/inmutable.

Preferencias cosméticas y meta se sincronizan en Firestore; permisos del sistema siguen bajo control Android. La meta se recoge durante el registro y es editable luego. Hay un único toggle de notificaciones apagado por defecto. Perfil permite editar solo el nombre visible; correo/Auth permanecen inmutables. Privacidad enlaza a Ajustes Android de estadísticas de uso. Usar avatar de catálogo/iniciales, sin fotos.

### Catálogo cosmético confirmado

Usar los 15 artículos y costos de `documentos de referencia/Recompensas_desconectado.docx`: cuatro temas (100/150/150/200), dos packs de íconos (80/100), tres fondos (60/80/80), dos marcos (100/150), estrella especial (200), animación de logro (120), sonido (60) y caja sorpresa (120). El usuario aprobó IDs slug derivados de los nombres. La caja elige solo artículos cosméticos no poseídos y no cobra si no hay elegibles. Conservar cupones existentes; retirar insignias de compra sin borrar canjes históricos; las insignias se obtienen por logros.

## Riesgos y validación de diseño

1. La ampliación de catálogos puede borrar IDs con el seed actual; conservarlos como archivados/inactivos.
2. Una caja sorpresa exige selección y registro atómicos, de modo que un reintento entregue el mismo premio.
3. Los estados de semana/racha dependen de zona horaria y cambio de fecha; preguntar la política antes de implementarla y mantenerla coherente en perfil, logros y pruebas.
4. Antes de escribir reglas, validar la operación concurrente canje-vencimiento y el límite de accesos a documentos de Firestore Rules.
5. Reinicio solicitado para el lanzamiento: conservar documentos `users/{uid}` y Auth; restablecer `pointsBalance`, eliminar movimientos/canjes/resultados de desafíos y borrar desafíos activos sin registrar resultado. Preparar script Admin con dry-run, chunks y guardia de proyecto; no ejecutarlo durante desarrollo. Firebase Admin bypassa Rules, por eso el script debe exigir project ID y modo explícito.
6. Firestore Rules no puede iterar/sumar una cantidad arbitraria de lotes dentro de una transacción ni probar que el cliente no omitió un lote más antiguo. Decisión confirmada: Android selecciona FIFO; Rules valida saldo de cada lote, delta de balance, idempotencia y pending. Un pending por cuenta bloquea otros débitos/expiraciones y se reanuda al volver a la tienda. El usuario acepta esta confianza en el cliente y que el pending se muestre solo en la tienda.