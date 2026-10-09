import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { REFERENCIAS_EXTERNAS, TIPOS_RECOMPENSA, validarRecompensas } from '../../seed/validate.mjs';

const aqui = dirname(fileURLToPath(import.meta.url));
const catalogo = JSON.parse(readFileSync(resolve(aqui, '../../seed/catalog.json'), 'utf8'));
const recompensas = catalogo.rewards ?? [];

// FR-018 y FR-019: catálogo genérico de recompensas digitales, sin beneficios fuera de la app.
describe('catálogo de recompensas sembrado (catalog.json)', () => {
  it('cumple todas las invariantes del validador', () => {
    assert.deepEqual(validarRecompensas(recompensas), []);
  });

  it('tiene 15 cosméticos aprobados, conserva los dos cupones y no vende insignias', () => {
    assert.equal(recompensas.length, 17);
    assert.equal(recompensas.filter((r) => r.kind !== 'coupon').length, 15);
    assert.equal(recompensas.filter((r) => r.kind === 'coupon').length, 2);
    assert.equal(recompensas.some((r) => r.kind === 'badge'), false);
    assert.deepEqual(
      recompensas.filter((r) => r.kind === 'coupon').map((r) => r.id).sort(),
      ['cupon-descuento-de-ejemplo', 'cupon-regalo-sorpresa'],
    );
  });

  it('cada recompensa tiene nombre y descripción no vacíos, costo entero mayor que 0 y tipo válido', () => {
    for (const r of recompensas) {
      assert.ok(typeof r.name === 'string' && r.name.trim().length > 0, `${r.id}: name`);
      assert.ok(typeof r.description === 'string' && r.description.trim().length > 0, `${r.id}: description`);
      assert.ok(Number.isInteger(r.costPoints) && r.costPoints > 0, `${r.id}: costPoints`);
      assert.ok(TIPOS_RECOMPENSA.includes(r.kind), `${r.id}: kind`);
    }
  });

  it('cada cosmético trae configuración y costo/ID aprobado por el documento', () => {
    const costos = {
      'theme-bosque': 100,
      'theme-atardecer': 150,
      'theme-oceano': 150,
      'theme-noche': 200,
      'icon-pack-minimal': 80,
      'icon-pack-naturaleza': 100,
      'background-montanas': 60,
      'background-noche-estrellada': 80,
      'background-amanecer': 80,
      'profile-frame': 100,
      'profile-frame-naturaleza': 150,
      'point-icon-estrella-especial': 200,
      'completion-animation-logro': 120,
      'completion-sound-finalizacion': 60,
      'surprise-box': 120,
    };
    const porId = new Map(recompensas.map((r) => [r.id, r]));
    assert.deepEqual([...porId.keys()].filter((id) => id in costos).sort(), Object.keys(costos).sort());
    for (const [id, costo] of Object.entries(costos)) {
      const recompensa = porId.get(id);
      assert.equal(recompensa?.costPoints, costo, `${id}: costPoints`);
      assert.ok(recompensa?.config && Object.keys(recompensa.config).length > 0, `${id}: config`);
    }
  });

  it('las recompensas van ordenadas por costo ascendente', () => {
    const ordenadas = [...recompensas].sort((a, b) => a.order - b.order).map((r) => r.costPoints);
    assert.deepEqual(ordenadas, [...ordenadas].sort((a, b) => a - b));
  });

  it('ninguna recompensa de tipo coupon menciona comercios, locales ni descuentos reales', () => {
    for (const r of recompensas.filter((x) => x.kind === 'coupon')) {
      assert.doesNotMatch(`${r.name} ${r.description}`, REFERENCIAS_EXTERNAS, `${r.id}: menciona beneficios externos`);
    }
  });

  it('las recompensas aclaran que son digitales y de la app', () => {
    for (const r of recompensas) {
      assert.match(r.description, /app|\(des\)Conectado/i, `${r.id}: no aclara que es solo de la app`);
    }
  });

  it('preserva costos y nombres de los cupones existentes', () => {
    const porId = new Map(recompensas.map((r) => [r.id, r]));
    assert.equal(porId.get('cupon-descuento-de-ejemplo')?.costPoints, 200);
    assert.equal(porId.get('cupon-regalo-sorpresa')?.costPoints, 500);
  });
});

describe('validarRecompensas detecta datos inválidos', () => {
  const base = (extra = {}) => ({
    id: 'r',
    name: 'Tema',
    description: 'Un tema digital dentro de la app.',
    costPoints: 50,
    kind: 'theme',
    config: { palette: 'forest' },
    active: true,
    order: 1,
    ...extra,
  });
  const cinco = (cambios = {}) =>
    [1, 2, 3, 4, 5].map((n) => base({ id: `r${n}`, order: n, costPoints: n * 50, ...(cambios[n] ?? {}) }));

  it('acepta cinco recompensas válidas', () => {
    assert.deepEqual(validarRecompensas(cinco()), []);
  });

  it('rechaza menos de 5 recompensas', () => {
    assert.ok(validarRecompensas(cinco().slice(0, 4)).length > 0);
  });

  it('rechaza campos inválidos', () => {
    assert.ok(validarRecompensas(cinco({ 1: { costPoints: 0 } })).length > 0);
    assert.ok(validarRecompensas(cinco({ 2: { kind: 'otro' } })).length > 0);
    assert.ok(validarRecompensas(cinco({ 3: { name: '' } })).length > 0);
    assert.ok(validarRecompensas(cinco({ 4: { description: '' } })).length > 0);
  });

  it('rechaza un cupón que menciona comercios o descuentos reales', () => {
    const datos = cinco({ 3: { kind: 'coupon', description: 'Un descuento real en comercios de la zona.' } });
    assert.ok(validarRecompensas(datos).length > 0);
  });

  it('rechaza recompensas que no van por costo ascendente', () => {
    assert.ok(validarRecompensas(cinco({ 5: { costPoints: 10 } })).length > 0);
  });
});
