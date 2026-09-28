# Contrato: Datos y reglas de seguridad de Firestore

**Spec**: [../spec.md](../spec.md) | **Modelo**: [../data-model.md](../data-model.md)

Interfaz entre la app y el backend. Cualquier cambio requiere actualizar este contrato, las reglas
en `firebase/firestore.rules` y sus pruebas.

## Colecciones

| Colección        | Documento     | Contenido                                |
|------------------|---------------|------------------------------------------|
| `users`          | `{uid}`       | Perfil y saldo de puntos (ver data-model) |
| `users/{uid}/movements` | `{id}` | Movimientos de puntos (solo lectura hoy)  |
| `challenges`     | `{id}`        | Desafíos del catálogo                     |
| `rewards`        | `{id}`        | Recompensas del catálogo                  |

## Permisos

| Recurso              | Leer                          | Crear                                        | Modificar / borrar |
|----------------------|-------------------------------|----------------------------------------------|--------------------|
| `users/{uid}`        | Solo el dueño (`auth.uid == uid`) | Solo el dueño, con las condiciones de abajo | Nadie desde la app |
| `users/{uid}/movements/{id}` | Solo el dueño (`auth.uid == uid`) | Nadie desde la app (FR-032)  | Nadie desde la app |
| `challenges/{id}`    | Cualquier persona autenticada | Nadie desde la app                           | Nadie desde la app |
| `rewards/{id}`       | Cualquier persona autenticada | Nadie desde la app                           | Nadie desde la app |
| Cualquier otra ruta  | Nadie                         | Nadie                                        | Nadie              |

Los catálogos los escribe únicamente el script de siembra con credenciales de administrador, que
no pasan por las reglas.

### Condiciones para crear `users/{uid}`

1. La persona está autenticada y `auth.uid == uid`.
2. El documento tiene los campos `username`, `email` y `createdAt`, y opcionalmente
   `pointsBalance`; ninguno más.
3. `username` es texto de 1 a 30 caracteres.
4. `email` es igual, en minúsculas, al correo del token de la persona autenticada.
5. `createdAt` es igual a la hora del servidor al momento de la escritura.
6. Si viene `pointsBalance`, es un entero igual a 0 (FR-030): nadie puede crearse una cuenta con
   puntos. Su ausencia equivale a 0. Es opcional para que el APK de la entrega 1, que no escribe
   ese campo, siga pudiendo registrar cuentas cuando se publiquen estas reglas; la app nueva
   siempre lo escribe en 0.

Los perfiles anteriores al ajuste 2026-09-23 no tienen `pointsBalance`: siguen siendo legibles y la
app los lee como 0. Como `users/{uid}` no admite modificaciones, no se les agrega el campo.

## Puntos y movimientos (solo lectura, FR-027, FR-032)

- La app lee `users/{uid}.pointsBalance` con el resto del perfil.
- Los últimos desafíos hechos se piden con `where type == "credit"`, `orderBy createdAt desc`,
  `limit 5`, forzando el servidor. Requiere el índice compuesto (`movements`: `type` asc, `createdAt`
  desc) de `firebase/firestore.indexes.json`, que se despliega con las reglas.
- Cualquier `create`, `update` o `delete` sobre `users/{uid}/movements/**` y cualquier `update` sobre
  `users/{uid}` (incluido `pointsBalance`) se rechaza. Nadie puede leer ni escribir los datos de
  otra cuenta (Principio VI, invariante 1).
- En las pruebas de reglas y de UI los movimientos se cargan con credenciales de administrador
  (sin pasar por las reglas); la app real no siembra ninguno.

## Lectura de catálogos

- Los listados se piden ordenados por `order` ascendente y forzando la lectura desde el servidor
  (sin caché), según la política sin conexión de `research.md` D-7.
- Volumen esperado: menos de 20 documentos por colección; sin paginación.

## Evolución prevista (no implementada en esta entrega)

Las entregas siguientes agregarán la acreditación, el canje y el vencimiento. La app originará esas
escrituras y Firestore las validará con reglas de seguridad (Principio VI): acreditar un movimiento
`credit` junto con el cambio de `pointsBalance` en un lote atómico, con el monto del catálogo y una
sola vez por desafío; después, canjes y vencimientos. También se agregarán el desafío en curso
(`users/{uid}/activeChallenge/current`) y el historial de intentos (`users/{uid}/attempts/{id}`);
ver `research.md` D-22. Al agregarlos, este contrato MUST actualizar la tabla de permisos: hoy nada
de esto admite escrituras desde la app. Las reglas exactas se definen en el plan de la entrega del
01/10 (`TODO(PUNTOS_SEGURIDAD)`).
