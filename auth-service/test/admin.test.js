import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';
import { build } from 'esbuild';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { random, hash } from '../src/core.js';
import { emptyBackup } from '../src/sync.js';

test('admin: verified subject pin, authorization, index recovery, aggregates and read-only routes', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'coffee-admin-'));
  const bundle = await build({ entryPoints: [new URL('../src/index.js', import.meta.url).pathname], bundle: true, format: 'esm', platform: 'browser', external: ['cloudflare:workers'], write: false });
  const options = { modules: true, script: bundle.outputFiles[0].text, compatibilityDate: '2026-10-03', durableObjects: {
    SESSIONS: { className: 'LoginAttempt', useSQLite: true }, ADMIN_DIRECTORY: { className: 'AdminDirectory', useSQLite: true }
  }, durableObjectsPersist: dir, bindings: { PUBLIC_ORIGIN: 'https://auth.test', GOOGLE_CLIENT_ID: 'test', ADMIN_GOOGLE_EMAIL: 'master@example.com', ALLOWED_EMAILS: 'master@example.com,other@example.com' } };
  let mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
  try {
    const master = `cd.${random()}`, other = `cd.${random()}`, changedSubject = `cd.${random()}`;
    const ns = await mf.getDurableObjectNamespace('SESSIONS');
    for (const [token, id, email] of [[master, 'master-id', 'master@example.com'], [other, 'other-id', 'other@example.com'], [changedSubject, 'replacement-id', 'master@example.com']]) {
      const stub = ns.get(ns.idFromName(`access_${await hash(token)}`));
      assert.equal((await stub.fetch('https://session/access_create', { method: 'POST', body: JSON.stringify({ user: { id, email } }) })).status, 200);
    }
    const request = (path, token = master, method = 'GET', body) => mf.dispatchFetch(`https://auth.test${path}`, {
      method, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, ...(body ? { body: JSON.stringify(body) } : {})
    });
    assert.equal((await mf.dispatchFetch('https://auth.test/api/admin/overview', { headers: { 'X-User-Email': 'master@example.com' } })).status, 401);
    assert.equal((await request('/api/admin/overview', other)).status, 403);
    assert.equal((await request('/api/admin/account?subject=master-id&email=master@example.com', other)).status, 403);
    assert.deepEqual(await (await request('/api/admin/access', other)).json(), { allowed: false });
    assert.deepEqual(await (await request('/api/admin/access')).json(), { allowed: true });
    assert.equal((await request('/api/admin/overview', changedSubject)).status, 403);
    assert.equal((await request('/api/admin/overview', master, 'POST', { role: 'master' })).status, 405);
    assert.equal((await request('/api/admin/account?subject=missing')).status, 404);
    const backup = { ...emptyBackup(), schemaVersion: 2, beans: [{ id: 'b', name: 'Test', roaster: '' }], shots: [
      { id: 's', beanId: 'b', createdAt: Date.now(), dose: 18, output: 36, seconds: 20, grind: 'Medio', temperature: null, notes: 'Private note', rating: 4, style: 'Americano', extraWater: 120 }
    ] };
    // Existing account not yet indexed: read/sync adds it without changing the source.
    const old = ns.get(ns.idFromName('sync_v2_google_other-id'));
    await old.fetch('https://session/sync_write', { method: 'POST', body: JSON.stringify({ protocol: 2, baseRevision: 0, backup }) });
    assert.equal((await (await request('/api/admin/overview')).json()).accounts, 0);
    assert.equal((await request('/api/sync', other)).status, 200);
    let overview = await (await request('/api/admin/overview')).json();
    assert.equal(overview.accounts, 1); assert.equal(overview.shots, 1); assert.equal(overview.ratingSum, 4);
    assert.deepEqual(overview.styles, [{ label: 'Americano', count: 1 }]);
    assert.equal(overview.daily[0].count, 1);
    assert.equal(overview.shots7Days, 1);
    const detail = await (await request('/api/admin/account?subject=other-id')).json();
    assert.equal(detail.shots[0].notes, 'Private note'); assert.equal(detail.shots[0].extraWater, 120);
    assert.equal(detail.checkpoint, undefined); assert.equal(detail.syncToken, undefined);
    assert.equal((await request('/api/admin/account?subject=other-id', other)).status, 403);
    const directoryNSForAlias = await mf.getDurableObjectNamespace('ADMIN_DIRECTORY');
    const aliasDirectory = directoryNSForAlias.get(directoryNSForAlias.idFromName('directory-v1'));
    await aliasDirectory.record({ id: 'apple-other', email: ' OTHER@example.com ', provider: 'apple' }, { revision: 1, backup });
    const deduped = await (await request('/api/admin/overview')).json();
    assert.equal(deduped.accounts, 1);
    assert.equal(deduped.shots, 1);
    assert.equal(deduped.daily[0].count, 1);
    const canonicalDetail = await (await request('/api/admin/account?subject=' + encodeURIComponent(deduped.accountsPage[0].subject))).json();
    assert.equal(canonicalDetail.shots.length, 1);
    const edited = structuredClone(backup); edited.shots[0].rating = 2;
    assert.equal((await request('/api/sync', other, 'POST', { protocol: 2, baseRevision: 1, backup: edited })).status, 200);
    const directoryNS = await mf.getDurableObjectNamespace('ADMIN_DIRECTORY');
    const directory = directoryNS.get(directoryNS.idFromName('directory-v1'));
    await directory.record({ id: 'other-id', email: 'other@example.com' }, { revision: 1, backup });
    overview = await (await request('/api/admin/overview')).json();
    assert.equal(overview.ratingSum, 2); // delayed older index update cannot overwrite new state
    assert.equal((await request('/api/sync', other, 'POST', { protocol: 2, baseRevision: 2, backup: { ...edited, shots: [] } })).status, 200);
    overview = await (await request('/api/admin/overview')).json();
    assert.equal(overview.shots, 0); assert.equal(overview.styles.length, 0);
    for (let i = 0; i < 51; i++) {
      await directory.record({ id: `page-${String(i).padStart(3, '0')}`, email: `test-${i}@example.com` }, { revision: 0, backup: emptyBackup() });
    }
    const page1 = await (await request('/api/admin/overview')).json();
    const page2 = await (await request(`/api/admin/overview?after=${page1.next}`)).json();
    assert.equal(page1.accounts, 52); assert.equal(page1.accountsPage.length, 50);
    assert.equal(page2.accountsPage.length, 2); assert.equal(page2.next, null);
    assert.equal(new Set([...page1.accountsPage, ...page2.accountsPage].map(x => x.subject)).size, 52);
    await mf.dispose(); mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
    assert.equal((await request('/api/admin/overview', changedSubject)).status, 403);
    assert.equal((await (await request('/api/admin/overview')).json()).accounts, 52);
    assert.equal((await request('/api/logout', master, 'POST', {})).status, 200);
    assert.equal((await request('/api/admin/overview')).status, 401);
  } finally { await mf.dispose(); await rm(dir, { recursive: true, force: true }); }
});
