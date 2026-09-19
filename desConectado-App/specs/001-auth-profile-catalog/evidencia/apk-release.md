# APK de entrega (T106 y T108)

Fecha: 2026-09-19. Archivo: `dist/desConectado-entrega1.apk` (no se versiona; la carpeta `dist/` está ignorada).

| Dato | Valor |
|------|-------|
| Tamaño | 13.9 MB |
| SHA-256 del APK | `60bea2ea9224afd47388628487df967bf2f6ad14b405e039bb6b98ca3690d5be` |
| Certificado de firma (SHA-1) | `1acfe34bd649e729db32019811dcafc295e0ca4d` (alias `desconectado`) |
| Huella registrada en Firebase | Sí; el `google-services.json` vigente trae esa huella de release y la de depuración |
| Verificación de firma | `apksigner verify`: válido (esquema v2) |
| Apunta a emuladores | No (`USE_FIREBASE_EMULATOR = false`) |
| Depurable | No (`BuildConfig.DEBUG = false`) |
| `targetSdk` | 37 (minSdk 26) |
| Proyecto de Firebase | `des-conectado` |
| Permisos declarados | `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_GSERVICES` y el permiso interno de AndroidX (ver `revision-constitucion.md`) |

## Pendiente
- T109: instalarlo en el teléfono y confirmar que abre y muestra Ingreso.
- T111: repetir una pasada corta de los guiones con este APK, en especial M-6 (Google), porque la firma
  de release es distinta de la de depuración con la que se probó hasta ahora.
- Guardar una copia de `keystore/desconectado-release.jks` y de sus contraseñas **fuera del repositorio**:
  sin ellas no se puede publicar una actualización firmada con la misma clave.
