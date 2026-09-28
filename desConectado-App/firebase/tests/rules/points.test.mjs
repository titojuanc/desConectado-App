import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  addDoc, collection, deleteDoc, doc, getDocs, limit, orderBy, query, serverTimestamp, setDoc, updateDoc,
  where, writeBatch,
} from 'firebase/firestore';
import { comoPersona, crearEntorno, sinSesion } from './helpers.mjs';

// Principio VI, entrega 24/09: los puntos y su registro son de solo lectura desde la app.
// Contrato: contracts/firestore-data.md, "Puntos y movimientos".
describe('reglas de users/{uid}/movements', () => {
  let entorno;

  before(async () => {
    entorno = await crearEntorno();
  });
  after(async () => {
    await entorno.cleanup();
  });
  afterEach(async () => {
    await entorno.clearFirestore();
  });

  const UID = 'ana';
  const OTRA = 'beto';
  const CORREO = 'ana@mail.com';
  const ana = () => comoPersona(entorno, UID, CORREO);
  const movimientos = (db, uid = UID) => collection(db, 'users', uid, 'movements');

  const credito = (extra = {}) => ({
    type: 'credit',
    amount: 10,
    challengeId: 'facil-30-minutos',
    challengeTitle: 'Salir a caminar',
    createdAt: new Date('2026-09-20T10:00:00Z'),
    ...extra,
  });

  /** Carga datos con credenciales de administrador (sin pasar por las reglas). */
  async function sembrar(uid = UID, cantidad = 3, saldo = 30) {
    await entorno.withSecurityRulesDisabled(async (contexto) => {
      const db = contexto.firestore();
      await setDoc(doc(db, 'users', uid), {
        username: 'Ana Prueba', email: CORREO, createdAt: new Date(), pointsBalance: saldo,
      });
      for (let i = 0; i < cantidad; i++) {
        await setDoc(doc(db, 'users', uid, 'movements', `m${i}`), credito({
          createdAt: new Date(Date.UTC(2026, 8, 20 + i)),
        }));
      }
      await setDoc(doc(db, 'users', uid, 'movements', 'canje'), {
        type: 'redeem', amount: 5, createdAt: new Date(Date.UTC(2026, 8, 30)),
      });
    });
  }

  describe('leer', () => {
    it('la dueña lee sus movimientos', async () => {
      await sembrar();
      await assertSucceeds(getDocs(movimientos(ana())));
    });

    it('la dueña ejecuta la consulta de los últimos desafíos hechos (credit, createdAt desc, límite 5)', async () => {
      await sembrar(UID, 6);
      const consulta = query(movimientos(ana()), where('type', '==', 'credit'), orderBy('createdAt', 'desc'), limit(5));
      const resultado = await assertSucceeds(getDocs(consulta));
      const ids = resultado.docs.map((d) => d.id);
      // Los credit más recientes primero y ningún canje.
      if (ids.length !== 5 || ids[0] !== 'm5' || ids.includes('canje')) {
        throw new Error(`orden o filtro incorrectos: ${ids.join(',')}`);
      }
    });

    it('otra persona autenticada no lee los movimientos ajenos', async () => {
      await sembrar();
      await assertFails(getDocs(movimientos(comoPersona(entorno, OTRA, 'beto@mail.com'))));
    });

    it('sin sesión no se leen movimientos', async () => {
      await sembrar();
      await assertFails(getDocs(movimientos(sinSesion(entorno))));
    });
  });

  describe('manipulaciones rechazadas (la app no escribe puntos)', () => {
    it('la dueña no puede crear un movimiento, ni uno con apariencia válida de acreditación', async () => {
      await sembrar(UID, 0, 0);
      await assertFails(addDoc(movimientos(ana()), { ...credito(), createdAt: serverTimestamp() }));
      await assertFails(setDoc(doc(ana(), 'users', UID, 'movements', 'x'), { ...credito(), createdAt: serverTimestamp() }));
    });

    it('no se puede acreditar dos veces el mismo desafío', async () => {
      await sembrar(UID, 1, 10);
      await assertFails(setDoc(doc(ana(), 'users', UID, 'movements', 'repetido'), {
        ...credito(), createdAt: serverTimestamp(),
      }));
    });

    it('no se puede acreditar con un monto distinto o negativo', async () => {
      await sembrar(UID, 0, 0);
      for (const amount of [999, -10, 0]) {
        await assertFails(setDoc(doc(ana(), 'users', UID, 'movements', `m-${amount}`), {
          ...credito({ amount }), createdAt: serverTimestamp(),
        }));
      }
    });

    it('no se puede editar ni borrar un movimiento existente', async () => {
      await sembrar();
      await assertFails(updateDoc(doc(ana(), 'users', UID, 'movements', 'm0'), { amount: 9999 }));
      await assertFails(deleteDoc(doc(ana(), 'users', UID, 'movements', 'm0')));
    });

    it('no se puede cambiar el saldo junto con un movimiento en un lote', async () => {
      await sembrar(UID, 0, 0);
      const db = ana();
      const lote = writeBatch(db);
      lote.set(doc(db, 'users', UID, 'movements', 'lote'), { ...credito(), createdAt: serverTimestamp() });
      lote.update(doc(db, 'users', UID), { pointsBalance: 10 });
      await assertFails(lote.commit());
    });

    it('no se puede dejar el saldo negativo ni subirlo', async () => {
      await sembrar(UID, 0, 0);
      await assertFails(updateDoc(doc(ana(), 'users', UID), { pointsBalance: -50 }));
      await assertFails(updateDoc(doc(ana(), 'users', UID), { pointsBalance: 500 }));
    });

    it('no se puede escribir en la cuenta de otra persona', async () => {
      await sembrar(OTRA, 1, 10);
      await assertFails(setDoc(doc(ana(), 'users', OTRA, 'movements', 'x'), { ...credito(), createdAt: serverTimestamp() }));
      await assertFails(updateDoc(doc(ana(), 'users', OTRA), { pointsBalance: 9999 }));
      await assertFails(deleteDoc(doc(ana(), 'users', OTRA, 'movements', 'm0')));
    });

    it('sin sesión no se escribe nada', async () => {
      await sembrar(UID, 0, 0);
      await assertFails(setDoc(doc(sinSesion(entorno), 'users', UID, 'movements', 'x'), credito()));
    });
  });

  describe('denegación por defecto', () => {
    it('una subcolección no prevista de users se deniega para leer y escribir', async () => {
      await sembrar();
      await assertFails(getDocs(collection(ana(), 'users', UID, 'otra')));
      await assertFails(setDoc(doc(ana(), 'users', UID, 'otra', 'x'), { a: 1 }));
    });
  });
});
