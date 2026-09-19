# **1. Identidad del Proyecto** 

### **1.1. Nombre y Marca** 

- **<u>Nombre de la Aplicación:</u>** (des)conectado 

- **<u>Fundamento del Nombre:</u>** Uso estilizado de paréntesis para significar la desconexión del entorno digital como requisito previo para la reconexión con el entorno físico y social real. 

### **1.2. Problema Central y Propósito** 

- **<u>Problema Afrontado:</u>** La dependencia excesiva y adicción al smartphone en jóvenes y también en adultos, caracterizada por un alto tiempo en pantalla pasivo (redes sociales, consumo compulsivo de contenido) y un déficit de interacciones y actividades en la vida real. 

- **<u>Propósito del Producto:</u>** Crear una solución digital gamificada que actúe como un catalizador para abandonar el uso del teléfono, incentivando la realización de hábitos presenciales saludables mediante un sistema estricto de metas, verificación fotográfica por IA y economía de recompensas. 

### **1.3. Definición del Producto y Modelo de Uso** 

- **<u>Naturaleza Tecnológica:</u>** Aplicación Web (Web App) accesible desde navegadores móviles. 

- **<u>Condición de Conectividad Estricta:</u>** La aplicación funciona exclusivamente mediante red Wi-Fi y datos móviles. Si la conexión se interrumpe o la app detecta que el dispositivo quedó en estado offline, la sesión o inicio de desafío queda restringido, ya que requiere validar los envíos de imágenes en tiempo real y la sincronización del estado del temporizador. 

### **1.4. Público Objetivo** 

- **<u>Perfil:</u>** Jóvenes y adolescentes que identifican patrones de distracción por el uso del celular y buscan un mecanismo de autocontrol positivo basado en recompensas directas y no en bloqueos restrictivos o punitivos. 

# **2. Descripción de la Primera Versión (MVP)** 

### **2.1. Descripción Oficial de la Primera Versión (Frase del MVP)** 

"La primera versión de nuestra aplicación incluirá un sistema de registro y creación de perfil con mail/teléfono y contraseña; verificación automatizada de objetivos realizados a través de una foto en tiempo real (al iniciar y finalizar el desafío) por una inteligencia artificial y un sistema de puntos como recompensa por completar objetivos propuestos por la aplicación. Esto permitirá que el usuario deje de usar el celular y canjee sus puntos, en principio, dentro de la tienda de la app. Esta propuesta no incluirá un ranking de competencia, amistades entre usuarios, beneficios fuera de la app y la sincronización con un reloj inteligente." 

### **2.2. Evaluación del MVP** 

El desarrollador debe considerar las siguientes respuestas sobre la viabilidad del MVP: 

1. **<u>¿Esta primera versión resuelve el problema central?</u>** Sí, logra que el usuario reduzca su tiempo en pantalla mediante desafíos verificados e incentivados por puntos. 

2. **<u>¿Una persona podría utilizarla de principio a fin?</u>** Sí, el flujo cubre desde el registro, la selección y ejecución del desafío con bloqueo de pantalla, hasta la verificación de fotos y el canje en la tienda interna. 

3. **<u>¿Se incluyó algo no indispensable?</u>** 

   - No, se descartaron rankings, red social, mapas y hardware externo para simplificar el MVP. 

4. **<u>¿Falta algo indispensable?</u>** 

   - No, la verificación por IA y el temporizador garantizan la validación del hábito sin fraudes. 

5. **<u>¿Es realizable con el tiempo y recursos?</u>** Sí, al desarrollarse como una Web App (PWA) e integrar APIs existentes para notificaciones e IA, la implementación es viable en el corto plazo. 

## **3. COMPONENTES DE LA SOLUCIÓN** 

Esta sección desglosa la infraestructura técnica, el modelo de distribución nativa, los permisos de sistema, la conectividad y las integraciones con servicios externos que componen la arquitectura del producto. 

### **3.1. Entorno de Despliegue, Distribución y Permisos de Sistema** 

