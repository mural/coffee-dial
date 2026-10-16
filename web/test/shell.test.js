import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync(new URL('../../shared/src/wasmJsMain/resources/browser.js', import.meta.url), 'utf8');

function page({ stored = null, language = 'en-US', dark = false } = {}) {
  const values = new Map();
  if (stored != null) values.set('coffee_language', stored);
  const elements = {
    'web-notice': { textContent: '', style: { display: 'none' } },
    loading: { textContent: '', hidden: false, dataset: {} }
  };
  const context = {
    localStorage: { getItem: key => values.get(key) ?? null },
    navigator: { language },
    document: {
      documentElement: { lang: 'es', style: { setProperty() {} } },
      getElementById: id => elements[id] ?? null,
      querySelector: () => null
    },
    window: { matchMedia: () => ({ matches: dark }), dispatchEvent() {} },
    requestAnimationFrame: fn => fn(),
    Event: class Event { constructor(type) { this.type = type; } }
  };
  vm.runInNewContext(source, context);
  return { context, elements };
}

test('shell copy follows the saved app language, then the browser', () => {
  const english = page({ stored: 'ENGLISH', language: 'es-AR' });
  assert.equal(english.elements['web-notice'].textContent.startsWith('Data stays'), true);
  assert.equal(english.elements.loading.textContent, 'Loading Coffee Dial…');
  assert.equal(english.context.document.documentElement.lang, 'en');

  const code = page({ stored: 'en', language: 'es' });
  assert.equal(code.elements.loading.textContent, 'Loading Coffee Dial…');

  const spanish = page({ stored: 'SPANISH', language: 'en-US' });
  assert.equal(spanish.elements.loading.textContent, 'Cargando Coffee Dial…');
  assert.equal(spanish.elements['web-notice'].textContent.includes('navegador'), true);

  const system = page({ stored: 'SYSTEM', language: 'es-AR' });
  assert.equal(system.elements.loading.textContent, 'Cargando Coffee Dial…');

  const browser = page({ language: 'en-GB' });
  assert.equal(browser.elements.loading.textContent, 'Loading Coffee Dial…');
});

test('changing language in the app updates the notice without a reload', () => {
  const { context, elements } = page({ stored: 'SPANISH', language: 'es-AR' });
  elements['web-notice'].style.display = 'block';
  context.coffeeApplyShell(true);
  assert.equal(elements['web-notice'].textContent.startsWith('Data stays'), true);
  assert.equal(elements.loading.textContent, 'Loading Coffee Dial…');
});

test('a failed open keeps the translated error instead of the loading line', () => {
  const { context, elements } = page({ stored: 'ENGLISH', language: 'es' });
  context.CoffeeBrowser.failed();
  assert.match(elements.loading.textContent, /Could not open your data/);
  context.coffeeApplyShell(true);
  assert.match(elements.loading.textContent, /Could not open your data/);
});
