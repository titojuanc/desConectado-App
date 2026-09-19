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
npm run emulators      # en una terminal, con JDK 21
node seed/seed.mjs --emulator   # en otra: carga desafíos y recompensas
cd ..
./gradlew connectedDebugAndroidTest -PuseEmulator=true
```

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
