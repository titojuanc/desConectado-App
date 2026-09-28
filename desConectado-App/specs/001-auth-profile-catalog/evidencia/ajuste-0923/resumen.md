# Evidencia del ajuste 2026-09-23 (T118, T134, T137 a T141)

Ajuste sobre la entrega del 24/09 (constitución v2.2.1): desafíos con nombres de actividades (US2) y puntos
de solo lectura (US7). Fecha de las pruebas: 2026-09-23.

## Pruebas automatizadas (todas en verde)

| Suite | Comando | Resultado | Salida |
|-------|---------|-----------|--------|
| JVM | `./gradlew testDebugUnitTest` | **117/117** (antes 92) | [`../automatizadas/jvm-testDebugUnitTest-ajuste-0923.txt`](../automatizadas/jvm-testDebugUnitTest-ajuste-0923.txt) |
| Instrumentadas (AVD API 36, emuladores de Firebase) | `./gradlew connectedDebugAndroidTest -PuseEmulator=true` | **79/79** (antes 59) | [`../automatizadas/instrumentadas-connectedDebugAndroidTest-ajuste-0923.txt`](../automatizadas/instrumentadas-connectedDebugAndroidTest-ajuste-0923.txt) |
| Reglas de seguridad | `cd firebase && npm test` | **48/48** (antes 30) | [`../automatizadas/firebase-reglas-ajuste-0923.txt`](../automatizadas/firebase-reglas-ajuste-0923.txt) |
| Datos sembrados | `cd firebase && npm run test:seed` | **25/25** (antes 22) | [`../automatizadas/firebase-siembra-ajuste-0923.txt`](../automatizadas/firebase-siembra-ajuste-0923.txt) |

Las pruebas nuevas se escribieron y se vieron fallar antes de implementar (Principio IV): la siembra falló con
el catálogo anterior; las reglas fallaron 6 pruebas con las reglas de la entrega 1 (lectura de movimientos,
creación con `pointsBalance`); las JVM no compilaban sin `formatearPuntos`, `normalizarSaldo`, `PointsRepository`
ni `SaldoViewModel`.

### Cobertura por requisito

| Requisito | Pruebas |
|-----------|---------|
| FR-013, FR-014 (títulos de actividad, descripción con "redes") | `tests/seed/challenges.test.mjs`, `DesafiosScreenTest`, `CatalogDesafiosEmulatorTest` |
| FR-027, FR-030 (saldo por usuario, cuenta nueva en 0) | `tests/rules/users.test.mjs`, `PointsRepositoryEmulatorTest` (saldo 0 en perfil y en Firestore; perfil anterior sin campo se lee como 0), `PerfilFlowEmulatorTest` |
| FR-028 (indicador arriba a la izquierda, toque a Perfil) | `PuntosUiTest`, `SaldoViewModelTest`, `PuntosTest` (`formatearPuntos`), `PerfilFlowEmulatorTest` |
| FR-029 (últimos 5, orden, vacío) | `PointsRepositoryEmulatorTest`, `PerfilViewModelTest`, `PerfilScreenTest` |
| FR-031 (saldo entero no negativo, igual a la suma) | `PuntosTest` (`normalizarSaldo`), `PointsRepositoryEmulatorTest` (saldo = suma de acreditaciones), `points.test.mjs` |
| FR-032, SC-011 (sin escrituras de puntos) | `tests/rules/points.test.mjs` (crear, duplicar, monto distinto, editar, borrar, lote con saldo, saldo negativo, cuenta ajena, sin sesión, ruta no prevista), `PerfilScreenTest` (solo "Cerrar sesión" es accionable) |
| FR-033 (sin saldo inventado, Reintentar) | `SaldoViewModelTest`, `PerfilViewModelTest`, `PerfilScreenTest`, `PuntosUiTest` (guion) |
| SC-010 (indicador coincide con el Perfil) | `SaldoViewModelTest.elSaldoDelIndicadorCoincideConElDelPerfil`, `PuntosUiTest` |

## Revisión en el emulador de Android (AVD API 36, APK de depuración contra los emuladores de Firebase)

Hecha por el asistente con capturas; no reemplaza la pasada en teléfono real (ver "Pendiente").

| Guion | Resultado | Captura |
|-------|-----------|---------|
| M-16 (desafíos) | 6 títulos de actividad ("Salir a caminar", "Andar en bici", "Salir a trotar"…), descripción con "sin redes" | [`us2/captura-desafios-con-indicador.png`](us2/captura-desafios-con-indicador.png) |
| M-13 (indicador) | Ícono con el saldo arriba a la izquierda en Recompensas y en las otras pestañas; tocarlo abre Perfil | [`us7/captura-recompensas-con-indicador.png`](us7/captura-recompensas-con-indicador.png) |
| M-14 (con datos, emulador) | Perfil con saldo 1.250 y los últimos desafíos, del más reciente al más antiguo | [`us7/captura-perfil-con-puntos.png`](us7/captura-perfil-con-puntos.png) |

En esa revisión apareció un defecto que las pruebas no habían mostrado: en el primer ingreso de una cuenta la
sesión empieza antes de que termine de crearse el perfil, y el indicador quedaba en guion hasta cambiar de
pestaña. Se corrigió con un reintento automático acotado (2 reintentos cada 2 s) en `SaldoViewModel`, con tres
pruebas nuevas (`SaldoViewModelTest`).

## APK

`dist/desConectado-entrega1-ajuste.apk` (SHA-256 `b1a99978701c6b191f647ebfc0745ef1f95680a418b47d387067106cd43d9787`), firmado con la misma keystore de la entrega 1 (SHA-1 de la firma
`1acfe34bd649e729db32019811dcafc295e0ca4d`, `apksigner verify` válido). Permisos: `INTERNET`,
`ACCESS_NETWORK_STATE` y los dos que agregan las bibliotecas (`READ_GSERVICES` y el permiso interno de
AndroidX), igual que en la entrega 1; el ajuste no agrega permisos.

## Nota de entorno

Las instrumentadas fallaron la primera vez (18 de 79, incluso pruebas que no se tocaron) porque los emuladores
estaban levantados con `npm run emulators` (proyecto `demo-desconectado`), mientras la app de depuración usa
`des-conectado`: la limpieza de cuentas entre pruebas no llegaba al proyecto correcto y el registro fallaba con
"correo en uso". Se resolvió levantando los emuladores con `--project des-conectado` y sembrando con
`EMULATOR_PROJECT_ID=des-conectado node seed/seed.mjs --emulator`. Ver `quickstart.md` §3. Además, las pruebas de
reglas ahora corren en serie (`--test-concurrency=1`): con la subcolección nueva compartían datos en paralelo.

## Pendiente (requiere a la persona)

- T135: publicar reglas e índice en el proyecto real (`firebase deploy --only firestore:rules,firestore:indexes`)
  y volver a sembrar los catálogos.
- T136: M-14 en el proyecto real (cargar saldo y movimientos desde la consola y borrarlos al terminar).
- T137: instalar `dist/desConectado-entrega1-ajuste.apk` en el teléfono y ejecutar M-13 a M-16.
- T134: M-15 (modo avión) en dispositivo; el comportamiento está cubierto por pruebas automatizadas, falta la
  pasada manual.