- **<u>Arquitectura y Distribución Nativa:</u>** La solución se desarrollará como una aplicación móvil nativa/híbrida disponible para descarga e instalación directa desde Google PlayStore (Android) y Apple AppStore (iOS). 

- **<u>Compatibilidad de Dispositivos:</u>** Soporte para smartphones con sistemas operativos Android y iOS actualizados que dispongan de cámara integrada, módulo GPS y soporte nativo para notificaciones push. 

- **<u>Requisito Obligatorio de Conectividad:</u>** La aplicación exige una **conexión activa a Internet** (Wi-Fi, datos móviles o red compartida) para operar. Si el dispositivo carece de conectividad, el sistema bloqueará el inicio de desafíos, la transmisión de imágenes al servidor de IA y la sincronización del saldo de puntos. 

- **<u>Permisos Especiales del Sistema Operativo:</u>** Para garantizar el bloqueo de aplicaciones distractoras durante un desafío, la aplicación requerirá que el usuario conceda permisos avanzados de accesibilidad y administración del dispositivo (Servicios de Accesibilidad en Android y Screen Time API / Family Controls en iOS). 

### **3.2. Módulo de Autenticación y Gestión de Cuentas** 

- **<u>Métodos de Registro e Ingreso:</u>** Formulario nativo con correo electrónico o número telefónico más contraseña encriptada. 

- **<u>Autenticación Delegada (OAuth 2.0):</u>** Integración de botones de inicio de sesión rápido vía Google Sign-In y Apple ID. 

- **<u>Recuperación de Accesos:</u>** Sistema de restablecimiento de contraseña mediante envío de tokens temporales de verificación por correo electrónico o mensaje SMS al número registrado. 

### **3.3. Servidor de Notificaciones Push Nativas** 

- **<u>Infraestructura de Alertas:</u>** Servicio backend conectado a Firebase Cloud Messaging (FCM) y Apple Push Notification service (APNs) encargado de emitir notificaciones push nativas directamente a la pantalla de bloqueo o barra de estado del dispositivo. 

- **<u>Lógica de Disparo:</u>** Frecuencias de envío configuradas según patrones de inactividad del usuario, actuando como un disparador externo para motivar la apertura de la app e incentivar el cumplimiento de misiones. 

### **3.4. Motor de IA y Verificación de Imágenes** 

- **<u>Procesamiento de Visión por Computadora:</u>** Conexión vía API a un modelo de IA entrenado para la validación automática de fotografías en vivo (capturadas al inicio y al final de la sesión). 

- **<u>Validación Anti-Fraude:</u>** Restricción nativa a nivel de interfaz para impedir la selección de archivos guardados en la galería local, exigiendo el uso exclusivo de la cámara nativa en tiempo real dentro de la app. 

## **4. FUNCIONAMIENTO DETALLADO** 

Definición paso a paso de los flujos de interacción, lógica de negocio, manejo de excepciones y el sistema de bloqueo estricto de pantalla/aplicaciones durante la ejecución de los desafíos. 

### **4.1. Flujo Técnico End-to-End del Desafío** 

1. **<u>Selección del Desafío:</u>** El usuario explora y elige una misión dentro del catálogo categorizado. 

2. **<u>Fotografía e Inspección Inicial:</u>** La app activa la cámara nativa en vivo. El usuario captura la foto que evidencia el inicio de la actividad y la envía a la API de IA. 

3. **<u>Activación del Modo Enfoque y Bloqueo de Sistema:</u>** Tras la confirmación exitosa de la IA, la app activa el temporizador e invoca el sistema de bloqueo de aplicaciones externas del dispositivo. 

4. **<u>Entorno Restringido y Accesos de Emergencia:</u>** Mientras el temporizador esté corriendo, la app bloquea el acceso y las notificaciones de redes sociales y aplicaciones no autorizadas. Únicamente se permite el acceso directo, desde el panel de la app, a las herramientas esenciales autorizadas: 

   - a. Aplicaciones de mensajería instantánea (WhatsApp, Teléfono; excluyendo redes sociales). 

   - b. Aplicación nativa de Cámara. 

   - c. Aplicación de GPS / Mapas. 

