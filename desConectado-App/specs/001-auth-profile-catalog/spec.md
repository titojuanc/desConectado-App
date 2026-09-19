# Feature Specification: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Feature Branch**: `001-auth-profile-catalog`

**Created**: 2026-09-19

**Status**: Draft

**Input**: User description: "Vamos a empezar por la entrega del 24/09. Pide un registro básico con mail y contraseña, y nosotros le vamos a agregar el google sign-in, ya que vamos a usar firebase así aprovechamos las cualidades de ese backend. El catálogo de recompensas será genérico y el perfil mostrará todos los datos que se pidan en el registro. Los desafíos serán algunos estilo "no uses Redes por x tiempo" en fácil, normal o difícil."

## Clarifications

### Session 2026-09-19

- Q: Si la persona abre la app sin Internet y ya tenía la sesión iniciada, ¿qué debe ver? → A: Entra
  a la pantalla principal con un aviso claro de "se requiere conexión"; Desafíos, Recompensas y
  Perfil no cargan datos y ofrecen reintentar; la sesión no se pierde.
- Q: ¿La persona debe confirmar su correo con un enlace antes de poder usar la app? → A: No; la
  cuenta funciona apenas se crea y no se envía ningún correo de confirmación.
- Q: ¿Qué datos debe pedir el formulario de registro con correo, además de la contraseña? → A:
  Correo y nombre de usuario (3 a 30 caracteres, no único); el perfil muestra ambos.
- Q: ¿La recuperación de contraseña ("Olvidé mi contraseña") entra en la entrega del 24/09? → A: Sí;
  Ingreso ofrece un enlace que envía un correo para restablecer la contraseña (Historia 6).
- Q: ¿En qué variante de español deben estar los textos de la app? → A: Rioplatense con "vos"
  ("Ingresá", "Creá tu cuenta", "Ya tenés una cuenta").

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrarse e ingresar con correo y contraseña (Priority: P1)

Una persona que instala (des)Conectado por primera vez crea su cuenta indicando un nombre de
usuario, su correo y una contraseña. Más adelante puede cerrar la app, volver a abrirla y seguir
dentro de su cuenta, o cerrar sesión e ingresar de nuevo con su correo y contraseña.

**Why this priority**: sin cuenta no existe ninguna otra función de la app. Es la base sobre la que
se apoyan el perfil, los desafíos y, en entregas siguientes, los puntos.

**Independent Test**: instalar la app en un dispositivo Android, registrar una cuenta nueva,
cerrar y reabrir la app, cerrar sesión e ingresar otra vez. Entrega valor por sí sola: una cuenta
funcional y persistente.

**Acceptance Scenarios**:

1. **Given** la app sin sesión iniciada, **When** la persona completa nombre de usuario, correo
   válido y contraseña válida y confirma el registro, **Then** se crea la cuenta, la sesión queda
   iniciada y se muestra la pantalla principal.
2. **Given** un correo ya registrado, **When** alguien intenta registrarse con ese correo, **Then**
   no se crea una cuenta nueva y se informa que el correo ya está en uso.
3. **Given** un correo con formato inválido o una contraseña demasiado corta, **When** se intenta
   registrar, **Then** el registro se rechaza y el mensaje indica qué campo debe corregirse.
4. **Given** una cuenta existente y sesión cerrada, **When** la persona ingresa su correo y
   contraseña correctos, **Then** accede a la pantalla principal.
5. **Given** una cuenta existente, **When** se ingresa una contraseña incorrecta o un correo no
   registrado, **Then** se rechaza el ingreso con un mensaje que no revela cuál de los dos datos
   falló.
6. **Given** una sesión iniciada, **When** la persona cierra y vuelve a abrir la app, **Then**
   entra directamente a la pantalla principal sin volver a ingresar sus datos.
7. **Given** una sesión iniciada, **When** la persona elige cerrar sesión, **Then** vuelve a la
   pantalla de ingreso y, al reabrir la app, sigue sin sesión.

---

### User Story 2 - Explorar el catálogo de desafíos (Priority: P2)

Con la sesión iniciada, la persona ve el catálogo de desafíos disponibles. Cada desafío consiste
en no usar redes sociales durante un tiempo determinado y está clasificado como Fácil, Normal o
Difícil. Puede leer de qué trata cada uno, cuánto dura y cuántos puntos otorgará.

**Why this priority**: el catálogo es lo que muestra el propósito de la app y prepara la entrega
del 01/10, donde se podrá participar en un desafío. Requiere una cuenta (Historia 1).

**Independent Test**: iniciar sesión y abrir el catálogo de desafíos; verificar que hay desafíos de
las tres dificultades con toda su información visible.

