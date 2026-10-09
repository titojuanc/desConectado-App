# Validación funcional: Progreso, Recompensas y Perfil

Este documento cubre solo las funciones nuevas de `003-engagement-progress`. Los flujos ya aprobados de la entrega 2 se usan como precondición, no se repiten como alcance.

## Prerrequisitos

- Android con una cuenta de prueba y permiso de estadísticas de uso cuando el escenario lo requiera.
- Firebase Auth/Firestore de emulador; JDK 21 para Firebase Emulator Suite según la versión instalada.
- Catálogo con 28 desafíos, recompensas/cosméticos aprobados y definiciones de logros.
- Reloj inyectable para casos de vencimiento, racha y límite semanal.

## Escenarios

1. **Valoración**: completar un desafío; seleccionar y guardar estrellas de 1 a 5 antes de volver al catálogo. Confirmar que no hay etiquetas ni opción para omitir/editar; un resultado cancelado no ofrece calificación.
2. **Historial de desafíos**: crear resultados completados, fallidos, cancelados e invalidados en varias fechas; verificar que aparecen todos, con puntos correctos y orden descendente.
3. **Historial de canjes**: canjear elementos distintos y un cupón; verificar costo, fecha, propiedad/código y que un usuario distinto no puede leerlos.
4. **Vencimiento**: crear dos lotes con vencimientos distintos; comprobar saldo antes/después, consumo primero del lote más próximo, expiración única, reintento idempotente y rechazo de débito prematuro o excesivo. En perfil, comprobar que se muestra bajo el saldo la suma del lote que vence antes y sus días calendario en la zona original.
5. **Categorías**: comprobar Todos y cada una de las cuatro categorías; verificar los 28 datos editoriales y que los IDs históricos no desaparecen.
6. **Logros/progreso**: alcanzar umbrales por cantidad, tiempo y categoría; repetir la última operación; comprobar concesión única, progreso parcial, total acumulado, racha y semana de lunes a domingo. El día de racha usa la zona del resultado completado; la racha de ayer se conserva durante hoy. La app calcula umbrales desde resultados; Rules protege propiedad/monotonicidad, pero no puede verificar el conteo completo.
7. **Cosméticos**: canjear tema/fondo/pack/marco/ícono/animación/sonido; activarlo y reiniciar app; verificar persistencia. Probar falta de saldo, elemento no poseído, caja sorpresa con grant único y caja agotada sin débito.
8. **Perfil/preferencias**: elegir meta en Registro; editar username y meta; cambiar toggle de notificaciones y apariencia; verificar persistencia/aislamiento y que correo/Auth no se altera.
9. **Privacidad/notificaciones**: revisar explicación y acceso a Ajustes de Android; denegar/revocar permiso; desactivar notificaciones y verificar que no se solicita cámara ni acceso de accesibilidad.

## Criterio de aceptación

Para declarar una función terminada, adjuntar resultado automatizado de dominio/reglas donde sea posible y evidencia manual de los flujos Android que dependen de permisos o configuración del sistema. La matriz por requisito debe quedar referenciada en `evidencia/` de esta feature. No iniciar la réplica visual hasta que los nueve escenarios funcionales estén cubiertos.