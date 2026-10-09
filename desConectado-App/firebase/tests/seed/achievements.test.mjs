import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validarLogros } from '../../seed/validate.mjs';
import { catalogoPrueba, opcionesPrueba } from '../../seed/seed-debug.mjs';

const aqui = dirname(fileURLToPath(import.meta.url));
const logros = JSON.parse(readFileSync(resolve(aqui, '../../seed/achievements.json'), 'utf8'));

describe('datos inmediatos de testeo', () => {
  it('mantiene IDs aislados y desafios activos de diez segundos y un punto', () => {
    for (const records of Object.values(catalogoPrueba)) {
      assert.ok(records.every((record) => record.id.startsWith('debug-')));
      assert.equal(new Set(records.map((record) => record.id)).size, records.length);
    }
    assert.equal(catalogoPrueba.challenges.length, 4);
    assert.ok(catalogoPrueba.challenges.every((challenge) => challenge.active && challenge.durationSeconds === 10 && challenge.points === 1));
    assert.ok(catalogoPrueba.rewards.every((reward) => reward.active && reward.costPoints === 1));
  });
  it('define logros validos alcanzables con un resultado completado de diez segundos', () => {
    assert.deepEqual(validarLogros([...catalogoPrueba.achievements, ...logros.slice(0, 7)]), []);
    assert.ok(catalogoPrueba.achievements.every((achievement) => achievement.threshold <= (achievement.criterion === 'completed_challenges' ? 1 : 10)));
  });
  it('exige un modo explicito y rechaza opciones ambiguas', () => {
    assert.throws(() => opcionesPrueba([]));
    assert.throws(() => opcionesPrueba(['--project']));
    assert.throws(() => opcionesPrueba(['--dry-run', '--project', 'des-conectado']));
    assert.throws(() => opcionesPrueba(['--reset']));
    assert.equal(opcionesPrueba(['--dry-run']).dryRun, true);
    assert.equal(opcionesPrueba(['--project', 'des-conectado']).projectId, 'des-conectado');
  });
});

describe('catálogo de logros (achievements.json)', () => {
  it('cumple todas las invariantes del validador', () => {
    assert.deepEqual(validarLogros(logros), []);
  });

  it('incluye los diez logros de FR-009 con umbrales exactos', () => {
    assert.equal(logros.length, 10);
    const porId = new Map(logros.map((logro) => [logro.id, logro]));
    assert.equal(porId.get('primer-paso')?.threshold, 1);
    assert.equal(porId.get('en-marcha')?.threshold, 5);
    assert.equal(porId.get('modo-presente')?.threshold, 18_000);
    assert.equal(porId.get('sin-apuro')?.threshold, 10_800);
    assert.equal(porId.get('aire-libre')?.category, 'move');
    assert.equal(porId.get('foco-total')?.category, 'focus');
    assert.equal(porId.get('mas-cerca')?.category, 'social');
    assert.equal(porId.get('tiempo-para-mi')?.category, 'rest');
    assert.equal(porId.get('explorador')?.threshold, 4);
    assert.equal(porId.get('100-horas-presentes')?.threshold, 360_000);
  });
});

describe('validarLogros detecta datos inválidos', () => {
  it('rechaza duplicados, criterios inválidos y categorías ausentes o inesperadas', () => {
    const valido = {
      id: 'logro', name: 'Logro', description: 'Descripción',
      criterion: 'completed_challenges', threshold: 1, order: 1, active: true,
    };
    assert.ok(validarLogros([valido, { ...valido }]).some((error) => error.includes('repetido')));
    assert.ok(validarLogros([{ ...valido, criterion: 'desconocido' }]).length > 0);
    assert.ok(validarLogros([{ ...valido, criterion: 'category_completions' }]).length > 0);
    assert.ok(validarLogros([{ ...valido, category: 'social' }]).length > 0);
  });
});