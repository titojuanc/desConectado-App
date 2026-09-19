# Evidencia manual en dispositivo real (guiones de quickstart.md §4)

Resultados **informados por la persona que probó**; no hay capturas ni cronometraje asociados.

- **Fecha**: 2026-09-19
- **Dispositivo**: teléfono Android físico (modelo por completar), con Google Play Services y cuenta de Google
- **Build**: `dist/desConectado-debug.apk` (depuración; firmada con la clave de depuración, SHA-1
  `18ce60cfa3cc1b6581590bc88f6c7e08b151072a` registrado en Firebase). **No es el APK de entrega**:
  hay que repetir una pasada corta con el de release (T108 → T111).
- **Backend**: proyecto real de Firebase `des-conectado` (reglas publicadas, catálogos sembrados)

| Guion | Historia | Resultado |
|-------|----------|-----------|
| M-1 Registrar cuenta nueva | 1 | Aprobado |
| M-2 Repetir el registro con el mismo correo | 1 | Aprobado |
| M-3 Cerrar y reabrir la app (sesión persistente) | 1 | Aprobado |
| M-4 Cerrar sesión; contraseña incorrecta y luego correcta | 1 | Aprobado |
| M-5 Modo avión al intentar ingresar | 1 | Aprobado |
| M-6 Continuar con Google, cuenta nueva | 4 | Aprobado |
| M-7 Repetir con la misma cuenta de Google | 4 | Aprobado |
| M-8 Cancelar la pantalla de Google | 4 | Aprobado |
| M-9 Desafíos (6) y Recompensas (5) | 2, 5 | Aprobado |
| M-10 Perfil, cuenta de correo y de Google | 3 | Aprobado |
| M-11 Restablecer contraseña con correo real | 6 | Aprobado (correo recibido con la plantilla en español de fábrica, en tuteo; ver T102) |
| M-12 Con sesión, modo avión y reabrir la app | 1 | Aprobado |
| T-VERIF-1 Correo con contraseña y luego Google con ese correo | 4 | Aprobado; **falta anotar qué ocurrió** (¿misma cuenta directamente, o diálogo de vinculación?) |

## Sin medir todavía (para T112)
SC-001 (registro < 2 min), SC-002 (ingreso < 30 s), SC-003 (Google < 30 s y sin escribir datos) y SC-009
(correo de restablecimiento < 2 min e ingreso con la contraseña nueva < 5 min).

## Pasada con el APK de entrega (T109 y T111)

Resultado **informado por la persona que probó**; sin capturas ni cronometraje.

- **Fecha**: 2026-09-19
- **Build**: `dist/desConectado-entrega1.apk` (release, firmada con la keystore `desconectado`, SHA-1
  `1acfe34bd649e729db32019811dcafc295e0ca4d`, registrado en Firebase). Detalles en `../apk-release.md`.
- **T109**: instalado en el teléfono real; abre y muestra Ingreso. Hecho.
- **T111**: guiones ejecutados con este APK contra el proyecto real `des-conectado`. Hecho (informado),
  incluido M-6 (Google) con la firma de release.
- **Pendiente**: tiempos medidos de SC-001, SC-002, SC-003 y SC-009 (sin registrar) y el resultado exacto
  de T-VERIF-1.
