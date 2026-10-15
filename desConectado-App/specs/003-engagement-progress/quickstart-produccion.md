# Despliegue y smoke en Firebase real

Alcance: validar el cliente Android contra el proyecto Firebase `des-conectado`. Usar solo una cuenta de prueba desechable creada desde la app. No iniciar sesión con cuentas reales de usuarios. No ejecutar `firebase/maintenance/reset-delivery-3.mjs` ni borrar colecciones.

## 1. Preflight y despliegue

Desde PowerShell, en la raíz del repositorio:

```powershell
git status --short --branch
Push-Location firebase
npx firebase projects:list
$project = 'des-conectado'
$credential = (Resolve-Path 'service-account.json').Path
$env:GOOGLE_APPLICATION_CREDENTIALS = $credential
node seed/seed.mjs --dry-run
npx firebase deploy --only 'firestore:rules,firestore:indexes' --project $project
node seed/seed.mjs --project $project
Pop-Location
```

Antes del primer write, confirmar en la salida que el proyecto es `des-conectado` y el catálogo declara 28 desafíos, 17 recompensas y 10 logros. El seed sincroniza esos documentos, archiva los sobrantes de esas colecciones y no borra perfiles ni datos de usuarios. Despliega únicamente Firestore Rules e índices declarados; no despliegues Functions ni ejecutes un reset.

## 2. AVD contra producción

Verificar que `app/google-services.json` corresponde a `des-conectado` y que el modo Firebase Emulator no está habilitado. En este proyecto el modo emulator solo se activa pasando `-PuseEmulator=true`; no pasar esa propiedad para el smoke real.

```powershell
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
Start-Process -FilePath $emulator -ArgumentList '-avd desconectado'
& $adb wait-for-device
& $adb devices -l
.\gradlew.bat :app:installDebug
& $adb shell monkey -p com.desconectado.app 1
```

El dispositivo debe aparecer como `device`, no `offline`. Crear una cuenta nueva desde la app con un mailbox de QA controlado y contraseña única. Registrar el correo de QA localmente para poder repetir ingreso y restablecimiento; no incluir credenciales en el repo, logs o evidencia.

### Panel Debug QA

La variante `debug` agrega una cuarta pestaña **Debug** junto a Desafíos, Recompensas y Perfil. Está desactivada al instalar; al final del Perfil, encender el switch **Sección Debug** para mostrarla, y apagarlo para ocultarla. La preferencia queda guardada en el dispositivo. El panel muestra el proyecto Firebase, estado de conectividad y permiso de uso; **Ejecutar diagnóstico Firebase** consulta sesión, perfil, preferencias, catálogos, desafío activo, historiales y movimientos sin escribir datos. Los botones de escenario abren las pantallas reales para probar sus flujos. Release nunca incluye el switch ni la pestaña Debug.

El build debug apunta a Firebase de producción por defecto, igual que la configuración actual de `google-services.json`; el panel lo advierte. Para pruebas aisladas, iniciar Auth/Firestore Emulator, compilar/instalar con `-PuseEmulator=true` y sembrar solo fixtures `debug-` con `seed/seed-debug.mjs --emulator`. El diagnóstico no altera datos, pero las pruebas de racha sí generan créditos reales para la cuenta autorizada.

### Preparación de la nueva racha y pruebas Debug

No ejecutar estos pasos sin aprobación del proyecto y UID correspondientes. Antes de distribuir APKs
nuevos, detener créditos de versiones anteriores, migrar cada cuenta con puntos ganados hoy,
desplegar las reglas actuales y recién entonces habilitar el cliente nuevo. Las cuentas nuevas no
necesitan migración. No mezclar clientes viejos/nuevos durante la preparación.

Desde `firebase`, con `GOOGLE_APPLICATION_CREDENTIALS` apuntando a credenciales Admin del proyecto:

```powershell
node maintenance/migrate-daily-streak.mjs --project des-conectado --uid <UID> --dry-run
node maintenance/migrate-daily-streak.mjs --project des-conectado --uid <UID> --apply
npx firebase deploy --only firestore:rules --project des-conectado
```

