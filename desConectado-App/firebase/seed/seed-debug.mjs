import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

export const catalogoPrueba = {
  challenges: ['move', 'focus', 'social', 'rest'].map((category, index) => ({
    id: index === 0 ? 'debug-10-segundos' : `debug-${category}-10-segundos`,
    title: `[TEST] ${category}: 10 segundos`,
    description: 'Prueba inmediata: diez segundos sin redes sociales. Otorga un punto.',
    durationMinutes: 1,
    durationSeconds: 10,
    difficulty: 'easy',
    points: 1,
    order: -4 + index,
    category,
    active: true,
  })),
  rewards: [
    { id: 'debug-theme-1-punto', name: '[TEST] Tema Atardecer', kind: 'theme', config: { palette: 'sunset' } },
    { id: 'debug-sound-1-punto', name: '[TEST] Sonido de finalizacion', kind: 'completion-sound', config: { sound: 'completion' } },
    { id: 'debug-cupon-1-punto', name: '[TEST] Cupon digital', kind: 'coupon', config: {} },
  ].map((reward, index) => ({
    ...reward,
    description: 'Recompensa digital de testeo de un punto. Sin valor fuera de la app.',
    costPoints: 1,
    order: -3 + index,
    active: true,
  })),
  achievements: [
    { id: 'debug-primer-desafio', name: '[TEST] Primera medalla', criterion: 'completed_challenges', threshold: 1 },
    { id: 'debug-diez-segundos', name: '[TEST] Diez segundos presente', criterion: 'completed_seconds', threshold: 10 },
    { id: 'debug-desafio-corto', name: '[TEST] Desafio de diez segundos', criterion: 'single_challenge_seconds', threshold: 10 },
  ].map((achievement, index) => ({
    ...achievement,
    description: 'Se desbloquea al completar un desafio de prueba de diez segundos.',
    order: index + 11,
    active: true,
  })),
};

export function opcionesPrueba(args) {
  const opciones = { dryRun: false, emulator: false, projectId: null };
  for (let index = 0; index < args.length; index++) {
    if (args[index] === '--dry-run') opciones.dryRun = true;
    else if (args[index] === '--emulator') opciones.emulator = true;
    else if (args[index] === '--project') {
      opciones.projectId = args[++index];
      if (!opciones.projectId || opciones.projectId.startsWith('--')) throw new Error('Falta project id');
    } else throw new Error(`Argumento desconocido: ${args[index]}`);
  }
  if ([opciones.dryRun, opciones.emulator, Boolean(opciones.projectId)].filter(Boolean).length !== 1) {
    throw new Error('Usa exactamente uno: --dry-run, --emulator o --project <id>');
  }
  return opciones;
}

async function main() {
  const opciones = opcionesPrueba(process.argv.slice(2));
  if (opciones.dryRun) {
    console.log('Sin escribir: 4 desafios de 10 segundos/1 punto, 3 recompensas de 1 punto y 3 logros inmediatos. Solo IDs debug-; no borra ni archiva documentos.');
    return;
  }
  if (opciones.emulator) {
    process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
    initializeApp({ projectId: process.env.EMULATOR_PROJECT_ID ?? 'des-conectado' });
  } else {
    if (process.env.FIRESTORE_EMULATOR_HOST) throw new Error('Quita FIRESTORE_EMULATOR_HOST para produccion');
    if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) throw new Error('Falta GOOGLE_APPLICATION_CREDENTIALS');
    const credential = JSON.parse(readFileSync(process.env.GOOGLE_APPLICATION_CREDENTIALS, 'utf8'));
    if (credential.project_id !== opciones.projectId) throw new Error('Proyecto de credencial distinto al solicitado');
    initializeApp({ credential: applicationDefault(), projectId: opciones.projectId });
  }
  const db = getFirestore();
  const batch = db.batch();
  for (const [collection, records] of Object.entries(catalogoPrueba)) {
    for (const { id, ...data } of records) batch.set(db.collection(collection).doc(id), data);
  }
  await batch.commit();
  console.log(`Datos de testeo sembrados en ${opciones.emulator ? 'emulador' : opciones.projectId}; catalogo normal intacto.`);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => { console.error(error.message); process.exitCode = 1; });
}
