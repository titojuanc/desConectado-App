// Validación de las invariantes de los catálogos (data-model.md).
// La usan el script de siembra (seed.mjs) y las pruebas de tests/seed.

/** Dificultades de menor a mayor. Los valores son los que se guardan en Firestore. */
export const DIFICULTADES = ['easy', 'normal', 'hard'];

/** Tipos de recompensa admitidos. */
export const TIPOS_RECOMPENSA = ['badge', 'theme', 'coupon'];

/** Palabras que sugieren un beneficio fuera de la app (FR-019); no deben aparecer en los cupones. */
export const REFERENCIAS_EXTERNAS = /\b(comercios?|locales?|tiendas? f[ií]sicas?|descuentos? reales?)\b/i;

const MIN_DESAFIOS_POR_DIFICULTAD = 2;
const MIN_RECOMPENSAS = 5;
const PREFIJO_TITULO = 'No uses redes sociales por ';

const esTextoNoVacio = (valor) => typeof valor === 'string' && valor.trim().length > 0;
const esEnteroPositivo = (valor) => Number.isInteger(valor) && valor > 0;

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

  const ids = new Set();
  for (const d of desafios) {
    const nombre = d?.id ?? '(sin id)';
    if (!esTextoNoVacio(d?.id)) errores.push(`${nombre}: id vacío`);
    if (ids.has(d?.id)) errores.push(`${nombre}: id repetido`);
    ids.add(d?.id);

    if (!esTextoNoVacio(d?.title)) {
      errores.push(`${nombre}: title vacío`);
    } else if (!d.title.startsWith(PREFIJO_TITULO)) {
      errores.push(`${nombre}: title debe empezar con "${PREFIJO_TITULO}"`);
    } else if (esEnteroPositivo(d.durationMinutes) && d.title !== PREFIJO_TITULO + formatearDuracion(d.durationMinutes)) {
      errores.push(`${nombre}: title no coincide con durationMinutes (${d.durationMinutes})`);
    }
    if (!esTextoNoVacio(d?.description)) errores.push(`${nombre}: description vacía`);
    if (!esEnteroPositivo(d?.durationMinutes)) errores.push(`${nombre}: durationMinutes debe ser un entero mayor que 0`);
    if (!esEnteroPositivo(d?.points)) errores.push(`${nombre}: points debe ser un entero mayor que 0`);
    if (!DIFICULTADES.includes(d?.difficulty)) errores.push(`${nombre}: difficulty debe ser easy, normal o hard`);
    if (!Number.isInteger(d?.order)) errores.push(`${nombre}: order debe ser un entero`);
  }

  for (const dificultad of DIFICULTADES) {
    const cantidad = desafios.filter((d) => d?.difficulty === dificultad).length;
    if (cantidad < MIN_DESAFIOS_POR_DIFICULTAD) {
      errores.push(`${dificultad}: hay ${cantidad} desafíos y se necesitan al menos ${MIN_DESAFIOS_POR_DIFICULTAD}`);
    }
  }

  // Cualquier desafío de una dificultad mayor dura más y otorga más puntos que cualquiera de una menor.
  for (let i = 0; i < DIFICULTADES.length - 1; i++) {
    const menores = desafios.filter((d) => d?.difficulty === DIFICULTADES[i]);
    const mayores = desafios.filter((d) => d?.difficulty === DIFICULTADES[i + 1]);
    if (menores.length === 0 || mayores.length === 0) continue;
    const maxDuracion = Math.max(...menores.map((d) => d.durationMinutes));
    const maxPuntos = Math.max(...menores.map((d) => d.points));
    for (const d of mayores) {
      if (!(d.durationMinutes > maxDuracion)) {
        errores.push(`${DIFICULTADES[i + 1]}: ${d.id} no dura más que los desafíos ${DIFICULTADES[i]}`);
      }
      if (!(d.points > maxPuntos)) {
        errores.push(`${DIFICULTADES[i + 1]}: ${d.id} no otorga más puntos que los desafíos ${DIFICULTADES[i]}`);
      }
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
