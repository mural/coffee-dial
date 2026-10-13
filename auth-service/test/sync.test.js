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


test('Kotlin omitted defaults remain compatible', async () => {
  const data = backup(); delete data.machines; delete data.cups;
  const result = await updateSync(new Store(), { baseRevision: 0, backup: data });
  assert.equal(result.protocol, 2);
  assert.deepEqual(result.backup.machines, []);
  assert.deepEqual(result.backup.cups, []);
});
test('explicit overwrite respects CAS and clears restored tombstones', async () => {
  const storage = new Store();
  await updateSync(storage, request(0));
  await updateSync(storage, request(1, emptyBackup()));
  assert.equal((await updateSync(storage, { ...request(1), force: true })).conflict, true);
  const restored = await updateSync(storage, { ...request(2), force: true });
  assert.deepEqual(restored.deleted.shots, []);
  assert.deepEqual(restored.deleted.beans, []);
  assert.equal((await updateSync(storage, request(3))).revision, 4);
});


test('v12 defaults round trip, reject downgrade and validate unique category slots', async () => {
  const storage = new Store();
  const value = { ...backup(), schemaVersion: 12, brewDefaults: [{ id: 'beans', selectedId: 'b' }] };
  const first = await updateSync(storage, request(0, value));
  assert.deepEqual(first.backup.brewDefaults, value.brewDefaults);
  await assert.rejects(updateSync(storage, request(1, { ...backup(), schemaVersion: 11 })), /backup_upgrade_required/);
  for (const brewDefaults of [[{ id: 'invalid' }], [{ id: 'beans', selectedId: '' }], [{ id: 'beans' }, { id: 'beans' }]]) {
    await assert.rejects(updateSync(storage, request(1, { ...value, brewDefaults })));
  }
  await assert.rejects(updateSync(storage, request(1, { ...value, schemaVersion: 11 })), /invalid_brew_default/);
  await updateSync(storage, request(1, { ...value, brewDefaults: [] }));
  const selectedAgain = await updateSync(storage, request(2, value));
  assert.equal(selectedAgain.conflict, undefined);
  assert.deepEqual(selectedAgain.deleted.brewDefaults, []);
  const unset = await updateSync(storage, request(3, { ...value, brewDefaults: [{ id: 'beans', selectedId: null }] }));
  assert.equal(unset.backup.brewDefaults[0].selectedId, null);
});

test('old snapshot responses do not gain unsupported preference fields', async () => {
  const storage = new Store();
  const result = await updateSync(storage, request(0));
  assert.equal(Object.hasOwn(result.backup, 'brewDefaults'), false);
});

test('v14 accepts bean roastDate, origin, process, and bagWeightGrams', async () => {
  const storage = new Store();
  const bean = { id: 'b', name: 'Brasil', roaster: 'Fuego', roastDate: 1700000000000, origin: 'Mogiana', process: 'Natural', bagWeightGrams: 250 };
  const value = { ...backup(), schemaVersion: 14, beans: [bean] };
  const first = await updateSync(storage, request(0, value));
  assert.deepEqual(first.backup.beans, value.beans);
  await assert.rejects(updateSync(storage, request(1, { ...value, beans: [{ ...bean, bagWeightGrams: -10 }] })), /invalid_bean/);
  await assert.rejects(updateSync(storage, request(1, { ...value, schemaVersion: 13 })), /invalid_bean/);
});

test('v14 sensory scores use 1–5; old snapshots retain their legacy interpretation', async () => {
  const storage = new Store();
  const legacy = { ...backup(), schemaVersion: 13 };
  legacy.shots[0].taste = { body: 9, acidity: 0, bitterness: null };
  await updateSync(storage, request(0, legacy));
  const current = structuredClone(legacy); current.schemaVersion = 14;
  current.shots[0].taste = { body: 5, acidity: 1, bitterness: null };
  const result = await updateSync(storage, request(1, current));
  assert.deepEqual(result.backup.shots[0].taste, current.shots[0].taste);
  await assert.rejects(updateSync(storage, request(2, legacy)), /backup_upgrade_required/);
  for (const value of [0, 6, 10, 1.5, '3']) {
    const invalid = structuredClone(current); invalid.shots[0].taste.body = value;
    await assert.rejects(updateSync(storage, request(2, invalid)), /invalid_shot/);
  }
});
