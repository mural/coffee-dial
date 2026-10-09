import { test } from 'node:test';
import assert from 'node:assert/strict';
import { photoRequest, validJpeg } from '../src/photos.js';
import { validateBackup, emptyBackup } from '../src/sync.js';
const jpeg = "/9j/4AAQSkZJRgABAQAASABIAAD/4QBMRXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAABAAEAAKACAAQAAAABAAAAEKADAAQAAAABAAAAEAAAAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAAA4QklNBCUAAAAAABDUHYzZjwCyBOmACZjs+EJ+/8AAEQgAEAAQAwEiAAIRAQMRAf/EAB8AAAEFAQEBAQEBAAAAAAAAAAABAgMEBQYHCAkKC//EALUQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+v/EAB8BAAMBAQEBAQEBAQEAAAAAAAABAgMEBQYHCAkKC//EALURAAIBAgQEAwQHBQQEAAECdwABAgMRBAUhMQYSQVEHYXETIjKBCBRCkaGxwQkjM1LwFWJy0QoWJDThJfEXGBkaJicoKSo1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoKDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uLj5OXm5+jp6vLz9PX29/j5+v/bAEMAAgICAgICAwICAwUDAwMFBgUFBQUGCAYGBgYGCAoICAgICAgKCgoKCgoKCgwMDAwMDA4ODg4ODw8PDw8PDw8PD//bAEMBAgICBAQEBwQEBxALCQsQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEP/dAAQAAf/aAAwDAQACEQMRAD8A+f6KKK/n8/pg/9k=";
const id = '11111111-1111-4111-8111-111111111111';
test('private immutable photos enforce account isolation, size, type and references', async () => {
  const values = new Map();
  const env = { PHOTOS: {
    get: async key => values.has(key) ? { text: async () => values.get(key) } : null,
    put: async (key, value) => { values.set(key, value); return {}; }
  }, RATE_LIMITER: { limit: async () => ({ success: true }) } };
  const json = (value, status = 200) => new Response(JSON.stringify(value), { status });
  const call = (method, email, body = { id, jpeg }, photoId = id) => photoRequest(
    new Request('https://test/api/photos/' + photoId, { method, ...(method === 'POST' ? { body: JSON.stringify(body) } : {}) }),
    env, { email }, photoId, json, request => request.json());
  assert.equal((await call('POST', 'one@example.com')).status, 200);
  assert.equal((await call('POST', 'one@example.com')).status, 200);
  assert.equal((await call('GET', 'two@example.com')).status, 404);
  assert.equal((await (await call('GET', 'ONE@example.com')).json()).jpeg, jpeg);
  assert.equal((await call('POST', 'one@example.com', { id, jpeg: 'x'.repeat(136537) })).status, 400);
  assert.equal((await call('POST', 'one@example.com', { id, jpeg: btoa('not an image') })).status, 400);
  assert.equal((await call('GET', 'one@example.com', null, '../other')).status, 400);
  assert.equal(validJpeg(new Uint8Array(102401)), false);
  const b = { ...emptyBackup(), schemaVersion: 5, beans: [{ id: 'b', name: 'Brasil', roaster: '', photo: { id } }] };
  validateBackup(b);
  assert.throws(() => validateBackup({ ...b, schemaVersion: 4 }));
  b.beans[0].photo.jpeg = jpeg;
  assert.throws(() => validateBackup(b));
});
