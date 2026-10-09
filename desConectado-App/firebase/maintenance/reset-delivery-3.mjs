import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const RESET_COLLECTIONS = Object.freeze([
  'movements',
  'pointLots',
  'pointLotSessions',
  'redeemedRewards',
  'pendingRedemptions',
  'challengeResults',
  'challengeRatings',
  'activeChallenge',
  'achievements',
  'cosmeticOwnership',
]);

export function buildUserResetPatch({ deleteField, serverTimestamp }) {
  return {
    pointsBalance: 0,
    lastMovementId: deleteField(),
    delivery3ResetAt: serverTimestamp(),
  };
}

const DEFAULT_CHUNK_SIZE = 400;
const MAX_CHUNK_SIZE = 450;

export function parseResetOptions(argv) {
  const options = {
    projectId: null,
    dryRun: true,
    execute: false,
    confirmation: null,
    chunkSize: DEFAULT_CHUNK_SIZE,
  };

  for (let index = 0; index < argv.length; index++) {
    const argument = argv[index];
    if (argument === '--project') options.projectId = argv[++index] ?? null;
    else if (argument === '--dry-run') {
      if (options.execute) throw new Error('--dry-run y --execute son incompatibles');
      options.dryRun = true;
    } else if (argument === '--execute') {
      if (options.dryRun === false) throw new Error('Se recibió más de un modo');
      options.execute = true;
      options.dryRun = false;
    } else if (argument === '--confirm-reset-delivery-3') options.confirmation = argv[++index] ?? null;
    else if (argument === '--chunk-size') {
      const value = Number(argv[++index]);
      if (!Number.isInteger(value) || value < 1 || value > MAX_CHUNK_SIZE) {
        throw new Error(`--chunk-size debe estar entre 1 y ${MAX_CHUNK_SIZE}`);
      }
      options.chunkSize = value;
    } else {
      throw new Error(`Argumento desconocido: ${argument}`);
    }
  }

  if (!options.projectId) throw new Error('Se requiere --project <id> explícito');
  if (options.execute && options.confirmation !== options.projectId) {
    throw new Error('La confirmación debe coincidir exactamente con --project <id>');
  }
  if (!options.execute && options.confirmation != null) {
    throw new Error('La confirmación solo se permite junto con --execute');
  }
  return options;
}

async function crearPlan(db) {
  const users = await db.collection('users').get();
  const ops = [];
  const totals = Object.fromEntries(RESET_COLLECTIONS.map((name) => [name, 0]));

  for (const user of users.docs) {
    const userRef = db.collection('users').doc(user.id);
    ops.push({ type: 'reset-user', ref: userRef });
    for (const collectionName of RESET_COLLECTIONS) {
      const documents = await userRef.collection(collectionName).get();
      totals[collectionName] += documents.size;
      for (const document of documents.docs) ops.push({ type: 'delete', ref: document.ref });
    }
  }
  return { usersCount: users.size, totals, ops };
}

async function aplicarPlan(db, ops, chunkSize) {
  for (let offset = 0; offset < ops.length; offset += chunkSize) {
    const batch = db.batch();
    for (const operation of ops.slice(offset, offset + chunkSize)) {
      if (operation.type === 'reset-user') {
        batch.set(operation.ref, buildUserResetPatch({
          deleteField: () => FieldValue.delete(),
          serverTimestamp: () => FieldValue.serverTimestamp(),
        }), { merge: true });
      } else {
        batch.delete(operation.ref);
      }
    }
    await batch.commit();
  }
}

export async function ejecutarReset(options, db) {
  const plan = await crearPlan(db);
  console.log(`Proyecto: ${options.projectId}`);
  console.log(`Modo: ${options.dryRun ? 'DRY-RUN (sin escrituras)' : 'EJECUCION CONFIRMADA'}`);
  console.log(`Perfiles conservados: ${plan.usersCount}`);
  for (const collectionName of RESET_COLLECTIONS) {
    console.log(`  ${collectionName}: ${plan.totals[collectionName]} documentos para borrar`);
  }
  console.log(`Saldo: 0; lastMovementId: eliminado; delivery3ResetAt: ${plan.usersCount} perfiles marcados`);
  console.log('Auth, users/{uid} y users/{uid}/preferences/current se conservan. No se crean challengeResults.');

  if (!options.execute) return { usersCount: plan.usersCount, totals: plan.totals, writes: 0 };
  await aplicarPlan(db, plan.ops, options.chunkSize);
  console.log(`Reset aplicado en ${Math.ceil(plan.ops.length / options.chunkSize)} batches.`);
  return { usersCount: plan.usersCount, totals: plan.totals, writes: plan.ops.length };
}

async function main() {
  const options = parseResetOptions(process.argv.slice(2));
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    throw new Error('Falta GOOGLE_APPLICATION_CREDENTIALS; no se inicializó Firebase.');
  }
  initializeApp({ credential: applicationDefault(), projectId: options.projectId });
  await ejecutarReset(options, getFirestore());
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(`Reset cancelado: ${error.message}`);
    process.exitCode = 1;
  });
}