<!--
SYNC IMPACT REPORT (temporal; eliminar antes de commitear)
Version change: 2.0.0 → 2.1.0
Bump: MINOR. Se resuelve una decisión abierta (Firebase App Check queda descartado porque el
  proyecto no se publicará en Google Play), se agrega un elemento a "Fuera de alcance" y se
  posterga una regla de seguridad hasta comprobarla en la práctica. Ninguna redefinición
  incompatible de un principio.
Principios modificados:
  II. Android Nativo (distribución solo por APK directo, sin Google Play)
  VI. Integridad de la Economía de Puntos (la condición "no antes de que transcurra la duración"
      deja de ser exigible hasta comprobar su viabilidad)
Secciones agregadas: ninguna
Secciones modificadas: Fuera de alcance, Decisiones resueltas, Decisiones abiertas
Secciones eliminadas: ninguna
Decisión resuelta: evaluación de App Check (descartado)
Plantillas y artefactos dependientes (no se modifican aquí): sin pendientes; los de
  specs/001-auth-profile-catalog se alinearon con esta versión en el mismo cambio.
TODOs diferidos (a resolver con el usuario):
  - TODO(MEDICION_USO)  - TODO(OFFLINE_TOLERANCIA)  - TODO(NOTIFICACIONES_FRECUENCIA)
  - TODO(CATALOGO_RECOMPENSAS)  - TODO(CATALOGO_DESAFIOS)  - TODO(PUNTOS_SEGURIDAD)
-->

# (des)Conectado Constitution

(des)Conectado es una app móvil que fomenta el no-uso de redes sociales para "conectar
desconectando" con los vínculos reales entre personas. El usuario elige un desafío, lo inicia, y
la app mide su uso del teléfono; si lo cumple, gana puntos canjeables dentro de la app. Los
documentos de referencia (`documentos de referencia/`) son la propuesta del cliente
(`(DES)CONECTADO-V1.md`) y el plan de entregas (`Entregables - (des)conectado.md`). Cuando
contradicen esta constitución, prevalece esta constitución.

## Core Principles

### I. Desafío, no Bloqueo (NON-NEGOTIABLE)
La app NO bloquea, oculta, cierra ni superpone pantallas sobre otras aplicaciones. Presenta un
desafío genuino y deja la decisión al usuario. Reglas:
- La app MUST NOT solicitar Servicios de Accesibilidad, administrador de dispositivo ni permiso
  para mostrarse sobre otras apps.
- El cumplimiento de un desafío MUST determinarse a partir del uso medido de las apps de redes
  sociales de una lista fija predefinida; el resto del uso del teléfono (llamadas, mensajería,
  cámara, mapas) MUST NOT contar contra el límite. No basta una declaración del usuario.