5. **<u>Fotografía e Inspección Final:</u>** Concluido el tiempo del temporizador, la app se desbloquea temporalmente para exigir la captura de una segunda foto en vivo para verificar la finalización. 

6. **<u>Validación y Acreditación:</u>** Si la IA aprueba la foto de cierre, el backend calcula e incrementa el saldo de puntos en el perfil del usuario y restablece el uso normal del dispositivo. 

### **4.2. Reglas de Negocio, Bloqueo Nativo y Excepciones** 

- **<u>Mecanismo de Bloqueo Anti-Distracción:</u>** Si el usuario intenta abrir una aplicación no autorizada (como redes sociales) mediante trucos del sistema o gestos de pantalla, el servicio en segundo plano de la app interceptará la acción, superpondrá la pantalla de bloqueo con el temporizador y emitirá un aviso de advertencia. De no retornar a la app permitida, el desafío se marcará como fallido. 

- **<u>Manejo de Cancelación y Urgencias:</u>** La pantalla de enfoque incluye un botón destacado de "Cancelar / Interrumpir Desafío". Su activación detiene el contador inmediatamente, deshabilita el bloqueo de aplicaciones nativo, invalida la sesión activa y no otorga puntos, permitiendo al usuario disponer de su teléfono libremente en caso de emergencia. 

- **<u>Flexibilidad de Selección y Cambio de Misiones:</u>** El usuario tiene libertad para seleccionar entre múltiples tipos de desafíos predefinidos. Si cancela una misión 

en curso, puede iniciar una nueva inmediatamente, perdiendo únicamente el tiempo acumulado de la sesión cancelada. 

- **<u>Catálogo Predefinido:</u>** En la versión MVP, todos los desafíos son provistos por la plataforma. No se permite la creación de misiones personalizadas por parte del usuario en esta fase. 

- **<u>Evaluación de Experiencia (Feedback Post-Desafío):</u>** Al completar un desafío de forma exitosa, la app despliega un componente modal para calificar la actividad (puntuación de 1 a 5 estrellas y etiquetas de retroalimentación) antes de regresar a la pantalla principal. 

## **5. CONTENIDOS E INFORMACIÓN** 

Esta sección especifica la arquitectura de datos, el modelo de almacenamiento, los contenidos administrados por la plataforma y las reglas que rigen la economía de puntos y recompensas dentro de la aplicación. 

### **5.1. Datos de Perfil y Almacenamiento del Usuario** 

- **<u>Información del Perfil:</u>** El backend de la aplicación almacenará los datos de registro (correo electrónico o teléfono encriptado, nombre de usuario y contraseña hasheada), la fecha de alta, el saldo actual de puntos acumulados, la cantidad total de tiempo de desconexión logrado y el historial de desafíos completados y fallidos. 

- **<u>Tratamiento de Fotografías (Privacidad y Almacenamiento):</u>** Las fotografías capturadas en tiempo real al inicio y al final de cada desafío se procesarán únicamente de manera temporal en el servidor para su análisis por parte del motor de IA. Las imágenes no se guardarán públicamente ni se almacenarán en la galería local del dispositivo. Únicamente se registrará en la base de datos el resultado de la validación (Aprobado/Rechazado), la fecha y la hora del intento. 

- **<u>Límites de Interacción Social:</u>** La aplicación no funcionará como una red social en su primera versión. No se permitirá publicar fotografías, añadir descripciones de ubicaciones ni interactuar o agregar amigos dentro de la plataforma. 

### **5.2. Catálogo de Desafíos y Contenidos** 

- **<u>Tipos de Objetivos:</u>** El sistema contará con un catálogo de desafíos predefinidos clasificados por categorías (actividad física como salir a caminar, estudio/trabajo enfocado, lectura, socialización presencial o desconexión pasiva por tiempo). 

