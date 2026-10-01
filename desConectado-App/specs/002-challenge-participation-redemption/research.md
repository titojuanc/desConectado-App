# Research: Participacion en Desafios y Canje de Puntos

## R1. Medicion de uso en Android

**Decision**: encapsular `UsageStatsManager` detras de un `UsageStatsRepository`; solicitar al usuario el acceso de uso desde Ajustes y medir solo estos paquetes: Instagram `com.instagram.android`, TikTok `com.zhiliaoapp.musically`, Facebook `com.facebook.katana`, X `com.twitter.android`, Snapchat `com.snapchat.android` y YouTube `com.google.android.youtube`. Si un paquete no esta instalado, se omite y cuenta como cero uso.

**Rationale**: Android expone el tiempo de uso y eventos de foreground mediante `UsageStatsManager`, pero el acceso depende de una autorizacion especial del usuario. El adaptador permite probar la regla de cumplimiento con un reloj y un proveedor falso, sin probar la API de Android en cada test JVM. Para evitar contar otras apps, la lista de paquetes se mantiene fija y versionada; WhatsApp queda fuera. La ausencia de un paquete no es una falla de medicion.

**Alternatives considered**: `AccessibilityService` y superposicion de pantalla fueron rechazados por violar la constitucion; una declaracion del usuario no es verificable; guardar el historial completo de uso seria mas invasivo que guardar solo los totales necesarios.

## R2. Resolucion del limite de cero minutos

**Decision**: medir el uso acumulado de cada paquete objetivo dentro de la ventana `[inicio, fin]`. El resultado es cumplido solo si la suma es exactamente cero; cualquier foreground positivo marca el desafio no cumplido.

**Rationale**: coincide con la decision de producto y evita que cada desafio tenga reglas distintas. La logica pura debe aceptar intervalos y un reloj inyectable para probar limites exactos y cambios de dia.

**Alternatives considered**: permitir cinco minutos de uso haria la definicion mas flexible pero contradiria la decision tomada; muestrear solo la app visible puede perder eventos y no es suficiente como fuente unica.

## R3. Tolerancia de conexion

**Decision**: acumular el tiempo sin conexion por desafio. Hasta 5 minutos se mantiene el estado activo y la medicion local; al superar 5 minutos, el desafio se invalida sin puntos. El resultado no se publica hasta recuperar conexion, salvo que la invalidacion ya haya sido guardada.

**Rationale**: conserva la decision de permitir actividades al aire libre sin convertir una caida breve en una penalizacion. El tiempo offline se calcula con reloj monotono/instantes persistidos, no con una hora editable por el usuario.

**Alternatives considered**: invalidar inmediatamente era mas simple pero contradice la enmienda 2.3.0; tolerancia por cada interrupcion permitiria evadir el limite mediante reconexiones y por eso se usa un acumulado por desafio.

## R4. Acreditacion y canje con Firestore

**Decision**: guardar el desafio en curso y los movimientos bajo la cuenta, usar un identificador determinista para el resultado y el credito (`credit-{challengeRunId}`), y usar un identificador determinista `unique-{rewardId}` para insignias/temas y generado para cada cupon. El codigo de cada cupon se deriva del `redemptionId` aleatorio, sin datos personales ni una coleccion adicional. Movimiento mas `pointsBalance` se escriben en una transaccion. Las reglas validan propiedad, catalogo, monto, unicidad y saldo no negativo.

**Rationale**: una transaccion evita que dos reintentos consuman o acrediten el mismo saldo. El identificador determinista de un desafio hace idempotente el cierre; los IDs deterministas de credito, insignia y tema permiten expresar la unicidad con rutas Firestore. Los cupones pueden repetirse porque cada uno usa un ID propio.

**Alternatives considered**: dos escrituras independientes podrian dejar saldo y movimiento desincronizados; Cloud Functions o un servidor propio elevarian el costo y estan fuera del stack aprobado; permitir escrituras directas sin reglas no es aceptable porque el cliente no es confiable.

## R5. Duracion validada por hora del servidor

**Decision**: el cierre cumplido solo puede acreditar si la hora del servidor ya supero `startedAt + durationMinutes`. La prueba de reglas intentara imponer esta condicion mediante `request.time` y el `startedAt` fijado al crear el desafio. Si Firestore Rules no puede expresar o probar la comparacion sin Cloud Functions, la escritura de acreditacion se rechaza y el resultado queda sin puntos.

**Rationale**: evita que un cliente cierre antes de tiempo y reciba puntos. El fallback de rechazo conserva la integridad de la economia sin agregar un servidor propio.

**Alternatives considered**: confiar en el reloj del dispositivo deja una via de manipulacion; Cloud Functions seria mas autoritativo pero contradice el stack gratuito aprobado.

## R6. Codigo de recompensa

**Decision**: derivar un codigo opaco unico con la formula `DC-` + `redemptionId` codificado en Base64 URL-safe sin padding al confirmar cada cupon y guardar la recompensa canjeada como registro inmutable asociado al movimiento de debito. Insignias y temas tambien se registran sin codigo y usan una clave determinista que impide repetirlos.

**Rationale**: el codigo solo tiene valor dentro de la app y permite demostrar el canje sin integrar comercios externos. El catalogo sigue siendo de solo lectura desde el cliente.

**Alternatives considered**: reutilizar el id de recompensa permitiria colisiones entre canjes; un codigo derivado del correo expondria datos personales; beneficios reales estan fuera de alcance.

## R7. Persistencia y restauracion

**Decision**: persistir en DataStore una instantanea minima del desafio activo: id, titulo, duracion, puntos, inicio, tiempo offline acumulado y estado. Al revocar el acceso de uso se invalida de inmediato; al abrir la app se recalcula el progreso desde los instantes guardados y se finaliza de forma idempotente si la ventana ya termino.

**Rationale**: permite recuperar el flujo tras cierre o reinicio sin guardar cada muestra de uso ni crear un servicio en segundo plano complejo.

**Alternatives considered**: mantener todo solo en memoria perderia el desafio; un worker permanente seria mas complejo y no es necesario para una entrega demostrable.

## R8. Riesgo aceptado de cliente

**Decision**: aceptar que un dispositivo modificado puede falsear estadisticas de uso, tal como define la constitucion, porque las recompensas no tienen valor fuera de la app. Se registran estados y movimientos auditables y se impide la manipulacion normal mediante reglas.

**Rationale**: no se agrega verificacion de servidor ni plan pago. La prueba de seguridad se centra en duplicados, importes y propiedad.

**Alternatives considered**: verificacion autoritativa con servidor o Cloud Functions requeriria una enmienda de arquitectura y queda fuera de esta entrega.
