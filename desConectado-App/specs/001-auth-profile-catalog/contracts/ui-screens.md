# Contrato: Pantallas y navegación

**Spec**: [../spec.md](../spec.md)

Toda la interfaz está en español rioplatense, tratando a la persona de "vos" ("Ingresá", "Creá tu
cuenta", "Ya tenés una cuenta"), y usa la marca "(des)Conectado" (FR-023). Los textos exactos van en
los recursos de la app; aquí se fija la estructura y el comportamiento verificable.

## Mapa de navegación

```text
Arranque ──► (Cargando)
                │
      ┌─────────┴──────────┐
  SinSesión             ConSesión
      │                     │
  Ingreso ⇄ Registro    Navegación inferior (una pulsación por destino, SC-005)
    │                   ├── Desafíos
    ▼                   ├── Recompensas
  Restablecer           └── Perfil ──► Cerrar sesión ──► Ingreso
  contraseña
  (vuelve a Ingreso)
```

En cada una de las tres pestañas, en la barra superior y a la izquierda, se ve el **indicador de
puntos** (FR-028).

Sin sesión solo son alcanzables Ingreso, Registro y Restablecer contraseña (FR-012, FR-025). Con
sesión, ninguna de las tres es alcanzable.

Con sesión y sin conexión al abrir la app, se entra igual a la navegación inferior (la sesión no se
cierra) con el aviso de conexión requerida visible en la parte superior; cada sección muestra su
estado "Sin conexión" con Reintentar (FR-011).

## Pantallas

| Pantalla       | Contenido                                                                                             | FR                    |
|----------------|-------------------------------------------------------------------------------------------------------|-----------------------|
| Espera         | Marca "(des)Conectado" mientras se determina la sesión                                                 | FR-005, FR-023        |
| Ingreso        | Marca; campos correo y contraseña (oculta); botón Ingresar; botón "Continuar con Google"; enlace a Registro; enlace "Olvidé mi contraseña" | FR-004, FR-007, FR-010, FR-025 |
| Restablecer contraseña | Campo de correo; botón para enviar el enlace; tras enviar, mensaje de confirmación siempre igual y enlace para volver a Ingreso | FR-025, FR-026 |
| Registro       | Campos nombre de usuario, correo y contraseña (oculta); botón Crear cuenta; botón "Continuar con Google"; enlace a Ingreso | FR-001, FR-002, FR-007 |
| Desafíos       | Lista ordenada de 6; cada tarjeta con el título de la actividad, descripción (que dice que la condición es no usar redes), duración, puntos y una etiqueta de dificultad (texto y color) | FR-013 a FR-016      |
| Recompensas    | Lista ordenada por costo; cada tarjeta con nombre, descripción y costo en puntos; sin botón de canje    | FR-018 a FR-020       |
| Perfil         | Nombre de usuario, correo, saldo de puntos, últimos cinco desafíos hechos (título, puntos, fecha; texto explicativo si no hay), botón Cerrar sesión; sin contraseña | FR-006, FR-021, FR-022, FR-029 |
| Indicador de puntos | Ícono pequeño con el saldo, arriba a la izquierda de Desafíos, Recompensas y Perfil; al tocarlo lleva a Perfil (desde Perfil no hace nada) y no ofrece ninguna acción sobre los puntos | FR-028, FR-032 |

## Estados obligatorios de cada pantalla

Cada pantalla de red debe resolver estos estados de forma visible; ninguna queda en blanco:

| Estado          | Comportamiento                                                                          |
|-----------------|-----------------------------------------------------------------------------------------|
| Cargando        | Indicador de progreso                                                                   |
| Error           | Mensaje en español y botón Reintentar                                                   |
| Sin conexión    | Aviso claro de que se requiere conexión; los botones que usan la red quedan deshabilitados (FR-011) |
| Enviando        | Mientras hay una petición en curso el botón se deshabilita, evitando el doble toque     |

## Puntos: estados específicos (FR-030, FR-033)

| Situación                        | Indicador                       | Sección de puntos del Perfil                              |
|----------------------------------|---------------------------------|-----------------------------------------------------------|
| Cargando                         | Guion                           | Indicador de progreso                                     |
| Cuenta nueva o sin datos         | 0                               | Saldo 0 y texto explicativo ("Todavía no hiciste desafíos"), no un espacio vacío |
| Con desafíos hechos              | Saldo                           | Saldo y hasta cinco desafíos, del más reciente al más antiguo |
| Sin conexión o error de carga    | Guion (nunca un 0 inventado)    | Mensaje y botón Reintentar; el resto del Perfil sigue visible |

Un fallo en los puntos no bloquea el resto de la pantalla. Ninguna pantalla ofrece acciones para
ganar, gastar ni modificar puntos (FR-032).

## Mensajes de error de formularios

- Cada campo inválido muestra su propio mensaje junto al campo (FR-002).
- Correo repetido: "Este correo ya está registrado." (FR-003).
- Credenciales incorrectas: un único mensaje genérico, por ejemplo "Correo o contraseña incorrectos.",
  que no revela cuál de los dos falló (FR-004).
- Cancelar Google: vuelve al formulario sin mensaje de error (Historia 4, esc. 4).
- Correo inválido en Restablecer contraseña: mensaje junto al campo y no se envía nada (FR-025).
- Restablecimiento solicitado: siempre el mismo mensaje de confirmación, por ejemplo "Si el correo
  está registrado, te enviamos un enlace para elegir una contraseña nueva.", exista o no la cuenta
  (FR-026).

## Identificadores de prueba

Cada campo, botón y lista clave tiene una etiqueta de prueba estable (por ejemplo `campo_correo`,
`boton_ingresar`, `lista_desafios`) para que las pruebas de UI no dependan del texto visible. Los de
Restablecer contraseña son `enlace_olvide_password` (en Ingreso), `campo_correo_restablecer`,
`boton_enviar_restablecimiento` y `mensaje_restablecimiento_enviado`. Los de puntos son
`indicador_puntos` (en cada pestaña), `puntos_perfil`, `lista_desafios_hechos`,
`texto_sin_desafios_hechos` y `boton_reintentar_puntos`.
