# (des)Conectado

App Android nativa (Kotlin, Jetpack Compose y Firebase) que fomenta el no-uso de redes sociales.
Esta entrega (24/09) incluye cuenta (correo y contraseña, y Google), recuperación de contraseña,
perfil y los catálogos de desafíos y recompensas.

Documentos de diseño: [`specs/001-auth-profile-catalog/`](specs/001-auth-profile-catalog/), en
particular [`quickstart.md`](specs/001-auth-profile-catalog/quickstart.md) (guion de pruebas y matriz
de evidencia) y [`tasks.md`](specs/001-auth-profile-catalog/tasks.md).

## Requisitos

- Android Studio con el SDK (plataforma 37, build-tools y platform-tools) y un emulador con imagen
  **Google Play** de API 34 o superior. Variable `ANDROID_HOME` (o `local.properties` con `sdk.dir`).
- JDK 17 para Gradle y **JDK 21** para los emuladores de Firebase (`JAVA_HOME` apuntando al 21 al
  correrlos).
- Node.js. Firebase CLI se instala con `npm install` en `firebase/`; se usa con `npx firebase`.

## Configuración de Firebase

`app/google-services.json` no se versiona. El que hay en un checkout nuevo es **provisorio**
(proyecto `demo-desconectado`): sirve para compilar y para correr contra los emuladores, pero no para
un uso real. Para el APK de entrega, descargá el archivo real desde la consola de Firebase (tareas
T002 y T106 de `tasks.md`).

## Compilar

```bash
./gradlew assembleDebug
```

## Pruebas

```bash
# Unitarias de JVM (validaciones, ViewModels)
./gradlew testDebugUnitTest

# Reglas de seguridad de Firestore y datos sembrados (Node)
cd firebase
npm install
npm test               # reglas, contra el emulador de Firestore (necesita JDK 21)
npm run test:seed      # invariantes del catálogo

# Instrumentadas (UI y repositorios contra los emuladores de Firebase)
npx firebase emulators:start --only auth,firestore --project des-conectado   # en una terminal, con JDK 21
EMULATOR_PROJECT_ID=des-conectado node seed/seed.mjs --emulator           # en otra: carga desafíos y recompensas
cd ..
./gradlew connectedDebugAndroidTest -PuseEmulator=true
```

## Apps consideradas prohibidas durante un desafío

La app contabiliza el uso de Discord, Instagram, X, YouTube, Threads, Snapchat, Netflix, Disney+,
Max, Prime Video, Paramount+, Crunchyroll, Pluto TV, Pinterest, TikTok y Facebook. También cuenta
Google Play Juegos y cualquier aplicación que Android clasifique como juego. Esto se usa para evaluar
el desafío; no bloquea ni cierra las aplicaciones del dispositivo.

Al iniciar un desafío desde el catálogo se muestra una pantalla con indicador de progreso. Cuando
el contador termina, aparece "Completando el desafío..." mientras se guarda el resultado y se
acreditan los puntos. Esta pantalla se muestra desde cualquier pestaña y no se puede cerrar durante
la operación; después se muestra el resultado o el error correspondiente.

Las tarjetas del catálogo muestran "Ver más" en lugar de la descripción. Al tocarlo se abre la
confirmación con la descripción completa y el botón para mantener presionado e iniciar el desafío.

Los resultados fallidos, invalidados o cancelados muestran una tarjeta con el motivo registrado
(uso de apps no permitidas, desconexión prolongada, tiempo incompleto o cancelación), una sugerencia
para el siguiente intento y el acceso al catálogo. Se usan los colores del tema activo, sin cambiar
las condiciones de cumplimiento ni la acreditación.
Los cierres por uso de apps o revocación del permiso conservan el motivo en el resultado; no se
presentan como problemas de Internet. En resultados antiguos sin motivo no se supone una desconexión.
Las notificaciones usan un icono monocromático propio; fallos del servicio de notificaciones o del
audio se registran sin abortar el resultado. Si el cierre falla por almacenamiento o una excepción
inesperada, la pantalla ofrece reintentar en la sesión actual sin presentar un éxito ficticio ni
duplicar créditos confirmados. El sonido fallido muestra un aviso dentro de la app.

