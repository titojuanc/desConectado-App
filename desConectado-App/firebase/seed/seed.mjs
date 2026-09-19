// Siembra los catálogos (desafíos y recompensas) en Firestore. Es idempotente: cada ejecución deja
// las colecciones `challenges` y `rewards` exactamente como en catalog.json.
//
// Uso (desde la carpeta firebase/):
//   node seed/seed.mjs --dry-run              valida y muestra qué escribiría, sin conectarse
//   node seed/seed.mjs --emulator             escribe en el emulador de Firestore (127.0.0.1:8080)
//   node seed/seed.mjs --project <id>         escribe en el proyecto real de Firebase; requiere la
//                                             variable GOOGLE_APPLICATION_CREDENTIALS con la ruta a
//                                             una clave de cuenta de servicio (no se versiona)
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { validarCatalogo } from './validate.mjs';

const aqui = dirname(fileURLToPath(import.meta.url));
const PROYECTO_EMULADOR = 'demo-desconectado';

function leerArgumentos(argv) {
  const opciones = { dryRun: false, emulador: false, proyecto: null };
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === '--dry-run') opciones.dryRun = true;
    else if (argv[i] === '--emulator') opciones.emulador = true;
    else if (argv[i] === '--project') opciones.proyecto = argv[++i];
    else fallar(`Argumento desconocido: ${argv[i]}`);
  }
  const modos = [opciones.dryRun, opciones.emulador, Boolean(opciones.proyecto)].filter(Boolean).length;
  if (modos !== 1) fallar('Indicá exactamente uno: --dry-run, --emulator o --project <id>');
  return opciones;
}

function fallar(mensaje) {
  console.error(`Error: ${mensaje}`);
  process.exit(1);
}

/** Convierte una lista con `id` en pares [id, datos] donde los datos no incluyen el `id`. */
function comoDocumentos(lista) {
  return lista.map(({ id, ...datos }) => [id, datos]);
}

async function sincronizarColeccion(db, nombre, documentos) {
  const coleccion = db.collection(nombre);
  const existentes = await coleccion.listDocuments();
  const idsNuevos = new Set(documentos.map(([id]) => id));

  const lote = db.batch();
  for (const [id, datos] of documentos) lote.set(coleccion.doc(id), datos);
  const sobrantes = existentes.filter((ref) => !idsNuevos.has(ref.id));
  for (const ref of sobrantes) lote.delete(ref);
  await lote.commit();

  console.log(`  ${nombre}: ${documentos.length} escritos, ${sobrantes.length} eliminados`);
}

async function main() {
  const opciones = leerArgumentos(process.argv.slice(2));
  const catalogo = JSON.parse(readFileSync(resolve(aqui, 'catalog.json'), 'utf8'));

  const errores = validarCatalogo(catalogo);
  if (errores.length > 0) {
    console.error('El catálogo no es válido; no se escribió nada:');
    for (const error of errores) console.error(`  - ${error}`);
    process.exit(1);
  }

  const desafios = comoDocumentos(catalogo.challenges);
  const recompensas = comoDocumentos(catalogo.rewards);

  if (opciones.dryRun) {
    console.log(`Catálogo válido: ${desafios.length} desafíos y ${recompensas.length} recompensas (sin escribir).`);
    return;
  }

  let proyecto;
  if (opciones.emulador) {
    process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
    proyecto = PROYECTO_EMULADOR;
    initializeApp({ projectId: proyecto });
  } else {
    if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      fallar('Falta GOOGLE_APPLICATION_CREDENTIALS con la ruta a la clave de la cuenta de servicio.');
    }
    proyecto = opciones.proyecto;
    initializeApp({ credential: applicationDefault(), projectId: proyecto });
  }

  console.log(`Sembrando el proyecto ${proyecto}${opciones.emulador ? ' (emulador)' : ''}:`);
  const db = getFirestore();
  await sincronizarColeccion(db, 'challenges', desafios);
  await sincronizarColeccion(db, 'rewards', recompensas);
  console.log('Listo.');
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