**Acceptance Scenarios**:

1. **Given** una sesión iniciada, **When** la persona abre el catálogo de desafíos, **Then** ve una
   lista de desafíos de tipo "No uses redes sociales por X tiempo" en las tres dificultades.
2. **Given** el catálogo abierto, **When** la persona mira un desafío, **Then** ve su título,
   descripción, duración, dificultad y puntos que otorga.
3. **Given** el catálogo abierto, **When** la persona compara desafíos de distinta dificultad,
   **Then** los de mayor dificultad tienen mayor duración y otorgan más puntos.
4. **Given** el catálogo abierto, **When** la persona mira la lista, **Then** identifica de un
   vistazo la dificultad de cada desafío.

---

### User Story 3 - Ver mi perfil (Priority: P2)

La persona abre su perfil y ve los datos que dio al registrarse. Desde allí también puede cerrar
sesión.

**Why this priority**: el plan de entregas pide un perfil con datos personales para esta fecha, y
sirve como comprobación visible de que la cuenta se creó bien.

**Independent Test**: registrar una cuenta con datos conocidos, abrir el perfil y comprobar que
coinciden exactamente con lo ingresado, y que la contraseña no aparece.

**Acceptance Scenarios**:

1. **Given** una cuenta creada con correo y contraseña, **When** la persona abre su perfil,
   **Then** ve su nombre de usuario y su correo tal como los ingresó al registrarse.
2. **Given** cualquier cuenta, **When** se abre el perfil, **Then** la contraseña no se muestra en
   ninguna forma.
3. **Given** el perfil abierto, **When** la persona elige cerrar sesión, **Then** la sesión termina
   y vuelve a la pantalla de ingreso.

---

### User Story 4 - Ingresar con Google (Priority: P2)

Como alternativa al correo y contraseña, la persona puede registrarse o ingresar con su cuenta de
Google en un solo paso, sin escribir datos.

**Why this priority**: el equipo lo definió como complemento explícito del registro básico y
reduce la fricción de entrada. Es independiente de las Historias 2 y 3, pero el registro con correo
(Historia 1) debe existir primero.

**Independent Test**: en un dispositivo con una cuenta de Google, elegir "Continuar con Google" sin
tener cuenta previa y comprobar que se accede a la app; repetirlo con la sesión cerrada y comprobar
que se recupera la misma cuenta.

**Acceptance Scenarios**:

1. **Given** una persona sin cuenta en la app, **When** elige continuar con Google y acepta, **Then**
   se crea su cuenta y accede a la pantalla principal.
2. **Given** una cuenta creada con Google y sesión cerrada, **When** vuelve a elegir continuar con
   Google, **Then** accede a la misma cuenta y conserva sus datos.
3. **Given** una cuenta creada con Google, **When** abre su perfil, **Then** ve el nombre y el correo
   obtenidos de Google.
4. **Given** la pantalla de Google abierta, **When** la persona la cancela, **Then** vuelve a la
   pantalla de ingreso sin error ni cuenta creada.
5. **Given** una cuenta existente creada con correo y contraseña, **When** la persona ingresa con
   una cuenta de Google con ese mismo correo, **Then** accede a esa misma cuenta y no se crea una
   duplicada.

---

### User Story 5 - Explorar el catálogo de recompensas (Priority: P3)

La persona ve el catálogo de recompensas que en el futuro podrá canjear con sus puntos. En esta
entrega el catálogo es genérico y de solo lectura.

**Why this priority**: muestra la motivación detrás de los desafíos y prepara el canje de la
entrega del 01/10, pero por sí solo no permite ninguna acción.

**Independent Test**: iniciar sesión y abrir el catálogo de recompensas; verificar que se listan al
menos cinco recompensas con nombre, descripción y costo.

**Acceptance Scenarios**:

1. **Given** una sesión iniciada, **When** la persona abre el catálogo de recompensas, **Then** ve
   una lista de recompensas digitales.
2. **Given** el catálogo abierto, **When** la persona mira una recompensa, **Then** ve su nombre,
   descripción y costo en puntos.
3. **Given** el catálogo abierto, **When** la persona intenta canjear una recompensa, **Then** no
   existe ninguna acción de canje disponible en esta entrega.

---

### User Story 6 - Recuperar mi contraseña (Priority: P3)

Una persona que olvidó su contraseña la restablece desde la pantalla de Ingreso: indica su correo,
recibe un mensaje con un enlace, elige una contraseña nueva y vuelve a ingresar con ella.

