import { test } from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { readFileSync } from 'node:fs';
const source = readFileSync(new URL('../../shared/src/wasmJsMain/resources/auth.js', import.meta.url), 'utf8');
function browser(hash = '') {
  const storage = () => { const values = new Map(); return { getItem: key => values.get(key) ?? null, setItem: (k,v) => values.set(k,v), removeItem: k => values.delete(k) }; };
  const context = { URL, URLSearchParams, Uint8Array, TextEncoder, crypto, btoa, Date, sessionStorage: storage(), localStorage: storage(), location: { origin: 'http://localhost:8085', pathname: '/', search: '', hash, assign(url) { this.assigned = url; } }, history: { replaceState() {} }, fetch: async () => { throw new Error('unexpected network'); } };
  vm.runInNewContext(source, context);
  return context;
}
test('web login creates fresh proofs and requests exact localhost return', async () => {
  const c = browser(); await c.CoffeeAuth.start();
  const url = new URL(c.location.assigned), first = url.searchParams.get('state');
  assert.equal(url.searchParams.get('return_to'), 'http://localhost:8085/');
  assert.equal(url.searchParams.get('platform'), 'web');
  assert.equal(url.searchParams.get('challenge').length, 43);
  await c.CoffeeAuth.start(); assert.notEqual(new URL(c.location.assigned).searchParams.get('state'), first);
});
test('email URL cannot establish identity; mismatched callback rejected', async () => {
  const c = browser('#email=forged@example.com'); assert.equal(await c.CoffeeAuth.complete(), '');
  await c.CoffeeAuth.start(); c.location.hash = '#attempt=' + 'a'.repeat(43) + '&state=wrong&code=' + 'b'.repeat(43);
  await assert.rejects(c.CoffeeAuth.complete());
});
test('matching callback redeems ticket, preserves subject, and is single use', async () => {
  const c = browser(); await c.CoffeeAuth.start();
  const pending = JSON.parse(c.sessionStorage.getItem('coffee_oauth_attempt'));
  c.location.hash = '#' + new URLSearchParams({ attempt: 'a'.repeat(43), state: pending.state, code: 'b'.repeat(43) });
  c.fetch = async (url, request) => {
    assert.equal(JSON.parse(request.body).verifier, pending.verifier);
    return { ok: true, json: async () => ({ id: 'google-subject', email: 'test@example.com', displayName: 'Águstin' }) };
  };
  assert.equal(JSON.parse(await c.CoffeeAuth.complete()).id, 'google-subject');
  await assert.rejects(c.CoffeeAuth.complete());
  c.CoffeeAuth.clear(); assert.equal(c.sessionStorage.getItem('coffee_verified_profile_v1'), null);
});
