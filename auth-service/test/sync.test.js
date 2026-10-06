import { test } from 'node:test';
import assert from 'node:assert/strict';
import { emptyBackup, updateSync, seedLegacy } from '../src/sync.js';
class Store {
  data = new Map(); queue = Promise.resolve(); fail = false;
  async get(key) { return structuredClone(this.data.get(key)); }
  async put(key, value) { if (this.fail) throw new Error('disk'); this.data.set(key, structuredClone(value)); }
  transaction(fn) { const next = this.queue.then(() => fn(this)); this.queue = next.catch(() => {}); return next; }
}
const backup = () => ({ ...emptyBackup(), beans: [{ id: 'b', name: 'Brasil', roaster: '' }], shots: [{ id: 's', beanId: 'b', createdAt: 1, dose: 18, output: 36, seconds: 28, grind: '12', temperature: null, notes: '', rating: 4 }] });
const request = (baseRevision, data = backup()) => ({ protocol: 2, baseRevision, backup: data });
test('concurrent writes accept exactly one expected revision', async () => {
  const storage = new Store();
  const results = await Promise.all([updateSync(storage, request(0)), updateSync(storage, request(0))]);
  assert.equal(results.filter(x => x.conflict).length, 1);
  assert.equal((await storage.get('sync_v2')).revision, 1);
});
test('deletion persists tombstone and rejects stale resurrection', async () => {
  const storage = new Store(); await updateSync(storage, request(0));
  const next = backup(); next.shots = [];
  const result = await updateSync(storage, request(1, next));
  assert.deepEqual(result.deleted.shots, ['s']);
  assert.equal((await updateSync(storage, request(2))).conflict, true);
  assert.equal((await storage.get('sync_v2')).backup.shots.length, 0);
});
test('storage failures and invalid references never report success', async () => {
  const storage = new Store(); storage.fail = true;
  await assert.rejects(updateSync(storage, request(0)));
  assert.equal(await storage.get('sync_v2'), undefined);
  storage.fail = false;
  const invalid = backup(); invalid.shots[0].beanId = 'missing';
  await assert.rejects(updateSync(storage, request(0, invalid)));
  assert.equal(await storage.get('sync_v2'), undefined);
});
test('legacy migration is idempotent and cannot overwrite later revisions', async () => {
  const storage = new Store(); const first = await seedLegacy(storage, backup());
  assert.equal(first.revision, 1);
  const changed = backup(); changed.shots[0].notes = 'edited';
  await updateSync(storage, request(1, changed));
  const restored = await seedLegacy(storage, backup());
  assert.equal(restored.revision, 2);
  assert.equal(restored.backup.shots[0].notes, 'edited');
});

test('v2 preserves water and style and prevents an older client from dropping them', async () => {
  const storage = new Store();
  await updateSync(storage, request(0));
  const v2 = backup(); v2.schemaVersion = 2;
  v2.shots[0].extraWater = 120.5; v2.shots[0].style = 'Americano';
  const result = await updateSync(storage, request(1, v2));
  assert.equal(result.backup.shots[0].extraWater, 120.5);
  assert.equal(result.backup.shots[0].style, 'Americano');
  await assert.rejects(updateSync(storage, request(2)), /backup_upgrade_required/);
  assert.deepEqual((await storage.get('sync_v2')).backup, v2);
});
test('rejects invalid optional water and style without changing storage', async () => {
  const storage = new Store();
  for (const water of [-1, 0, 1001, '100', Infinity]) {
    const value = backup(); value.schemaVersion = 2; value.shots[0].extraWater = water;
    await assert.rejects(updateSync(storage, request(0, value)), /invalid_shot/);
  }
  const value = backup(); value.schemaVersion = 2; value.shots[0].style = 5;
  await assert.rejects(updateSync(storage, request(0, value)), /invalid_shot/);
  assert.equal(await storage.get('sync_v2'), undefined);
});

test('v3 upgrades stored snapshots without cups and protects cup deletions', async () => {
  const storage = new Store();
  const old = backup(); old.schemaVersion = 2; delete old.cups;
  await storage.put('sync_v2', { protocol: 2, revision: 5, backup: old, deleted: { beans: [], shots: [], machines: [] } });
  const next = { ...old, schemaVersion: 3, cups: [{ id: 'cup', name: 'Cerámica', weight: 120 }] };
  const updated = await updateSync(storage, request(5, next));
  assert.deepEqual(updated.backup.cups, next.cups);
  await assert.rejects(updateSync(storage, request(6, old)), /backup_upgrade_required/);
  const deleted = await updateSync(storage, request(6, { ...next, cups: [] }));
  assert.deepEqual(deleted.deleted.cups, ['cup']);
  assert.equal((await updateSync(storage, request(7, next))).conflict, true);
});

test('v4 removing a bean from the catalog preserves its shots and rejects old clients', async () => {
  const storage = new Store();
  const data = backup(); data.schemaVersion = 4; data.beans[0].archived = true;
  const result = await updateSync(storage, request(0, data));
  assert.equal(result.backup.shots.length, 1);
  assert.equal(result.backup.beans[0].archived, true);
  assert.deepEqual(result.deleted.beans, []);
  await assert.rejects(updateSync(storage, request(1, backup())), /backup_upgrade_required/);
  data.beans[0].archived = 'true';
  await assert.rejects(updateSync(storage, request(1, data)), /invalid_bean/);
});
