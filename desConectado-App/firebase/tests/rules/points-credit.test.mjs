import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de acreditación de puntos', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const ana = () => comoPersona(entorno, 'ana', 'ana@mail.com');

  async function sembrar() {
    await entorno.withSecurityRulesDisabled(async (contexto) => {
      await setDoc(doc(contexto.firestore(), 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 0,
      });
      await setDoc(doc(contexto.firestore(), 'challenges/facil-30-minutos'), {
        title: 'Salir a caminar', points: 10, durationMinutes: 30,
      });
    });
  }

  it('rechaza crear un crédito directamente sin actualizar el saldo en el mismo lote', async () => {
    await sembrar();
    await assertFails(setDoc(doc(ana(), 'users/ana/movements/credit-run-1'), {
      type: 'credit', amount: 10, challengeId: 'facil-30-minutos',
      sourceId: 'run-1', createdAt: new Date(),
    }));
  });

  it('rechaza modificar el saldo sin movimiento autorizado', async () => {
    await sembrar();
    await assertFails(updateDoc(doc(ana(), 'users/ana'), { pointsBalance: 10 }));
  });
});