- **<u>Estructura del Desafío:</u>** Cada objetivo incluirá un título, una descripción explicativa de la tarea a realizar, la duración requerida del temporizador, la cantidad de puntos que otorga al ser completado y los parámetros visuales que la IA buscará en la fotografía inicial y final. 

### **5.3. Sistema de Puntaje, Economía y Tienda de Recompensas** 

- **<u>Cálculo e Incremento de Puntos:</u>** La asignación de puntos se calcula en función de la duración y la dificultad del objetivo completado (por ejemplo, mayor puntaje a desafíos de mayor tiempo o que requieran actividad física sostenida). 

- **<u>Caducidad y Vencimiento de Puntos:</u>** Para incentivar la rotación y el canje constante de beneficios, el sistema implementará una regla de vencimiento de puntos (por ejemplo, descuento o expiración mensual del saldo no utilizado). 

- **<u>Catálogo de la Tienda de Recompensas (In-App Store):</u>** 

   - **_Fase MVP (Recompensas Digitales)_ :** El usuario podrá visualizar su saldo disponible y canjear sus puntos acumulados por beneficios digitales e internos dentro de la plataforma (cupones de descuento digital, insignias de logro o elementos estéticos para la app). 

   - **_Fase Futura (Beneficios Reales)_ :** La base de datos y la arquitectura de la tienda quedarán preparadas para integrar en versiones posteriores cupones de descuento en comercios adheridos (locales de ropa, gastronomía, entretenimiento). 

- **<u>Mecanismo de Canje:</u>** Al seleccionar una recompensa y confirmar el canje, el sistema verificará que el saldo de puntos sea igual o superior al costo del beneficio. Tras la confirmación, se descontarán los puntos automáticamente del perfil y se generará un código único o cupón digital almacenado en la sección "Mis Recompensas". 

## **6. IDENTIDAD VISUAL Y EXPERIENCIA (UI/UX)** 

Esta sección define las directrices del diseño de interfaz, la estructura visual, la navegabilidad y la experiencia de usuario que debe implementar el equipo de desarrollo para garantizar una plataforma limpia, atractiva y funcional. 

### **6.1. Concepto Visual e Interfaz de Usuario (UI)** 

- **<u>Estilo General de Diseño:</u>** Interfaz minimalista, moderna y libre de elementos distractores. Se utilizarán paletas de colores que transmitan calma, enfoque y serenidad (tonos verdes, azules y contrastes neutros en fondo claro/oscuro). 

- **<u>Tipografía y Marca:</u>** El nombre del producto se representará visualmente como (des)conectado, enfatizando mediante el uso de paréntesis la transición entre la desconexión del smartphone y la reconexión con el entorno real. 

- **<u>Diseño Adaptable (Responsive Layout):</u>** Maquetación nativa adaptada a las proporciones de pantalla de dispositivos móviles iOS y Android, priorizando zonas táctiles de fácil acceso. 

### **6.2. Arquitectura de Pantallas Clave** 

- **<u>Pantalla Principal (Dashboard / Home):</u>** 

   - Resumen del saldo actual de puntos acumulados. 

   - Acceso directo al catálogo de desafíos disponibles. 

   - Notificaciones de estado e incentivos para iniciar misiones. 

- **<u>Pantalla de Enfoque y Bloqueo (Temporizador Activo):</u>** 

   - Contador regresivo visible de gran tamaño. 

   - Menú de accesos directos únicamente a aplicaciones esenciales autorizadas (Teléfono, WhatsApp, Cámara y Maps). 

   - Botón destacado de "Cancelar/Interrumpir Desafío" para emergencias. 

- **<u>Tienda de Recompensas:</u>** 

   - Catálogo de beneficios digitales clasificados por costo de puntos. 

   - Vista de "Mis Recompensas" con el historial de cupones digitales canjeados. 

