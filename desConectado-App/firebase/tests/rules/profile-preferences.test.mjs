import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de perfil y preferencias', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const ana = () => comoPersona(entorno, 'ana', 'ana@mail.com');
  const beto = () => comoPersona(entorno, 'beto', 'beto@mail.com');

  async function seed() {
    await entorno.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 0,
      });
    });
  }

  it('permite cambiar solo el nombre visible del perfil', async () => {
    await seed();
    const profile = doc(ana(), 'users/ana');
    await assertSucceeds(updateDoc(profile, { username: 'Lucía' }));
    await assertFails(updateDoc(profile, { username: '' }));
    await assertFails(updateDoc(profile, { username: 'a'.repeat(31) }));
    await assertFails(updateDoc(profile, { email: 'otra@mail.com' }));
    await assertFails(updateDoc(profile, { createdAt: new Date() }));
    await assertFails(updateDoc(doc(beto(), 'users/ana'), { username: 'Beto' }));
  });

  it('permite meta opcional para perfiles legacy y valida el rango configurado', async () => {
    await seed();
    const preferences = doc(ana(), 'users/ana/preferences/current');
    await assertSucceeds(setDoc(preferences, {
      notificationsEnabled: false,
      activeCosmetics: {},
    }));
    await assertSucceeds(updateDoc(preferences, { weeklyGoalMinutes: 30 }));
    await assertSucceeds(updateDoc(preferences, { weeklyGoalMinutes: 840 }));
    for (const weeklyGoalMinutes of [0, 45, 870]) {
      await assertFails(updateDoc(preferences, { weeklyGoalMinutes }));
    }
    await assertFails(updateDoc(preferences, { notificationsEnabled: 'sí' }));
    await assertFails(updateDoc(doc(beto(), 'users/ana/preferences/current'), {
      notificationsEnabled: true,
    }));
  });
});