**Why this priority**: sin esto, quien olvida su contraseña queda fuera de su cuenta. Se incorpora a
esta entrega por decisión del equipo (ver Clarifications) y no bloquea a las Historias 1 a 5.

**Independent Test**: con una cuenta de correo existente, solicitar el restablecimiento, abrir el
enlace recibido, elegir una contraseña nueva e ingresar con ella; la anterior deja de funcionar.

**Acceptance Scenarios**:

1. **Given** la pantalla de Ingreso, **When** la persona elige "Olvidé mi contraseña", **Then** ve un
   campo para escribir su correo.
2. **Given** un correo con formato inválido, **When** la persona lo envía, **Then** se rechaza
   indicando que debe corregirlo y no se envía ningún mensaje.
3. **Given** el correo de una cuenta existente, **When** la persona solicita el restablecimiento,
   **Then** recibe en ese correo un mensaje en español con un enlace para elegir una contraseña
   nueva, y la app muestra un mensaje de confirmación.
4. **Given** un correo sin cuenta, **When** la persona solicita el restablecimiento, **Then** la app
   muestra el mismo mensaje de confirmación y no se envía ningún correo, sin revelar si ese correo
   está registrado.
5. **Given** una contraseña nueva elegida mediante el enlace, **When** la persona ingresa con ella,
   **Then** accede a la app; con la contraseña anterior el ingreso se rechaza.
6. **Given** un dispositivo sin conexión, **When** la persona intenta solicitar el restablecimiento,
   **Then** se informa que se requiere conexión y no se envía nada.

---

### Edge Cases

- **Sin conexión**: si el dispositivo no tiene Internet, el registro, el ingreso y la carga de los
  catálogos y del perfil no proceden, y se informa con claridad que se requiere conexión.
- **Sesión iniciada y app abierta sin conexión**: la persona ve la pantalla principal con el aviso
  de conexión requerida; las secciones no cargan datos y ofrecen reintentar; al volver la conexión
  y reintentar, cargan con normalidad sin necesidad de ingresar de nuevo.
- **Correo con mayúsculas o espacios**: "Ana@Mail.com " y "ana@mail.com" se consideran el mismo
  correo.
- **Doble toque**: pulsar dos veces el botón de registro o ingreso no crea dos cuentas ni dos
  sesiones.
- **Cancelar Google**: cancelar la selección de cuenta de Google no deja a la app en un estado
  intermedio.
- **Cuenta de Google sin nombre**: si Google no entrega un nombre, el perfil muestra el correo y no
  queda un campo vacío o roto.
- **Cuenta inexistente con sesión guardada**: si la cuenta deja de existir mientras había una sesión
  guardada, la app vuelve a la pantalla de ingreso en lugar de fallar.
- **Fallo al cargar un catálogo**: si la carga falla, se muestra un mensaje con opción de reintentar
  y no una pantalla en blanco.
- **Contraseña con espacios o caracteres especiales**: se aceptan y se respetan tal cual se escriben.
- **Restablecimiento de una cuenta creada solo con Google**: la app muestra el mismo mensaje de
  confirmación que en cualquier otro caso, y la persona puede seguir ingresando con Google.
- **Enlace de restablecimiento ya usado o vencido**: el enlace no permite cambiar la contraseña y la
  persona puede solicitar uno nuevo desde la pantalla de Ingreso.
- **Solicitudes repetidas de restablecimiento**: pulsar dos veces el botón de envío no genera dos
  solicitudes.

## Requirements *(mandatory)*

### Functional Requirements

**Cuenta y acceso**

- **FR-001**: El sistema MUST permitir crear una cuenta indicando nombre de usuario, correo y
  contraseña. La cuenta MUST quedar operativa de inmediato, sin exigir confirmar el correo y sin
  enviar ningún correo de confirmación.
- **FR-002**: El sistema MUST validar el registro antes de crear la cuenta: correo con formato
  válido, contraseña de al menos 8 caracteres y nombre de usuario de entre 3 y 30 caracteres; ante
  un dato inválido MUST indicar cuál corregir.
- **FR-003**: El sistema MUST rechazar el registro con un correo ya registrado e informarlo.
- **FR-004**: El sistema MUST permitir ingresar con correo y contraseña; ante credenciales
  incorrectas MUST mostrar un mensaje que no revele si falló el correo o la contraseña.
- **FR-005**: El sistema MUST mantener la sesión iniciada al cerrar y reabrir la app, hasta que la
  persona cierre sesión.
- **FR-006**: El sistema MUST permitir cerrar sesión desde el perfil; tras cerrarla, MUST exigir
  volver a ingresar.
