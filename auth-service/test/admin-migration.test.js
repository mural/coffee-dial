import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';
import { build } from 'esbuild';
import { mkdtemp, rm, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { emptyBackup } from '../src/sync.js';

test('legacy duplicate emails collapse without deleting original indexes; canonical revision wins after restart', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'coffee-admin-migration-'));
  const source = await readFile(new URL('../src/admin.js', import.meta.url), 'utf8');
  // A test-only fixture method writes the exact legacy SQL layout, never deployed.
  const fixture = source.replace('  authorize(user) {', `  seedFixture() {
    this.sql.exec("INSERT INTO accounts VALUES ('google-old','SAME@example.com',20,100,5,20),('apple-old','same@example.com',3,200,7,28)");
    this.sql.exec("INSERT INTO daily VALUES ('google-old','2026-10-08',5),('apple-old','2026-10-08',7)");
    this.sql.exec("INSERT INTO styles VALUES ('google-old','Latte',5),('apple-old','Latte',7)");
  }
  legacyCount() { return this.sql.exec('SELECT COUNT(*) AS n FROM accounts').one().n; }
  authorize(user) {`);
  const bundled = await build({ stdin: { contents: fixture + '\nexport default { fetch() { return new Response("test") } };', resolveDir: new URL('../src', import.meta.url).pathname }, bundle: true, format: 'esm', platform: 'browser', external: ['cloudflare:workers'], write: false });
  const options = { modules: true, script: bundled.outputFiles[0].text, compatibilityDate: '2026-10-03', durableObjects: { DIRECTORY: { className: 'AdminDirectory', useSQLite: true } }, durableObjectsPersist: dir };
  let mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
  const directory = async () => { const ns = await mf.getDurableObjectNamespace('DIRECTORY'); return ns.get(ns.idFromName('test')); };
  try {
    let d = await directory(); await d.seedFixture();
    let view = await d.overview();
    assert.equal(view.accounts, 1); assert.equal(view.shots, 7);
    assert.equal(view.styles[0].count, 7);
    assert.equal((await d.account('google-old')).subject, 'email:same@example.com');
    // Revision numbers in the old namespaces aren't comparable to canonical revisions.
    await d.record({ id: 'google-old', provider: 'google', email: 'same@example.com' }, { revision: 1, backup: emptyBackup() });
    view = await d.overview(); assert.equal(view.accounts, 1); assert.equal(view.shots, 0);
    assert.equal(view.styles.length, 0); assert.equal(view.daily.length, 0);
    assert.equal(await d.legacyCount(), 2);
    await mf.dispose(); mf = new Miniflare({ ...convertV4MiniflareOptions(options), resourcePersistencePath: dir });
    d = await directory(); view = await d.overview();
    assert.equal(view.accounts, 1); assert.equal(view.shots, 0);
    assert.equal(await d.legacyCount(), 2);
  } finally { await mf.dispose(); await rm(dir, { recursive: true, force: true }); }
});
