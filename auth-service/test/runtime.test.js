import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';
import { build } from 'esbuild';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { random, hash } from '../src/core.js';
import { emptyBackup } from '../src/sync.js';

test('real Worker/SQLite: authenticated isolation, CAS, tombstones, revocation and restart', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'coffee-sync-runtime-'));
  const bundle = await build({ entryPoints: [new URL('../src/index.js', import.meta.url).pathname], bundle: true, format: 'esm', platform: 'browser', external: ['cloudflare:workers'], write: false });
  const options = { modules: true, script: bundle.outputFiles[0].text, compatibilityDate: '2026-10-03', durableObjects: { SESSIONS: { className: 'LoginAttempt', useSQLite: true } }, durableObjectsPersist: dir, bindings: { PUBLIC_ORIGIN: 'https://auth.test', GOOGLE_CLIENT_ID: 'test', ALLOWED_EMAILS: 'test@example.com,user1@example.com,user2@example.com' } };
  let mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
  try {
    const namespace = await mf.getDurableObjectNamespace('SESSIONS');
    const token = `cd.${random()}`, other = `cd.${random()}`;
    for (const [credential, id, email] of [[token, 'one', 'user1@example.com'], [other, 'two', 'user2@example.com']]) {
      const stub = namespace.get(namespace.idFromName(`access_${await hash(credential)}`));
      const response = await stub.fetch('https://session/access_create', { method: 'POST', body: JSON.stringify({ user: { id, email } }) });
      assert.equal(response.status, 200);
      assert.deepEqual(await response.json(), { ok: true });
    }
    const request = (method, credential = token, body) => mf.dispatchFetch('https://auth.test/api/sync', { method, headers: { Authorization: `Bearer ${credential}`, 'Content-Type': 'application/json' }, ...(body ? { body: JSON.stringify(body) } : {}) });
    assert.equal((await mf.dispatchFetch('https://auth.test/api/sync?email=test@example.com', { headers: { 'X-User-Email': 'test@example.com' } })).status, 401);
    assert.equal((await request('GET', `cd.${random()}`)).status, 401);
    const profile = await mf.dispatchFetch('https://auth.test/api/session', { headers: { Authorization: `Bearer ${token}` } });
    assert.equal(profile.status, 200);
    assert.equal((await profile.json()).id, 'one');
    assert.equal((await mf.dispatchFetch('https://auth.test/api/session', { method: 'POST', headers: { Authorization: `Bearer ${token}` } })).status, 400);
    const b = { ...emptyBackup(), beans: [{ id: 'b', name: 'Brasil', roaster: '' }] };
    const uploads = await Promise.all([request('POST', token, { protocol: 2, baseRevision: 0, backup: b }), request('POST', token, { protocol: 2, baseRevision: 0, backup: b })]);
    assert.deepEqual(uploads.map(x => x.status).sort(), [200, 409]);
    assert.equal((await (await request('GET', other)).json()).revision, 0);
    assert.equal((await request('POST', token, { protocol: 2, baseRevision: 1, backup: emptyBackup() })).status, 200);
    await mf.dispose(); mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
    const restored = await (await request('GET')).json();
    assert.equal(restored.revision, 2); assert.deepEqual(restored.deleted.beans, ['b']);
    assert.equal((await request('POST', token, { protocol: 2, baseRevision: 2, backup: b })).status, 409);
    const v2 = { ...emptyBackup(), schemaVersion: 2,
      beans: [{ id: 'new-bean', name: 'Test', roaster: '' }],
      shots: [{ id: 'new-shot', beanId: 'new-bean', createdAt: 1, dose: 18, output: 36,
        seconds: 20, grind: 'Medio', temperature: null, notes: '', rating: 3,
        extraWater: 120, style: 'Americano' }] };
    assert.equal((await request('POST', token, { protocol: 2, baseRevision: 2, backup: v2 })).status, 200);
    await mf.dispose(); mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
    const upgraded = await (await request('GET')).json();
    assert.equal(upgraded.backup.shots[0].extraWater, 120);
    assert.equal(upgraded.backup.shots[0].style, 'Americano');
    assert.equal((await request('POST', token, { protocol: 2, baseRevision: 3, backup: emptyBackup() })).status, 426);
    assert.equal((await (await request('GET')).json()).revision, 3);
    assert.equal((await mf.dispatchFetch('https://auth.test/api/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } })).status, 200);
    assert.equal((await request('GET')).status, 401);
  } finally { await mf.dispose(); await rm(dir, { recursive: true, force: true }); }
});
