import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, serverTimestamp, setDoc, writeBatch } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de canje pending por lotes', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const uid = 'ana';
  const redemptionId = 'redemption-pending-1';
  const rewardId = 'theme-test';
  const ana = () => comoPersona(entorno, uid, 'ana@mail.com');

  async function seed(balance = 70, olderExpiresAt = new Date('2026-11-01T03:00:00Z')) {
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, `users/${uid}`), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: balance,
      });
      await setDoc(doc(db, `rewards/${rewardId}`), {
        name: 'Tema de prueba', description: 'Digital', costPoints: 70, kind: 'theme', order: 1,
      });
      for (const [lotId, amount, date] of [
        ['lot-older', 40, '2026-10-01'],
        ['lot-newer', 50, '2026-10-02'],
      ]) {
        await setDoc(doc(db, `users/${uid}/pointLots/${lotId}`), {
          lotId, localDate: date, timeZoneId: 'America/Argentina/Buenos_Aires',
          windowStartsAt: new Date(`${date}T03:00:00Z`),
          windowEndsAt: new Date(`${date}T03:00:00Z`),
          earnedAt: new Date(`${date}T12:00:00Z`),
          expiresAt: lotId === 'lot-older' ? olderExpiresAt : new Date('2026-11-01T03:00:00Z'),
          issuedPoints: amount, remainingPoints: amount,
        });
      }
    });
  }

  async function iniciarPending() {
    await assertSucceeds(setDoc(doc(ana(), `users/${uid}/pendingRedemptions/current`), {
      redemptionId,
      rewardId,
      name: 'Tema de prueba',
      costPoints: 70,
      code: null,
      pointsDebited: 0,
      lotDebits: {},
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }));
  }

  function debit(lotId, amount, nextDebited, balanceAfter, lotBalanceAfter, lotDebits) {
    const db = ana();
    const movement = doc(db, `users/${uid}/movements/redeem-${redemptionId}-${lotId}`);
    const batch = writeBatch(db);
    batch.update(doc(db, `users/${uid}/pointLots/${lotId}`), { remainingPoints: lotBalanceAfter });
    batch.set(movement, {
      type: 'redeem', amount, rewardId, sourceId: redemptionId,
      redemptionId, lotId, createdAt: serverTimestamp(), code: null,
    });
    batch.update(doc(db, `users/${uid}/pendingRedemptions/current`), {
      pointsDebited: nextDebited, lotDebits, updatedAt: serverTimestamp(),
    });
    batch.update(doc(db, `users/${uid}`), {
      pointsBalance: balanceAfter, lastMovementId: movement.id,
    });
    return batch.commit();
  }

  it('completa un canje con débitos FIFO por lote y registra la recompensa al final', async () => {
    await seed();
    await iniciarPending();

    await assertSucceeds(debit('lot-older', 40, 40, 30, 0, { 'lot-older': 40 }));
    await assertSucceeds(debit('lot-newer', 30, 70, 0, 20, { 'lot-older': 40, 'lot-newer': 30 }));

    const db = ana();
    const batch = writeBatch(db);
    batch.set(doc(db, `users/${uid}/redeemedRewards/${redemptionId}`), {
      redemptionId,
      rewardId,
      name: 'Tema de prueba',
      costPoints: 70,
      movementId: `redeem-${redemptionId}-lot-older`,
      movementIds: [`redeem-${redemptionId}-lot-older`, `redeem-${redemptionId}-lot-newer`],
      code: null,
      createdAt: serverTimestamp(),
    });
    batch.delete(doc(db, `users/${uid}/pendingRedemptions/current`));
    await assertSucceeds(batch.commit());
  });

  it('rechaza iniciar un pending con saldo insuficiente', async () => {
    await seed(20);
    await assertFails(setDoc(doc(ana(), `users/${uid}/pendingRedemptions/current`), {
      redemptionId, rewardId, name: 'Tema de prueba', costPoints: 70,
      code: null, pointsDebited: 0, lotDebits: {},
      createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
    }));
  });

  it('rechaza iniciar un segundo canje mientras el primero está pending', async () => {
    await seed();
    await iniciarPending();

    await assertFails(setDoc(doc(ana(), `users/${uid}/pendingRedemptions/current`), {
      redemptionId: 'redemption-pending-2', rewardId, name: 'Otro tema', costPoints: 10,
      code: null, pointsDebited: 0, lotDebits: {},
      createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
    }));
  });

  it('rechaza un vencimiento mientras hay un pending', async () => {
    await seed(70, new Date(Date.now() - 60_000));
    await iniciarPending();
    const db = ana();
    const movement = doc(db, `users/${uid}/movements/expire-lot-older`);
    const batch = writeBatch(db);
    batch.update(doc(db, `users/${uid}/pointLots/lot-older`), { remainingPoints: 0 });
    batch.set(movement, {
      type: 'expire', amount: 40, lotId: 'lot-older', sourceId: 'lot-older',
      createdAt: serverTimestamp(),
    });
    batch.update(doc(db, `users/${uid}`), { pointsBalance: 30, lastMovementId: movement.id });

    await assertFails(batch.commit());
  });
});