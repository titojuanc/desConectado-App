# Data Model: Cuenta, Perfil y Catálogos (Entrega 24/09)

**Fecha**: 2026-09-19 | **Spec**: [spec.md](spec.md) | **Contratos**: [contracts/](contracts/)

Los nombres de campo son los del almacén de datos (inglés, `camelCase`); los textos que ve la
persona van en español en los recursos de la app.

## Cuenta de usuario (gestionada por el servicio de autenticación)

Identidad de la persona. No se almacena en la base de datos propia; la conserva el servicio de
autenticación, que además guarda la contraseña de forma segura.

| Campo        | Descripción                                                     |
|--------------|-----------------------------------------------------------------|
| `uid`        | Identificador único e inmutable de la cuenta                    |
| `email`      | Correo, único por cuenta, sin distinguir mayúsculas             |
| proveedores  | Correo y contraseña, Google, o ambos                            |

**Reglas**: un correo pertenece a una sola cuenta (FR-008). La contraseña nunca sale del servicio
de autenticación ni se muestra (FR-010).

## Perfil — `users/{uid}`

Datos de la persona que la app muestra en el perfil (FR-021, FR-022) y su saldo de puntos
(FR-027, FR-028). Es la base a la que las entregas siguientes agregarán la acreditación, el canje y
el vencimiento (ver `research.md` D-19 y D-22).

| Campo       | Tipo      | Reglas                                                                 |
|-------------|-----------|------------------------------------------------------------------------|
| `username`  | string    | Registro: 3 a 30 caracteres, recortado. Servidor: 1 a 30 (ver nota)     |
| `email`     | string    | Minúsculas y sin espacios; igual al correo de la cuenta autenticada     |
| `createdAt` | timestamp | Lo fija el servidor al crear; no se modifica                            |
| `pointsBalance` | int   | Saldo de puntos. Al crear la cuenta vale 0; nunca negativo. Documentos anteriores al ajuste 2026-09-23 no lo tienen y se leen como 0 |

**Nota**: para una cuenta de Google, `username` es el nombre que entrega Google; si viene vacío, la
parte local del correo recortada a 30 caracteres. Por eso el servidor acepta desde 1 carácter; el
mínimo de 3 solo lo impone el formulario de registro.

**Relaciones**: 1 cuenta ↔ 1 perfil, con el mismo `uid` como identificador del documento.

**Transiciones**: el perfil se crea una sola vez y es de solo lectura después (spec: perfil no
editable), incluido `pointsBalance`: en esta entrega el cliente no puede cambiarlo (FR-032). Si una persona autenticada no tiene perfil (registro interrumpido, primera vez con
Google), la app lo crea al iniciar sesión.

## Movimiento de puntos — `users/{uid}/movements/{id}` (solo lectura en esta entrega)

Registro de un cambio en los puntos de la cuenta (Principio VI). En esta entrega ningún cliente lo
crea: solo se leen los que existan (FR-027, FR-029). Los cargará en el futuro la app al acreditar,
validada por las reglas de seguridad.

| Campo            | Tipo      | Reglas                                                                    |
|------------------|-----------|---------------------------------------------------------------------------|
| `type`           | string    | `credit` hoy; `redeem` y `expire` en entregas futuras                      |
| `amount`         | int       | Mayor que 0; el signo lo da `type` (`credit` suma; `redeem` y `expire` restan) |
| `challengeId`    | string    | Id del desafío del catálogo (solo en `credit`)                             |
| `challengeTitle` | string    | Título del desafío al momento de acreditar (solo en `credit`)              |
| `createdAt`      | timestamp | Lo fija el servidor; es la fecha de acreditación y base del vencimiento    |

**Reglas**: solo de agregado (no se edita ni se borra); pertenece a la cuenta cuyo `uid` está en su
camino. El saldo de la cuenta es la suma de sus movimientos con signo y nunca es negativo (FR-031).

**Últimos desafíos hechos** (FR-029): los cinco movimientos `credit` más recientes por `createdAt`,
del más nuevo al más antiguo, mostrados con `challengeTitle`, `amount` y `createdAt`.

**Estado inicial**: una cuenta nueva no tiene movimientos y su saldo es 0 (FR-030).

