import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { IDBFactory } from 'fake-indexeddb';
const source = readFileSync(new URL('../../shared/src/wasmJsMain/resources/browser.js', import.meta.url), 'utf8');
function tab(indexedDB) {
  const context = vm.createContext({indexedDB});
  vm.runInContext(source, context);
  return context.CoffeeBrowser;
}
test('IndexedDB survives a fresh app instance and serializes competing tab writes', async () => {
  const indexedDB = new IDBFactory();
  const a = tab(indexedDB), b = tab(indexedDB);
  assert.equal(await a.read(), '');
  assert.equal(await a.write('', '{"version":1}'), 'ok');
  assert.equal(await b.read(), '{"version":1}');
  const results = await Promise.all([a.write('{"version":1}', 'next-a'), b.write('{"version":1}', 'next-b')]);
  assert.deepEqual(results.sort(), ['conflict', 'ok']);
  assert.equal(await tab(indexedDB).read(), 'next-a');
  assert.equal(await b.write('outdated', 'lost'), 'conflict');
  assert.equal(await a.read(), 'next-a');
});
test('Unavailable storage rejects rather than falling back to temporary memory', async () => {
  const a = tab({open() { throw new Error('Storage denied'); }});
  await assert.rejects(a.read(), /Storage denied/);
  await assert.rejects(a.write('', 'lost'), /Storage denied/);
});
