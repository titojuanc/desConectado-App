import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, runTransaction, serverTimestamp, setDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de canje de recompensas', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const UID = 'ana';
  const ana = () => comoPersona(entorno, UID, 'ana@mail.com');
  const rewardId = 'debug-cupon-1-punto';
  const redemptionId = 'redemption-test-001';
  const code = 'DC-cmVkZW1wdGlvbi10ZXN0LTAwMQ';

  async function seed(balance = 1, cost = 1) {
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, `users/${UID}`), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: balance,
      });
      await setDoc(doc(db, `rewards/${rewardId}`), {
        name: 'Cupón debug', description: 'Prueba', costPoints: cost, kind: 'coupon', order: 0,
      });
    });
  }

  it('rechaza el canje atómico legacy sin lote y pending', async () => {
    await seed(1, 1);
    const db = ana();
    const user = doc(db, `users/${UID}`);
    const redemption = doc(db, `users/${UID}/redeemedRewards/${redemptionId}`);
    const movement = doc(db, `users/${UID}/movements/redeem-${redemptionId}`);

    await assertFails(runTransaction(db, async (transaction) => {
      const snapshot = await transaction.get(user);
      const balance = snapshot.data().pointsBalance;
      transaction.set(redemption, {
        redemptionId, rewardId, name: 'Cupón debug', costPoints: 1,
        movementId: movement.id, code, createdAt: serverTimestamp(),
      });
      transaction.set(movement, {
        type: 'redeem', amount: 1, rewardId, sourceId: redemptionId,
        createdAt: serverTimestamp(), code,
      });
      transaction.update(user, { pointsBalance: balance - 1, lastMovementId: movement.id });
    }));
  });

  it('rechaza un canje directo sin actualizar el saldo en la misma transacción', async () => {
    await seed(1, 1);
    await assertFails(setDoc(doc(ana(), `users/${UID}/movements/redeem-direct`), {
      type: 'redeem', amount: 1, rewardId, sourceId: 'direct', challengeId: null,
      createdAt: serverTimestamp(), code,
    }));
  });
});
