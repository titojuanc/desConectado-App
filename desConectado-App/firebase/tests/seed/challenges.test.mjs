import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { DIFICULTADES, formatearDuracion, validarDesafios } from '../../seed/validate.mjs';

const aqui = dirname(fileURLToPath(import.meta.url));
const catalogo = JSON.parse(readFileSync(resolve(aqui, '../../seed/catalog.json'), 'utf8'));
const desafios = catalogo.challenges;

// FR-013 a FR-016: catálogo de desafíos "No uses redes sociales por X tiempo".
describe('catálogo de desafíos sembrado (catalog.json)', () => {
  it('cumple todas las invariantes del validador', () => {
    assert.deepEqual(validarDesafios(desafios), []);
  });

  it('tiene al menos 2 desafíos por cada dificultad', () => {
    for (const dificultad of DIFICULTADES) {
      const cantidad = desafios.filter((d) => d.difficulty === dificultad).length;
      assert.ok(cantidad >= 2, `${dificultad} tiene ${cantidad} desafíos`);
    }
  });

  it('un desafío de mayor dificultad dura más y otorga más puntos que cualquiera de menor dificultad', () => {
    for (let i = 0; i < DIFICULTADES.length - 1; i++) {
      const menores = desafios.filter((d) => d.difficulty === DIFICULTADES[i]);
      const mayores = desafios.filter((d) => d.difficulty === DIFICULTADES[i + 1]);
      const maxDuracion = Math.max(...menores.map((d) => d.durationMinutes));
      const maxPuntos = Math.max(...menores.map((d) => d.points));
      for (const d of mayores) {
        assert.ok(d.durationMinutes > maxDuracion, `${d.id}: duración no supera a ${DIFICULTADES[i]}`);
        assert.ok(d.points > maxPuntos, `${d.id}: puntos no superan a ${DIFICULTADES[i]}`);
      }
    }
  });

  it('todos los títulos empiezan con "No uses redes sociales por" y coinciden con la duración', () => {
    for (const d of desafios) {
      assert.match(d.title, /^No uses redes sociales por /);
      assert.equal(d.title, `No uses redes sociales por ${formatearDuracion(d.durationMinutes)}`);
    }
  });

  it('cada desafío tiene descripción no vacía, duración y puntos enteros mayores que 0', () => {
    for (const d of desafios) {
      assert.ok(typeof d.description === 'string' && d.description.trim().length > 0, `${d.id}: descripción`);
      assert.ok(Number.isInteger(d.durationMinutes) && d.durationMinutes > 0, `${d.id}: durationMinutes`);
      assert.ok(Number.isInteger(d.points) && d.points > 0, `${d.id}: points`);
      assert.ok(DIFICULTADES.includes(d.difficulty), `${d.id}: difficulty`);
    }
  });

  it('contiene los 6 valores iniciales del spec', () => {
    const resumen = [...desafios]
      .sort((a, b) => a.order - b.order)
      .map((d) => [d.durationMinutes, d.difficulty, d.points]);
    assert.deepEqual(resumen, [
      [30, 'easy', 10],
      [60, 'easy', 20],
      [120, 'normal', 50],
      [240, 'normal', 100],
      [480, 'hard', 200],
      [720, 'hard', 320],
    ]);
  });
});

describe('formatearDuracion', () => {
  it('formatea minutos y horas como en la app', () => {
    assert.equal(formatearDuracion(30), '30 minutos');
    assert.equal(formatearDuracion(60), '1 hora');
    assert.equal(formatearDuracion(120), '2 horas');
    assert.equal(formatearDuracion(90), '1 hora 30 minutos');
  });
});

// El validador debe detectar de verdad los datos inválidos (si no, las pruebas de arriba no valdrían).
describe('validarDesafios detecta datos inválidos', () => {
  const base = (extra = {}) => ({
    id: 'x',
    title: 'No uses redes sociales por 30 minutos',
    description: 'Algo',
    durationMinutes: 30,
    difficulty: 'easy',
    points: 10,
    order: 1,
    ...extra,
  });

  it('rechaza un catálogo con menos de 2 desafíos por dificultad', () => {
    assert.ok(validarDesafios([base()]).length > 0);
  });

  it('rechaza una dificultad mayor que no dura más o no da más puntos', () => {
    const datos = [
      base({ id: 'a', order: 1 }),
      base({ id: 'b', order: 2, durationMinutes: 60, title: 'No uses redes sociales por 1 hora', points: 20 }),
      base({ id: 'c', order: 3, difficulty: 'normal', durationMinutes: 45, title: 'No uses redes sociales por 45 minutos', points: 50 }),
      base({ id: 'd', order: 4, difficulty: 'normal', durationMinutes: 120, title: 'No uses redes sociales por 2 horas', points: 60 }),
      base({ id: 'e', order: 5, difficulty: 'hard', durationMinutes: 480, title: 'No uses redes sociales por 8 horas', points: 200 }),
      base({ id: 'f', order: 6, difficulty: 'hard', durationMinutes: 720, title: 'No uses redes sociales por 12 horas', points: 320 }),
    ];
    assert.ok(validarDesafios(datos).some((e) => e.includes('normal')));
  });

  it('rechaza campos inválidos', () => {
    assert.ok(validarDesafios([base({ durationMinutes: 0 })]).length > 0);
    assert.ok(validarDesafios([base({ points: -5 })]).length > 0);
    assert.ok(validarDesafios([base({ difficulty: 'imposible' })]).length > 0);
    assert.ok(validarDesafios([base({ description: '  ' })]).length > 0);
    assert.ok(validarDesafios([base({ title: 'Otro título' })]).length > 0);
    assert.ok(validarDesafios([base({ title: 'No uses redes sociales por 5 minutos' })]).length > 0);
  });
});
