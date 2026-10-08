import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de calificación de desafíos', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const ana = () => comoPersona(entorno, 'ana', 'ana@mail.com');
  const resultado = (status) => ({
    challengeRunId: 'run-1', challengeId: 'test', challengeTitle: 'Prueba',
    durationMinutes: 1, durationSeconds: 60, startedAt: new Date(Date.now() - 120_000),
    finishedAt: new Date(), status, measuredSocialSeconds: 0, offlineSeconds: 0, pointsAwarded: 10,
  });

  async function seed(status = 'COMPLETED') {
    await entorno.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 10,
      });
      await setDoc(doc(context.firestore(), 'users/ana/challengeResults/run-1'), resultado(status));
    });
  }

  it('acepta una única valoración de 1 a 5 para resultado completado', async () => {
    await seed();

    await assertSucceeds(setDoc(doc(ana(), 'users/ana/challengeRatings/run-1'), {
      challengeRunId: 'run-1', stars: 5, createdAt: serverTimestamp(),
    }));
  });

  it('rechaza estrellas fuera de 1..5', async () => {
    await seed();
    for (const stars of [0, 6]) {
      await assertFails(setDoc(doc(ana(), `users/ana/challengeRatings/run-${stars}`), {
        challengeRunId: 'run-1', stars, createdAt: serverTimestamp(),
      }));
    }
  });

  it('rechaza calificar un resultado no completado, duplicarlo, editarlo o borrarlo', async () => {
    await seed('FAILED');
    await assertFails(setDoc(doc(ana(), 'users/ana/challengeRatings/run-1'), {
      challengeRunId: 'run-1', stars: 4, createdAt: serverTimestamp(),
    }));

    await seed();
    const rating = doc(ana(), 'users/ana/challengeRatings/run-1');
    await assertSucceeds(setDoc(rating, { challengeRunId: 'run-1', stars: 4, createdAt: serverTimestamp() }));
    await assertFails(setDoc(rating, { challengeRunId: 'run-1', stars: 5, createdAt: serverTimestamp() }));
    await assertFails(updateDoc(rating, { stars: 5 }));
    await assertFails(deleteDoc(rating));
  });
});