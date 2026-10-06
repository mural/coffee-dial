import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import vm from 'node:vm';
const script = await readFile(new URL('../../shared/src/wasmJsMain/resources/auth.js', import.meta.url), 'utf8');
const key = 'coffee_verified_profile_v1';
const storage = () => { const map = new Map(); return {
  getItem: k => map.get(k) ?? null, setItem: (k, v) => map.set(k, v), removeItem: k => map.delete(k)
}; };
const user = { id: 'subject', displayName: 'Test', syncToken: 'cd.session' };
function app(localStorage, sessionStorage, fetch) {
  const context = vm.createContext({ localStorage, sessionStorage, fetch, URLSearchParams, AbortSignal, location: { hash: '' } });
  vm.runInContext(script, context);
  return context.CoffeeAuth;
}
test('verified web session survives reopening and migrates the previous tab session', async () => {
  const local = storage(), tab = storage(); tab.setItem(key, JSON.stringify(user));
  const fetch = async () => ({ ok: true, status: 200, json: async () => ({ id: user.id }) });
  assert.equal(JSON.parse(await app(local, tab, fetch).complete()).id, user.id);
  assert.equal(tab.getItem(key), null);
  assert.equal(JSON.parse(await app(local, storage(), fetch).complete()).syncToken, user.syncToken);
});
test('expired and revoked web sessions are removed without retaining a false login', async () => {
  const local = storage(), tab = storage(); local.setItem(key, JSON.stringify(user));
  const auth = app(local, tab, async () => ({ status: 401 }));
  assert.equal(await auth.complete(), '');
  assert.equal(local.getItem(key), null);
});
test('offline keeps the local profile; mismatched verified identity clears it', async () => {
  const local = storage(), tab = storage(); local.setItem(key, JSON.stringify(user));
  assert.equal(JSON.parse(await app(local, tab, async () => { throw Error('offline'); }).complete()).id, user.id);
  assert.equal(await app(local, tab, async () => ({ ok: true, json: async () => ({ id: 'other' }) })).complete(), '');
  assert.equal(local.getItem(key), null);
});
