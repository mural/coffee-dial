import { test } from 'node:test';
import assert from 'node:assert/strict';
import { loginDestination, callbackDestination } from '../src/returns.js';
const start = (platform, target) => new URL('https://auth.test/google/start?' + new URLSearchParams({ platform, ...(target ? { return_to: target } : {}) }));
test('web returns to production or exact loopback origin and port', () => {
  for (const target of ['https://coffee.muralooo.win/', 'http://localhost:8080/', 'http://127.0.0.1:8085/', 'http://[::1]:8080/']) {
    const route = loginDestination(start('web', target));
    const result = callbackDestination({ ...route, appState: 'state', ticket: 'ticket' }, 'attempt');
    assert.equal(new URL(result.location).origin, new URL(target).origin);
    assert.equal(new URLSearchParams(new URL(result.location).hash.slice(1)).get('code'), 'ticket');
    assert.ok(!result.location.includes('email='));
  }
});
test('rejects open redirects and malformed destinations', () => {
  for (const target of ['https://evil.test/', 'http://localhost.evil.test:8080/', 'https://coffee.muralooo.win.evil.test/', 'https://evil@coffee.muralooo.win/', 'https://coffee.muralooo.win/?next=evil', 'http://localhost:8080/#x', 'javascript:alert(1)', 'https://coffee.muralooo.win/path']) {
    assert.throws(() => loginDestination(start('web', target)));
  }
  assert.throws(() => loginDestination(start('unknown')));
  assert.throws(() => loginDestination(start('android', 'http://localhost/')));
});
test('mobile and cancellations keep the initiating platform', () => {
  for (const platform of ['android', 'ios']) {
    const result = callbackDestination({ ...loginDestination(start(platform)), appState: 'state', error: 'cancelled' }, 'attempt');
    assert.equal(result.platform, platform);
    assert.equal(new URL(result.location).protocol, 'coffeedial:');
    assert.equal(new URL(result.location).searchParams.get('error'), 'cancelled');
  }
});
