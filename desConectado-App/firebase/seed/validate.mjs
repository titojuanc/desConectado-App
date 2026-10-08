// Validación de las invariantes de los catálogos (data-model.md).
// La usan el script de siembra (seed.mjs) y las pruebas de tests/seed.

/** Dificultades de menor a mayor. Los valores son los que se guardan en Firestore. */
export const DIFICULTADES = ['easy', 'normal', 'hard'];
export const CATEGORIAS_DESAFIO = ['move', 'focus', 'social', 'rest'];

/** Tipos de recompensa admitidos. */
export const TIPOS_RECOMPENSA = ['badge', 'theme', 'coupon'];

/** Palabras que sugieren un beneficio fuera de la app (FR-019); no deben aparecer en los cupones. */
export const REFERENCIAS_EXTERNAS = /\b(comercios?|locales?|tiendas? f[ií]sicas?|descuentos? reales?)\b/i;

const CANTIDAD_DESAFIOS = 28;
const DESAFIOS_POR_CATEGORIA = 7;
const MIN_RECOMPENSAS = 5;
// Formato anterior del título; el título ahora es el nombre de una actividad (FR-013).
const TITULO_FORMATO_ANTERIOR = /^No uses redes/i;
const MULTIPLICADOR_DIFICULTAD = { easy: 1, normal: 1.25, hard: 1.5 };

const esTextoNoVacio = (valor) => typeof valor === 'string' && valor.trim().length > 0;
const esEnteroPositivo = (valor) => Number.isInteger(valor) && valor > 0;

export function puntosEsperadosDesafio({ durationMinutes, difficulty }) {
  const rawPoints = 10 * durationMinutes / 30 * MULTIPLICADOR_DIFICULTAD[difficulty];
  return Math.floor(rawPoints / 5 + 0.5) * 5;
}

/** Duración legible, igual que en la app: "30 minutos", "1 hora", "2 horas", "1 hora 30 minutos". */
export function formatearDuracion(minutos) {
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  const textoHoras = horas === 1 ? '1 hora' : `${horas} horas`;
  if (horas === 0) return `${resto} minutos`;
  if (resto === 0) return textoHoras;
  return `${textoHoras} ${resto} minutos`;
}

/** Devuelve la lista de errores de los desafíos (vacía si son válidos). */
export function validarDesafios(desafios) {
  const errores = [];
  if (!Array.isArray(desafios)) return ['challenges debe ser una lista'];

  if (desafios.length !== CANTIDAD_DESAFIOS) {
    errores.push(`hay ${desafios.length} desafíos y debe haber exactamente ${CANTIDAD_DESAFIOS}`);
  }

  const ids = new Set();
  const titulos = new Set();
  for (const d of desafios) {
    const nombre = d?.id ?? '(sin id)';
    if (!esTextoNoVacio(d?.id)) errores.push(`${nombre}: id vacío`);
    if (ids.has(d?.id)) errores.push(`${nombre}: id repetido`);
    ids.add(d?.id);

    if (!esTextoNoVacio(d?.title)) {
      errores.push(`${nombre}: title vacío`);
    } else {
      if (TITULO_FORMATO_ANTERIOR.test(d.title)) {
        errores.push(`${nombre}: title debe ser el nombre de una actividad, no "No uses redes…"`);
      }
      if (titulos.has(d.title.trim())) errores.push(`${nombre}: title repetido`);
      titulos.add(d.title.trim());
    }
    if (!esTextoNoVacio(d?.description)) errores.push(`${nombre}: description vacía`);
    if (!esEnteroPositivo(d?.durationMinutes)) errores.push(`${nombre}: durationMinutes debe ser un entero mayor que 0`);
    if (!esEnteroPositivo(d?.points)) errores.push(`${nombre}: points debe ser un entero mayor que 0`);
    if (!DIFICULTADES.includes(d?.difficulty)) errores.push(`${nombre}: difficulty debe ser easy, normal o hard`);
    if (esEnteroPositivo(d?.durationMinutes) && DIFICULTADES.includes(d?.difficulty)
      && d?.points !== puntosEsperadosDesafio(d)) {
      errores.push(`${nombre}: points debe ser ${puntosEsperadosDesafio(d)} según duración y dificultad`);
    }
    if (!CATEGORIAS_DESAFIO.includes(d?.category)) errores.push(`${nombre}: category debe ser move, focus, social o rest`);
    if (typeof d?.active !== 'boolean') errores.push(`${nombre}: active debe ser boolean`);
    if (!Number.isInteger(d?.order) || d.order < 1) errores.push(`${nombre}: order debe ser un entero positivo`);
  }

  for (const categoria of CATEGORIAS_DESAFIO) {
    const cantidad = desafios.filter((d) => d?.category === categoria).length;
    if (cantidad !== DESAFIOS_POR_CATEGORIA) {
      errores.push(`categoría ${categoria}: hay ${cantidad} desafíos y debe haber exactamente ${DESAFIOS_POR_CATEGORIA}`);
    }
  }
  return errores;
}

/** Devuelve la lista de errores de las recompensas (vacía si son válidas). */
export function validarRecompensas(recompensas) {
  const errores = [];
  if (!Array.isArray(recompensas)) return ['rewards debe ser una lista'];
  if (recompensas.length < MIN_RECOMPENSAS) {
    errores.push(`hay ${recompensas.length} recompensas y se necesitan al menos ${MIN_RECOMPENSAS}`);
  }

  const ids = new Set();
  for (const r of recompensas) {
    const nombre = r?.id ?? '(sin id)';
    if (!esTextoNoVacio(r?.id)) errores.push(`${nombre}: id vacío`);
    if (ids.has(r?.id)) errores.push(`${nombre}: id repetido`);
    ids.add(r?.id);

    if (!esTextoNoVacio(r?.name)) errores.push(`${nombre}: name vacío`);
    if (!esTextoNoVacio(r?.description)) errores.push(`${nombre}: description vacía`);
    if (!esEnteroPositivo(r?.costPoints)) errores.push(`${nombre}: costPoints debe ser un entero mayor que 0`);
    if (!TIPOS_RECOMPENSA.includes(r?.kind)) errores.push(`${nombre}: kind debe ser badge, theme o coupon`);
    if (!Number.isInteger(r?.order)) errores.push(`${nombre}: order debe ser un entero`);

    if (r?.kind === 'coupon' && REFERENCIAS_EXTERNAS.test(`${r.name} ${r.description}`)) {
      errores.push(`${nombre}: un cupón no puede mencionar comercios, locales ni descuentos reales`);
    }
  }

  // El listado va por costo ascendente: ordenar por `order` no debe alterar el orden de costos.
  const porOrden = [...recompensas].filter((r) => Number.isInteger(r?.order)).sort((a, b) => a.order - b.order);
  for (let i = 1; i < porOrden.length; i++) {
    if (porOrden[i].costPoints < porOrden[i - 1].costPoints) {
      errores.push(`${porOrden[i].id}: el orden no va por costo ascendente`);
    }
  }
  return errores;
}

/** Valida el catálogo completo (desafíos y recompensas). */
export function validarCatalogo(catalogo) {
  return [...validarDesafios(catalogo?.challenges), ...validarRecompensas(catalogo?.rewards)];
}