- **FR-007**: El sistema MUST permitir registrarse e ingresar con una cuenta de Google.
- **FR-008**: El sistema MUST asociar una cuenta de Google y una cuenta de correo con el mismo
  correo a una única cuenta, sin duplicarla.
- **FR-009**: El sistema MUST tratar el correo sin distinguir mayúsculas de minúsculas ni espacios
  sobrantes al inicio o al final.
- **FR-010**: El sistema MUST ocultar la contraseña mientras se escribe y MUST NOT mostrarla en
  ninguna pantalla después de ingresada.
- **FR-011**: El sistema MUST informar con claridad cuando no hay conexión a Internet y MUST NOT
  permitir registro, ingreso, solicitud de restablecimiento de contraseña ni carga de catálogos ni
  de perfil en ese estado. Si la persona abre la
  app sin conexión con la sesión iniciada, MUST mostrar la pantalla principal con el aviso de
  conexión requerida, sin cargar datos en Desafíos, Recompensas ni Perfil (cada sección ofrece
  reintentar) y sin cerrar la sesión.
- **FR-012**: El sistema MUST mostrar solo las pantallas de registro e ingreso a quien no tenga
  sesión iniciada, y tras autenticarse MUST dar acceso a Desafíos, Recompensas y Perfil.

- **FR-025**: La pantalla de Ingreso MUST ofrecer la opción "Olvidé mi contraseña". Dado un correo
  con formato válido de una cuenta existente, el sistema MUST enviar a ese correo un mensaje en
  español con un enlace para elegir una contraseña nueva; tras usarlo, la contraseña anterior MUST
  dejar de servir para ingresar. Ante un correo con formato inválido MUST indicar que se corrija y
  no enviar nada.
- **FR-026**: Tras solicitar el restablecimiento, el sistema MUST mostrar siempre el mismo mensaje de
  confirmación, exista o no una cuenta con ese correo, sin revelar si está registrado, y MUST
  ignorar una segunda solicitud mientras la primera está en curso.

**Catálogo de desafíos**

- **FR-013**: El sistema MUST mostrar un catálogo de desafíos predefinidos de tipo "No uses redes
  sociales por X tiempo", con al menos dos desafíos por cada dificultad: Fácil, Normal y Difícil.
- **FR-014**: Cada desafío MUST mostrar título, descripción, duración, dificultad y puntos que
  otorga.
- **FR-015**: A mayor dificultad, los desafíos MUST tener mayor duración y otorgar más puntos.
- **FR-016**: El catálogo MUST presentar la dificultad de cada desafío de forma visible e
  inequívoca.
- **FR-017**: El sistema MUST NOT permitir iniciar desafíos en esta entrega; el catálogo es de
  consulta.

**Catálogo de recompensas**

- **FR-018**: El sistema MUST mostrar un catálogo genérico de al menos cinco recompensas digitales,
  cada una con nombre, descripción y costo en puntos.
- **FR-019**: Las recompensas MUST presentarse como digitales y propias de la app, y MUST NOT
  presentarse como beneficios canjeables fuera de ella.
- **FR-020**: El sistema MUST NOT ofrecer ninguna acción de canje en esta entrega.

**Perfil**

- **FR-021**: El perfil MUST mostrar todos los datos solicitados en el registro, excepto la
  contraseña: nombre de usuario y correo.
- **FR-022**: Para una cuenta creada con Google, el perfil MUST mostrar el nombre y el correo
  obtenidos de esa cuenta.

**Experiencia y evidencia**

- **FR-023**: La app MUST estar en español rioplatense, tratando a la persona de "vos" en todos los
  textos ("Ingresá", "Creá tu cuenta", "Ya tenés una cuenta"), y MUST mostrar el nombre
  "(des)Conectado" con paréntesis en las pantallas de registro e ingreso.
- **FR-024**: Cada requisito de esta especificación MUST contar con al menos una prueba
  documentada (automatizada o procedimiento manual con resultado) incluida en la entrega.

### Key Entities *(include if feature involves data)*

- **Cuenta de usuario**: identidad de la persona en la app. Tiene correo, nombre de usuario, la
  forma de acceso (correo y contraseña, Google o ambas) y la fecha de alta. Es única por correo.
- **Perfil**: vista de los datos de la cuenta que la persona ve de sí misma; refleja los datos
  pedidos en el registro.
- **Sesión**: estado de "persona identificada" en el dispositivo; persiste hasta cerrar sesión.
- **Desafío**: propuesta predefinida de no usar redes sociales durante un tiempo. Tiene título,
  descripción, duración, dificultad (Fácil, Normal o Difícil) y puntos que otorga. Es provisto por
  la plataforma y no lo crea el usuario.
