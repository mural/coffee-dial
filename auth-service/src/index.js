import { DurableObject } from 'cloudflare:workers';
import { Session, random, hash, validProof } from './core.js';

const headers = { 'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer',
  'X-Content-Type-Options': 'nosniff', 'Content-Security-Policy': "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'" };
const json = (value, status = 200) => new Response(JSON.stringify(value), { status, headers: { ...headers, 'Content-Type': 'application/json' } });
const html = (body, status = 200, extra = {}) => new Response(`<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Coffee Dial</title><body><h1>Coffee Dial</h1>${body}</body></html>`, { status, headers: { ...headers, 'Content-Type': 'text/html; charset=utf-8', ...extra } });
const cookieName = state => `__Host-coffee-${state}`;
const cookie = (state, value, maxAge) => `${cookieName(state)}=${value}; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=${maxAge}`;
const session = (env, id) => env.SESSIONS.get(env.SESSIONS.idFromName(id));
async function call(env, id, action, input) {
  const result = await session(env, id).fetch(new Request(`https://session/${action}`, { method: 'POST', body: JSON.stringify(input) }));
  if (!result.ok) throw new Error('invalid');
  return result.json();
}
async function boundedJson(request) {
  if (!(request.headers.get('Content-Type') || '').startsWith('application/json')) throw new Error('type');
  const reader = request.body?.getReader(); if (!reader) throw new Error('body');
  let size = 0, text = ''; const decoder = new TextDecoder();
  while (true) { const { done, value } = await reader.read(); if (done) break;
    size += value.length; if (size > 4096) { await reader.cancel(); throw new Error('size'); }
    text += decoder.decode(value, { stream: true }); }
  return JSON.parse(text + decoder.decode());
}
export class LoginAttempt extends DurableObject {
  async fetch(request) {
    try {
      const s = new Session(this.ctx.storage, this.env);
      const action = new URL(request.url).pathname;
      const input = await request.json();
      if (action === '/init') return json(await s.init(input));
      if (action === '/callback') return json(await s.callback(input));
      if (action === '/consume') return json(await s.consume(input));
      return json({ error: 'invalid' }, 400);
    } catch { return json({ error: 'invalid_or_expired' }, 400); }
  }
  async alarm() { await this.ctx.storage.deleteAll(); }
}
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.origin !== env.PUBLIC_ORIGIN) return json({ error: 'wrong_origin' }, 400);
    if (url.pathname === '/health' && request.method === 'GET') return json({ status: env.GOOGLE_CLIENT_SECRET ? 'ready' : 'not_configured' });
    if (url.pathname === '/' && request.method === 'GET') return html('<p>Servicio de acceso de Coffee Dial. Iniciá sesión desde la app.</p>');
    if (!env.GOOGLE_CLIENT_SECRET) return html('<p>El acceso web todavía no está configurado. Volvé a la app.</p>', 503);
    try {
      const ip = request.headers.get('CF-Connecting-IP') || 'local';
      if (!(await env.RATE_LIMITER.limit({ key: ip })).success) return json({ error: 'too_many_requests' }, 429);
      if (url.pathname === '/google/start' && request.method === 'GET') {
        const challenge = url.searchParams.get('challenge'), appState = url.searchParams.get('state');
        if (!validProof(challenge) || !validProof(appState)) return json({ error: 'invalid' }, 400);
        const state = random(), browser = random();
        const result = await call(env, state, 'init', { challenge, appState, browserHash: await hash(browser) });
        const google = new URL('https://accounts.google.com/o/oauth2/v2/auth');
        google.search = new URLSearchParams({ client_id: env.GOOGLE_CLIENT_ID, redirect_uri: `${env.PUBLIC_ORIGIN}/google/callback`,
          response_type: 'code', scope: 'openid email profile', state, nonce: result.nonce,
          code_challenge: result.googleChallenge, code_challenge_method: 'S256', prompt: 'select_account' }).toString();
        return new Response(null, { status: 302, headers: { ...headers, Location: google.toString(), 'Set-Cookie': cookie(state, browser, 300) } });
      }
      if (url.pathname === '/google/callback' && request.method === 'GET') {
        const state = url.searchParams.get('state');
        if (!validProof(state)) throw new Error('state');
        const browser = (request.headers.get('Cookie') || '').split(';').map(v => v.trim()).find(v => v.startsWith(`${cookieName(state)}=`))?.split('=')[1];
        if (!validProof(browser)) throw new Error('cookie');
        const result = await call(env, state, 'callback', { browserHash: await hash(browser), code: url.searchParams.get('code'), error: url.searchParams.get('error') });
        const params = new URLSearchParams({ state: result.appState, attempt: state });
        if (result.ticket) params.set('code', result.ticket); else params.set('error', result.error);
        // Fixed application destination. Only a short-lived ticket, never a Google token, leaves the server.
        const link = `coffeedial://auth/google?${params}`.replaceAll('&', '&amp;');
        return html(`<p>${result.ticket ? 'Acceso confirmado.' : 'No se completó el acceso.'}</p><p><a href="${link}">Volver a Coffee Dial</a></p><p>Si no se abre, volvé a la app e intentá otra vez.</p>`, 200, { 'Set-Cookie': cookie(state, '', 0) });
      }
      if (url.pathname === '/exchange' && request.method === 'POST') {
        const input = await boundedJson(request);
        if (!validProof(input.attempt)) throw new Error('attempt');
        return json(await call(env, input.attempt, 'consume', input));
      }
      return json({ error: 'not_found' }, 404);
    } catch { return html('<p>Este intento venció o no es válido. Volvé a Coffee Dial e iniciá sesión nuevamente.</p>', 400); }
  },
};
