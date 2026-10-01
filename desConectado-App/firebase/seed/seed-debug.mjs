// Siembra un desafío de prueba sin modificar los seis desafíos oficiales del catálogo.
// Uso: node seed/seed-debug.mjs --emulator
//      GOOGLE_APPLICATION_CREDENTIALS=... node seed/seed-debug.mjs --project des-conectado
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

const args = process.argv.slice(2);
const emulator = args.includes('--emulator');
const projectIndex = args.indexOf('--project');
const projectId = projectIndex >= 0 ? args[projectIndex + 1] : null;
if ((emulator ? 1 : 0) + (projectId ? 1 : 0) !== 1) {
  throw new Error('Usá exactamente uno: --emulator o --project <id>');
}

const id = 'debug-10-segundos';
const desafio = {
  title: 'Prueba de 10 segundos',
  description: 'Desafío técnico para probar la medición sin usar redes.',
  durationMinutes: 1,
  durationSeconds: 10,
  difficulty: 'easy',
  points: 1,
  order: 0,
};
const recompensa = {
  name: 'Cupón de prueba (1 punto)',
  description: 'Recompensa digital de prueba dentro de (des)Conectado. No tiene valor fuera de la app.',
  costPoints: 1,
  kind: 'coupon',
  order: 0,
};

if (emulator) {
  process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
  initializeApp({ projectId: process.env.EMULATOR_PROJECT_ID ?? 'des-conectado' });
} else {
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    throw new Error('Falta GOOGLE_APPLICATION_CREDENTIALS');
  }
  initializeApp({ credential: applicationDefault(), projectId });
}

const db = getFirestore();
await db.collection('challenges').doc(id).set(desafio);
await db.collection('rewards').doc('debug-cupon-1-punto').set(recompensa);
console.log(`Desafío ${id} y recompensa debug-cupon-1-punto sembrados en ${emulator ? 'emulador' : projectId}.`);
