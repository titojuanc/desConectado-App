import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { buildUserResetPatch, RESET_COLLECTIONS, parseResetOptions } from '../../maintenance/reset-delivery-3.mjs';

describe('reset de entrega 3: guardias de seguridad', () => {
  it('requiere un project id explícito y usa dry-run por defecto', () => {
    assert.throws(() => parseResetOptions([]), /--project/);
    assert.deepEqual(parseResetOptions(['--project', 'demo-desconectado']), {
      projectId: 'demo-desconectado',
      dryRun: true,
      execute: false,
      confirmation: null,
      chunkSize: 400,
    });
  });

  it('requiere confirmación inequívoca y coincidente para borrar', () => {
    assert.throws(() => parseResetOptions(['--project', 'des-conectado', '--execute']), /confirm/);
    assert.throws(() => parseResetOptions([
      '--project', 'des-conectado', '--execute', '--confirm-reset-delivery-3', 'otro-proyecto',
    ]), /coincidir/);
    assert.deepEqual(parseResetOptions([
      '--project', 'des-conectado', '--execute', '--confirm-reset-delivery-3', 'des-conectado',
    ]), {
      projectId: 'des-conectado',
      dryRun: false,
      execute: true,
      confirmation: 'des-conectado',
      chunkSize: 400,
    });
  });

  it('limita chunks y rechaza argumentos desconocidos o inseguros', () => {
    assert.equal(parseResetOptions(['--project', 'demo', '--chunk-size', '200']).chunkSize, 200);
    for (const size of ['0', '451', 'abc']) {
      assert.throws(() => parseResetOptions(['--project', 'demo', '--chunk-size', size]));
    }
    assert.throws(() => parseResetOptions(['--project', 'demo', '--force']));
  });

  it('solo borra colecciones de progreso/economía y preserva perfiles, preferencias y Auth', () => {
    assert.deepEqual(RESET_COLLECTIONS, [
      'movements', 'pointLots', 'pointLotSessions', 'redeemedRewards', 'pendingRedemptions',
      'challengeResults', 'challengeRatings', 'activeChallenge', 'achievements', 'cosmeticOwnership',
    ]);
    assert.equal(RESET_COLLECTIONS.includes('preferences'), false);
    assert.equal(RESET_COLLECTIONS.includes('users'), false);
  });

  it('resetea saldo/último movimiento y agrega marcador sin reemplazar el perfil', () => {
    const patch = buildUserResetPatch({
      deleteField: () => 'DELETE_FIELD',
      serverTimestamp: () => 'SERVER_TIMESTAMP',
    });
    assert.deepEqual(patch, {
      pointsBalance: 0,
      lastMovementId: 'DELETE_FIELD',
      delivery3ResetAt: 'SERVER_TIMESTAMP',
    });
    assert.equal('username' in patch, false);
    assert.equal('email' in patch, false);
  });
});