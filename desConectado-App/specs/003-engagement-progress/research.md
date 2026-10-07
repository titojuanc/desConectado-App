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

La meta semanal se configura en el registro y puede cambiarse después; no hay valor inicial/rango aprobado todavía. La racha se pierde cuando no se cumple, sin acumulación/compensación. Antes de implementar métricas temporales hay que preguntar qué califica como día cumplido y cómo tratar la zona horaria. Los conteos y horas se derivan solo de desafíos completados válidos.

### Catálogos

Agregar categorías y disponibilidad sin borrar documentos históricos. Mantener los puntos existentes para IDs previos, porque los movimientos y reglas consultan catálogos; pedir los puntos de desafíos nuevos antes de sembrarlos. Mantener separado `achievement` automático de `reward` comprable. No determinar cosméticos/costos todavía; se definen cuando se implemente esa función.

### Feedback y preferencias

Una valoración por resultado exitoso, con estrellas de 1 a 5. La propuesta menciona etiquetas, pero no las enumera; pedir su definición al comenzar la función. No inferir si se puede posponer o editar. Actualizar únicamente el recurso de valoración, no reabrir el resultado financiero/inmutable.

Preferencias cosméticas y meta se sincronizan en Firestore; permisos del sistema siguen bajo control Android. La meta se recoge durante el registro y es editable luego. Usar avatar de catálogo/iniciales, sin fotos.

### Catálogo cosmético propuesto

El catálogo y los costos se definirán cuando comience la implementación de cosméticos. No sembrarlos ni inferirlos ahora. Las insignias se conceden por progreso, nunca por compra.

## Riesgos y validación de diseño

1. La ampliación de catálogos puede borrar IDs con el seed actual; conservarlos como archivados/inactivos.
2. Una caja sorpresa exige selección y registro atómicos, de modo que un reintento entregue el mismo premio.
3. Los estados de semana/racha dependen de zona horaria y cambio de fecha; preguntar la política antes de implementarla y mantenerla coherente en perfil, logros y pruebas.
4. Antes de escribir reglas, validar la operación concurrente canje-vencimiento y el límite de accesos a documentos de Firestore Rules.
5. Reinicio solicitado para el lanzamiento: conservar documentos `users/{uid}` y Auth; restablecer `pointsBalance`, eliminar movimientos/canjes/resultados de desafíos y borrar desafíos activos sin registrar resultado. Preparar script Admin con dry-run, chunks y guardia de proyecto; no ejecutarlo durante desarrollo. Firebase Admin bypassa Rules, por eso el script debe exigir project ID y modo explícito.
6. Firestore Rules no puede iterar/sumar una cantidad arbitraria de lotes dentro de una transacción. Decisión confirmada: permitir canjes por etapas, cada débito por lote se valida individualmente; un pending por cuenta bloquea otros débitos/expiraciones y se reanuda al volver a la tienda. El usuario acepta que el balance cambie gradualmente y que el pending se muestre solo en la tienda.