import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createAccess, readAccess, SESSION_TTL } from '../src/access.js';
class Storage {
  data = new Map(); alarm = null;
  async get(key) { return structuredClone(this.data.get(key)); }
  async put(key, value) { this.data.set(key, structuredClone(value)); }
  async setAlarm(value) { this.alarm = value; }
  async transaction(fn) { return fn(this); }
}
const day = 24 * 60 * 60 * 1000;
test('session lasts 30 idle days and authenticated use renews its alarm', async () => {
  const store = new Storage(), user = { id: 'verified-subject' };
  await createAccess(store, user, 0);
  assert.equal(store.alarm, 30 * day);
  assert.deepEqual(await readAccess(store, day / 2), user);
  assert.equal(store.alarm, SESSION_TTL);
  assert.deepEqual(await readAccess(store, 29 * day), user);
  assert.equal(store.alarm, 59 * day);
  assert.deepEqual(await readAccess(store, 31 * day), user);
});
test('expired, revoked or absent sessions cannot renew themselves', async () => {
  const store = new Storage();
  await createAccess(store, { id: 'subject' }, 0);
  assert.equal(await readAccess(store, SESSION_TTL), null);
  assert.equal(store.alarm, SESSION_TTL);
  store.data.clear();
  assert.equal(await readAccess(store, 1), null);
});
test('still-valid legacy 12-hour session upgrades on use without trusting client expiry', async () => {
  const store = new Storage();
  await store.put('access', { user: { id: 'old' }, expires: day / 2 });
  assert.deepEqual(await readAccess(store, 1000), { id: 'old' });
  assert.equal(store.alarm, 1000 + SESSION_TTL);
});
