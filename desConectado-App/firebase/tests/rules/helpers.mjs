import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const aqui = dirname(fileURLToPath(import.meta.url));

export const PROJECT_ID = 'demo-desconectado';

/** Crea el entorno de pruebas cargando `firestore.rules` en el emulador de Firestore. */
export async function crearEntorno() {
  const [host, puerto] = (process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080').split(':');
  return initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(resolve(aqui, '../../firestore.rules'), 'utf8'),
      host,
      port: Number(puerto),
    },
  });
}

/** Firestore visto por una persona autenticada con ese `uid` y correo en su token. */
export function comoPersona(entorno, uid, email) {
  return entorno.authenticatedContext(uid, { email }).firestore();
}

/** Firestore visto por alguien que no inició sesión. */
export function sinSesion(entorno) {
  return entorno.unauthenticatedContext().firestore();
}