Los canjes también muestran "Ver más": la descripción y la confirmación están en el detalle.
Mientras se consulta el saldo, se descuentan los puntos y se entrega la recompensa, se muestra la
misma pantalla animada, con "Completando el canje...". Si una operación queda pendiente por un
error, se puede reintentar sin descontar nuevamente los pasos ya procesados.

## Racha y bonus diario

La meta diaria es la meta semanal dividida por siete, medida en segundos y redondeada hacia arriba.
Solo cuentan los días cuya suma de desafíos completados alcanza esa meta. Mientras hoy siga
pendiente, se conserva la racha de ayer, pero hoy no se suma ni aumenta el multiplicador.
Los desafíos pagan puntos base hasta completar la meta; en ese momento se acredita **una sola vez**
el extra sobre todos los puntos base acumulados ese día. Los desafíos posteriores del mismo día
dan solo puntos base. No hay bonus sin meta configurada.

Se conserva la fórmula `multiplicador = 1 + díasDeRacha × metaSemanalMinutos / 21000`.
El extra se redondea hacia arriba. Por ejemplo, con siete días, meta semanal de 210 minutos y
100 puntos base acumulados, completar la meta diaria de 30 minutos da 7 puntos extra de golpe.
El resultado, el acumulado diario, el movimiento, el lote y el saldo se confirman atómicamente.

### Pruebas reales desde Debug

Instalar el APK debug, activar **Sección Debug** en Perfil y configurar una meta semanal.
Los botones permiten preparar los dos días anteriores, sumar un punto sin avanzar tiempo,
completar la meta diaria y reiniciar los días de prueba. Muestran saldo, bonus y meta pendiente/cumplida.
Reiniciar conserva puntos y movimientos; no permite borrar desafíos reales recientes.
Cualquier cuenta con **Sección Debug** activada puede ejecutar estas escrituras en el APK debug;
por ahora no hay una lista de cuentas autorizadas. Release no incluye la implementación de pruebas.
La configuración administrativa y el guion están en
[quickstart-produccion.md](specs/003-engagement-progress/quickstart-produccion.md).

Con `-PuseEmulator=true` la compilación de depuración apunta a los emuladores en `10.0.2.2`. En un
dispositivo físico, usá `adb reverse tcp:9099 tcp:9099` y `adb reverse tcp:8080 tcp:8080` con
`-PemulatorHost=localhost`.

## Generar el APK

1. Creá la keystore y `keystore.properties` en la raíz (`storeFile`, `storePassword`, `keyAlias`,
   `keyPassword`); ambos están en `.gitignore` (tarea T106).
2. Publicá las reglas y los catálogos en el proyecto real (tarea T107).
3. Compilá:

   ```bash
   ./gradlew assembleRelease
   ```

4. Verificá la firma con `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`,
   copiá el APK a `dist/desConectado-entrega1.apk` e instalalo con
   `adb install -r dist/desConectado-entrega1.apk`.

Sin `keystore.properties` el APK de release sale **sin firmar** y no se puede instalar.

## Puntos y despliegue

La app muestra el saldo de puntos (ícono arriba a la izquierda en cada pestaña y en el Perfil) y los últimos
5 desafíos hechos, guardados por usuario en Firestore (`users/{uid}.pointsBalance` y
`users/{uid}/movements`). Créditos, bonus y canjes usan movimientos idempotentes y transacciones
validadas por Firestore Rules. La consulta de últimos desafíos necesita un índice.
Esta versión necesita las reglas actualizadas y, para cuentas con créditos de hoy de versiones
anteriores, la migración del acumulado antes de distribuir el APK; de lo contrario se informa un
error y no se omiten silenciosamente esos puntos. Ver el procedimiento en
[quickstart-produccion.md](specs/003-engagement-progress/quickstart-produccion.md).