- **<u>Perfil de Usuario:</u>** 

   - Datos personales del usuario, tiempo total de pantalla ahorrado e historial de logros. 

### **6.3. Experiencia de Usuario (UX) y Navegación** 

- **<u>Flujos Intuitivos:</u>** La navegación dentro de la app debe ser fluida y libre de menús complejos, permitiendo seleccionar e iniciar un desafío en pocos toques de pantalla. 

- **<u>Retroalimentación Inmediata:</u>** Modales de confirmación claros tras la validación de la IA y animaciones de felicitación al acreditar puntos para afianzar el refuerzo positivo de la gamificación 

## **7. ORDEN DE DESARROLLO DE LAS COSAS** 

Esta sección establece la secuencia cronológica y técnica ordenada por fases para la ejecución del proyecto (roadmap de implementación), asegurando que el desarrollador construya un Producto Mínimo Viable (MVP) funcional, estable y probado antes de incorporar las características avanzadas para futuras iteraciones. 

### **Fase 1: Configuración del Proyecto Nativo, Autenticación y Perfil de Usuario** 

- Configuración del proyecto en entorno de desarrollo nativo/híbrido preparado para compilación en Android (Google PlayStore) e iOS (Apple AppStore). 

- Implementación del módulo de registro e inicio de sesión seguro (Mail, Teléfono, Contraseña encriptada y accesos vía Google Sign-In y Apple ID). 

- Creación del modelo de base de datos para usuarios (datos de perfil, saldo de puntos y persistencia del progreso). 

- Desarrollo de la lógica de recuperación de cuenta e historial básico en la interfaz del perfil. 

### **Fase 2: Motor de Enfoque, Sistema de Bloqueo y Temporizador** 

- Maquetación de la pantalla de catálogo de desafíos predefinidos. 

- Desarrollo del componente de temporizador regresivo. 

- Configuración de permisos nativos avanzados del sistema operativo (Servicios de Accesibilidad en Android y Screen Time API en iOS) para el bloqueo de aplicaciones no autorizadas durante el desafío. 

- Implementación de la pantalla de enfoque con accesos directos únicamente a aplicaciones permitidas (Llamadas, WhatsApp, Cámara y Maps). 

- Lógica de gestión de emergencias: botón "Cancelar Desafío" y detección de intentos de evasión del bloqueo para la cancelación del desafío. 

### **Fase 3: Captura de Foto en Tiempo Real e Integración de IA** 

- Integración de la cámara nativa en vivo dentro del flujo del desafío, restringiendo la selección de archivos locales desde la galería del dispositivo. 

- Conexión con la API del modelo de IA (Visión por Computadora) para el procesamiento y validación automatizada de la foto inicial y la foto final. 

- Lógica de tratamiento de imágenes temporales (procesamiento sin almacenamiento público en servidor ni en galería del teléfono). 

### **Fase 4: Gamificación, Servidor de Notificaciones Push y Tienda de Recompensas** 

- Desarrollo del motor backend para la asignación y acreditación automática de puntos según la duración y complejidad del desafío completado. 

- Configuración e integración con servicios de notificaciones push nativas (Firebase Cloud Messaging / APNs) para el envío de alertas motivacionales. 

   - Desarrollo del catálogo de la Tienda Interna (In-App Store) para la visualización de saldo y canje de beneficios/descuentos digitales. 

   - Programación de la regla de caducidad o vencimiento periódico de puntos acumulados. 

- **Fase 5: Expansión y Funcionalidades Futuras (Post-MVP / Versión 2.0)** 

   - Integración con marcas y comercios externos para el canje de puntos por beneficios reales (gastronomía, indumentaria). 

   - Desarrollo del módulo de interacciones sociales: listas de amigos, desafíos grupales/en conjunto y tablas de posiciones (rankings/tops competitivos). 

   - Desarrollo del mapa interactivo con geolocalización avanzada. 

   - Sincronización y soporte para dispositivos vestibles (smartwatches) y personalización de avatares. 

