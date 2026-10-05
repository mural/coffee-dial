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
  const options = { modules: true, script: bundle.outputFiles[0].text, compatibilityDate: '2026-10-03', durableObjects: { SESSIONS: { className: 'LoginAttempt', useSQLite: true } }, durableObjectsPersist: dir, bindings: { PUBLIC_ORIGIN: 'https://auth.test', GOOGLE_CLIENT_ID: 'test', ALLOWED_EMAILS: 'test@example.com' } };
  let mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
  try {
    const namespace = await mf.getDurableObjectNamespace('SESSIONS');
    const token = `cd.${random()}`, other = `cd.${random()}`;
    for (const [credential, id] of [[token, 'one'], [other, 'two']]) {
      const stub = namespace.get(namespace.idFromName(`access_${await hash(credential)}`));
      const response = await stub.fetch('https://session/access_create', { method: 'POST', body: JSON.stringify({ user: { id, email: 'test@example.com' } }) });
      assert.equal(response.status, 200);
    }
    const request = (method, credential = token, body) => mf.dispatchFetch('https://auth.test/api/sync', { method, headers: { Authorization: `Bearer ${credential}`, 'Content-Type': 'application/json' }, ...(body ? { body: JSON.stringify(body) } : {}) });
    assert.equal((await mf.dispatchFetch('https://auth.test/api/sync?email=test@example.com', { headers: { 'X-User-Email': 'test@example.com' } })).status, 401);
    assert.equal((await request('GET', `cd.${random()}`)).status, 401);
    const b = { ...emptyBackup(), beans: [{ id: 'b', name: 'Brasil', roaster: '' }] };
    const uploads = await Promise.all([request('POST', token, { protocol: 2, baseRevision: 0, backup: b }), request('POST', token, { protocol: 2, baseRevision: 0, backup: b })]);
    assert.deepEqual(uploads.map(x => x.status).sort(), [200, 409]);
    assert.equal((await (await request('GET', other)).json()).revision, 0);
    assert.equal((await request('POST', token, { protocol: 2, baseRevision: 1, backup: emptyBackup() })).status, 200);
    await mf.dispose(); mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
    const restored = await (await request('GET')).json();
    assert.equal(restored.revision, 2); assert.deepEqual(restored.deleted.beans, ['b']);
    assert.equal((await request('POST', token, { protocol: 2, baseRevision: 2, backup: b })).status, 409);
    assert.equal((await mf.dispatchFetch('https://auth.test/api/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } })).status, 200);
    assert.equal((await request('GET')).status, 401);
  } finally { await mf.dispose(); await rm(dir, { recursive: true, force: true }); }
});
