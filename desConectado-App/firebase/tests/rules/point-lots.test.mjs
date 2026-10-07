import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, serverTimestamp, setDoc, writeBatch } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de lotes diarios de puntos', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const UID = 'ana';
  const LOT_ID = 'daily-2026-10-05-QW1lcmljYS9BcmdlbnRpbmEvQnVlbm9zX0FpcmVz';
  const ana = () => comoPersona(entorno, UID, 'ana@mail.com');

  async function seed() {
    const now = Date.now();
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, `users/${UID}`), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(now), pointsBalance: 0,
      });
      await setDoc(doc(db, 'challenges/test'), {
        title: 'Prueba', points: 10, durationMinutes: 1, durationSeconds: 60,
      });
      await setDoc(doc(db, `users/${UID}/challengeResults/run-1`), {
        challengeRunId: 'run-1', challengeId: 'test', challengeTitle: 'Prueba',
        status: 'COMPLETED', pointsAwarded: 10, durationSeconds: 60,
        startedAt: new Date(now - 120_000), finishedAt: new Date(now - 1_000),
        measuredSocialSeconds: 0, offlineSeconds: 0,
      });
    });
  }

  function creditoCompleto() {
    const db = ana();
    const lot = doc(db, `users/${UID}/pointLots/${LOT_ID}`);
    const session = doc(db, `users/${UID}/pointLotSessions/current`);
    const movement = doc(db, `users/${UID}/movements/credit-run-1`);
    const user = doc(db, `users/${UID}`);
    const batch = writeBatch(db);
    batch.set(lot, {
      lotId: LOT_ID,
      localDate: '2026-10-05',
      timeZoneId: 'America/Argentina/Buenos_Aires',
      windowStartsAt: new Date('2026-10-05T03:00:00Z'),
      windowEndsAt: new Date('2026-10-06T03:00:00Z'),
      earnedAt: new Date(Date.now() - 1_000),
      expiresAt: new Date('2026-11-04T03:00:00Z'),
      issuedPoints: 10,
      remainingPoints: 10,
    });
    batch.set(session, {
      lotId: LOT_ID,
      windowEndsAt: new Date('2026-10-06T03:00:00Z'),
    });
    batch.set(movement, {
      type: 'credit', amount: 10, challengeId: 'test', challengeTitle: 'Prueba',
      sourceId: 'run-1', lotId: LOT_ID, earnedAt: new Date(Date.now() - 1_000),
      createdAt: serverTimestamp(),
    });
    batch.update(user, { pointsBalance: 10, lastMovementId: movement.id });
    return batch;
  }

  it('acepta crédito atómico con lote y sesión diaria', async () => {
    await seed();

    await assertSucceeds(creditoCompleto().commit());
  });

  it('rechaza crédito que intenta cambiar el saldo sin crear un lote', async () => {
    await seed();
    const db = ana();
    const movement = doc(db, `users/${UID}/movements/credit-run-1`);
    const user = doc(db, `users/${UID}`);
    const batch = writeBatch(db);
    batch.set(movement, {
      type: 'credit', amount: 10, challengeId: 'test', challengeTitle: 'Prueba',
      sourceId: 'run-1', createdAt: serverTimestamp(),
    });
    batch.update(user, { pointsBalance: 10, lastMovementId: movement.id });

    await assertFails(batch.commit());
  });
});