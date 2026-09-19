# Contrato: Pantallas y navegación

**Spec**: [../spec.md](../spec.md)

Toda la interfaz está en español y usa la marca "(des)Conectado" (FR-023). Los textos exactos van
en los recursos de la app; aquí se fija la estructura y el comportamiento verificable.

## Mapa de navegación

```text
Arranque ──► (Cargando)
                │
      ┌─────────┴──────────┐
  SinSesión             ConSesión
      │                     │
  Ingreso ⇄ Registro    Navegación inferior (una pulsación por destino, SC-005)
                        ├── Desafíos
                        ├── Recompensas
                        └── Perfil ──► Cerrar sesión ──► Ingreso
```

Sin sesión solo son alcanzables Ingreso y Registro (FR-012). Con sesión, Ingreso y Registro no
son alcanzables.

## Pantallas

| Pantalla       | Contenido                                                                                             | FR                    |
|----------------|-------------------------------------------------------------------------------------------------------|-----------------------|
| Espera         | Marca "(des)Conectado" mientras se determina la sesión                                                 | FR-005, FR-023        |
| Ingreso        | Marca; campos correo y contraseña (oculta); botón Ingresar; botón "Continuar con Google"; enlace a Registro | FR-004, FR-007, FR-010 |
| Registro       | Campos nombre de usuario, correo y contraseña (oculta); botón Crear cuenta; botón "Continuar con Google"; enlace a Ingreso | FR-001, FR-002, FR-007 |
| Desafíos       | Lista ordenada; cada tarjeta con título, descripción, duración, puntos y una etiqueta de dificultad (texto y color) | FR-013 a FR-016      |
| Recompensas    | Lista ordenada por costo; cada tarjeta con nombre, descripción y costo en puntos; sin botón de canje    | FR-018 a FR-020       |
| Perfil         | Nombre de usuario, correo, botón Cerrar sesión; sin contraseña                                          | FR-006, FR-021, FR-022 |

## Estados obligatorios de cada pantalla

Cada pantalla de red debe resolver estos estados de forma visible; ninguna queda en blanco:

| Estado          | Comportamiento                                                                          |
|-----------------|-----------------------------------------------------------------------------------------|
| Cargando        | Indicador de progreso                                                                   |
| Error           | Mensaje en español y botón Reintentar                                                   |
| Sin conexión    | Aviso claro de que se requiere conexión; los botones que usan la red quedan deshabilitados (FR-011) |
| Enviando        | Mientras hay una petición en curso el botón se deshabilita, evitando el doble toque     |

## Mensajes de error de formularios

- Cada campo inválido muestra su propio mensaje junto al campo (FR-002).
- Correo repetido: "Este correo ya está registrado." (FR-003).
- Credenciales incorrectas: un único mensaje genérico, por ejemplo "Correo o contraseña incorrectos.",
  que no revela cuál de los dos falló (FR-004).
- Cancelar Google: vuelve al formulario sin mensaje de error (Historia 4, esc. 4).

## Identificadores de prueba

Cada campo, botón y lista clave tiene una etiqueta de prueba estable (por ejemplo `campo_correo`,
`boton_ingresar`, `lista_desafios`) para que las pruebas de UI no dependan del texto visible.
