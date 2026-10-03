import { test } from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPair, SignJWT, createLocalJWKSet, exportJWK } from 'jose';
import { Session, random, hash, verifyGoogle, TTL } from '../src/core.js';
class Store {
  map = new Map(); queue = Promise.resolve();
  async get(k) { return structuredClone(this.map.get(k)); }
  async put(k, v) { this.map.set(k, structuredClone(v)); }
  async delete(k) { this.map.delete(k); }
  async deleteAll() { this.map.clear(); }
  async setAlarm() {}
  async deleteAlarm() {}
  transaction(fn) { const next = this.queue.then(() => fn(this)); this.queue = next.catch(() => {}); return next; }
}
async function fixture(exchange = async () => ({ id: 'subject', email: 'test@example.com', displayName: 'Test' })) {
  const store = new Store(); let now = 1000;
  const session = new Session(store, {}, exchange, () => now);
  const verifier = random(), appState = random(), browserHash = await hash(random());
  await session.init({ challenge: await hash(verifier), appState, browserHash });
  return { session, store, verifier, appState, browserHash, expire: () => { now += TTL + 1; } };
}
test('success is single-use even with concurrent redemption', async () => {
  const f = await fixture(); const result = await f.session.callback({ browserHash: f.browserHash, code: 'google-code' });
  const input = { ...f, ticket: result.ticket };
  const outcomes = await Promise.allSettled([f.session.consume(input), f.session.consume(input)]);
  assert.equal(outcomes.filter(x => x.status === 'fulfilled').length, 1);
  assert.equal(await f.store.get('session'), undefined);
});
test('intercepted app callback cannot redeem without verifier', async () => {
  const f = await fixture(); const { ticket } = await f.session.callback({ browserHash: f.browserHash, code: 'code' });
  await assert.rejects(f.session.consume({ ...f, ticket, verifier: random() }));
  assert.equal((await f.session.consume({ ...f, ticket })).id, 'subject');
});
test('wrong browser cookie does not consume genuine attempt', async () => {
  const f = await fixture(); await assert.rejects(f.session.callback({ browserHash: random(), code: 'code' }));
  assert.ok((await f.session.callback({ browserHash: f.browserHash, code: 'code' })).ticket);
});
test('expired attempt cannot authenticate', async () => {
  const f = await fixture(); f.expire(); await assert.rejects(f.session.callback({ browserHash: f.browserHash, code: 'code' }));
});
test('expired ticket cannot be exchanged', async () => {
  const f = await fixture(); const { ticket } = await f.session.callback({ browserHash: f.browserHash, code: 'code' });
  f.expire(); await assert.rejects(f.session.consume({ ...f, ticket }));
});
test('replayed provider callback is rejected', async () => {
  const f = await fixture(); const request = { browserHash: f.browserHash, code: 'code' };
  await f.session.callback(request); await assert.rejects(f.session.callback(request));
});
test('cancelled and failed provider logins remove transient identity', async () => {
  for (const error of [true, false]) {
    const f = await fixture(async () => { throw new Error('provider failure'); });
    const result = await f.session.callback({ browserHash: f.browserHash, code: 'code', error });
    assert.ok(result.error); assert.equal(await f.store.get('session'), undefined);
  }
});
test('mismatched app state fails without consuming ticket', async () => {
  const f = await fixture(); const { ticket } = await f.session.callback({ browserHash: f.browserHash, code: 'code' });
  await assert.rejects(f.session.consume({ ...f, ticket, appState: random() }));
});
test('malformed challenge rejected', async () => {
  const f = await fixture(); await assert.rejects(f.session.init({ challenge: 'x' }));
});
const { privateKey, publicKey } = await generateKeyPair('RS256');
const jwk = await exportJWK(publicKey); jwk.kid = 'test';
const keys = createLocalJWKSet({ keys: [jwk] });
const env = { GOOGLE_CLIENT_ID: 'audience', ALLOWED_EMAILS: 'test@example.com' };
async function token(overrides = {}) {
  return new SignJWT({ sub: 'subject', email: 'test@example.com', email_verified: true, nonce: 'nonce', ...overrides })
    .setProtectedHeader({ alg: 'RS256', kid: 'test' }).setIssuer(overrides.iss ?? 'https://accounts.google.com')
    .setAudience(overrides.aud ?? 'audience').setIssuedAt().setExpirationTime(overrides.exp ?? '5m').sign(privateKey);
}
test('valid Google identity validated through signature and claims', async () => {
  assert.equal((await verifyGoogle(await token(), env, 'nonce', keys)).id, 'subject');
});
test('invalid issuer/audience/nonce/expiry/email rejected', async () => {
  for (const override of [{ iss: 'https://evil.test' }, { aud: 'other' }, { nonce: 'other' }, { exp: 1 },
    { email_verified: false }, { email: 'other@example.com' }]) {
    await assert.rejects(verifyGoogle(await token(override), env, 'nonce', keys));
  }
});
test('forged signature rejected', async () => {
  const signed = await token(); const parts = signed.split('.'); parts[2] = 'a'.repeat(parts[2].length);
  await assert.rejects(verifyGoogle(parts.join('.'), env, 'nonce', keys));
});
