import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, serverTimestamp, setDoc, writeBatch, deleteDoc, updateDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno } from './helpers.mjs';

describe('reglas de lotes diarios de puntos', () => {
  let entorno;
  before(async () => { entorno = await crearEntorno(); });
  after(async () => { await entorno.cleanup(); });
  afterEach(async () => { await entorno.clearFirestore(); });

  const UID = 'ana';
  const LOT_ID = 'daily-2026-10-05-QW1lcmljYS9BcmdlbnRpbmEvQnVlbm9zX0FpcmVz';
  const ana = () => comoPersona(entorno, UID, 'ana@mail.com');

  async function seed({ pointsAwarded = 10, pointsMultiplier = 1, streakDays = 0, weeklyGoalMinutes = 0 } = {}) {
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
        status: 'COMPLETED', pointsAwarded, pointsMultiplier, streakDays,
        weeklyGoalMinutesUsed: weeklyGoalMinutes, durationSeconds: 60,
        startedAt: new Date(now - 120_000), finishedAt: new Date(now - 1_000),
        measuredSocialSeconds: 0, offlineSeconds: 0,
      });
      if (weeklyGoalMinutes > 0) {
        await setDoc(doc(db, `users/${UID}/preferences/current`), { weeklyGoalMinutes });
      }
    });
  }

  function creditoCompleto(importe = 10, db = ana()) {
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
      issuedPoints: importe,
      remainingPoints: importe,
    });
    batch.set(session, {
      lotId: LOT_ID,
      windowEndsAt: new Date('2026-10-06T03:00:00Z'),
    });
    batch.set(movement, {
      type: 'credit', amount: importe, challengeId: 'test', challengeTitle: 'Prueba',
      sourceId: 'run-1', lotId: LOT_ID, earnedAt: new Date(Date.now() - 1_000),
      createdAt: serverTimestamp(),
    });
    batch.update(user, { pointsBalance: importe, lastMovementId: movement.id });
    return batch;
  }

  it('acepta crédito atómico con lote y sesión diaria', async () => {
    await seed();

    await assertSucceeds(creditoCompleto().commit());
  });

  it('rechaza multiplicar un crédito aislado sin acumulado y bonus diario', async () => {
    await seed({ pointsAwarded: 11, pointsMultiplier: 1.07, streakDays: 7, weeklyGoalMinutes: 210 });

    await assertFails(creditoCompleto(11).commit());
  });

  it('rechaza un importe que supera el redondeo calculado por la racha', async () => {
    await seed({ pointsAwarded: 12, pointsMultiplier: 1.07, streakDays: 7, weeklyGoalMinutes: 210 });

    await assertFails(creditoCompleto(12).commit());
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

  async function prepararDia({ bonusAwarded = false } = {}) {
    await seed({ weeklyGoalMinutes: 210 });
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await updateDoc(doc(db, `users/${UID}/challengeResults/run-1`), {
        durationSeconds: 900, startedAt: new Date(Date.now() - 3_600_000),
      });
      await setDoc(doc(db, `users/${UID}/dailyStreakProgress/2026-10-05`), {
        basePoints: 100, completedSeconds: 900, bonusAwarded,
        lastRunId: 'previous', updatedAt: new Date(),
      });
    });
  }

  function creditoDiario({ bonus = 8, seconds = 1800, dayPoints = 110, claimed = true, days = 7 } = {}) {
    const db = ana();
    const batch = creditoCompleto(10 + bonus, db);
    batch.update(doc(db, `users/${UID}/challengeResults/run-1`), {
      dailyKey: '2026-10-05',
      dailyBasePoints: bonus ? dayPoints : 0,
      streakBonusPoints: bonus,
      pointsAwarded: 10 + bonus,
      pointsMultiplier: bonus ? 1 + days * 210 / 21000 : 1,
      streakDays: bonus ? days : 0,
      weeklyGoalMinutesUsed: bonus ? 210 : 0,
    });
    batch.set(doc(db, `users/${UID}/dailyStreakProgress/2026-10-05`), {
      basePoints: dayPoints, completedSeconds: seconds, bonusAwarded: claimed,
      lastRunId: 'run-1', updatedAt: serverTimestamp(),
    });
    return batch;
  }

  it('inicializa el acumulado del día sin bonus y no permite repetir el crédito', async () => {
    await seed();
    await assertSucceeds(creditoDiario({ bonus: 0, seconds: 60, dayPoints: 10, claimed: false }).commit());
    await assertFails(creditoDiario({ bonus: 0, seconds: 60, dayPoints: 10, claimed: false }).commit());
  });

  it('acredita de una vez el bonus sobre todos los puntos base del día y protege el resultado final', async () => {
    await prepararDia();
    await assertSucceeds(creditoDiario().commit());
    await assertFails(updateDoc(doc(ana(), `users/${UID}/challengeResults/run-1`), { pointsAwarded: 999 }));
  });

  it('rechaza bonus antes de alcanzar la meta diaria', async () => {
    await prepararDia();
    await entorno.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), `users/${UID}/challengeResults/run-1`), { durationSeconds: 899 });
    });
    await assertFails(creditoDiario({ seconds: 1799 }).commit());
  });

  it('rechaza un segundo bonus del mismo día y permite acreditar puntos base posteriores', async () => {
    await prepararDia({ bonusAwarded: true });
    await assertFails(creditoDiario().commit());
    await assertSucceeds(creditoDiario({ bonus: 0 }).commit());
  });

  it('rechaza inflar los puntos acumulados o el redondeo del bonus', async () => {
    await prepararDia();
    await assertFails(creditoDiario({ dayPoints: 1000, bonus: 70 }).commit());
    await assertFails(creditoDiario({ bonus: 9 }).commit());
  });

  it('no permite borrar progreso diario de desafíos reales', async () => {
    await prepararDia();
    await assertFails(deleteDoc(doc(ana(), `users/${UID}/dailyStreakProgress/2026-10-05`)));
  });

  it('cualquier dueño puede reiniciar sus pruebas sin claim, nunca datos reales ni de otra cuenta', async () => {
    await prepararDia();
    const qa = ana();
    await assertFails(deleteDoc(doc(qa, `users/${UID}/challengeResults/run-1`)));
    await assertFails(deleteDoc(doc(qa, `users/${UID}/dailyStreakProgress/2026-10-05`)));
    await entorno.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, `users/${UID}/challengeResults/debug-streak-test`), { status: 'COMPLETED' });
      await updateDoc(doc(db, `users/${UID}/dailyStreakProgress/2026-10-05`), { lastRunId: 'debug-streak-test' });
    });
    const otraCuenta = entorno.authenticatedContext('otra-cuenta').firestore();
    await assertFails(deleteDoc(doc(otraCuenta, `users/${UID}/challengeResults/debug-streak-test`)));
    await assertFails(deleteDoc(doc(otraCuenta, `users/${UID}/dailyStreakProgress/2026-10-05`)));
    await assertSucceeds(deleteDoc(doc(qa, `users/${UID}/challengeResults/debug-streak-test`)));
    await assertSucceeds(deleteDoc(doc(qa, `users/${UID}/dailyStreakProgress/2026-10-05`)));
  });
});