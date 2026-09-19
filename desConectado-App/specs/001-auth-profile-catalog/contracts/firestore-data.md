# Contrato: Datos y reglas de seguridad de Firestore

**Spec**: [../spec.md](../spec.md) | **Modelo**: [../data-model.md](../data-model.md)

Interfaz entre la app y el backend. Cualquier cambio requiere actualizar este contrato, las reglas
en `firebase/firestore.rules` y sus pruebas.

## Colecciones

| Colección        | Documento     | Contenido                                |
|------------------|---------------|------------------------------------------|
| `users`          | `{uid}`       | Perfil de la persona (ver data-model)     |
| `challenges`     | `{id}`        | Desafíos del catálogo                     |
| `rewards`        | `{id}`        | Recompensas del catálogo                  |

## Permisos

| Recurso              | Leer                          | Crear                                        | Modificar / borrar |
|----------------------|-------------------------------|----------------------------------------------|--------------------|
| `users/{uid}`        | Solo el dueño (`auth.uid == uid`) | Solo el dueño, con las condiciones de abajo | Nadie desde la app |
| `challenges/{id}`    | Cualquier persona autenticada | Nadie desde la app                           | Nadie desde la app |
| `rewards/{id}`       | Cualquier persona autenticada | Nadie desde la app                           | Nadie desde la app |
| Cualquier otra ruta  | Nadie                         | Nadie                                        | Nadie              |

Los catálogos los escribe únicamente el script de siembra con credenciales de administrador, que
no pasan por las reglas.

### Condiciones para crear `users/{uid}`

1. La persona está autenticada y `auth.uid == uid`.
2. El documento tiene exactamente los campos `username`, `email` y `createdAt`; ninguno más.
3. `username` es texto de 1 a 30 caracteres.
4. `email` es igual, en minúsculas, al correo del token de la persona autenticada.
5. `createdAt` es igual a la hora del servidor al momento de la escritura.

## Lectura de catálogos

- Los listados se piden ordenados por `order` ascendente y forzando la lectura desde el servidor
  (sin caché), según la política sin conexión de `research.md` D-7.
- Volumen esperado: menos de 20 documentos por colección; sin paginación.

## Evolución prevista (no implementada en esta entrega)

Las entregas siguientes agregarán al perfil el saldo de puntos y colecciones de historial. Esos
campos **no** podrán ser escritos por la app; su escritura será autoritativa del backend
(Principio VI). Ver riesgo R-2 en `research.md`.
