import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, serverTimestamp, setDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de participación en desafíos', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const ana = () => comoPersona(entorno, 'ana', 'ana@mail.com');
  const otra = () => comoPersona(entorno, 'otra', 'otra@mail.com');
  const activo = () => ({
    challengeId: 'facil-30-minutos',
    challengeTitle: 'Salir a caminar',
    durationMinutes: 30,
    points: 10,
    startedAt: serverTimestamp(),
    offlineSeconds: 0,
    status: 'ACTIVE',
    updatedAt: serverTimestamp(),
  });

  it('el dueño puede crear y leer su desafío activo', async () => {
    const ref = doc(ana(), 'users/ana/activeChallenge/current');
    await assertSucceeds(setDoc(ref, activo()));
    await assertSucceeds(getDoc(ref));
  });

  it('otra cuenta no puede leer ni escribir el desafío activo', async () => {
    const ref = doc(otra(), 'users/ana/activeChallenge/current');
    await assertFails(getDoc(ref));
    await assertFails(setDoc(ref, activo()));
  });

  it('rechaza estados activos inválidos', async () => {
    await assertFails(setDoc(doc(ana(), 'users/ana/activeChallenge/current'), { ...activo(), status: 'DONE' }));
  });
});
