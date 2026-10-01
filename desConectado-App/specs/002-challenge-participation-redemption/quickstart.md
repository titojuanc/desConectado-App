# Quickstart: Participacion en Desafios y Canje

## Prerrequisitos

- Android Studio/SDK, JDK 17, Node.js y Firebase CLI segun el quickstart de la entrega 1.
- Emulador Android con Google Play y un dispositivo de prueba con acceso de uso habilitable.
- Emuladores Auth/Firestore ejecutandose con el proyecto `des-conectado`.

## Validacion automatizada

Desde la raiz:

```powershell
./gradlew testDebugUnitTest
```

Debe cubrir las funciones puras de tiempo, uso, conexion, estados, puntos y canje.

Con Firebase:

```powershell
cd firebase
npm test
cd ..
./gradlew connectedDebugAndroidTest -PuseEmulator=true
```

Las pruebas de reglas deben demostrar propiedad por usuario, acreditacion exacta, idempotencia, canje atomico, saldo no negativo, codigos unicos y rechazo de editar/borrar movimientos.

## Guiones manuales

1. **Permiso e inicio**: instalar el APK, iniciar sesion, abrir Desafios, aceptar la explicacion y otorgar acceso de uso. Iniciar un desafio en hasta tres acciones y cronometrar que el estado activo aparece en menos de 1 segundo al reabrir la app.
2. **Cumplimiento**: mantener las seis apps objetivo sin abrir durante un desafio corto de prueba; al terminar, comprobar resultado cumplido y una sola acreditacion. Repetir abriendo una app objetivo y comprobar resultado no cumplido y cero puntos.
3. **Apps ausentes**: no instalar una app objetivo; iniciar y completar el desafio; comprobar que esa app cuenta como cero y no bloquea el inicio.
4. **Revocacion**: iniciar un desafio, revocar el acceso de uso desde Ajustes y volver a la app; comprobar invalidacion inmediata, cero puntos y retorno al catalogo.
5. **Restauracion**: iniciar un desafio, cerrar y abrir la app; comprobar que conserva el desafio y que el progreso no vuelve a cero.
6. **Cancelacion**: cancelar un desafio, comprobar cero puntos e iniciar otro inmediatamente.
7. **Offline**: durante un desafio, activar modo avion por menos de 5 minutos y reconectar; verificar continuidad. Repetir superando 5 minutos acumulados; verificar invalidacion sin puntos.
8. **Canje valido**: cargar puntos mediante datos de prueba autorizados, elegir una recompensa con saldo suficiente, confirmar y comprobar el debito, el codigo del cupon y la recompensa persistida.
9. **Canje repetido**: canjear dos veces un cupon y comprobar dos codigos/movimientos, verificando la fórmula `DC-` + Base64 URL-safe sin padding; intentar repetir una insignia o tema y comprobar rechazo sin cambios.
10. **Canje invalido**: intentar una recompensa mas cara que el saldo; comprobar que no cambia ni el saldo ni el historial.
11. **Duracion server-side**: intentar cerrar/acreditar un desafio antes de que termine su duracion; comprobar que las reglas rechazan la acreditacion. Repetir despues de la duracion y comprobar que la acreditacion se acepta si el uso fue cero.
12. **Rendimiento**: cronometrar la decision de resultado y un canje con conexion normal; cada uno debe completar en menos de 3 segundos.

## Evidencia

Guardar salidas automatizadas y capturas en `specs/002-challenge-participation-redemption/evidencia/`. La entrega no se considera completa hasta que cada FR-001 a FR-017 tenga una prueba o un resultado manual referenciado.

## Riesgos de validacion

- El emulador puede no representar fielmente el acceso de uso; los guiones de `UsageStatsManager` deben ejecutarse tambien en un dispositivo Android real.
- No se deben usar credenciales de administrador en la app; solo los scripts de prueba pueden preparar datos iniciales.
- La tolerancia offline se mide acumulada por desafio, no por interrupcion individual.
