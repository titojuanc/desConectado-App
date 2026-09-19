import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, getDocs, collection, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno, sinSesion } from './helpers.mjs';

// Contrato: contracts/firestore-data.md, "Condiciones para crear users/{uid}".
describe('reglas de users/{uid}', () => {
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
  const CORREO = 'ana@mail.com';
  const perfilValido = () => ({ username: 'Ana Prueba', email: CORREO, createdAt: serverTimestamp() });
  const ana = () => comoPersona(entorno, UID, CORREO);

  async function sembrarPerfil(uid = UID) {
    await entorno.withSecurityRulesDisabled(async (contexto) => {
      await setDoc(doc(contexto.firestore(), 'users', uid), {
        username: 'Ana Prueba',
        email: CORREO,
        createdAt: new Date(),
      });
    });
  }

  describe('crear', () => {
    it('el dueño puede crear su perfil con exactamente username, email y createdAt', async () => {
      await assertSucceeds(setDoc(doc(ana(), 'users', UID), perfilValido()));
    });

    it('se rechaza un campo extra', async () => {
      await assertFails(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), puntos: 100 }));
    });

    it('se rechaza si falta un campo', async () => {
      const { createdAt, ...sinFecha } = perfilValido();
      await assertFails(setDoc(doc(ana(), 'users', UID), sinFecha));
    });

    it('acepta un username de 1 y de 30 caracteres', async () => {
      await assertSucceeds(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), username: 'a' }));
      await entorno.clearFirestore();
      await assertSucceeds(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), username: 'a'.repeat(30) }));
    });

    it('se rechaza un username vacío o de 31 caracteres', async () => {
      await assertFails(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), username: '' }));
      await assertFails(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), username: 'a'.repeat(31) }));
    });

    it('se rechaza un username que no es texto', async () => {
      await assertFails(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), username: 12345 }));
    });

    it('se rechaza un email distinto del correo del token', async () => {
      await assertFails(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), email: 'otra@mail.com' }));
    });

    it('se rechaza un email con mayúsculas (debe ir en minúsculas)', async () => {
      await assertFails(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), email: 'ANA@MAIL.COM' }));
    });

    it('se rechaza un createdAt que no es la hora del servidor', async () => {
      await assertFails(setDoc(doc(ana(), 'users', UID), { ...perfilValido(), createdAt: new Date('2020-01-01') }));
    });

    it('se rechaza crear el perfil de otro uid', async () => {
      await assertFails(setDoc(doc(ana(), 'users', 'otra-persona'), perfilValido()));
    });

    it('se rechaza sin iniciar sesión', async () => {
      await assertFails(setDoc(doc(sinSesion(entorno), 'users', UID), perfilValido()));
    });
  });

  describe('leer', () => {
    it('el dueño lee su perfil', async () => {
      await sembrarPerfil();
      await assertSucceeds(getDoc(doc(ana(), 'users', UID)));
    });

    it('no se puede leer el perfil de otra persona', async () => {
      await sembrarPerfil('otra-persona');
      await assertFails(getDoc(doc(ana(), 'users', 'otra-persona')));
    });

    it('sin sesión no se lee ningún perfil', async () => {
      await sembrarPerfil();
      await assertFails(getDoc(doc(sinSesion(entorno), 'users', UID)));
    });

    it('no se puede listar la colección de perfiles', async () => {
      await sembrarPerfil();
      await assertFails(getDocs(collection(ana(), 'users')));
    });
  });

  describe('modificar y borrar', () => {
    it('ni el dueño puede actualizar su perfil', async () => {
      await sembrarPerfil();
      await assertFails(updateDoc(doc(ana(), 'users', UID), { username: 'Otro nombre' }));
    });

    it('ni el dueño puede borrar su perfil', async () => {
      await sembrarPerfil();
      await assertFails(deleteDoc(doc(ana(), 'users', UID)));
    });
  });

  describe('denegación por defecto', () => {
    it('una ruta no declarada se deniega para leer y escribir', async () => {
      await assertFails(getDoc(doc(ana(), 'otras', 'cosa')));
      await assertFails(setDoc(doc(ana(), 'otras', 'cosa'), { x: 1 }));
    });
  });
});
