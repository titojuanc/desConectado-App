import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, serverTimestamp, setDoc, writeBatch } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de vencimiento de puntos', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const UID = 'ana';
  const LOT_ID = 'daily-2026-08-01-QW1lcmljYS9BcmdlbnRpbmEvQnVlbm9zX0FpcmVz';
  const ana = () => comoPersona(entorno, UID, 'ana@mail.com');

  async function seed(expiresAt, remainingPoints = 15) {
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, `users/${UID}`), {
        username: 'Ana', email: 'ana@mail.com', createdAt: new Date(), pointsBalance: remainingPoints,
      });
      await setDoc(doc(db, `users/${UID}/pointLots/${LOT_ID}`), {
        lotId: LOT_ID,
        localDate: '2026-08-01',
        timeZoneId: 'America/Argentina/Buenos_Aires',
        windowEndsAt: new Date('2026-08-02T03:00:00Z'),
        expiresAt,
        earnedAt: new Date('2026-08-01T15:00:00Z'),
        issuedPoints: 40,
        remainingPoints,
      });
    });
  }

  async function expirar(importe) {
    const db = ana();
    const user = doc(db, `users/${UID}`);
    const lot = doc(db, `users/${UID}/pointLots/${LOT_ID}`);
    const movement = doc(db, `users/${UID}/movements/expire-${LOT_ID}`);
    const batch = writeBatch(db);
    batch.update(lot, { remainingPoints: 0 });
    batch.set(movement, {
      type: 'expire',
      amount: importe,
      lotId: LOT_ID,
      sourceId: LOT_ID,
      createdAt: serverTimestamp(),
    });
    batch.update(user, {
      pointsBalance: 15 - importe,
      lastMovementId: movement.id,
    });
    return batch.commit();
  }

  it('acepta un vencimiento expirado y descuenta solo el remanente en una transacción', async () => {
    await seed(new Date(Date.now() - 60_000));

    await assertSucceeds(expirar(15));
  });

  it('rechaza el débito antes de expiresAt según hora del servidor', async () => {
    await seed(new Date(Date.now() + 60_000));

    await assertFails(expirar(15));
  });

  it('rechaza vencer más puntos que el remanente del lote', async () => {
    await seed(new Date(Date.now() - 60_000), 10);

    await assertFails(expirar(15));
  });
});