La migración reconstruye solo el día actual y no cambia saldos; un bonus antiguo ya pagado consume
el bonus de ese día. El script exige coincidencia entre proyecto y credencial y una cuenta
explícita. No guardar la credencial en Git. Los botones necesitan también el desafío activo
`debug-10-segundos` de un punto; usar el seed debug documentado sin reemplazar el catálogo normal.
Por ahora cualquier cuenta con Sección Debug activada puede usar los botones en el APK debug;
no requiere claims ni autorización por UID. La app comprueba el switch local antes de escribir y
release no incluye la implementación. Las reglas solo permiten borrar los resultados de prueba
propios y los acumulados marcados con su prefijo; no pueden comprobar la variante instalada ni un
switch local. Restringir a una cuenta concreta queda para una actualización posterior.

Guion en una cuenta QA sin desafíos reales recientes:

1. Configurar meta diaria de 30 minutos (meta semanal automática de 210 minutos) y habilitar Sección Debug en Perfil.
2. **Reiniciar días de prueba**: conserva el saldo, elimina solo resultados/acumulados de prueba de
   hoy y los dos días anteriores. No borra resultados reales.
3. **Sumar 1 punto** varias veces: saldo sube por puntos base, hoy sigue pendiente y no hay bonus.
4. **Activar racha**: completa los dos días anteriores. Hoy sigue pendiente; no adelanta su bonus.
5. **Completar meta diaria**: agrega el tiempo restante hasta 30 minutos y acredita de golpe el
   bonus sobre todos los puntos base de hoy. Hoy pasa a cumplido, racha de tres días.
6. **Sumar 1 punto** otra vez: solo un punto, sin otro bonus. Reiniciar la app confirma persistencia.
7. Reiniciar días y completar la meta sin activar los días previos: racha de un día. Para probar
   el corte por día incumplido, las pruebas JVM cubren ayer pendiente y el cambio de fecha.

Los botones usan resultado y crédito normales, no editan `pointsBalance` directamente. La pantalla
de procesamiento bloquea toques duplicados y muestra errores de Firebase.

## 3. Smoke de conexiones

Registrar resultado y errores por flujo. Después de cada escritura, salir de la pantalla o reiniciar la app y comprobar persistencia, no solo el estado en memoria.

| Servicio / flujo | Acción desde Android | Resultado esperado |
| --- | --- | --- |
| Firebase Auth | Crear cuenta de QA con meta diaria de 30 minutos; cerrar sesión, volver a entrar y reiniciar | Cuenta creada, sesión recuperada y perfil inicial sincronizado con 210 minutos semanales |
| Perfil y preferencias | Cambiar username, meta y notificaciones; recargar Perfil | Los campos permitidos persisten; el email no se modifica; notificaciones parten apagadas |
| Catálogo | Abrir desafíos y recompensas; cambiar filtros/categorías | Lee los 28 desafíos, recompensas y sus configuraciones actuales desde Firestore |
| Desafío y resultado | Iniciar un desafío elegible, completarlo o cancelarlo, volver al catálogo | Estado activo y resultado quedan asociados a la cuenta de QA; historial refleja tipo y puntos |
| Rating | Valorar un resultado completado y reabrir su historial | Rating de 1 a 5 persiste; cancelados no muestran opción de rating |
| Puntos y ledger | Consultar saldo antes y después de una concesión válida por completar desafío | Saldo y movimiento coinciden, sin movimiento duplicado al reintentar/recargar |
| Historial de canjes | Canjear un cosmético no económico poseído con saldo suficiente y reabrir historial | Débito, canje y propiedad se guardan una sola vez |
| Preferencias cosméticas | Aplicar/quitar un cosmético poseído y reiniciar la app | Preferencia activa persiste y no permite activar un elemento no poseído |
| Logros | Reabrir Perfil tras registrar resultados | Definiciones se cargan; progreso derivado y logros concedidos se muestran sin duplicarse |
| Google Auth | Probar solo si el proveedor está habilitado y el SHA-1 de debug está registrado | Ingreso y sincronización de perfil; si falla por OAuth/SHA, anotar como configuración pendiente, no cambiar reglas para ocultarlo |
| Restablecer contraseña | Solicitar restablecimiento para la cuenta de QA y revisar el mailbox | Auth acepta la solicitud y entrega el correo |

