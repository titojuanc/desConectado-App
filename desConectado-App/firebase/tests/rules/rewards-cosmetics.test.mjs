import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, serverTimestamp, setDoc, updateDoc, writeBatch } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de propiedad y aplicación de cosméticos', () => {
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
      await setDoc(doc(db, 'rewards/theme-bosque'), {
        name: 'Tema Bosque', description: 'Paleta verde para la app.',
        kind: 'theme', costPoints: 100, order: 1, active: true, config: { palette: 'forest' },
      });
      await setDoc(doc(db, 'rewards/background-montanas'), {
        name: 'Fondo Montañas', description: 'Fondo para desafíos.',
        kind: 'focus-background', costPoints: 60, order: 2, active: true, config: { background: 'mountains' },
      });
      await setDoc(doc(db, 'users/ana/redeemedRewards/redemption-theme'), {
        redemptionId: 'redemption-theme', rewardId: 'theme-bosque', name: 'Tema Bosque',
        costPoints: 100, movementId: 'redeem-redemption-theme-lot-1',
        movementIds: ['redeem-redemption-theme-lot-1'], code: null, createdAt: new Date(),
        kind: 'theme', config: { palette: 'forest' },
      });
      await setDoc(doc(db, 'users/ana/cosmeticOwnership/theme-bosque'), {
        rewardId: 'theme-bosque', name: 'Tema Bosque', kind: 'theme',
        config: { palette: 'forest' }, redemptionId: 'redemption-theme', createdAt: new Date(),
      });
    });
  }

  it('permite activar solo una personalización poseída y quitarla', async () => {
    await seed();
    const preferences = doc(ana(), 'users/ana/preferences/current');

    await assertSucceeds(setDoc(preferences, { activeCosmetics: { theme: 'theme-bosque' } }));
    await assertSucceeds(updateDoc(preferences, { activeCosmetics: { theme: null } }));
    await assertFails(setDoc(doc(ana(), 'users/ana/preferences/current'), {
      activeCosmetics: { theme: 'background-montanas' },
    }));
    await assertFails(setDoc(doc(beto(), 'users/ana/preferences/current'), {
      activeCosmetics: { theme: 'theme-bosque' },
    }));
  });

  it('lee propiedad solo su dueño y no permite crearla sin canje asociado, editarla o borrarla', async () => {
    await seed();
    const owned = doc(ana(), 'users/ana/cosmeticOwnership/theme-bosque');
    await assertFails(updateDoc(owned, { name: 'Modificado' }));
    await assertFails(deleteDoc(owned));
    await assertFails(setDoc(owned, {
      rewardId: 'theme-bosque', name: 'Tema Bosque', kind: 'theme',
      config: { palette: 'forest' }, redemptionId: 'inventado', createdAt: serverTimestamp(),
    }));
    await assertFails(setDoc(doc(beto(), 'users/ana/cosmeticOwnership/theme-bosque'), {}));
  });

  it('rechaza una caja que intenta volver a asignar un cosmético ya poseído', async () => {
    await seed();
    await entorno.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 500,
      });
      await setDoc(doc(context.firestore(), 'rewards/surprise-box'), {
        name: 'Caja sorpresa', description: 'Cosmético aleatorio.',
        kind: 'surprise-box', costPoints: 120, order: 3, active: true, config: {},
      });
    });

    await assertFails(setDoc(doc(ana(), 'users/ana/pendingRedemptions/current'), {
      redemptionId: 'box-redemption', rewardId: 'surprise-box', name: 'Caja sorpresa',
      costPoints: 120, code: null, pointsDebited: 0, lotDebits: {},
      createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      kind: 'surprise-box', config: {},
      grantedRewardId: 'theme-bosque', grantedRewardName: 'Tema Bosque',
      grantedRewardKind: 'theme', grantedRewardConfig: { palette: 'forest' },
    }));
  });

  it('acepta fijar en pending un premio cosmético activo que todavía no posee', async () => {
    await seed();
    await entorno.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 500,
      });
      await setDoc(doc(context.firestore(), 'rewards/surprise-box'), {
        name: 'Caja sorpresa', description: 'Cosmético aleatorio.',
        kind: 'surprise-box', costPoints: 120, order: 3, active: true, config: { eligibility: 'unowned-cosmetics' },
      });
    });

    await assertSucceeds(setDoc(doc(ana(), 'users/ana/pendingRedemptions/current'), {
      redemptionId: 'box-redemption', rewardId: 'surprise-box', name: 'Caja sorpresa',
      costPoints: 120, code: null, pointsDebited: 0, lotDebits: {},
      createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      kind: 'surprise-box', config: { eligibility: 'unowned-cosmetics' },
      grantedRewardId: 'background-montanas', grantedRewardName: 'Fondo Montañas',
      grantedRewardKind: 'focus-background', grantedRewardConfig: { background: 'mountains' },
    }));
  });

  it('finaliza la caja y crea el ownership del premio en la misma transacción', async () => {
    await seed();
    const redemptionId = 'box-redemption-complete';
    const grantedRewardId = 'background-montanas';
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 120,
      });
      await setDoc(doc(db, 'rewards/surprise-box'), {
        name: 'Caja sorpresa', description: 'Cosmético aleatorio.',
        kind: 'surprise-box', costPoints: 120, order: 3, active: true,
        config: { eligibility: 'unowned-cosmetics' },
      });
      await setDoc(doc(db, 'users/ana/pointLots/lot-box'), {
        lotId: 'lot-box', localDate: '2026-10-08', timeZoneId: 'UTC',
        windowStartsAt: new Date('2026-10-08T00:00:00Z'),
        windowEndsAt: new Date('2026-10-09T00:00:00Z'),
        earnedAt: new Date('2026-10-08T10:00:00Z'),
        expiresAt: new Date('2026-11-08T00:00:00Z'), issuedPoints: 120, remainingPoints: 120,
      });
    });

    await assertSucceeds(setDoc(doc(ana(), 'users/ana/pendingRedemptions/current'), {
      redemptionId, rewardId: 'surprise-box', name: 'Caja sorpresa', costPoints: 120,
      code: null, pointsDebited: 0, lotDebits: {},
      createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      kind: 'surprise-box', config: { eligibility: 'unowned-cosmetics' },
      grantedRewardId, grantedRewardName: 'Fondo Montañas',
      grantedRewardKind: 'focus-background', grantedRewardConfig: { background: 'mountains' },
    }));

    const db = ana();
    const movement = doc(db, `users/ana/movements/redeem-${redemptionId}-lot-box`);
    const debit = writeBatch(db);
    debit.update(doc(db, 'users/ana/pointLots/lot-box'), { remainingPoints: 0 });
    debit.set(movement, {
      type: 'redeem', amount: 120, rewardId: 'surprise-box', sourceId: redemptionId,
      redemptionId, lotId: 'lot-box', createdAt: serverTimestamp(), code: null,
    });
    debit.update(doc(db, 'users/ana/pendingRedemptions/current'), {
      pointsDebited: 120, lotDebits: { 'lot-box': 120 }, updatedAt: serverTimestamp(),
    });
    debit.update(doc(db, 'users/ana'), { pointsBalance: 0, lastMovementId: movement.id });
    await assertSucceeds(debit.commit());

    const finalize = writeBatch(db);
    finalize.set(doc(db, `users/ana/redeemedRewards/${redemptionId}`), {
      redemptionId, rewardId: 'surprise-box', name: 'Caja sorpresa', costPoints: 120,
      movementId: movement.id, movementIds: [movement.id], code: null, createdAt: serverTimestamp(),
      kind: 'surprise-box', config: { eligibility: 'unowned-cosmetics' },
      grantedRewardId, grantedRewardName: 'Fondo Montañas',
      grantedRewardKind: 'focus-background', grantedRewardConfig: { background: 'mountains' },
    });
    finalize.set(doc(db, `users/ana/cosmeticOwnership/${grantedRewardId}`), {
      rewardId: grantedRewardId, name: 'Fondo Montañas', kind: 'focus-background',
      config: { background: 'mountains' }, redemptionId, createdAt: serverTimestamp(),
    });
    finalize.delete(doc(db, 'users/ana/pendingRedemptions/current'));

    await assertSucceeds(finalize.commit());
  });

  it('finaliza una compra cosmética directa junto con su propiedad', async () => {
    await seed();
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, 'users/ana'), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: 60,
      });
      await setDoc(doc(db, 'users/ana/pointLots/lot-direct'), {
        lotId: 'lot-direct', localDate: '2026-10-08', timeZoneId: 'UTC',
        windowStartsAt: new Date('2026-10-08T00:00:00Z'), windowEndsAt: new Date('2026-10-09T00:00:00Z'),
        earnedAt: new Date('2026-10-08T10:00:00Z'), expiresAt: new Date('2026-11-08T00:00:00Z'),
        issuedPoints: 60, remainingPoints: 60,
      });
    });

    const redemptionId = 'direct-cosmetic-redemption';
    await assertSucceeds(setDoc(doc(ana(), 'users/ana/pendingRedemptions/current'), {
      redemptionId, rewardId: 'background-montanas', name: 'Fondo Montañas', costPoints: 60,
      code: null, pointsDebited: 0, lotDebits: {}, createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      kind: 'focus-background', config: { background: 'mountains' },
    }));

    const db = ana();
    const movement = doc(db, `users/ana/movements/redeem-${redemptionId}-lot-direct`);
    const debit = writeBatch(db);
    debit.update(doc(db, 'users/ana/pointLots/lot-direct'), { remainingPoints: 0 });
    debit.set(movement, {
      type: 'redeem', amount: 60, rewardId: 'background-montanas', sourceId: redemptionId,
      redemptionId, lotId: 'lot-direct', createdAt: serverTimestamp(), code: null,
    });
    debit.update(doc(db, 'users/ana/pendingRedemptions/current'), {
      pointsDebited: 60, lotDebits: { 'lot-direct': 60 }, updatedAt: serverTimestamp(),
    });
    debit.update(doc(db, 'users/ana'), { pointsBalance: 0, lastMovementId: movement.id });
    await assertSucceeds(debit.commit());

    const finalize = writeBatch(db);
    finalize.set(doc(db, `users/ana/redeemedRewards/${redemptionId}`), {
      redemptionId, rewardId: 'background-montanas', name: 'Fondo Montañas', costPoints: 60,
      movementId: movement.id, movementIds: [movement.id], code: null, createdAt: serverTimestamp(),
      kind: 'focus-background', config: { background: 'mountains' },
    });
    finalize.set(doc(db, 'users/ana/cosmeticOwnership/background-montanas'), {
      rewardId: 'background-montanas', name: 'Fondo Montañas', kind: 'focus-background',
      config: { background: 'mountains' }, redemptionId, createdAt: serverTimestamp(),
    });
    finalize.delete(doc(db, 'users/ana/pendingRedemptions/current'));

    await assertSucceeds(finalize.commit());
  });
});