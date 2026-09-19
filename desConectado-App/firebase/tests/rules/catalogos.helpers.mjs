import { after, afterEach, before, describe, it } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, setDoc, updateDoc } from 'firebase/firestore';
import { comoPersona, crearEntorno, sinSesion } from './helpers.mjs';

/**
 * Pruebas comunes a una colección de catálogo: la lee cualquier persona autenticada y nadie la
 * escribe desde la app (contracts/firestore-data.md, tabla de permisos).
 */
export function probarCatalogoDeSoloLectura(coleccion, documentoDeEjemplo) {
  describe(`reglas de ${coleccion}/{id}`, () => {
    let entorno;
    const ID = 'ejemplo';

    before(async () => {
      entorno = await crearEntorno();
    });
    after(async () => {
      await entorno.cleanup();
    });
    afterEach(async () => {
      await entorno.clearFirestore();
    });

    async function sembrar() {
      await entorno.withSecurityRulesDisabled(async (contexto) => {
        await setDoc(doc(contexto.firestore(), coleccion, ID), documentoDeEjemplo);
      });
    }
    const persona = () => comoPersona(entorno, 'ana', 'ana@mail.com');

    it('una persona autenticada lee un documento', async () => {
      await sembrar();
      await assertSucceeds(getDoc(doc(persona(), coleccion, ID)));
    });

    it('una persona autenticada lista la colección', async () => {
      await sembrar();
      await assertSucceeds(getDocs(collection(persona(), coleccion)));
    });

    it('sin iniciar sesión no se lee ni se lista', async () => {
      await sembrar();
      await assertFails(getDoc(doc(sinSesion(entorno), coleccion, ID)));
      await assertFails(getDocs(collection(sinSesion(entorno), coleccion)));
    });

    it('nadie crea documentos desde la app', async () => {
      await assertFails(setDoc(doc(persona(), coleccion, 'nuevo'), documentoDeEjemplo));
      await assertFails(setDoc(doc(sinSesion(entorno), coleccion, 'nuevo'), documentoDeEjemplo));
    });

    it('nadie modifica documentos desde la app', async () => {
      await sembrar();
      await assertFails(updateDoc(doc(persona(), coleccion, ID), { points: 999999, costPoints: 1 }));
      await assertFails(setDoc(doc(persona(), coleccion, ID), documentoDeEjemplo));
    });

    it('nadie borra documentos desde la app', async () => {
      await sembrar();
      await assertFails(deleteDoc(doc(persona(), coleccion, ID)));
    });
  });
}
