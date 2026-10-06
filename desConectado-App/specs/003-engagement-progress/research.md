# Investigación: Progreso, Recompensas y Perfil

**Fecha**: 2026-10-05
**Estado**: decisiones confirmadas y dudas técnicas/producto pendientes.

## Hallazgos del repositorio

- El catálogo actual está en `firebase/seed/catalog.json`: 6 desafíos sin categoría y 5 recompensas. `FirestoreCatalogRepository` descarta campos desconocidos y `Desafio`/`Recompensa` no representan etiquetas de categoría ni aplicación cosmética.
- `firebase/seed/seed.mjs` sincroniza cada colección exactamente y elimina documentos ausentes del JSON. No quitar IDs antiguos: movimientos y reglas de la entrega aprobada los referencian.
- Los resultados se guardan por estado, pero la consulta de perfil actual lee solo movimientos `credit` y limita a cinco. Los canjes sí se consultan en el repositorio, aunque no se presentan como historial en la pantalla actual.
- El documento `users/{uid}` permite cambiar saldo y último movimiento, pero no nombre ni preferencias. Las reglas admiten `credit` y `redeem`; todavía no admiten `expire` ni lotes de puntos.
- No hay Firebase Functions configurado en `firebase/firebase.json`. El Android compila con JDK 17; el emulador Firebase instalado requiere Java 21 para correr las pruebas de reglas.
- El perfil actual no almacena avatar, racha, meta semanal, tiempo acumulado ni logros. No hay tema seleccionable ni aplicación de cosméticos.

## Decisiones confirmadas y enfoque

### Vencimiento y ledger

**Confirmado por el usuario**: agrupar los puntos ganados cada día en una sesión/lote diario que vence 30 días después de haber sido ganado. Consumir en orden de antigüedad (FIFO); cada consumo reduce el remanente del lote de origen, por lo que en la fecha de vencimiento solo se debita lo que todavía queda de ese lote.

Pendiente antes de implementar: acordar el corte del día/huso horario de cada lote y cuándo se materializa un vencimiento si la app está cerrada. El proyecto debe mantenerse en productos Firebase del nivel gratuito; no incluir Cloud Functions pagas. Evaluar una operación iniciada por la app, protegida por reglas que comparen con hora de servidor, y comprobar que conserva las invariantes y cuotas gratuitas. Nunca aceptar fecha o saldo calculados únicamente por el cliente.

No se elige una fecha/huso ni un disparador por inferencia. Si el vencimiento exacto en segundo plano no es posible con los servicios gratuitos, se explicará la limitación y se pedirá una decisión antes de adoptar vencimiento perezoso.

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