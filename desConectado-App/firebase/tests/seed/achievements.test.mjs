import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validarLogros } from '../../seed/validate.mjs';

const aqui = dirname(fileURLToPath(import.meta.url));
const logros = JSON.parse(readFileSync(resolve(aqui, '../../seed/achievements.json'), 'utf8'));

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