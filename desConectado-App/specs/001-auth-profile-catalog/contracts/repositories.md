# Contrato: Repositorios y validaciones de la app

**Spec**: [../spec.md](../spec.md) | **Modelo**: [../data-model.md](../data-model.md)

Interfaz interna entre la interfaz de usuario y la capa de datos. Existe para poder probar los
`ViewModel` con dobles de prueba sin Firebase (Principio IV). Las firmas son ilustrativas del
comportamiento exigido, no código final.

## Resultados y errores

Toda operación de red devuelve un resultado que es éxito o uno de estos errores tipados. La UI
mapea cada error a un mensaje en español; nunca muestra el texto crudo del servicio.

| Error                | Cuándo                                                          | FR       |
|----------------------|-----------------------------------------------------------------|----------|
| `SinConexion`        | No hay red o falla el acceso al servicio por red                | FR-011   |
| `CorreoEnUso`        | Registro con un correo ya registrado                            | FR-003   |
| `CredencialesInvalidas` | Correo no registrado o contraseña incorrecta (no se distingue) | FR-004   |
| `Cancelado`          | La persona cerró la pantalla de Google                          | Historia 4 |
| `CuentaExistenteConOtroProveedor` | Google con un correo que ya tiene contraseña y Firebase no unifica solo | FR-008 |
| `Desconocido`        | Cualquier otro fallo                                            | —        |

## AuthRepository

```text
authState: Flujo<EstadoSesion>          // Cargando | SinSesion | ConSesion(uid)

registrar(username, email, password)    -> Resultado<Unit>
ingresar(email, password)               -> Resultado<Unit>
ingresarConGoogle(idToken)              -> Resultado<Unit>
vincularGoogle(idToken)                 -> Resultado<Unit>   // solo tras CuentaExistenteConOtroProveedor
cerrarSesion()                          -> Unit
verificarCuenta()                       -> Unit              // recarga; cierra sesión si la cuenta no existe
```

Comportamiento exigido:
- `registrar` recorta y pasa a minúsculas el correo (FR-009), crea la cuenta y el perfil, y deja la
  sesión iniciada (Historia 1, esc. 1). Un doble toque no crea dos cuentas.
- `ingresar` aplica la misma normalización de correo.
- `ingresarConGoogle` crea el perfil si no existe (Historia 4, esc. 1) y reutiliza la cuenta si ya
  existía (esc. 2).
- El `idToken` lo obtiene la capa de UI con el gestor de credenciales de Android; el repositorio no
  depende de `Activity`.

## ProfileRepository

```text
perfil(uid)   -> Resultado<Perfil>      // Perfil(username, email)
asegurarPerfil(uid, nombre?, email)     -> Resultado<Unit>   // crea si falta
```

`Perfil` nunca contiene la contraseña (FR-010, FR-021).

## CatalogRepository

```text
desafios()      -> Resultado<Lista<Desafio>>      // ordenados por `order`; lectura del servidor
recompensas()   -> Resultado<Lista<Recompensa>>   // ordenadas por `order`; lectura del servidor
```

Sin conexión devuelven `SinConexion`; nunca datos en caché (FR-011).

## ConnectivityMonitor

```text
estado: Flujo<Conectividad>   // Conectado | SinConexion
```

## Validaciones (funciones puras en `domain/`)

| Función                       | Regla                                                                  | FR     |
|-------------------------------|------------------------------------------------------------------------|--------|
| `normalizarCorreo(s)`         | Recorta espacios y pasa a minúsculas                                   | FR-009 |
| `validarCorreo(s)`            | Formato de correo válido tras normalizar                               | FR-002 |
| `validarPassword(s)`          | Al menos 8 caracteres; acepta espacios y caracteres especiales tal cual | FR-002 |
| `validarUsername(s)`          | Entre 3 y 30 caracteres tras recortar                                  | FR-002 |
| `validarRegistro(u, e, p)`    | Devuelve la lista de campos inválidos, cada uno con su motivo          | FR-002 |