- **Recompensa**: elemento digital del catálogo de la app. Tiene nombre, descripción y costo en
  puntos. Es provisto por la plataforma.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona nueva completa el registro con correo y ve la pantalla principal en menos
  de 2 minutos desde que abre la app.
- **SC-002**: Una persona con cuenta ingresa con correo y contraseña en menos de 30 segundos, y el
  100% de las veces que reabre la app con sesión iniciada llega a la pantalla principal sin
  reingresar datos.
- **SC-003**: Una persona con cuenta de Google accede a la app en menos de 30 segundos y sin
  escribir ningún dato.
- **SC-004**: El 100% de los intentos de registro inválidos (correo mal formado, contraseña corta,
  correo repetido, nombre de usuario fuera de rango) se rechazan sin crear cuenta y con un mensaje
  que identifica el problema.
- **SC-005**: Desde la pantalla principal, la persona llega a cada una de las tres secciones
  (Desafíos, Recompensas, Perfil) con un solo toque.
- **SC-006**: El catálogo de desafíos muestra al menos 6 desafíos (2 por dificultad) y el de
  recompensas al menos 5, cada uno con el 100% de sus datos visibles.
- **SC-007**: El perfil muestra el 100% de los datos pedidos en el registro y en ningún caso la
  contraseña.
- **SC-008**: El 100% de los requisitos funcionales tiene evidencia de prueba referenciada en la
  entrega del 24/09.
- **SC-009**: Una persona que olvidó su contraseña recibe el correo de restablecimiento en menos de
  2 minutos desde que lo solicita y puede ingresar con la contraseña nueva en menos de 5 minutos
  desde la solicitud.

## Assumptions

- **Alcance de esta entrega**: cubre "app base con login y registro, perfil con datos personales,
  desafíos disponibles y catálogo", más la recuperación de contraseña por correo. Participar en
  desafíos, medición de uso, canje de puntos, vencimiento, calificación, historial y notificaciones
  quedan para entregas posteriores.
- **Datos del registro**: el registro pide nombre de usuario, correo y contraseña (confirmado en
  Clarifications; el documento del cliente prevé el nombre de usuario como dato de perfil). La
  fecha de alta se registra pero no se pide ni se muestra en el perfil de esta entrega.
- **Reglas de datos**: contraseña de mínimo 8 caracteres; nombre de usuario de 3 a 30 caracteres y
  no necesita ser único, porque la app no tiene funciones sociales.
- **Recuperación de contraseña por correo**: entra en esta entrega (ver Clarifications). La página
  donde se elige la contraseña nueva la provee el servicio de autenticación, no la app, y aplica su
  propia regla de longitud mínima, que puede ser menor que la de FR-002; se acepta esa diferencia.
  La recuperación por teléfono o SMS queda fuera de alcance. La confirmación del correo no se exige
  (ver Clarifications).
- **Unificación de cuentas**: una cuenta de Google y una de correo con el mismo correo se consideran
  la misma persona.
- **Perfil de solo lectura**: no se pueden editar los datos en esta entrega.
- **Sin saldo de puntos visible**: no se muestra ni se calcula un saldo hasta que existan formas de
  ganar puntos (entrega del 01/10).
- **Plataforma y conectividad**: solo Android, con conexión a Internet obligatoria, según la
  constitución del proyecto.
- **Dependencia técnica**: el equipo ya eligió el proveedor de backend en la nube (ver Input), que
  además habilita el ingreso con Google. Esa decisión se formaliza en `/speckit-plan`.
- **Valores iniciales de los catálogos** (ajustables sin cambiar los requisitos):

  | Desafío (No uses redes sociales por…) | Dificultad | Puntos |
  |---------------------------------------|------------|--------|
  | 30 minutos                            | Fácil      | 10     |
  | 1 hora                                | Fácil      | 20     |
  | 2 horas                               | Normal     | 50     |
  | 4 horas                               | Normal     | 100    |
  | 8 horas                               | Difícil    | 200    |
  | 12 horas                              | Difícil    | 320    |

  | Recompensa (ejemplos genéricos)      | Costo en puntos |
  |--------------------------------------|-----------------|
  | Insignia "Primer paso"               | 50              |
  | Tema de color para la app            | 100             |
  | Cupón digital "Descuento de ejemplo" | 200             |
  | Insignia "Semana desconectada"       | 300             |
  | Cupón digital "Regalo sorpresa"      | 500             |

- **Contenido provisto por la plataforma**: los catálogos son los mismos para todas las personas y no
  los edita el usuario.
