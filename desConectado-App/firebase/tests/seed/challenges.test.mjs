import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { DIFICULTADES, formatearDuracion, validarDesafios } from '../../seed/validate.mjs';

const aqui = dirname(fileURLToPath(import.meta.url));
const catalogo = JSON.parse(readFileSync(resolve(aqui, '../../seed/catalog.json'), 'utf8'));
const desafios = catalogo.challenges;

const CATEGORIAS = ['move', 'focus', 'social', 'rest'];
const puntosEsperados = ({ durationMinutes, difficulty }) => {
  const multiplicador = { easy: 1, normal: 1.25, hard: 1.5 }[difficulty];
  return Math.floor((10 * durationMinutes / 30 * multiplicador) / 5 + 0.5) * 5;
};

// El catálogo funcional incluye las 28 propuestas, siete por categoría.
describe('catálogo de desafíos sembrado (catalog.json)', () => {
  it('cumple todas las invariantes del validador', () => {
    assert.deepEqual(validarDesafios(desafios), []);
  });

  it('tiene exactamente 28 desafíos, 7 por cada categoría', () => {
    assert.equal(desafios.length, 28);
    for (const categoria of CATEGORIAS) {
      const cantidad = desafios.filter((d) => d.category === categoria).length;
      assert.equal(cantidad, 7, `${categoria} tiene ${cantidad} desafíos`);
    }
  });

  it('calcula puntos por duración y dificultad con redondeo al múltiplo de 5, empate arriba', () => {
    for (const desafio of desafios) {
      assert.equal(desafio.points, puntosEsperados(desafio), `${desafio.id}: points`);
    }
  });

  it('cada título es el nombre de una actividad: no vacío, único y sin el formato anterior "No uses redes…"', () => {
    const titulos = desafios.map((d) => d.title);
    for (const titulo of titulos) {
      assert.ok(typeof titulo === 'string' && titulo.trim().length > 0);
      assert.doesNotMatch(titulo, /^No uses redes/i);
    }
    assert.equal(new Set(titulos).size, titulos.length, 'hay títulos repetidos');
  });

  it('cada desafío tiene categoría, descripción, duración, dificultad y puntos válidos', () => {
    for (const d of desafios) {
      assert.ok(CATEGORIAS.includes(d.category), `${d.id}: category`);
      assert.ok(typeof d.description === 'string' && d.description.trim().length > 0, `${d.id}: descripción`);
      assert.ok(Number.isInteger(d.durationMinutes) && d.durationMinutes > 0, `${d.id}: durationMinutes`);
      assert.ok(Number.isInteger(d.points) && d.points > 0, `${d.id}: points`);
      assert.ok(DIFICULTADES.includes(d.difficulty), `${d.id}: difficulty`);
      assert.equal(typeof d.active, 'boolean', `${d.id}: active`);
    }
  });

  it('preserva los IDs de las tres propuestas existentes exactas', () => {
    const porId = new Map(desafios.map((d) => [d.id, d]));
    assert.equal(porId.get('facil-30-minutos')?.title, 'Salir a caminar');
    assert.equal(porId.get('facil-1-hora')?.title, 'Andar en bici');
    assert.equal(porId.get('normal-4-horas')?.title, 'Juntarse con amigos');
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
// Catálogo válido de 6 desafíos; si se pasan más o menos títulos, cambia la cantidad (los extra son 'hard').
const catalogoDe = (titulos) => titulos.map((title, i) => ({
  id: `d${i}`, title, description: 'Descripción de prueba.', durationMinutes: 30,
  difficulty: 'easy', points: 10, category: CATEGORIAS[i % CATEGORIAS.length], active: true, order: i + 1,
}));

describe('validarDesafios detecta datos inválidos', () => {
  const base = (extra = {}) => ({
    id: 'x',
    title: 'Salir a caminar',
    description: 'Media hora sin redes.',
    durationMinutes: 30,
    difficulty: 'easy',
    points: 10,
    order: 1,
    ...extra,
  });

  it('rechaza una distribución que no tenga 7 desafíos en una categoría', () => {
    const datos = catalogoDe(Array.from({ length: 28 }, (_, i) => `D${i}`));
    datos[27].category = 'move';
    assert.ok(validarDesafios(datos).some((e) => e.includes('categoría')));
  });

  it('rechaza campos inválidos', () => {
    assert.ok(validarDesafios([base({ durationMinutes: 0 })]).length > 0);
    assert.ok(validarDesafios([base({ points: -5 })]).length > 0);
    assert.ok(validarDesafios([base({ difficulty: 'imposible' })]).length > 0);
    assert.ok(validarDesafios([base({ description: '  ' })]).length > 0);
    assert.ok(validarDesafios([base({ title: '  ' })]).length > 0);
  });

  it('rechaza el formato anterior de título y títulos repetidos', () => {
    assert.ok(validarDesafios([base({ title: 'No uses redes sociales por 30 minutos' })]).some((e) => e.includes('title')));
    assert.ok(validarDesafios(catalogoDe(['Salir a caminar', 'Salir a caminar'])).some((e) => e.includes('repetido')));
  });

  it('rechaza cantidad o distribución por categoría incorrectas', () => {
    assert.ok(validarDesafios(catalogoDe(Array.from({ length: 27 }, (_, i) => `D${i}`))).some((e) => e.includes('28')));
    const malDistribuido = catalogoDe(Array.from({ length: 28 }, (_, i) => `D${i}`));
    malDistribuido[27].category = 'move';
    assert.ok(validarDesafios(malDistribuido).some((e) => e.includes('categoría')));
  });

  it('rechaza puntos que no coinciden con la fórmula acordada', () => {
    const datos = catalogoDe(Array.from({ length: 28 }, (_, i) => `D${i}`));
    datos[0].points = 15;
    assert.ok(validarDesafios(datos).some((e) => e.includes('points')));
  });
});
