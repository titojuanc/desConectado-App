# Evidencia manual de la Historia 4: ingresar con Google (T082)

Resultados **informados por la persona que probó**; no hay capturas ni cronometraje asociados.

- **Fecha**: 2026-09-19
- **Dispositivo**: teléfono Android físico (modelo por completar), con Google Play Services y una cuenta de Google
- **Build**: `dist/desConectado-debug.apk` (depuración, firmada con la clave de depuración cuyo SHA-1
  `18ce60cfa3cc1b6581590bc88f6c7e08b151072a` está registrado en Firebase)
- **Backend**: proyecto real de Firebase `des-conectado` (reglas publicadas, catálogos sembrados)

| Guion | Qué se hizo | Resultado |
|-------|-------------|-----------|
| M-6 | "Continuar con Google" con una cuenta real, sin cuenta previa | Aprobado |
| M-7 | Cerrar sesión y repetir con la misma cuenta de Google | Aprobado (informado) |
| M-10 (cuenta de Google) | Abrir Perfil y comprobar nombre y correo de Google | Aprobado (informado) |
| M-8 | Cancelar la pantalla de Google | Aprobado (informado) |
| T-VERIF-1 | Registrar `X@gmail.com` con contraseña; cerrar sesión; entrar con Google con ese correo | Aprobado (informado). **Firebase NO unificó las cuentas solo: se activó el camino de vinculación** (diálogo "Ya tenés una cuenta con este correo…" pidiendo la contraseña) y la vinculación funcionó. Sin cuenta duplicada. |

## Observaciones
- La configuración de Google se verificó además por API: proveedor habilitado, cliente OAuth de Android
  con el SHA-1 correcto en `google-services.json`.
- Tiempos de SC-003 (menos de 30 s y sin escribir datos): **sin medir**.
- **Resultado de T-VERIF-1 (2026-09-19)**: Firebase no unifica solo una cuenta de correo con Google; se pide la contraseña y se vincula. Es el camino de respaldo que la spec (FR-008) y el diseño ya contemplaban; queda confirmado en el proyecto real. T082 cerrada.
