import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resumirDia } from '../../maintenance/migrate-daily-streak.mjs';

const now = new Date('2026-10-07T15:00:00Z');
const resultado = (id, changes = {}) => ({
  challengeRunId: id, challengeId: 'focus', status: 'COMPLETED',
  finishedAt: { toDate: () => new Date('2026-10-07T12:00:00Z') },
  timeZoneId: 'UTC', pointsAwarded: 10, durationSeconds: 60, ...changes,
});
const catalogo = new Map([['focus', 10]]);

test('reconstruye todos los puntos base ya acreditados sin incluir pendientes o fallidos', () => {
  const dias = resumirDia([
    resultado('uno'), resultado('dos'), resultado('pendiente'),
    resultado('fallido', { status: 'FAILED' }),
    resultado('ayer', { finishedAt: { toDate: () => new Date('2026-10-06T12:00:00Z') } }),
  ], new Set(['uno', 'dos', 'fallido', 'ayer']), catalogo, now);
  assert.deepEqual(dias.get('2026-10-07'), {
    basePoints: 20, completedSeconds: 120, bonusAwarded: false, lastRunId: 'dos',
  });
});

test('un multiplicador antiguo ya pagado consume el bonus del dia sin volver a pagar', () => {
  const dias = resumirDia([resultado('uno', { pointsAwarded: 11 })], new Set(['uno']), catalogo, now);
  assert.equal(dias.get('2026-10-07').basePoints, 10);
  assert.equal(dias.get('2026-10-07').bonusAwarded, true);
});

test('respeta la fecha local y rechaza catalogos incompletos', () => {
  const local = resultado('uno', {
    timeZoneId: 'America/Los_Angeles',
    finishedAt: { toDate: () => new Date('2026-10-07T04:00:00Z') },
  });
  assert.equal(resumirDia([local], new Set(['uno']), catalogo, now).size, 0);
  assert.throws(() => resumirDia([resultado('uno')], new Set(['uno']), new Map(), now), /invalidos/);
});
