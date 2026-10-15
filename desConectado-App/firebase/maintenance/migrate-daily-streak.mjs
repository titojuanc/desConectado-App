import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { FieldValue, Timestamp, getFirestore } from 'firebase-admin/firestore';

function fechaLocal(date, timeZone) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone, year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(date);
  return ['year', 'month', 'day'].map(type => parts.find(part => part.type === type).value).join('-');
}

export function resumirDia(resultados, creditos, puntosCatalogo, ahora) {
  const dias = new Map();
  for (const result of resultados) {
    if (result.status !== 'COMPLETED' || !creditos.has(result.challengeRunId)) continue;
    const finished = result.finishedAt.toDate();
    const key = fechaLocal(finished, result.timeZoneId);
    if (finished > ahora || key !== fechaLocal(ahora, result.timeZoneId)) continue;
    const base = puntosCatalogo.get(result.challengeId);
    if (!Number.isSafeInteger(base) || base < 1 || !Number.isSafeInteger(result.pointsAwarded) ||
        result.pointsAwarded < base || !Number.isSafeInteger(result.durationSeconds) || result.durationSeconds < 0) {
      throw new Error('Resultado o puntos de catalogo invalidos: no se puede reconstruir el acumulado.');
    }
    const day = dias.get(key) ?? { basePoints: 0, completedSeconds: 0, bonusAwarded: false };
    day.basePoints += base;
    day.completedSeconds += result.durationSeconds;
    day.bonusAwarded ||= result.pointsAwarded > base;
    day.lastRunId = result.challengeRunId;
    dias.set(key, day);
  }
  return dias;
}

async function main() {
  const [projectFlag, projectId, userFlag, uid, action = '--dry-run'] = process.argv.slice(2);
  if (projectFlag !== '--project' || !projectId || userFlag !== '--uid' || !uid ||
      !['--apply', '--dry-run'].includes(action) || ![6, 7].includes(process.argv.length)) {
    throw new Error('Uso: node maintenance/migrate-daily-streak.mjs --project <id> --uid <uid> [--dry-run|--apply]');
  }
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) throw new Error('Falta GOOGLE_APPLICATION_CREDENTIALS');
  if (process.env.FIRESTORE_EMULATOR_HOST) throw new Error('Quita FIRESTORE_EMULATOR_HOST para migrar la cuenta real');
  const credential = JSON.parse(readFileSync(process.env.GOOGLE_APPLICATION_CREDENTIALS, 'utf8'));
  if (credential.project_id !== projectId) throw new Error('Proyecto de credencial distinto al solicitado');
  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const user = db.collection('users').doc(uid);
  const ahora = new Date();
  const creados = await db.runTransaction(async transaction => {
    if (!(await transaction.get(user)).exists) throw new Error('No existe el perfil solicitado');
    const results = await transaction.get(user.collection('challengeResults')
      .where('finishedAt', '>=', Timestamp.fromMillis(ahora.getTime() - 48 * 3_600_000)));
    const completados = results.docs.map(doc => doc.data()).filter(result => result.status === 'COMPLETED');
    if (completados.length === 0) return 0;
    const credits = await transaction.getAll(...completados.map(result =>
      user.collection('movements').doc(`credit-${result.challengeRunId}`)));
    const acreditados = completados.filter((_, index) => credits[index].exists);
    if (acreditados.length === 0) return 0;
    const challengeIds = [...new Set(acreditados.map(result => result.challengeId))];
    const challenges = await transaction.getAll(...challengeIds.map(id => db.collection('challenges').doc(id)));
    const catalogo = new Map(challenges.map(doc => [doc.id, doc.get('points')]));
    const dias = resumirDia(acreditados, new Set(acreditados.map(result => result.challengeRunId)), catalogo, ahora);
    if (dias.size === 0) return 0;
    const refs = [...dias.keys()].map(key => user.collection('dailyStreakProgress').doc(key));
    const existentes = await transaction.getAll(...refs);
    let count = 0;
    existentes.forEach((doc, index) => {
      if (doc.exists) return;
      count++;
      if (action === '--apply') transaction.create(refs[index], {
        ...dias.get(refs[index].id), updatedAt: FieldValue.serverTimestamp(),
      });
    });
    return count;
  });
  console.log(`${action}: ${creados} acumulados diarios ${action === '--apply' ? 'creados' : 'por crear'}. Saldo y movimientos sin cambios.`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) await main();