- El límite de uso de cada desafío MUST ser lo bastante exigente para constituir un desafío real.
- Un desafío incumplido o invalidado (incluida la pérdida de conexión, ver "Arquitectura de
  datos") se registra como "no cumplido" y no otorga puntos. La app MUST NOT aplicar
  restricciones ni sanciones sobre el dispositivo.
- El usuario MUST poder cancelar o interrumpir un desafío en cualquier momento; cancelar no otorga
  puntos y permite iniciar otro de inmediato.

*Rationale*: el público objetivo busca autocontrol positivo basado en recompensas, no en bloqueos
punitivos. El bloqueo fue descartado explícitamente para el MVP.

### II. Android Nativo
El MVP es una aplicación Android nativa, instalable en un dispositivo real mediante un APK
distribuido directamente. La app MUST NOT publicarse en Google Play ni depender de que esté
publicada allí. iOS queda fuera de alcance: el proyecto MUST NOT incluir código, dependencias ni
trabajo de diseño específicos de iOS. La app MUST NOT ser una Web App/PWA. El framework y las
herramientas se fijan en "Stack tecnológico".

*Rationale*: reducir alcance a una plataforma para llegar con calidad a cada entrega.

### III. Sin IA ni Cámara: Validación por Medición de Uso
No se dispone de API de IA ni se integrará cámara. El MVP MUST NOT capturar, procesar ni
almacenar fotografías, ni invocar servicios de IA. No existe un paso de verificación aparte: el
resultado de un desafío es el resultado de comparar el uso medido con su límite. La app MUST NOT
presentarse ante el cliente como si tuviera verificación por IA.

*Rationale*: la "verificación por IA" de la propuesta original dependía de fotos de inicio y fin,
que quedan fuera del MVP; la medición de uso cumple la misma función de validar el desafío.

### IV. Evidencia Verificable por Entrega (NON-NEGOTIABLE)
Cada entrega MUST incluir pruebas concretas de todo lo que se comprometió. Reglas:
- Cada requisito funcional entregado MUST tener al menos una prueba (automatizada cuando sea
  viable; si no, un procedimiento manual documentado con su resultado).
- La lógica de negocio (cumplimiento de desafíos, asignación de puntos, canje, vencimiento) MUST
  desarrollarse test-first: prueba escrita, vista fallar, luego implementación.
- Ninguna funcionalidad puede declararse entregada sin su evidencia referenciada en la entrega.

*Rationale*: el plan de entregas exige "pruebas concretas de lo que se pide hacer" en cada fecha.

### V. Alcance Mínimo, Entrega Incremental
El proyecto se construye en las 4 entregas definidas en "Flujo de Trabajo y Entregas". Reglas:
- Cada entrega MUST dejar la app en un estado funcional y demostrable.
- Ningún trabajo MUST adelantarse desde una entrega posterior ni desde "Fuera de alcance" sin una
  enmienda aprobada a esta constitución.
- Ante la duda entre una solución simple y una flexible, MUST elegirse la simple (YAGNI); la
  complejidad adicional MUST justificarse por escrito en el plan.

*Rationale*: hay cuatro fechas fijas; el riesgo principal es el alcance, no la técnica.

### VI. Integridad de la Economía de Puntos
La economía de puntos MUST ser correcta, auditable y probable. La acreditación, el canje y el
vencimiento se originan en la app; los puntos y su historial residen en Firestore, protegidos por
reglas de seguridad. Reglas:
- Los puntos MUST acreditarse únicamente al cumplir un desafío, una sola vez por desafío.
- El saldo MUST derivarse de un registro de movimientos (acreditaciones, canjes, vencimientos) y
  MUST NOT ser negativo.
- Los puntos y su historial MUST persistir en Firestore, asociados a la cuenta, y MUST conservarse
  sin modificarse retroactivamente.
- Como el cliente no es de confianza, las reglas de seguridad de Firestore MUST rechazar toda
  escritura que incumpla estas invariantes verificables:
  1. Solo se escribe en los datos de la propia cuenta.
  2. El registro de movimientos es solo de agregado: no se edita ni se borra.
  3. El monto de una acreditación es igual a los puntos que el catálogo asigna al desafío, y ocurre
     una sola vez por desafío.
  4. Un canje tiene el costo que el catálogo asigna a la recompensa y solo procede con saldo
     suficiente.
  5. El saldo cambia únicamente junto con un movimiento y exactamente por el monto de este.
- Las reglas MUST versionarse en el repositorio y tener pruebas automatizadas contra el emulador
  que intenten manipular los puntos: acreditar dos veces o con un monto distinto, dejar el saldo
  negativo, editar o borrar historial, canjear sin saldo o con otro costo, y escribir en la cuenta
  de otra persona.
- Queda pendiente, sin ser exigible todavía, una condición adicional: que una acreditación no ocurra
  antes de que transcurra la duración del desafío según la hora del servidor. Solo se incorporará
  a la lista anterior si una prueba práctica confirma que las reglas pueden imponerla (ver
  TODO(PUNTOS_SEGURIDAD)).
- El vencimiento (30 días desde la acreditación) y el consumo de los puntos más antiguos primero
  (FIFO) MUST aplicarse en la app de forma determinista y ser probables mediante un reloj
  simulable. Las reglas de seguridad no pueden verificarlos; son reglas de equidad, no de
  seguridad.
- **Riesgo aceptado**: un dispositivo modificado puede falsear el resultado de la medición de uso,
  y ninguna regla puede comprobarlo. Se acepta mientras las recompensas no tengan valor fuera de la
  app. Si alguna vez lo tuvieran, MUST reintroducirse una verificación en servidor mediante una
  enmienda a esta constitución.

*Rationale*: la economía de puntos es el mecanismo de motivación; un error en ella destruye la
confianza del usuario. Un servidor propio o Cloud Functions requerirían un plan de pago, y con
recompensas ficticias el costo de una manipulación es bajo; las reglas de seguridad elevan el
esfuerzo de hacer trampa sin salir del plan gratuito.

## Alcance del MVP y Restricciones

### Dentro de alcance
Cuenta y login (correo + contraseña y Google Sign-In, recuperación por correo); participación en
desafíos con medición de uso; canje de puntos en la tienda de la app; perfil (datos personales,
tiempo total de pantalla, historial de logros y canjes); cancelar/interrumpir desafío; calificar
desafío; vencimiento de puntos; notificaciones.

### Fuera de alcance (MVP)
iOS y Apple ID; publicación en Google Play; login o recuperación por teléfono/SMS; cámara y fotos
de inicio/fin de desafío;
IA real o simulada como paso de verificación; bloqueo de aplicaciones; misiones personalizadas;
beneficios fuera de la app; rankings, amistades y funciones sociales; mapas y geolocalización;
sincronización con relojes inteligentes; avatares.

### Stack tecnológico
Definido en `specs/001-auth-profile-catalog/research.md`. Cambiarlo MUST tratarse como enmienda.
- **Lenguaje e interfaz**: Kotlin con Jetpack Compose y Material 3. API mínima 26 (Android 8.0);
  `targetSdk` la versión estable más reciente al crear el proyecto.
- **Arquitectura**: un solo módulo `app`; MVVM con `ViewModel` y `StateFlow`; acceso a datos tras
  interfaces (repositorios) para probar sin Firebase; validaciones y lógica de negocio como código
  puro, sin depender de Android ni de Firebase.
- **Backend**: Firebase en plan gratuito (Spark): Authentication (correo y contraseña; Google
  mediante Credential Manager) y Cloud Firestore. El MVP MUST NOT depender de Cloud Functions ni
  de un plan de pago.
- **Reglas de seguridad**: `firebase/firestore.rules`, con denegación por defecto.
- **Pruebas**: JUnit 4, kotlinx-coroutines-test y Turbine (JVM); Compose UI Test (instrumentadas);
  Firebase Emulator Suite con `@firebase/rules-unit-testing` (reglas e integración).
- **Herramientas**: Android Studio con emulador con Google Play, Firebase CLI y Node.js; JDK 17
  para Gradle y JDK 21 para los emuladores de Firebase.

### Arquitectura de datos
- Cuentas, puntos e historial residen en Firebase (Authentication y Firestore), que es la fuente de
  verdad persistente.
- La medición del uso de apps, el cálculo del cumplimiento y la acreditación de puntos se realizan
  en el dispositivo; Firestore valida cada escritura con sus reglas de seguridad (Principio VI).
- La app requiere conexión a Internet. Sin conexión no se puede iniciar un desafío, y si esta se
  pierde durante un desafío en curso, el desafío queda invalidado.

### Recompensas y notificaciones
- La tienda MUST ofrecer cupones digitales ficticios: al canjear se genera un código único
  visible en "Mis Recompensas". Los cupones MUST NOT presentarse como beneficios canjeables fuera
  de la app.
- Las notificaciones MUST incluir avisos de estado y recordatorios motivacionales por inactividad
  enviados desde el backend (push). MUST cumplir la regla de no insistencia de "Identidad visual".

### Seguridad, privacidad y datos
- Las contraseñas MUST NOT almacenarse, registrarse ni transmitirse en claro; MUST manejarse con
  hash o delegarse a un proveedor de identidad.
- Las reglas de seguridad de Firestore MUST denegar por defecto cualquier lectura o escritura que
  no esté permitida de forma explícita.
- La app MUST recolectar solo los datos necesarios y MUST explicar al usuario, antes de solicitar
  cada permiso, para qué se usa; MUST funcionar con los permisos mínimos indispensables.
- Los datos de uso del dispositivo MUST NOT compartirse con terceros más allá de la
  infraestructura de backend necesaria para operar el servicio.

### Identidad visual y experiencia
- Interfaz en español, minimalista y sin elementos distractores; paleta de calma y enfoque
  (verdes, azules y neutros), con modo claro y oscuro.
- El nombre se representa siempre como "(des)Conectado", con paréntesis.
- La app MUST NOT usar patrones que fomenten el uso compulsivo (feeds infinitos, alertas de
  culpa, notificaciones insistentes); es coherente con su propósito.
- Iniciar un desafío MUST requerir pocos toques.

### Decisiones resueltas
- 2026-09-19: solo Android; sin cámara ni fotos; sin IA (Principio III); la app no bloquea
  (Principio I).
- 2026-09-19: la validación del desafío es solo por medición de uso, sin paso de verificación.
- 2026-09-19: backend en la nube como fuente de verdad persistente.
- 2026-09-19: login con correo + contraseña y Google Sign-In; recuperación por correo.
- 2026-09-19: solo cuentan las apps de redes sociales de una lista fija.
- 2026-09-19: conexión obligatoria; perder la conexión durante un desafío lo invalida.
- 2026-09-19: los puntos vencen a los 30 días de otorgados (FIFO).
- 2026-09-19: notificaciones en la entrega del 2026-10-08, con avisos de estado y recordatorios
  motivacionales por inactividad desde el backend.
- 2026-09-19: la tienda ofrece cupones digitales ficticios.
- 2026-09-19: stack fijado (ver "Stack tecnológico").
- 2026-09-19: la acreditación de puntos se origina en la app y se guarda en Firestore, protegida
  por reglas de seguridad; sin Cloud Functions ni plan de pago (reemplaza la exigencia previa de
  operaciones autoritativas en el backend).
- 2026-09-19: el proyecto no se publica en Google Play; se distribuye por APK directo. Firebase App
  Check queda descartado, porque su proveedor de integridad en Android está pensado para apps
  distribuidas por Google Play.

### Decisiones abiertas (contradicciones del documento del cliente)
Se resuelven de a una con el usuario; cada resolución enmienda esta constitución.
- TODO(MEDICION_USO): lista concreta de apps de redes sociales (¿incluye WhatsApp?) y umbral
  máximo de uso por desafío.
- TODO(OFFLINE_TOLERANCIA): cuánto dura una desconexión antes de invalidar el desafío y cómo se
  detecta. Riesgo: desafíos de actividad física al aire libre, donde puede faltar señal.
- TODO(NOTIFICACIONES_FRECUENCIA): tope de frecuencia y definición de "inactividad".
- TODO(CATALOGO_RECOMPENSAS): qué cupones ofrece la tienda y a qué costo en puntos.
- TODO(CATALOGO_DESAFIOS): desafíos, duraciones y asignación de puntos. Hay valores iniciales
  propuestos en `specs/001-auth-profile-catalog/spec.md` (Assumptions).
- TODO(PUNTOS_SEGURIDAD): definir en el plan de la entrega del 2026-10-01 el modelo de datos de los
  puntos (registro de movimientos y saldo), sus reglas de seguridad y la cobertura de pruebas de
  manipulación; y comprobar en la práctica, contra el emulador, si las reglas pueden imponer que
  una acreditación no ocurra antes de que transcurra la duración del desafío según la hora del
  servidor. Si es viable, se agrega a las invariantes del Principio VI; si no, se descarta.

## Flujo de Trabajo y Entregas

### Calendario de entregas
| Fecha      | Objetivo                 | Entregable                                                   |
|------------|--------------------------|--------------------------------------------------------------|
| 2026-09-24 | Perfil y catálogo        | App base con login/registro, perfil y catálogo de desafíos    |
| 2026-10-01 | Participación y canje    | Aceptar desafío, medición de uso, canje de puntos             |
| 2026-10-08 | Validaciones e historial | Interrumpir y calificar desafío, vencimiento de puntos, notificaciones, historial de desafíos y canjes |
| 2026-10-15 | Ajustar                  | Integración completa, pruebas, corrección de errores, UI y demo |

### Flujo de desarrollo
- Cada entrega recorre el flujo de Spec Kit: `/speckit-specify` → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-implement`, en ese orden.
- Cada plan MUST incluir un "Constitution Check" contra los seis principios.
- Una entrega está completa solo si: sus funcionalidades funcionan en un dispositivo o emulador
  Android, su evidencia de pruebas está referenciada (Principio IV) y no incluye nada de "Fuera
  de alcance".

## Governance

Esta constitución prevalece sobre las demás prácticas del proyecto y sobre el documento del
cliente donde haya contradicción.

- **Enmiendas**: cualquier cambio MUST documentarse en este archivo con su justificación y
  actualizar la versión. Resolver una decisión abierta (`TODO`) o cambiar el stack tecnológico
  cuenta como enmienda.
- **Versionado** (semántico): MAJOR por eliminación o redefinición incompatible de un principio;
  MINOR por agregar un principio o sección, o ampliar guía de forma material (incluida la
  resolución de una decisión abierta); PATCH por aclaraciones y correcciones de redacción.
- **Cumplimiento**: toda spec, plan y entrega MUST verificarse contra esta constitución; las
  desviaciones MUST justificarse por escrito o corregirse antes de entregar.

**Version**: 2.1.0 | **Ratified**: 2026-09-19 | **Last Amended**: 2026-09-19
