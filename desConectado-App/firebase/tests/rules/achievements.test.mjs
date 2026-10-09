import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de logros', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const ana = () => comoPersona(entorno, 'ana', 'ana@mail.com');
  const beto = () => comoPersona(entorno, 'beto', 'beto@mail.com');

  async function seed() {
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 0,
      });
      await setDoc(doc(db, 'achievements/en-marcha'), {
        achievementId: 'en-marcha', title: 'En marcha', description: 'Cinco desafíos completados.',
        criterion: 'completed_challenges', threshold: 5, order: 2, active: true,
      });
    });
  }

  function progreso(progress, threshold = 5) {
    return {
      achievementId: 'en-marcha', progress, threshold,
      unlockedAt: progress >= threshold ? serverTimestamp() : null,
      updatedAt: serverTimestamp(),
    };
  }

  it('permite al dueño guardar progreso parcial y desbloquear una sola vez', async () => {
    await seed();
    const achievement = doc(ana(), 'users/ana/achievements/en-marcha');

    await assertSucceeds(setDoc(achievement, progreso(4)));
    await assertSucceeds(updateDoc(achievement, progreso(5)));

    const saved = (await getDoc(achievement)).data();
    await assertFails(updateDoc(achievement, {
      progress: 5, threshold: 5, unlockedAt: serverTimestamp(), updatedAt: serverTimestamp(),
    }));
    await assertFails(deleteDoc(achievement));
    await assertSucceeds(getDoc(doc(ana(), 'users/ana/achievements/en-marcha')));
    await assertFails(getDoc(doc(beto(), 'users/ana/achievements/en-marcha')));
    await assertFails(setDoc(doc(beto(), 'users/ana/achievements/en-marcha'), progreso(5)));
    await assertFails(setDoc(doc(ana(), 'users/ana/achievements/no-existe'), {
      ...progreso(5), achievementId: 'no-existe',
    }));
    if (!saved.unlockedAt) throw new Error('El logro debió persistir su fecha de desbloqueo');
  });

  it('rechaza progreso decreciente, umbral alterado y desbloqueo prematuro', async () => {
    await seed();
    const achievement = doc(ana(), 'users/ana/achievements/en-marcha');
    await assertSucceeds(setDoc(achievement, progreso(3)));

    await assertFails(updateDoc(achievement, { ...progreso(2), updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(achievement, { ...progreso(4, 6), updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(achievement, {
      ...progreso(4), unlockedAt: serverTimestamp(), updatedAt: serverTimestamp(),
    }));
  });
});