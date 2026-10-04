import { DurableObject } from 'cloudflare:workers';
import { Session, random, hash, validProof } from './core.js';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, X-User-Email, Authorization'
};

const headers = {
  ...corsHeaders,
  'Cache-Control': 'no-store',
  'Referrer-Policy': 'no-referrer',
  'X-Content-Type-Options': 'nosniff',
  'Content-Security-Policy': "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"
};

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
      if (action === '/sync_save') return json(await s.saveSync(input.data));
      if (action === '/sync_get') return json({ data: await s.getSync() });
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
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers });
    }

    const url = new URL(request.url);
    if (url.origin !== env.PUBLIC_ORIGIN) return json({ error: 'wrong_origin' }, 400);
    if (url.pathname === '/health' && request.method === 'GET') return json({ status: env.GOOGLE_CLIENT_SECRET ? 'ready' : 'not_configured' });
    if (url.pathname === '/' && request.method === 'GET') return html('<p>Servicio de acceso de Coffee Dial. Iniciá sesión desde la app.</p>');
    if (url.pathname === '/api/sync') {
      const email = request.headers.get('X-User-Email') || url.searchParams.get('email');
      if (!email) return json({ error: 'missing_email' }, 400);

      const doId = env.SESSIONS.idFromName(`sync_${email.trim().toLowerCase()}`);
      const stub = env.SESSIONS.get(doId);

      if (request.method === 'POST') {
        const body = await request.text();
        if (body && body.length > 2) {
          try {
            await stub.fetch(new Request('https://session/sync_save', { method: 'POST', body: JSON.stringify({ data: body }) }));
          } catch (_) {}
        }
        const res = await stub.fetch(new Request('https://session/sync_get', { method: 'POST', body: '{}' }));
        const resData = await res.json();
        const stored = resData?.data || body;
        return new Response(stored, { status: 200, headers: { ...headers, 'Content-Type': 'application/json' } });
      }

      if (request.method === 'GET') {
        const res = await stub.fetch(new Request('https://session/sync_get', { method: 'POST', body: '{}' }));
        const resData = await res.json();
        const stored = resData?.data;
        const responseData = stored || JSON.stringify({
          format: "coffee-dial-backup",
          schemaVersion: 1,
          exportedAt: new Date().toISOString(),
          beans: [],
          shots: [],
          machines: []
        });
        return new Response(responseData, { status: 200, headers: { ...headers, 'Content-Type': 'application/json' } });
      }
    }

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

        const email = result.user?.email || '';
        const name = result.user?.displayName || email.split('@')[0] || '';
        const webOrigin = env.PUBLIC_ORIGIN.replace('auth-coffee', 'coffee');
        const webRedirectUrl = `${webOrigin}/#email=${email}&name=${encodeURIComponent(name)}`;
        const mobileLink = `coffeedial://auth/google?${params}`.replaceAll('&', '&amp;');

        return html(`
          <p>${result.ticket ? 'Acceso confirmado para ' + email + '.' : 'No se completó el acceso.'}</p>
          <p style="margin-top: 16px;"><a href="${webRedirectUrl}" style="font-size: 18px; font-weight: bold; color: #704a32;">Volver a Coffee Dial (Web)</a></p>
          <p style="margin-top: 8px;"><a href="${mobileLink}">Volver a la App (Android / iOS)</a></p>
          <script>
            setTimeout(function() {
              if (window.location.href.indexOf('platform=web') !== -1 || !navigator.userAgent.includes('Android')) {
                window.location.href = "${webRedirectUrl}";
              }
            }, 1000);
          </script>
        `, 200, { 'Set-Cookie': cookie(state, '', 0) });
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