## Desafío — `challenges/{id}`

Propuesta predefinida de hacer una actividad al aire libre o social sin usar redes sociales durante
un tiempo. Provisto por la plataforma; ningún usuario lo crea ni lo modifica.

| Campo             | Tipo   | Reglas                                                       |
|-------------------|--------|--------------------------------------------------------------|
| `title`           | string | No vacío y único; nombre de la actividad ("Salir a trotar")    |
| `description`     | string | No vacía; menciona que la condición es no usar redes           |
| `durationMinutes` | int    | Mayor que 0                                                  |
| `difficulty`      | string | Uno de `easy`, `normal`, `hard`                              |
| `points`          | int    | Mayor que 0                                                  |
| `order`           | int    | Posición de listado; menor primero                           |

**Invariantes del catálogo** (FR-013, FR-015):
- Exactamente 6 desafíos, 2 por cada dificultad.
- Cualquier desafío de una dificultad mayor tiene estrictamente más `durationMinutes` y más
  `points` que cualquier desafío de una dificultad menor (`easy` < `normal` < `hard`).

**Presentación**: la duración se muestra en minutos u horas legibles ("30 minutos", "2 horas"); la
dificultad se muestra como Fácil, Normal o Difícil.

## Recompensa — `rewards/{id}`

Elemento digital del catálogo de la app. Provisto por la plataforma.

| Campo         | Tipo   | Reglas                                                        |
|---------------|--------|---------------------------------------------------------------|
| `name`        | string | No vacío                                                      |
| `description` | string | No vacía                                                      |
| `costPoints`  | int    | Mayor que 0                                                   |
| `kind`        | string | Uno de `badge`, `theme`, `coupon`                              |
| `order`       | int    | Posición de listado; menor primero (por costo ascendente)      |

**Invariantes**: al menos 5 recompensas (FR-018). Ninguna se presenta como beneficio fuera de la
app (FR-019): el texto de las recompensas de tipo `coupon` no menciona comercios ni descuentos
reales.

## Estado de sesión (solo en el dispositivo, no se persiste en la base de datos)

```text
Cargando ──► SinSesión ──► ConSesión
                 ▲              │
                 └──────────────┘  (cerrar sesión, o cuenta inexistente al verificarla)
```

| Estado      | Significado                                                                      |
|-------------|----------------------------------------------------------------------------------|
| `Cargando`  | La app está determinando si hay sesión guardada; muestra una pantalla de espera   |
| `SinSesión` | No hay persona identificada; se muestran solo registro e ingreso (FR-012)         |
| `ConSesión` | Persona identificada; se muestra la navegación principal                          |

Independiente de la sesión, la app publica un estado de **conectividad** (`Conectado` /
`SinConexión`) que bloquea acciones de red (FR-011).

## Camino previsto (no implementado)

No forma parte de esta entrega; se anota para que el modelo no obligue a migrar datos (detalle en
`research.md` D-22):

- `users/{uid}/activeChallenge/current`: el desafío que la persona está haciendo, si hay uno
  (`challengeId`, `startedAt`). Entrega del 01/10.
- `users/{uid}/attempts/{id}`: historial de intentos con estado (cumplido, no cumplido, cancelado,
  invalidado). Entrega del 08/10.
- Movimientos `redeem` y `expire`: canje y vencimiento a los 30 días de `createdAt`, con consumo
  FIFO. Entregas del 01/10 y 08/10.

## Datos de siembra

Fuente única: `firebase/seed/catalog.json`, con los valores iniciales de `spec.md` (Assumptions).
La siembra solo carga catálogos; no crea usuarios, saldos ni movimientos (no hay datos demo):

- 6 desafíos: "Salir a caminar" (30 min) y "Andar en bici" (1 h), Fácil, 10 y 20 pts; "Salir a
  trotar" (2 h) y "Juntarse con amigos" (4 h), Normal, 50 y 100 pts; "Excursión al aire libre" (8 h)
  y "Escapada a la naturaleza" (12 h), Difícil, 200 y 320 pts.
- 5 recompensas: de 50 a 500 puntos.

El script de siembra valida las invariantes antes de escribir y rechaza datos que las incumplan.