No canjear cupones reales durante el smoke ni completar flujos que produzcan un beneficio externo. Para el canje, usar un cosmético de la cuenta desechable. No manipular directamente el saldo productivo para forzar escenarios.

### Smoke instrumentado opt-in

Cerrar la sesión existente del AVD antes de ejecutar. El test crea una cuenta QA nueva con correo no entregable y contraseña aleatoria en ejecución; no usa credenciales reales. Sin bandera productiva se omite para no escribir accidentalmente desde suites habituales.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.desconectado.app.data.ProductionConnectionsTest' '-Pandroid.testInstrumentationRunnerArguments.productionSmoke=des-conectado'
```

Para caminos positivos sin esperar desafíos reales, el usuario autorizó resultados sintéticos exclusivamente en QA. Agregar `'-Pandroid.testInstrumentationRunnerArguments.syntheticQa=true'` al comando anterior. El test escribe resultados completados con tiempos pasados y acredita mediante el repositorio normal; no altera el saldo con Admin. Prueba crédito, rating, logros, tema, caja sorpresa y expiración; no simula una comprobación real de uso del dispositivo ni canjea cupones.

Cada ejecución deja una cuenta QA hasta su limpieza. Revisar el UID generado en `ProductionSmoke`, corroborar Auth y perfil con el correo QA y marcador del test, y borrar solo ese UID/subcolecciones con Admin. No enumerar/borrar usuarios en bloque. El último lote de cinco cuentas fue limpiado y verificado; repetir el comando exige limpiar sus nuevas cuentas. Registrar el resultado por flujo, sin conservar credenciales ni datos personales.

## 4. Límites y evidencia

### Recorrido inmediato de testeo

El proyecto `des-conectado` tiene datos adicionales con IDs `debug-` y nombres `[TEST]`: cuatro desafios de 10 segundos que dan 1 punto, tres logros de umbral inmediato y tres recompensas de 1 punto. El catalogo normal no se reemplaza.

1. En Desafios, iniciar un `[TEST]` con permiso de uso habilitado. La barra aparece sobre la navegacion inferior, cuenta en `00:00` y sigue visible al cambiar de pestana. No hay controles manuales de actualizar/comprobar/finalizar; la cruz cancela.
2. Esperar diez segundos sin abrir redes sociales. La comprobacion/finalizacion es automatica; valorar el resultado. El saldo debe aumentar un punto.
3. En Recompensas, alternar Canjeables y Logros. Los logros de test aparecen desbloqueados; en Perfil se muestran como medallas, no como articulos comprables.
4. Canjear `[TEST] Tema Atardecer` por 1 punto y Aplicar/Quitar. Otro desafio permite probar el sonido de finalizacion o cupon digital de test, sin beneficio externo.

Para volver a sembrar solo estos datos, desde `firebase/` y con la credencial local configurada:

```powershell
node seed/seed-debug.mjs --dry-run
node seed/seed-debug.mjs --project des-conectado
```

El seed general archiva IDs adicionales no incluidos en el catalogo oficial; si se ejecuta, volver a correr este seed despues para habilitar los datos de testeo. No ejecutar reset ni tocar cuentas existentes.

- El smoke prueba SDK Android conectado a producción; Firebase Emulator Suite y Rules tests no sustituyen esta prueba.
- Estadísticas de uso/notificaciones dependen de permiso y comportamiento del SO; validarlos en el teléfono real además del AVD.
- Los flujos que necesitan duración real, saldo insuficiente/alto, caja sorpresa agotada o un proveedor OAuth configurado se registran como manuales/configuración, no como verificados si no se ejecutaron.
- Guardar solo fecha, dispositivo, build, flujo, resultado y mensaje de error redactado en `evidencia/`. Nunca guardar email, contraseña, tokens, códigos de canje ni datos personales.
- Mantener aislada la cuenta de QA. No borrar documentos de usuarios en bloque; cualquier limpieza productiva se revisa como operación aparte.
- Ejecutar las suites `npm test`, `npm run test:seed`, `npm run test:maintenance` y `:app:testDebugUnitTest` después de corregir fallos.

La validación en teléfono real se completa con este mismo checklist, permiso de estadísticas de uso cuando aplique, y feedback del usuario antes de iniciar la adaptación visual final.
